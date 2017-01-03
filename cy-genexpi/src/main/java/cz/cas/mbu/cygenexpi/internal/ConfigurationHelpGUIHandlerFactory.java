package cz.cas.mbu.cygenexpi.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.GUITunableHandlerFactory;

public class ConfigurationHelpGUIHandlerFactory implements GUITunableHandlerFactory<ConfigurationHelpGUIHandler> {

		@Override
		public ConfigurationHelpGUIHandler createTunableHandler(Field field, Object instance, Tunable t) {
			if (!ConfigurationHelp.class.isAssignableFrom(field.getType()))
				return null;

			return new ConfigurationHelpGUIHandler(field, instance, t);
		}

		@Override
		public ConfigurationHelpGUIHandler createTunableHandler(Method getter, Method setter, Object instance, Tunable tunable) {
			if (!ConfigurationHelp.class.isAssignableFrom(getter.getReturnType()))
				return null;

			return new ConfigurationHelpGUIHandler(getter, setter, instance, tunable);
		}

}
