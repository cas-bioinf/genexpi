package cz.cas.mbu.cygenexpi;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;

public class ProfileTags {
	public static final String NO_TAG = "";
	public static final String NO_CHANGE = "no change";
	public static final String CONSTANT_SYNTHESIS = "constant synthesis";
	
	public static final String EXCLUDED_NO_CHANGE_REGULATOR = "excluded (no change regulator)"; 
	public static final String EXCLUDED_NO_CHANGE_TARGET = "excluded (no change target)"; 
	public static final String EXCLUDED_CONSTANT_SYNTHESIS = "excluded (constant synth.)"; 
	public static final String NO_FIT = "no fit"; 
	public static final String GOOD_FIT = "good fit";
	public static final String BORDERLINE_FIT = "borderline fit"; 

	private static Map<String, Color> tagColors;
	
	static {
		tagColors = new HashMap<>();
		Color excludedColor = new Color(0xFFD0B0);
		tagColors.put(EXCLUDED_CONSTANT_SYNTHESIS, excludedColor);
		tagColors.put(EXCLUDED_NO_CHANGE_REGULATOR, excludedColor);
		tagColors.put(EXCLUDED_NO_CHANGE_TARGET, excludedColor);		
		tagColors.put(NO_CHANGE, excludedColor);
		
		tagColors.put(CONSTANT_SYNTHESIS, new Color(0xf8ffb0));
		
		tagColors.put(NO_FIT, Color.WHITE);
		tagColors.put(GOOD_FIT, new Color(0xC7E0BA));
		tagColors.put(BORDERLINE_FIT, new Color(0xeef6ea));
	}
	
	public static Color getBackgroundColorForTag(String tag)
	{
		return tagColors.get(tag);
	}
}
