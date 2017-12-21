package edu.bu.zaman.MMHModel.Visualizer;

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
	private static HashMap<String, PropertyKeyConfigurationPanel> m_configurationPanels = new HashMap<>();
	
	/**
	 * Provides a configuration panel for a property key.
	 * 
	 * @param key	the key for which a panel should be provided
	 * @return the configuration panel
	 */
	public static PropertyKeyConfigurationPanel getConfigurationPanel(String key, HashMap<String, ArrayList<String>> componentProperties)
	{
		// Return a cached panel if it already exists
		if (m_configurationPanels.containsKey(key))
		{
			return m_configurationPanels.get(key);
		}
		
		// Create new panel and cache it
		PropertyKeyConfigurationPanel panel = new PropertyKeyConfigurationPanel(key, componentProperties); 
		m_configurationPanels.put(key, panel);
		
		return panel; 
	}
	
	/**
	 * Clears the current set of configuration panels.
	 */
	public static void clear()
	{
		m_configurationPanels.clear();
	}
}
