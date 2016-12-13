package cz.cas.mbu.cygenexpi.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

import cz.cas.mbu.cygenexpi.RememberAllValues;
import cz.cas.mbu.cygenexpi.RememberValue;
import cz.cas.mbu.cygenexpi.RememberValueRecursive;
import cz.cas.mbu.cygenexpi.RememberValueService;

public class RememberValueServiceImpl implements RememberValueService {

	private final Preferences permanentStorage;
	private final Map<String, Object> sessionStorage;
	
	private final Logger logger = Logger.getLogger(RememberValueServiceImpl.class); 
		
	@FunctionalInterface
	protected interface FieldProcesser {
		void process(Object target, Field f) throws IllegalArgumentException, IllegalAccessException, BackingStoreException;
	}
	
	public RememberValueServiceImpl() {
		sessionStorage = new HashMap<>();
		permanentStorage = Preferences.userNodeForPackage(RememberValueService.class).node("RememberValueService");
	}
	
	
	protected String getSessionStorageIdentifier(Object obj, Field f)
	{
		return obj.getClass().getName() + "::" + f.getName();
	}
	
	protected String getPermanentStorageCategory(Object obj)
	{
		return obj.getClass().getName();
	}
	
	protected String getPermanentStorageKey(Field f)
	{
		return f.getName();
	}
	
	protected void processFields(Object obj, FieldProcesser sessionStorageProcessor, FieldProcesser permanentStorageProcessor)
	{
		RememberAllValues rememberAll = obj.getClass().getAnnotation(RememberAllValues.class);
		
		for(Field f : obj.getClass().getFields())
		{
			//Static fields are ignored
			if(Modifier.isStatic(f.getModifiers()))
			{
				if(f.isAnnotationPresent(RememberValue.class))
				{
					throw new IllegalStateException("RememberValue annotation can be used only on non-static fields: " + f);
				}
				continue;
			}
			try {
				RememberValue.Type processType = null;
				if(f.isAnnotationPresent(RememberValue.class))
				{
					if(!Modifier.isPublic(f.getModifiers()))
					{
						throw new IllegalStateException("RememberValue annotation can be used only on public fields: " + f);
					}
					processType = f.getAnnotation(RememberValue.class).type();
				}
				else if(rememberAll != null)
				{
					if(Modifier.isPublic(f.getModifiers()) && (!rememberAll.restrictToTunables() || f.isAnnotationPresent(Tunable.class) || f.isAnnotationPresent(ContainsTunables.class)))
					{
						processType = rememberAll.type();						
					}
				}
				
				boolean processField = processType != null && processType != RememberValue.Type.NEVER;
				
				if(processField)
				{	
					if(f.isAnnotationPresent(ContainsTunables.class) || f.isAnnotationPresent(RememberValueRecursive.class))
					{
						Object value = f.get(obj); 
						if(value != null)
						{
							processFields(value, sessionStorageProcessor, permanentStorageProcessor);
						}
					}
					else
					{
						if(processType == RememberValue.Type.SESSION)
						{
							sessionStorageProcessor.process(obj, f);
						}
						else if(processType == RememberValue.Type.PERMANENTLY)
						{
							permanentStorageProcessor.process(obj, f);
						}
						else if(processType != RememberValue.Type.NEVER) //NEVER means no processing performed
						{
							throw new IllegalStateException("Unrecognized storage type: " + processType.name());
						}
					}
				}
			} 
			catch(IllegalArgumentException | IllegalAccessException ex)
			{
				logger.error("Could not process field " + f.getName() + " on object class " + obj.getClass().getName());
			}
			catch(BackingStoreException ex)
			{
				logger.error("Could not use permanent storage", ex);
			}
		}		
	}
	
	protected void loadSessionProperty(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException
	{
		String storageId = getSessionStorageIdentifier(obj, f);
		if(sessionStorage.containsKey(storageId))
		{
			Object storedValue = sessionStorage.get(storageId);
			if(f.getType().isPrimitive() || f.getType().equals(String.class))
			{
				f.set(obj, storedValue);
			}
			else if(f.getType().equals(ListSingleSelection.class))
			{
				@SuppressWarnings("unchecked") //Using the most generic version (since I later assign only objects read from the getPossibleValues, it is safe)
				ListSingleSelection<Object> selection = (ListSingleSelection<Object>)f.get(obj); 
				if(selection != null)
				{
					Optional<Object> selectedValue = selection.getPossibleValues().stream()
							.filter(x -> x.equals(storedValue))
							.findAny();
					
					if(selectedValue.isPresent())
					{
						selection.setSelectedValue(selectedValue);
					}
				}
			}
			else
			{
				throw new IllegalStateException("Session storage supports only primitive types, strings and ListSingleSelection, not " + f.getType().getName());
			}
		}		
	}
	
	protected void loadPermanentProperty(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException, BackingStoreException
	{
		String categoryName = getPermanentStorageCategory(obj);
		if(!permanentStorage.nodeExists(categoryName))
		{
			return;
		}
		Preferences classPreferences = permanentStorage.node(categoryName);
		
				
		String storageId = getPermanentStorageKey(f);
		boolean keyPresent = Arrays.stream(classPreferences.keys()).anyMatch( x -> x.equals(storageId)); //just a way to code "array contains" 
		if(keyPresent)
		{
			if(f.getType().equals(boolean.class))
			{
				f.setBoolean(obj, classPreferences.getBoolean(storageId, f.getBoolean(obj)));
			}
			else if(f.getType().equals(float.class))
			{
				f.setFloat(obj, classPreferences.getFloat(storageId, f.getFloat(obj)));
			}
			else if(f.getType().equals(double.class))
			{
				f.setDouble(obj, classPreferences.getDouble(storageId, f.getDouble(obj)));
			}
			else if(f.getType().equals(int.class))
			{
				f.setInt(obj, classPreferences.getInt(storageId, f.getInt(obj)));
			}
			else if(f.getType().equals(long.class))
			{
				f.setLong(obj, classPreferences.getLong(storageId, f.getLong(obj)));
			}
			else if(f.getType().equals(String.class))
			{
				f.set(obj, classPreferences.get(storageId, null));
			}
			else if(f.getType().equals(ListSingleSelection.class))
			{
				String stringValue = classPreferences.get(storageId, null);

				@SuppressWarnings("unchecked") //Using the most generic version (since I later assign only objects read from the getPossibleValues, it is safe)
				ListSingleSelection<Object> listSelection = (ListSingleSelection<Object>)f.get(obj); 
				if(stringValue != null)
				{
					Optional<Object> selectedValue = listSelection.getPossibleValues().stream()
							.filter( x -> x.toString().equals(stringValue))
							.findAny();
					
					if(selectedValue.isPresent())
					{
						listSelection.setSelectedValue(selectedValue.get());
					}
				}
			}
			else
			{
				throw new IllegalStateException("Permanent storage supports only primitive types, strings and ListSingleSelection, not " + f.getType().getName());
			}
		}		
	}
		
	@Override
	public void loadProperties(Object obj) {		
		processFields(obj, this::loadSessionProperty, this::loadPermanentProperty);		
	}

	protected void saveSessionProperty(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException 
	{
		String storageId = getSessionStorageIdentifier(obj, f);
		if(f.getType().isPrimitive() || f.getType().equals(String.class))
		{
			sessionStorage.put(storageId, f.get(obj));
		}
		else if(f.getType().equals(ListSingleSelection.class))
		{
			ListSingleSelection<?> selection = (ListSingleSelection<?>)f.get(obj); 
			if(selection != null)
			{
				sessionStorage.put(storageId, selection.getSelectedValue());
			}
		}
		else
		{
			throw new IllegalStateException("Session storage supports only primitive types, strings and ListSingleSelection, not " + f.getType().getName());
		}		
	}
	
	protected void savePermanentProperty(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException 
	{
		Preferences classPreferences = permanentStorage.node(getPermanentStorageCategory(obj));							

		String storageId = getPermanentStorageKey(f);
		if(f.getType().equals(boolean.class))
		{
			classPreferences.putBoolean(storageId, f.getBoolean(obj));
		}
		else if(f.getType().equals(float.class))
		{
			classPreferences.putFloat(storageId, f.getFloat(obj));
		}
		else if(f.getType().equals(double.class))
		{
			classPreferences.putDouble(storageId, f.getDouble(obj));
		}
		else if(f.getType().equals(int.class))
		{
			classPreferences.putInt(storageId, f.getInt(obj));
		}
		else if(f.getType().equals(long.class))
		{
			classPreferences.putLong(storageId, f.getLong(obj));
		}
		else if(f.getType().equals(String.class))
		{
			classPreferences.put(storageId, (String)f.get(obj));
		}
		else if(f.getType().equals(ListSingleSelection.class))
		{
			@SuppressWarnings("unchecked") //Using the most generic version 
			ListSingleSelection<Object> listSelection = (ListSingleSelection<Object>)f.get(obj); 
			
			Object selectedValue = listSelection.getSelectedValue();
			if(selectedValue != null)
			{
				classPreferences.put(storageId, listSelection.getSelectedValue().toString());
			}
		}
		else
		{
			throw new IllegalStateException("Permanent storage supports only primitive types, strings and ListSingleSelection, not " + f.getType().getName());
		}		
	}
	
	@Override
	public void saveProperties(Object obj)	
	{
		processFields(obj, this::saveSessionProperty, this::savePermanentProperty);
		try	
		{
			permanentStorage.sync();
		}
		catch (BackingStoreException ex)
		{
			logger.error("Could not use permanent storage", ex);			
		}
	}
}
