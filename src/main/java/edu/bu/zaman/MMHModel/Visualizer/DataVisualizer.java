package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.DataSources;
import org.apache.poi.ss.usermodel.charts.ScatterChartSeries;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.charts.XSSFChartLegend;
import org.apache.poi.xssf.usermodel.charts.XSSFScatterChartData;
import org.apache.poi.xssf.usermodel.charts.XSSFValueAxis;
import org.json.JSONArray;
import org.json.JSONObject;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
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
	
	private enum ChartType
	{
		XY_SCATTER("XY Scatter"),
		BAR_CHART("Bar Chart"),
		PIE_CHART("Pie Chart");
		
		private final String m_name;
		private ChartType(final String name)
		{
			m_name = name;
		}
		
		public String toString()
		{
			return m_name;
		}
	}
	
	/**
	 * Serial version UID used for object serialization
	 */
	private static final long serialVersionUID = 1109397701001808148L;
	private static final String PLACEHOLDER_SERIES = "_placeholder";
	
	private JSONObject m_modelData;
	private XYChart m_xyChart;
	private CategoryChart m_categoryChart;
	private HashMap<String, Series> m_chartSeries = new HashMap<>();
	
	private ArrayList<String> m_propertyKeys = new ArrayList<>();
	private HashMap<String, ArrayList<String>> m_componentProperties = new HashMap<>();
	private DefaultListModel<String> m_chartKeysModel = new DefaultListModel<>();
	
	private JPanel m_chartContainerPanel;
	private JComboBox<ChartType> m_chartTypes = new JComboBox<ChartType>(new ChartType[]{
			ChartType.XY_SCATTER,
			ChartType.BAR_CHART,
			ChartType.PIE_CHART
	});
	private XChartPanel<XYChart> m_xyChartPanel;
	private XChartPanel<CategoryChart> m_categoryChartPanel;
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
		chartTypeLabel.setFont(Visualizer.labelFont);
		chartTypeLabel.setForeground(Visualizer.labelColor);
		m_chartContainerPanel.add(chartTypeLabel);

		m_chartTypes.addActionListener(new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				switchToChart((ChartType)m_chartTypes.getSelectedItem());
			}
		});
		
		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() 
		{			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				Path exportPath = Paths.get(Visualizer.getOutputDirectory(), "test.xlsx");
				
				exportChart(exportPath.toAbsolutePath().toString());
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
					removeSeries(seriesName);
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
		// Remove the current chart
		if (m_xyChartPanel != null)
		{			
			m_chartContainerPanel.remove(m_xyChartPanel);
			m_xyChartPanel = null;
			m_xyChart = null;
		}
		else if (m_categoryChartPanel != null)
		{
			m_chartContainerPanel.remove(m_categoryChartPanel);
			m_categoryChartPanel = null;
			m_categoryChart = null;
		}			
		
		ChartType chartType = (ChartType) m_chartTypes.getSelectedItem();		
		switch (chartType)
		{
		
		case XY_SCATTER:
			
			m_xyChart = new XYChartBuilder()
				.title("Test Chart")
				.xAxisTitle("X Axis")
				.yAxisTitle("Y Axis")
				.build();
						
			m_xyChart.getStyler().setLegendPosition(LegendPosition.InsideN);
			
			m_xyChartPanel = new XChartPanel<XYChart>(m_xyChart);					
			m_chartContainerPanel.add(m_xyChartPanel, "grow, push, south");
				
			m_xyChartPanel.revalidate();
			m_xyChartPanel.repaint();
			
			break;
		
		case BAR_CHART:
			m_categoryChart = new CategoryChartBuilder()
					.title("Test Chart")
					.xAxisTitle("X Axis")
					.yAxisTitle("Y Axis")
					.build();
			
			m_categoryChart.getStyler().setLegendPosition(LegendPosition.InsideN);
			m_categoryChart.getStyler().setAvailableSpaceFill(.96);
			m_categoryChart.getStyler().setOverlapped(true);
			m_categoryChart.getStyler().setLegendVisible(false);
			
			// Need to have a series plotted to avoid an exception
			m_categoryChart.addSeries(PLACEHOLDER_SERIES, new double[] {0}, new double[] {0});
			
			m_categoryChartPanel = new XChartPanel<CategoryChart>(m_categoryChart);
			m_chartContainerPanel.add(m_categoryChartPanel, "grow, push, south");
			
			m_categoryChartPanel.revalidate();
			m_categoryChartPanel.repaint();
			
			break;
			
		case PIE_CHART:
			break;
			
		}
	}
	
	/**
	 * Clears the current chart and associated property keys from the options panel.
	 */
	private void clearChart()
	{
		m_chartKeysModel.clear();
		m_chartSeries.clear();
		
		PropertyKeyConfigurationManager.clear();
		
		m_configurationPanel.removeAll();
		m_configurationPanel.revalidate();
		m_configurationPanel.repaint();
		
		if (m_categoryChart != null)
		{
			m_categoryChart.getStyler().setLegendVisible(false);
		}
		
		newChart();
	}
	
	private void switchToChart(ChartType type)
	{
		HashMap<String, ArrayList<Pair<String, String>>> existingSeries = new HashMap<>();
		
		if (m_chartSeries.size() > 0)
		{
			Collection<Series> seriesSet = m_chartSeries.values();
			
			for (Series series : seriesSet)
			{
				ArrayList<Pair<String, String>> seriesData = new ArrayList<>();
				if (series instanceof XYSeries)
				{
					XYSeries xySeries = (XYSeries)series;
					double[] xData = xySeries.getXData();
					double[] yData = xySeries.getYData();
					
					for (int index = 0;index < xData.length; index++)
					{
						seriesData.add(new Pair<String, String>("" + xData[index], "" + yData[index]));
					}										
				}
				else if (series instanceof CategorySeries)
				{
					CategorySeries categorySeries = (CategorySeries)series;
					Object[] xData = categorySeries.getXData().toArray();
					Object[] yData = categorySeries.getYData().toArray();
					
					for (int index = 0;index < xData.length; index++)
					{
						seriesData.add(new Pair<String, String>(xData[index].toString(), yData[index].toString()));
						System.out.println(xData.toString() + ", " + yData.toString());
					}									
				}
				
				existingSeries.put(series.getName(), seriesData);
			}
		}
		
		// Create a new chart based on the current chart type selection
		newChart();
		
		// Populate new chart with previous series if any existed
		if (existingSeries.size() > 0)
		{
			for (Map.Entry<String, ArrayList<Pair<String, String>>> series : existingSeries.entrySet())
			{
				plotData(series.getValue(), series.getKey());
			}
		}
	}
	
	/**
	 * Extracts property data into an array of data points represented as Pair<Integer, String>
	 * objects, where the integer values represents an x-axis property, and the string value
	 * represents an associated value. An empty data set is returned if a problem is encountered.
	 * 
	 * @param key	the property key from which model data should be extracted
	 * @return array of Pair<Integer, String> objects that represent XY data points for plotting
	 */
	private ArrayList<Pair<String, String>> getChartData(PropertyKeyConfigurationPanel panel)
	{
		
		ArrayList<Pair<String, String>> chartData = new ArrayList<>();
		
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
						return chartData;
					}
				}
				else
				{
					// Check to see if the array associated with the current path component has been
					// marked as an x-axis property
					if (!conditionSets.containsKey(pathComponent))
					{
						// An error has occurred if the array path component is not in the conditionSets map
						return chartData;
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
							return chartData;
						}
						
						ArrayList<JSONObject> elements = arrayElementSatisfyingConditions(array, conditionSet, true); 						
						if (elements == null || elements.size() == 0)
						{
							// Return an empty data set if an array element matching the condition set was not found
							return chartData;
						}
						
						data = elements.get(0); // Only inspect first array element that satisfies conditions before the loop key
					}
				}
				
				pathIndex++;
			}
			
			if (loopKey == null)
			{
				// An error has occurred if a loop key was not found
				return chartData;
			}
			
			// Find any property keys that could be used to generate multiple series
			String multiSeriesKey = null;
			for (int index = pathIndex; index < keyComponents.length - 1; index++)
			{
				String pathComponent = keyComponents[index];
				if (pathComponent.contains(Visualizer.ARRAY_DELIMITER))
				{
					ArrayPropertyConditionSet conditionSet = conditionSets.get(pathComponent);
					if (conditionSet == null)
					{
						// Error has occurred if a condition set could not be found
						return chartData;
					}
					
					if (!conditionSet.isUnique())
					{
						multiSeriesKey = pathComponent;
					}
				}
			}
			
			JSONArray xAxisArray = data.getJSONArray(loopKey.substring(1)); // Remove array delimeter from beginning of key
			for (int index = 0; index < xAxisArray.length(); index++)
			{
				// Attempt to navigate through the array element using the property key to isolate a scalar value
				JSONObject elementData = xAxisArray.getJSONObject(index);
				if (elementData == null)
				{
					// Error has occurred if this element is null
					return chartData;
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
						ArrayList<JSONObject> matchingElements = arrayElementSatisfyingConditions(array, conditionSet, false);
						
						elementData = null;
						if (matchingElements != null && matchingElements.size() > 0)
						{
							elementData = matchingElements.get(0); 
						}
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
							chartData.add(new Pair<String, String>("" + index, tempData.toString()));
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
		
		// Perform any post-processing on the data based on the series options
		switch (panel.getDataOption())
		{
		
		case HISTOGRAM:
			
			HashMap<String, Integer> counter = new HashMap<>();
			ArrayList<Pair<String, String>> countData = new ArrayList<>();
			
			for (Pair<String, String> dataPoint : chartData)
			{
				String value = dataPoint.getValue();
				Integer count = counter.get(value);
				if (count == null)
				{
					count = 0;
				}
				
				count++;
				counter.put(value, count);
			}
			
			for (Entry<String, Integer> entry : counter.entrySet())
			{
				// Rename key if it refers to a boolean value
				String entryKey = entry.getKey();
				if (entryKey.equals("true"))
				{
					entryKey = "Yes";
				}
				else if (entryKey.equals("false"))
				{
					entryKey = "No";
				}
				
				System.out.println("Count " + entryKey + " : " + entry.getValue());
				countData.add(new Pair<String, String>(entryKey, entry.getValue().toString()));
			}
			
			return countData;
			
		default:
			return chartData;
			
		}			
	}
	
	/**
	 * Searches a JSONArray for an array element that matches a condition set.
	 * 
	 * @param array			the array to search
	 * @param conditionSet	the condition set used to match an element
	 * @param findFirst		flag indicating whether or not only the first match should be returned
	 * 
	 * @return a list of array elements that match the condition set, or null if one was not found
	 */
	private ArrayList<JSONObject> arrayElementSatisfyingConditions(JSONArray array, ArrayPropertyConditionSet conditionSet, boolean findFirst)
	{
		ArrayList<JSONObject> matches = new ArrayList<>();
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
				matches.add(arrayElement);
				if (findFirst)
				{
					return matches;
				}
			}
		}
		
		if (matches.size() == 0)
		{		
			return null;
		}
		
		return matches;
	}
	
	/**
	 * Plots a set of data using an appropriate chart based on the data type of the data set.
	 * If the supplied data is numerical, the data is plotted as an XY chart.
	 * 
	 * @param arrayList		the data to plot
	 * @param seriesName	the series name to use for the data
	 */
	private Series plotData(ArrayList<Pair<String, String>> arrayList, String seriesName)
	{
		if (arrayList.size() <= 0)
		{
			removeSeries(seriesName);			
			return null;
		}
		
		ChartType plotType = (ChartType) m_chartTypes.getSelectedItem();
		Pair<String, String> dataPoint = arrayList.get(0);				
		
		switch (getDataType(dataPoint.getKey()))
		{
		
		case NUMERIC:
			DataType valueType = getDataType(dataPoint.getValue());
			
			ArrayList<Double> xDoubleData = new ArrayList<>(arrayList.size());
			ArrayList<Double> yDoubleData = new ArrayList<>(arrayList.size());
			
			if (valueType == DataType.NUMERIC)
			{							
				for (int index = 0; index < arrayList.size(); index++)
				{
					dataPoint = arrayList.get(index);
					
					xDoubleData.add(Double.parseDouble(dataPoint.getKey()));
					yDoubleData.add(Double.parseDouble(dataPoint.getValue()));
				}
			}
			else if (valueType == DataType.BOOLEAN)
			{
				for (int index = 0; index < arrayList.size(); index++)
				{
					dataPoint = arrayList.get(index);
					
					xDoubleData.add(Double.parseDouble(dataPoint.getKey()));
					yDoubleData.add((Boolean.parseBoolean(dataPoint.getValue())) ? 1.0 : 0.0);					
				}
			}
			else if (valueType == DataType.STRING)
			{
				return null;
			}
			
			if (plotType == ChartType.XY_SCATTER)
			{
				return plotXYData(xDoubleData, yDoubleData, seriesName);
			}
			else if (plotType == ChartType.BAR_CHART)
			{
				return plotCategoryData(xDoubleData, yDoubleData, seriesName);
			}
			
			return null;	
			
		case STRING:
			
			ArrayList<String> xCategoryData = new ArrayList<>(arrayList.size());
			ArrayList<Double> yCategoryData = new ArrayList<>(arrayList.size());
								
			for (int index = 0; index < arrayList.size(); index++)
			{
				dataPoint = arrayList.get(index);
				
				xCategoryData.add(dataPoint.getKey());
				yCategoryData.add(Double.parseDouble(dataPoint.getValue()));
			}
			
			if (plotType == ChartType.BAR_CHART)
			{
				return plotCategoryData(xCategoryData, yCategoryData, seriesName);
			}
			
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
	private XYSeries plotXYData(ArrayList<? extends Number> xData, ArrayList<? extends Number> yData, String seriesName)
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
		
		m_xyChartPanel.revalidate();
		m_xyChartPanel.repaint();
		
		return series;
	}
	
	private CategorySeries plotCategoryData(ArrayList<?> xData, ArrayList<? extends Number> yData, String seriesName)
	{
		// Make sure the chart object has been instantiated
		if (m_categoryChart == null)
		{
			return null;
		}
		
		CategorySeries series;
		try 
		{		
			series = m_categoryChart.addSeries(seriesName, xData, yData);
		} 
		catch (IllegalArgumentException iae)
		{
			// This is thrown if the data series already existst so we should updated it instead
			try
			{
				series = m_categoryChart.updateCategorySeries(seriesName, xData, yData, null);
			}
			catch (ClassCastException cce)
			{
				// This is thrown if the x-axis data type has changed for the same series
				removeSeries(seriesName);
				series = m_categoryChart.addSeries(seriesName, xData, yData);
			}
		}
		
		// Remove the placeholder series if it exists
		m_categoryChart.removeSeries(PLACEHOLDER_SERIES);
		m_categoryChart.getStyler().setLegendVisible(true);
		
		m_categoryChartPanel.revalidate();
		m_categoryChartPanel.repaint();
		
		return series;
	}
	
	private void removeSeries(String series)
	{
		if (m_xyChart != null && m_xyChartPanel != null)
		{
			m_xyChart.removeSeries(series);
			
			m_xyChartPanel.revalidate();
			m_xyChartPanel.repaint();
		}
		else if (m_categoryChart != null && m_categoryChartPanel != null)
		{
			Map<String, CategorySeries> map = m_categoryChart.getSeriesMap();
			if (map.size() == 1 && !map.values().stream().findFirst().get().getName().equals(PLACEHOLDER_SERIES))
			{
				m_categoryChart.addSeries(PLACEHOLDER_SERIES, new double[] {0}, new double[] {0});
			}
			
			m_categoryChart.removeSeries(series);
			
			m_categoryChartPanel.revalidate();
			m_categoryChartPanel.repaint();
		}
	}
	
	/**
	 * Exports the current chart as an excel file.
	 * 
	 * @param path the path where the excel file should be saved
	 */
	private void exportChart(String path)
	{
		// Export the chart based on its type
		if (m_xyChart != null)
		{
			Workbook workbook = new XSSFWorkbook(); // Create a new excel workbook
			Sheet seriesSheet = workbook.createSheet("Series");
			
			XSSFDrawing drawing = (XSSFDrawing) seriesSheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 10, 15);
    
            XSSFChart chart = drawing.createChart(anchor);
            XSSFChartLegend legend = chart.getOrCreateLegend();
            legend.setPosition(org.apache.poi.ss.usermodel.charts.LegendPosition.TOP_RIGHT);
    
            XSSFValueAxis bottomAxis = chart.createValueAxis(AxisPosition.BOTTOM);
            XSSFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);
            
            XSSFScatterChartData data = chart.getChartDataFactory().createScatterChartData();
			
			Map<String, XYSeries> seriesMap = m_xyChart.getSeriesMap();
			int seriesIndex = 0;
			
			for (Map.Entry<String, XYSeries> entry : seriesMap.entrySet())
			{
				String seriesName = entry.getKey();
				XYSeries series = entry.getValue();
				
				Row row = seriesSheet.getRow(0);
				if (row == null)
				{
					row = seriesSheet.createRow(0);
				}
				
				int columnOffset = seriesIndex * 3;
				
				Cell seriesNameCell = row.createCell(columnOffset);
				seriesNameCell.setCellValue(seriesName);
				
				row = seriesSheet.getRow(1);
				if (row == null)
				{
					row = seriesSheet.createRow(1);
				}
				
				Cell xLabelCell = row.createCell(columnOffset);
				Cell yLabelCell = row.createCell(columnOffset + 1);
				xLabelCell.setCellValue("X");
				yLabelCell.setCellValue("Y");
				
				double[] xData = series.getXData();
				double[] yData = series.getYData();
				
				for (int index = 0; index < xData.length; index++)
				{
					row = seriesSheet.getRow(2 + index);
					if (row == null)
					{
						row = seriesSheet.createRow(2 + index);
					}
					
					Cell xValue = row.createCell(columnOffset);
					Cell yValue = row.createCell(columnOffset + 1);
					
					xValue.setCellValue(xData[index]);
					yValue.setCellValue(yData[index]);
				}
				
				// Add the series to the chart
				ChartDataSource<Number> xDataSource = DataSources.fromNumericCellRange(seriesSheet, new CellRangeAddress(2, 2 + xData.length - 1, columnOffset, columnOffset));
	            ChartDataSource<Number> yDataSource = DataSources.fromNumericCellRange(seriesSheet, new CellRangeAddress(2, 2 + xData.length - 1, columnOffset + 1, columnOffset + 1));
					            
	            ScatterChartSeries dataSeries = data.addSerie(xDataSource, yDataSource);
	            dataSeries.setTitle(new CellReference(seriesNameCell));
	            
				seriesIndex++;
			}
			
			chart.plot(data, bottomAxis, leftAxis);
			
			try (FileOutputStream out = new FileOutputStream(path)) 
			{
                workbook.write(out);
                workbook.close();
            }
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
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
			removeSeries(panel.getSeriesName());
		}
	}
	
	@SuppressWarnings("unchecked")
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
				else if (currentSeries instanceof CategorySeries)
				{
					CategorySeries categorySeries = (CategorySeries)currentSeries;
					
					List<Object> xData = (List<Object>) categorySeries.getXData();
					List<Number> yData = (List<Number>) categorySeries.getYData();					
					
					Series series = m_categoryChart.addSeries(newName, xData, yData);
					if (series != null)
					{
						m_chartSeries.put(propertyKey, series);
					}
				}
			}
			
			removeSeries(currentSeries.getName());
		}
	}
	
	/**
	 * Returns the data type for a given String value.
	 * 
	 * @param value		a string value
	 * @return the data type of the string
	 */
	private DataType getDataType(String value)
	{
		try
		{
			Double.parseDouble(value);
			return DataType.NUMERIC;
		} 
		catch (NumberFormatException nfe)
		{
			if (value.equals("true") || value.equals("false"))
			{
				return DataType.BOOLEAN;
			}
			else
			{
				return DataType.STRING;
			}
		}
	}
}
