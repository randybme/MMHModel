package edu.bu.zaman.ModelVisualizer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Singleton class that serves as a manager that creates and provides configuration 
 * panels for property keys of the MMHModel data.
 * 
 * @author Darash Desai
 *
 */
public class PropertyKeyConfigurationManager 
{
	/**
	 * Provides a configuration panel for a property key.
	 * 
	 * @param key	the key for which a panel should be provided
	 * @return the configuration panel
	 */
	public static PropertyKeyConfigurationPanel getConfigurationPanel(String key, HashMap<String, ArrayList<String>> componentProperties)
	{
		return new PropertyKeyConfigurationPanel(key, componentProperties);
	}
}
