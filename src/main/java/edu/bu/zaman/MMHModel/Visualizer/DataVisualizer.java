package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;

import javafx.util.Pair;
import net.miginfocom.swing.MigLayout;

public class DataVisualizer extends JFrame 
	implements PropertyKeyConfigurationChangedListener
{
	private enum DataType
	{
		NUMERIC,
		BOOLEAN,
		STRING
	};
	
	/**
	 * Serial version UID used for object serialization
	 */
	private static final long serialVersionUID = 1109397701001808148L;

	private JSONObject m_modelData;
	private XYChart m_xyChart;
	
	private ArrayList<String> m_propertyKeys = new ArrayList<>();
	private HashMap<String, ArrayList<String>> m_componentProperties = new HashMap<>();
	private DefaultListModel<String> m_chartKeysModel = new DefaultListModel<>();
	
	private XChartPanel<XYChart> m_chartPanel;
	private JPanel m_optionsPanel;
	private JPanel m_configurationPanel;
	
	public DataVisualizer(Path dataPath)
	{
		loadModelData(dataPath);
		initializeViews();
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	
	/**
	 * Initializes the view components.
	 */
	private void initializeViews()
	{
		// Initialize components
		newChart();
		
		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new MigLayout());
		
		// Create property keys list
		final JListArrayListAdapter<String> propertyKeysAdapter = new JListArrayListAdapter<>(m_propertyKeys);
		
		JList<String> propertyKeysList = new JList<>(propertyKeysAdapter);
		JScrollPane propertyOptionsScroll = new JScrollPane(propertyKeysList);
				
		propertyKeysList.setLayoutOrientation(JList.VERTICAL);
		propertyKeysList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		propertyKeysList.addListSelectionListener(new ListSelectionListener() 
		{		
			@Override
			public void valueChanged(ListSelectionEvent e) 
			{
				// Make sure the selection has completed
				if (!e.getValueIsAdjusting())
				{
					String selectedKey = propertyKeysList.getSelectedValue();
					m_chartKeysModel.addElement(selectedKey);
				}				
			}
		});
		
		m_optionsPanel.add(propertyOptionsScroll, "span");
		
		// Create chart keys list
		JList<String> chartKeysList = new JList<>(m_chartKeysModel);
		JScrollPane chartKeysScroll = new JScrollPane(chartKeysList);
		
		chartKeysList.setLayoutOrientation(JList.VERTICAL);
		chartKeysList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chartKeysList.addListSelectionListener(new ListSelectionListener() 
		{		
			@Override
			public void valueChanged(ListSelectionEvent e) 
			{
				if (!e.getValueIsAdjusting())
				{
					String selectedKey = chartKeysList.getSelectedValue();
					if (selectedKey == null)
					{
						return;
					}
					
					HashMap<String, ArrayList<String>> componentProperties = new HashMap<>();
					String[] components = selectedKey.split("\\.");
					
					String globalKey = "";
					for (String component : components)
					{
						globalKey += component;
						if (component.contains(Visualizer.ARRAY_DELIMITER))
						{
							componentProperties.put(component, m_componentProperties.get(globalKey));
						}
						
						globalKey += ".";
					}
					
					PropertyKeyConfigurationPanel configurationPanel = PropertyKeyConfigurationManager.getConfigurationPanel(selectedKey, componentProperties);
					configurationPanel.addPropertyKeyConfigurationChangedListener(DataVisualizer.this);
					
					m_configurationPanel.removeAll(); // Remove existing panel
					m_configurationPanel.add(configurationPanel, BorderLayout.CENTER);
					
					m_configurationPanel.revalidate();
					m_configurationPanel.repaint();
				}
			}
		});
		
		m_optionsPanel.add(chartKeysScroll, "grow, span");
		
		// Configure configuration panel
		m_configurationPanel = new JPanel(new BorderLayout());
		m_optionsPanel.add(m_configurationPanel, "grow, span");
		
		// Create buttons		
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{ 
				clearChart();
			}
		});
		
		m_optionsPanel.add(clearButton);
		
		// Set up frame
		add(m_optionsPanel, BorderLayout.LINE_END);
	}
	
	/**
	 * Loads the model data as a JSON object and parses the data for property keys
	 * that can be used to generate plots.
	 * 
	 * @param dataPath	the file path for the model data
	 * @return whether the model data was successfully loaded
	 */
	private boolean loadModelData(Path dataPath)
	{
		// Check to make sure the data path exists
		if (!Files.exists(dataPath))
		{
			return false;
		}
		
		try
		{
			String jsonText = new String(Files.readAllBytes(dataPath));
			m_modelData = new JSONObject(jsonText);
			
			HashSet<String> propertyKeys = new HashSet<String>();			
			parseJSONData(m_modelData, null, propertyKeys);
			
			m_propertyKeys = new ArrayList<>(propertyKeys);
			m_componentProperties = new HashMap<>();
			
			for (String key : m_propertyKeys)
			{
				String[] components = key.split("\\.");
				if (components.length <= 0)
				{
					continue;
				}
				
				String globalKey = "";
				for (String component : components)
				{
					globalKey += component;					
					if (!m_componentProperties.containsKey(globalKey) && component.contains(Visualizer.ARRAY_DELIMITER))
					{					
						m_componentProperties.put(globalKey, getComponentProperties(globalKey + ".", m_propertyKeys));
					}
					
					globalKey += ".";
				}				
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Helper function that returns a list of directly accessible properties and subproperties for 
	 * a particular property key path from a list of options. Any children to the key path that are 
	 * arrays are ignored.
	 * 
	 * @param key		the property key path
	 * @param allKeys	a list of all key paths to filter from
	 * @return 			a list of key paths that represent child properties of `key`
	 */
	private ArrayList<String> getComponentProperties(String key, ArrayList<String> allKeys)
	{
		ArrayList<String> properties = new ArrayList<>();
		for (String propertyKey : allKeys)
		{
			if (propertyKey.startsWith(key))
			{
				String propertyChain = propertyKey.substring(key.length());
				if (!propertyChain.contains(Visualizer.ARRAY_DELIMITER))
				{
					properties.add(propertyChain);
				}
			}
		}
		
		return properties;
	}
	
	/**
	 * Recursively parses JSON data and populates a set of unique property keys available at each 
	 * leaf of the JSON object tree. This function assumes that arrays of arrays do not exist and 
	 * that all JSONArray objects are values to named properties.
	 * 
	 * @param data			the data to parse
	 * @param key			the property key for the current data object, or null for the root object
	 * @param propertyKeys	set that stores the unique property keys found within the data object
	 */
	private static void parseJSONData(JSONObject data, String key, HashSet<String> propertyKeys)
	{
		Set<String> keys = data.keySet();
		for (String dataKey : keys)
		{
			Object value = data.get(dataKey);
			if (value instanceof JSONObject)
			{
				String accumulatedKey = (key == null) ? dataKey : key + "." + dataKey;
				parseJSONData((JSONObject)value, accumulatedKey, propertyKeys);
			}
			else if (value instanceof JSONArray)
			{
				// Add # to the accumulated key to signify that subsequent properties are
				// found within array elements
				String accumulatedKey = (key == null) ? Visualizer.ARRAY_DELIMITER + dataKey : key + "." + Visualizer.ARRAY_DELIMITER + dataKey;
				
				JSONArray array = (JSONArray)value;
				for (int index = 0; index < array.length(); index++)
				{
					Object arrayValue = array.get(index);
					if (arrayValue instanceof JSONObject)
					{
						parseJSONData((JSONObject)arrayValue, accumulatedKey, propertyKeys);
					}
				}
			}
			else
			{
				String accumulatedKey = (key == null) ? dataKey : key + "." + dataKey;
				propertyKeys.add(accumulatedKey);
			}
		}
	}	
	
	/**
	 * Removes any existing charts in the charting panel and adds a new clear
	 * one.
	 */
	private void newChart()
	{
		if (m_chartPanel != null)
		{
			remove(m_chartPanel);
		}
		
		m_xyChart = new XYChartBuilder()
			.width(600)
			.height(400)
			.title("Test Chart")
			.xAxisTitle("X Axis")
			.yAxisTitle("Y Axis")
			.build();
		
		m_xyChart.getStyler().setLegendPosition(LegendPosition.InsideN);
		
		m_chartPanel = new XChartPanel<XYChart>(m_xyChart);		
		
		add(m_chartPanel, BorderLayout.CENTER);
		
		m_chartPanel.revalidate();
		m_chartPanel.repaint();
	}
	
	/**
	 * Clears the current chart and associated property keys from the options panel.
	 */
	private void clearChart()
	{
		m_chartKeysModel.clear();
		m_configurationPanel.removeAll();
		
		newChart();
	}
	
	/**
	 * Extracts property data into an array of data points represented as Pair<Integer, String>
	 * objects, where the integer values represents an x-axis property, and the string value
	 * represents an associated value. An empty data set is returned if a problem is encountered.
	 * 
	 * @param key	the property key from which model data should be extracted
	 * @return array of Pair<Integer, String> objects that represent XY data points for plotting
	 */
	private ArrayList<Pair<Integer, String>> getChartData(PropertyKeyConfigurationPanel panel)
	{
		
		ArrayList<Pair<Integer, String>> xyData = new ArrayList<>();
		
		String key = panel.getPropertyKey();		
		String[] keyComponents = key.split("\\.");
		
		if (keyComponents.length > 0)
		{	
			JSONObject data = m_modelData;
			Map<String, ArrayPropertyConditionSet> conditionSets = panel.getConditionSets();
						
			String loopKey = null;
			int pathIndex = 0;
			while (loopKey == null && pathIndex < keyComponents.length)
			{
				String pathComponent = keyComponents[pathIndex];
				if (!pathComponent.contains(Visualizer.ARRAY_DELIMITER))
				{
					// Object at the current path component is assumed to be a JSONObject as we know
					// that it is not an array based on the if-statement above, and only the final
					// component of the key path should represent a property that is a leaf in the data
					// object tree (i.e. has a scalar value associated with it)
					data = data.getJSONObject(pathComponent);
					if (data == null)
					{
						// An error has occurred if the path component does not return a JSONObject
						return xyData;
					}
				}
				else
				{
					// Check to see if the array associated with the current path component has been
					// marked as an x-axis property
					if (!conditionSets.containsKey(pathComponent))
					{
						// An error has occurred if the array path component is not in the conditionSets map
						return xyData;
					}
					
					ArrayPropertyConditionSet conditionSet = conditionSets.get(pathComponent);
					if (conditionSet.isXAxisProperty())
					{
						// Set the loop key to the current path component and allow the current while loop iteration
						// to complete so that pathIndex is incremented and points to the next path component after
						// this x-axis property component
						loopKey = pathComponent;						
					}
					else
					{
						// Loop through the current array to find the first element that matches the conditions
						// associated with this array component
						
						JSONArray array = data.getJSONArray(pathComponent);
						if (array == null)
						{
							// An error has occurred if a JSONArray is not associated with this path component
							return xyData;
						}
						
						data = arrayElementSatisfyingConditions(array, conditionSet);
						if (data == null)
						{
							// Return an empty data set of an array element matching the condition set was not found
							return xyData;
						}
					}
				}
				
				pathIndex++;
			}
			
			if (loopKey == null)
			{
				// An error has occurred if a loop key was not found
				return xyData;
			}
			
			JSONArray xAxisArray = data.getJSONArray(loopKey.substring(1)); // Remove array delimeter from beginning of key
			for (int index = 0; index < xAxisArray.length(); index++)
			{
				// Attempt to navigate through the array element using the property key to isolate a scalar value
				JSONObject elementData = xAxisArray.getJSONObject(index);
				if (elementData == null)
				{
					// Error has occurred if this element is null
					return xyData;
				}
				
				for (int propertyIndex = pathIndex; propertyIndex < keyComponents.length; propertyIndex++)
				{
					String pathComponent = keyComponents[propertyIndex];
					if (pathComponent.contains(Visualizer.ARRAY_DELIMITER))
					{
						// This means the next component in the path is an array, so we need to find an element in the array that
						// matches the array component's associated conditions set
						JSONArray array = elementData.getJSONArray(pathComponent.substring(1));
						ArrayPropertyConditionSet conditionSet = conditionSets.get(pathComponent);
						if (conditionSet == null)
						{
							// Error has occurred if a condition set could not be found
							return xyData;
						}
						
						elementData = arrayElementSatisfyingConditions(array, conditionSet);
					}
					else
					{
						Object tempData = elementData.get(pathComponent);
						if (tempData instanceof JSONObject)
						{
							elementData = ((JSONObject)tempData);
						}
						else
						{
							xyData.add(new Pair<Integer, String>(index, tempData.toString()));
						}
					}
					
					if (elementData == null)
					{
						// A matching array element could not be found, so this index of the x-axis array
						// should be skipped
						break;
					}
				}
			}				
		}
		
		return xyData;
	}
	
	/**
	 * Searches a JSONArray for an array element that matches a condition set.
	 * 
	 * @param array			the array to search
	 * @param conditionSet	the condition set used to match an element
	 * 
	 * @return the first array element that matches the condition set, or null if one was not found
	 */
	private JSONObject arrayElementSatisfyingConditions(JSONArray array, ArrayPropertyConditionSet conditionSet)
	{
		for (int index = 0; index < array.length(); index++)
		{
			JSONObject arrayElement = array.getJSONObject(index);
			if (arrayElement == null)
			{
				// An error has occurred if the array element is not a JSONObject
				return null;
			}
			
			if (conditionSet.isSatisfied(arrayElement))
			{
				return arrayElement;
			}
		}
		
		return null;
	}
	
	/**
	 * Plots a set of data using an appropriate chart based on the data type of the data set.
	 * If the supplied data is numerical, the data is plotted as an XY chart.
	 * 
	 * @param data			the data to plot
	 * @param seriesName	the name of the series being plotted
	 */
	private void plotData(ArrayList<Pair<Integer, String>> data, String seriesName)
	{
		if (data.size() <= 0)
		{
			m_xyChart.removeSeries(seriesName);
			m_chartPanel.revalidate();
			m_chartPanel.repaint();
			
			return;
		}
		
		Pair<Integer, String> dataPoint = data.get(0);
		String value = dataPoint.getValue();
		
		DataType type;
		
		try
		{
			Double.parseDouble(value);
			type = DataType.NUMERIC;
		} 
		catch (NumberFormatException nfe)
		{
			if (value.equals("true") || value.equals("false"))
			{
				type = DataType.BOOLEAN;
			}
			else
			{
				type = DataType.STRING;
			}
		}
		
		switch (type)
		{
		
		case NUMERIC:
			double[] xDoubleData = new double[data.size()];
			double[] yDoubleData = new double[data.size()];
			
			for (int index = 0; index < data.size(); index++)
			{
				dataPoint = data.get(index);
				
				xDoubleData[index] = (double)dataPoint.getKey();
				yDoubleData[index] = Double.parseDouble(dataPoint.getValue());
				
				System.out.println("Data point: (" + xDoubleData[index] + ", " + yDoubleData[index] + ")");
			}
			
			plotXYData(xDoubleData, yDoubleData, seriesName);
			return;
			
		case BOOLEAN:
			double[] xBooleanData = new double[data.size()];
			double[] yBooleanData = new double[data.size()];
			
			for (int index = 0; index < data.size(); index++)
			{
				dataPoint = data.get(index);
				
				xBooleanData[index] = (double)dataPoint.getKey();
				yBooleanData[index] = (Boolean.parseBoolean(dataPoint.getValue())) ? 1 : 0;
				
				System.out.println("Data point: (" + xBooleanData[index] + ", " + yBooleanData[index] + ")");
			}
			
			plotXYData(xBooleanData, yBooleanData, seriesName);
			return;
			
		case STRING:
			
		}
	}
	
	/**
	 * Plots XY data points on the current chart using the specified series name. Note that
	 * the data series name must be unique. If it is not, an exception will be thrown by
	 * the chart.
	 * 
	 * @param xyData		array of XY data to plot
	 * @param seriesName	a unique name to use for the data series
	 */
	private void plotXYData(double[] xData, double[] yData, String seriesName)
	{
		// Make sure the chart object has been instantiated
		if (m_xyChart == null)
		{
			return;
		}			
		
		try 
		{		
			m_xyChart.addSeries(seriesName, xData, yData);
		} 
		catch (IllegalArgumentException iae)
		{
			// This is thrown if the data series already existst so we should updated it instead
			m_xyChart.updateXYSeries(seriesName, xData, yData, null);
		}
		
		m_chartPanel.revalidate();
		m_chartPanel.repaint();
	}	
	
	@Override
	public void configurationChanged(PropertyKeyConfigurationPanel panel)
	{
		// Plot the data series if the panel has a valid configuration (i.e. each array component
		// in the property key path has a set of conditions or is marked as an x-axis property
		if (panel.hasValidConfiguration())
		{
			plotData(getChartData(panel), panel.getPropertyKey());
		}
	}
	
	/**
	 * Adapter class that enables the use of an ArrayList<> to populate a JList.
	 * 
	 * @author Darash Desai
	 *
	 * @param <T>
	 */
	private class JListArrayListAdapter<T> extends AbstractListModel<T>
	{	
		private static final long serialVersionUID = 1L;
		
		private ArrayList<T> m_data;
		//private ArrayList<T> m_filteredData;
		
		public JListArrayListAdapter(ArrayList<T> data)
		{
			m_data = data;
			//m_filteredData = m_data;
		}
		
		public int getSize()
		{
			return m_data.size();
			//return m_filteredData.size();
		}
		
		public T getElementAt(int index)
		{
			return m_data.get(index);
			//return m_filteredData.get(index);
		}

		/*
		public void setFilter(String filter)
		{
			if (filter == null)
			{
				m_filteredData = m_data;
			}
			else
			{
				m_filteredData = new ArrayList<>();
				for (T key : m_data)
				{
					if (key.toString().startsWith(filter))
					{
						m_filteredData.add(key);
					}
				}
				
				// Notify list that all of the list contents have changed
				fireContentsChanged(this, 0, m_filteredData.size());
			}
		}
		*/
	}
}
