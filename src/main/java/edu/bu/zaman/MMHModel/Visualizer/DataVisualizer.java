package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.json.JSONArray;
import org.json.JSONObject;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.series.Series;
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
	private HashMap<String, Series> m_chartSeries = new HashMap<>();
	
	private ArrayList<String> m_propertyKeys = new ArrayList<>();
	private HashMap<String, ArrayList<String>> m_componentProperties = new HashMap<>();
	private DefaultListModel<String> m_chartKeysModel = new DefaultListModel<>();
	
	private JPanel m_chartContainerPanel;
	private JComboBox m_chartTypes;
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
		setLayout(new MigLayout());
		
		// Set up the chart panel
		m_chartContainerPanel = new JPanel(new MigLayout("fill"));
		m_chartContainerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JLabel chartTypeLabel = new JLabel("Chart type:");
		m_chartContainerPanel.add(chartTypeLabel);
		
		m_chartTypes = new JComboBox<>();

		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() 
		{			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				System.out.println("exporting data");
			}
		});
		
		m_chartContainerPanel.add(m_chartTypes, "pushx, grow");
		m_chartContainerPanel.add(exportButton, "wrap");
		
		newChart();
		
		// Set up the options panel
		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new MigLayout("fill"));
		
		// Create property keys tree
		
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
		for (String keyPath : m_propertyKeys)
		{
			populateTreeWithPropertyKey(rootNode, keyPath);
		}
		
		sortTree(rootNode);
		
		JTree propertyKeysTree = new JTree(rootNode);
		propertyKeysTree.expandRow(0);
		propertyKeysTree.setRootVisible(false);
		propertyKeysTree.setSelectionModel(new DefaultTreeSelectionModel()
		{
			private static final long serialVersionUID = 3366693914310519223L;

			/**
			 * Toggles the expansion of a tree path. This method does nothing if the path
			 * points to a tree leaf.
			 * 
			 * @param path	the path to toggle
			 */
			private void toggleNode(TreePath path)
			{
				Object lastComponent = path.getLastPathComponent(); 
				if (lastComponent instanceof DefaultMutableTreeNode)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)lastComponent;
					if (node.isLeaf()) 
					{
						return;
					}
					
					if (propertyKeysTree.isExpanded(path))
					{
						propertyKeysTree.collapsePath(path);
					}
					else
					{
						propertyKeysTree.expandPath(path);
					}
				}
			}
			
			/**
			 * Filters the tree paths array for paths that only lead to leaves. This method
			 * also optionally toggles the expansion of tree nodes.
			 * 
			 * @param paths			the paths to filter
			 * @param toggleNodes	whether expansion of nodes within the paths array should be toggled 
			 * 
			 * @return an array of TreePaths that only represent leaves
			 */
			private TreePath[] getLeafPaths(TreePath[] paths, boolean toggleNodes)
			{
				ArrayList<TreePath> leafPaths = new ArrayList<>();
				for (TreePath path : paths)
				{
					Object lastComponent = path.getLastPathComponent();
					if (lastComponent != null && 
							lastComponent instanceof DefaultMutableTreeNode &&
								((DefaultMutableTreeNode)lastComponent).isLeaf())
					{
						leafPaths.add(path);
					}
					else if (toggleNodes)
					{
						toggleNode(path);
					}
				}
				
				return leafPaths.toArray(new TreePath[leafPaths.size()]);
			}
			
			@Override
			public void setSelectionPaths(TreePath[] paths)
			{
			    super.setSelectionPaths(getLeafPaths(paths, true));
			}

			@Override
			public void addSelectionPaths(TreePath[] paths)
			{
			    super.addSelectionPaths(getLeafPaths(paths, true));
			}
		});
		propertyKeysTree.addTreeSelectionListener(new TreeSelectionListener() 
		{
			@Override
			public void valueChanged(TreeSelectionEvent e) 
			{
				TreePath selectedPath = e.getPath();
				
				Object lastPathComponent = selectedPath.getLastPathComponent();
				if (lastPathComponent != null && lastPathComponent instanceof DefaultMutableTreeNode)
				{
					ArrayList<Object> pathComponents = new ArrayList<>(Arrays.asList(selectedPath.getPath()));				
					pathComponents.remove(0); // Remove the empty root node from the path					
					String path = String.join(".", pathComponents.stream().map(object -> object.toString()).collect(Collectors.toList()));
					
					// Only add the path if it hasn't already been added
					if (!m_chartKeysModel.contains(path))
					{
						m_chartKeysModel.addElement(path);
					}
				}
			}
		});
		
		JScrollPane propertyOptionsScroll = new JScrollPane(propertyKeysTree);
		
		JLabel modelPropertiesLabel = new JLabel("Model Data Properties:");
		modelPropertiesLabel.setFont(Visualizer.labelFont);
		modelPropertiesLabel.setForeground(Visualizer.labelColor);
		
		m_optionsPanel.add(modelPropertiesLabel, "wrap");
		m_optionsPanel.add(propertyOptionsScroll, "grow, span, hmax 200");
		
		// Create chart keys list
		JPanel chartKeysPanel = new JPanel(new MigLayout("fill, ins 0"));
		JList<String> chartKeysList = new JList<>(m_chartKeysModel);
		JScrollPane chartKeysScroll = new JScrollPane(chartKeysList);

		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{ 
				String selectedProperty = chartKeysList.getSelectedValue();
				if (selectedProperty == null)
				{
					return;
				}
				
				int index = chartKeysList.getSelectedIndex();
				m_chartKeysModel.remove(index);
				
				Component configPanel = m_configurationPanel.getComponent(0);
				if (configPanel != null && configPanel instanceof PropertyKeyConfigurationPanel)
				{
					String seriesName = ((PropertyKeyConfigurationPanel)configPanel).getSeriesName();
					m_xyChart.removeSeries(seriesName);
					
					m_chartPanel.revalidate();
					m_chartPanel.repaint();
				}
				
				m_configurationPanel.removeAll();
				m_configurationPanel.revalidate();
				m_configurationPanel.repaint();				
			}
		});				
		
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{ 
				propertyKeysTree.clearSelection();
				clearChart();
			}
		});		
				
		chartKeysPanel.add(chartKeysScroll, "grow, span, hmax 70, wrap");
		chartKeysPanel.add(removeButton, "split 2, align right");
		chartKeysPanel.add(clearButton, "wrap");
		
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
		
		JLabel chartPropertiesLabel = new JLabel("Chart Properties:");
		chartPropertiesLabel.setFont(Visualizer.labelFont);
		chartPropertiesLabel.setForeground(Visualizer.labelColor);
		
		m_optionsPanel.add(chartPropertiesLabel, "wrap, gaptop 15");
		m_optionsPanel.add(chartKeysPanel, "grow, span");
		
		// Configure configuration panel
		m_configurationPanel = new JPanel(new BorderLayout());
		m_optionsPanel.add(m_configurationPanel, "grow, span, pushy, hmin 300");
				
		// Set up frame	
		add(m_chartContainerPanel, "west");
		add(m_optionsPanel, "east, w 650, wmax 650");
	}
	
	/**
	 * Populates a tree node with child nodes and leafs described by a key path provided in
	 * dot notation (i.e. root.childnode.childleaf).
	 * 
	 * @param root		the root node to populate
	 * @param keyPath	the key path describing what children must be added to the root node
	 */
	private void populateTreeWithPropertyKey(DefaultMutableTreeNode root, String keyPath)
	{
		String[] pathComponents = keyPath.split("\\.");
		for (String component : pathComponents)
		{
			@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> children = root.children();
			
			boolean foundNode = false;
			DefaultMutableTreeNode node;
			
			while (children.hasMoreElements())
			{
				node = children.nextElement();
				if (node.getUserObject().toString().equals(component))
				{
					root = node;
					foundNode = true;
					break;
				}
			}
			
			if (!foundNode)
			{
				node = new DefaultMutableTreeNode(component);
				root.add(node);
				root = node;
			}
		}
	}
	
	private void sortTree(DefaultMutableTreeNode root)
	{
		if (root == null)
		{
			return;
		}
		
		ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
		ArrayList<DefaultMutableTreeNode> leaves = new ArrayList<>();
		
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> children = root.children();
		while (children.hasMoreElements())
		{
			DefaultMutableTreeNode node = children.nextElement();
			sortTree(node); // Recursively sort node
			
			if (node.isLeaf())
			{
				leaves.add(node);
			}
			else
			{
				nodes.add(node);
			}
		}
		
		Comparator<DefaultMutableTreeNode> comparator = (node1, node2) -> 
				 node1.getUserObject().toString().compareToIgnoreCase(
						node2.getUserObject().toString());
				 
		Collections.sort(nodes, comparator);
		Collections.sort(leaves, comparator);
		
		root.removeAllChildren();
		nodes.forEach(root::add);
		leaves.forEach(root::add);
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
			m_chartContainerPanel.remove(m_chartPanel);
		}
		
		m_xyChart = new XYChartBuilder()
			.title("Test Chart")
			.xAxisTitle("X Axis")
			.yAxisTitle("Y Axis")
			.build();
		
		m_xyChart.getStyler().setLegendPosition(LegendPosition.InsideN);
		
		m_chartPanel = new XChartPanel<XYChart>(m_xyChart);		
		
		m_chartContainerPanel.add(m_chartPanel, "grow, push, south");
		
		m_chartPanel.revalidate();
		m_chartPanel.repaint();
	}
	
	/**
	 * Clears the current chart and associated property keys from the options panel.
	 */
	private void clearChart()
	{
		m_chartKeysModel.clear();
		m_chartSeries.clear();
		
		m_configurationPanel.removeAll();
		m_configurationPanel.revalidate();
		m_configurationPanel.repaint();
		
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
						
						JSONArray array = data.getJSONArray(pathComponent.substring(1));
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
	 * @param seriesName	the series name to use for the data
	 */
	private Series plotData(ArrayList<Pair<Integer, String>> data, String seriesName)
	{
		if (data.size() <= 0)
		{
			m_xyChart.removeSeries(seriesName);
			m_chartPanel.revalidate();
			m_chartPanel.repaint();
			
			return null;
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
			
			return plotXYData(xDoubleData, yDoubleData, seriesName);
			
			
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
			
			return plotXYData(xBooleanData, yBooleanData, seriesName);
			
			
		case STRING:
			return null;
			
		default:
			return null;
			
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
	private XYSeries plotXYData(double[] xData, double[] yData, String seriesName)
	{
		// Make sure the chart object has been instantiated
		if (m_xyChart == null)
		{
			return null;
		}			
		
		XYSeries series;
		try 
		{		
			series = m_xyChart.addSeries(seriesName, xData, yData);
		} 
		catch (IllegalArgumentException iae)
		{
			// This is thrown if the data series already existst so we should updated it instead
			series = m_xyChart.updateXYSeries(seriesName, xData, yData, null);
		}
		
		m_chartPanel.revalidate();
		m_chartPanel.repaint();
		
		return series;
	}	
	
	@Override
	public void configurationChanged(PropertyKeyConfigurationPanel panel)
	{
		// Plot the data series if the panel has a valid configuration (i.e. each array component
		// in the property key path has a set of conditions or is marked as an x-axis property
		if (panel.hasValidConfiguration())
		{
			String propertyKey = panel.getPropertyKey();
			Series series = plotData(getChartData(panel), panel.getSeriesName());
			
			if (series != null)
			{
				m_chartSeries.put(propertyKey, series);
			}
		}
		else
		{
			m_chartSeries.remove(panel.getPropertyKey());
			m_xyChart.removeSeries(panel.getSeriesName());
			
			m_chartPanel.revalidate();
			m_chartPanel.repaint();
		}
	}
	
	@Override
	public void seriesNameChanged(PropertyKeyConfigurationPanel panel, String newName) 
	{
		String propertyKey = panel.getPropertyKey();
		Series currentSeries = m_chartSeries.get(propertyKey);
		
		if (currentSeries == null)
		{
			// Series has not yet been added to plot the data so check to see if it has a valid
			// configuration and then add it
			if (panel.hasValidConfiguration())
			{
				Series series = plotData(getChartData(panel), panel.getSeriesName());
				if (series != null)
				{
					m_chartSeries.put(propertyKey, series);
				}
			}
		}
		else
		{		
			String currentSeriesName = currentSeries.getName();
			if (currentSeriesName.equals(newName))
			{
				return;
			}
			
			// Update the series only if the panel still has a valid configuration
			if (panel.hasValidConfiguration())
			{
				if (currentSeries instanceof XYSeries)
				{
					XYSeries xySeries = (XYSeries)currentSeries;
					
					double xData[] = xySeries.getXData();
					double yData[] = xySeries.getYData();
					
					Series series = m_xyChart.addSeries(newName, xData, yData);
					if (series != null)
					{
						m_chartSeries.put(propertyKey, series);
					}
				}
			}
			
			m_xyChart.removeSeries(currentSeries.getName());
			
			m_chartPanel.revalidate();
			m_chartPanel.repaint();
		}
	}
}
