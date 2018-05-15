package edu.bu.zaman.MMHModel.Visualizer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.util.Pair;
import net.miginfocom.swing.MigLayout;

public class DataParser extends JFrame 
{
	private ArrayList<String> m_propertyKeys = new ArrayList<>();
	private HashMap<String, ArrayList<String>> m_componentProperties = new HashMap<>();
	private DefaultListModel<String> m_chartKeysModel = new DefaultListModel<>();
	private ArrayList<PropertyKeyConfigurationPanel> m_configurationPanels = new ArrayList<>();
	
	private JPanel m_optionsPanel;
	private JPanel m_configurationPanel;
	
	private ArrayList<File> m_directories;
	private File m_directory;
	private File[] m_outputFiles;
	
	public DataParser(File directory)
	{
		FilenameFilter outputFilesFilter = new FilenameFilter() 
		{
			@Override
			public boolean accept(File dir, String name) 
			{
				return name.endsWith(Visualizer.MODEL_FILE_EXT);
			}			
		};

		// List all directories and subdirectories that contain output files
		m_directories = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(directory.toPath()))
		{				
			paths.filter(path -> (Files.isDirectory(path) && path.toFile().listFiles(outputFilesFilter).length > 0))
				.forEach(path -> m_directories.add(path.toFile()));			
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		m_directory = m_directories.get(0);
		m_outputFiles = m_directory.listFiles(outputFilesFilter);
		
		if (m_outputFiles.length < 1)
		{
			return;
		}
		
		// Populate property keys based on those found in the first output file
		loadModelData(m_outputFiles[0].toPath());
		initializeViews();
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	
	private void initializeViews()
	{
		setLayout(new MigLayout());
		
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
				if (!e.isAddedPath())
				{
					return;
				}
				
				TreePath selectedPath = e.getPath();
				
				Object lastPathComponent = selectedPath.getLastPathComponent();
				if (lastPathComponent != null && lastPathComponent instanceof DefaultMutableTreeNode)
				{
					ArrayList<Object> pathComponents = new ArrayList<>(Arrays.asList(selectedPath.getPath()));				
					pathComponents.remove(0); // Remove the empty root node from the path					
					String path = String.join(".", pathComponents.stream().map(object -> object.toString()).collect(Collectors.toList()));
					
					// Allow adding same key multiple times (to allow for varying conditions)
					String propertyKey = "";
					if (m_chartKeysModel.contains(path))
					{
						int counter = 0;
						for (Object key : m_chartKeysModel.toArray())
						{
							if (key.toString().startsWith(path))
							{
								counter++;
							}							
						}
						
						propertyKey = path + "-" + counter;
					}
					else
					{
						propertyKey = path;
					}
					
					m_chartKeysModel.addElement(propertyKey);
					
					propertyKeysTree.clearSelection();
					
					// Create a new configuration panel for the key
					HashMap<String, ArrayList<String>> componentProperties = new HashMap<>();
					String[] components = path.split("\\.");
					
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
					
					PropertyKeyConfigurationPanel configurationPanel = PropertyKeyConfigurationManager.getConfigurationPanel(propertyKey, componentProperties);
					m_configurationPanels.add(configurationPanel);
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
				m_configurationPanels.remove(index);

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
				m_chartKeysModel.removeAllElements();
				m_configurationPanels.clear();
				
				propertyKeysTree.clearSelection();
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
					int selectedIndex = chartKeysList.getSelectedIndex();
					if (selectedIndex < 0)
					{
						return;
					}
					
					PropertyKeyConfigurationPanel configurationPanel = m_configurationPanels.get(selectedIndex);
					
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
						
		JButton parseButton = new JButton("Parse");
		parseButton.addActionListener(new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				System.out.println(m_configurationPanels.size());
				
				// Make sure all properties are properly configured
				for (PropertyKeyConfigurationPanel panel : m_configurationPanels)
				{
					if(!panel.hasValidConfiguration())
					{
						System.out.println(panel.getPropertyKey());
						JOptionPane.showMessageDialog(null, "All property keys must have a valid configuration");
						return;
					}
				}
				
				for(File outputDirectory : m_directories)
				{
					System.out.println("Parsing output files in " + outputDirectory.getPath());
										
					if (Files.exists(Paths.get(outputDirectory.getPath().toString(), "outputData.xlsx")))
					{
						continue;
					}
					
					m_directory = outputDirectory;
					m_outputFiles = m_directory.listFiles(new FilenameFilter() 
					{
						@Override
						public boolean accept(File dir, String name) 
						{
							return name.endsWith(Visualizer.MODEL_FILE_EXT);
						}			
					});
					
					HashMap<String, ArrayList<ArrayList<Pair<String, String>>>> allData = new HashMap<>();
					int run = 0;
					for(File outputFile : m_outputFiles)
					{
						try 
						{
							String jsonText = new String(Files.readAllBytes(outputFile.toPath()));
							JSONObject modelData = new JSONObject(jsonText);
							for (PropertyKeyConfigurationPanel panel : m_configurationPanels)
							{
								ArrayList<Pair<String, String>> data = extractData(modelData, panel);
								ArrayList<ArrayList<Pair<String, String>>> seriesData = allData.get(panel.getSeriesName());
								
								if (seriesData == null)
								{
									seriesData = new ArrayList<>();
									seriesData.add(data);
									
									allData.put(panel.getSeriesName(), seriesData);
								}
								else
								{
									seriesData.add(data);
								}
							}						
						} 
						catch (IOException ioe) 
						{
							ioe.printStackTrace();
						}
						
						System.out.println("Run " + (++run) + " completed.");
					}
					
					writeData(allData);
				}
			}
		});
				
		// Set up frame
		m_optionsPanel.add(parseButton, "wrap, align right");
		add(m_optionsPanel, "east, w 800, wmax 800");		
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
			JSONObject modelData = new JSONObject(jsonText);
			
			HashSet<String> propertyKeys = new HashSet<String>();			
			parseJSONData(modelData, null, propertyKeys);
			
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
	 * Extracts property data into an array of data points represented as Pair<Integer, String>
	 * objects, where the integer values represents an x-axis property, and the string value
	 * represents an associated value. An empty data set is returned if a problem is encountered.
	 * 
	 * @param outputData	the output data from which to extract a specific property key 
	 * @param key			the property key from which model data should be extracted
	 * @return array of Pair<Integer, String> objects that represent XY data points for plotting
	 */
	private ArrayList<Pair<String, String>> extractData(JSONObject outputData, PropertyKeyConfigurationPanel panel)
	{
		
		ArrayList<Pair<String, String>> chartData = new ArrayList<>();
		
		String key = panel.getPropertyKey();
		String[] keyComponents = key.split("\\.");
		
		if (keyComponents.length > 0)
		{	
			JSONObject data = outputData;
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
	 * Outputs all of the extracted data to an excel file written to the input directory.
	 * 
	 * @param data The extracted data, keyed by the series name.
	 */
	private void writeData(HashMap<String, ArrayList<ArrayList<Pair<String, String>>>> data)
	{
		System.out.println("Writing data...");
		Workbook workbook = new XSSFWorkbook();
		
		CellStyle percentStyle = workbook.createCellStyle();
		percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));		
		
		for (String series : data.keySet())
		{
			Sheet seriesSheet = workbook.createSheet(series);
			ArrayList<ArrayList<Pair<String, String>>> seriesData = data.get(series);
			if (seriesData == null)
			{
				continue;
			}
			
			// Pre-create the first two rows
			seriesSheet.createRow(0);
			seriesSheet.createRow(1);
			
			Row row;
			Cell cell;
			
			for (int run = 0; run < seriesData.size(); run++)
			{
				ArrayList<Pair<String, String>> runData = seriesData.get(run);
								
				seriesSheet.getRow(0).createCell(run * 3).setCellValue("Run " + (run + 1));
				seriesSheet.getRow(1).createCell(run * 3).setCellValue("x");
				seriesSheet.getRow(1).createCell(run * 3 + 1).setCellValue("y");

				for(int dataRow = 0; dataRow < runData.size(); dataRow++)
				{					
					Pair<String, String> dataPoint = runData.get(dataRow);
					
					row = seriesSheet.getRow(dataRow + 2);
					if (row == null)
					{
						row = seriesSheet.createRow(dataRow + 2);
					}
					
					// Set x-value
					cell = row.createCell(run * 3);
					try 
					{						
						cell.setCellValue(Double.parseDouble(dataPoint.getKey()));
					}
					catch (NumberFormatException nfe)
					{
						cell.setCellValue(dataPoint.getKey());
					}
					
					// Set y-value
					cell = row.createCell(run * 3 + 1);
					try 
					{						
						cell.setCellValue(Double.parseDouble(dataPoint.getValue()));
					}
					catch (NumberFormatException nfe)
					{
						cell.setCellValue(dataPoint.getValue());
					}									
				}
			}	
			
			int meanColumn = seriesData.size() * 3;
			
			seriesSheet.getRow(1).createCell(meanColumn).setCellValue("Mean");
			seriesSheet.getRow(1).createCell(meanColumn + 1).setCellValue("Stdev");
			seriesSheet.getRow(1).createCell(meanColumn + 2).setCellValue("RSD");
			
			int dataRow = 2;
			while ((row = seriesSheet.getRow(dataRow)) != null)
			{
				int lastCell = row.getLastCellNum() - 1;
				
			    String firstCellReference = (new CellReference(row.getRowNum(), row.getFirstCellNum())).formatAsString();
			    String lastCellReference = (new CellReference(row.getRowNum(), lastCell)).formatAsString();
			    
			    CellReference meanCell = new CellReference(row.getRowNum(), meanColumn);
			    seriesSheet.setArrayFormula(
			    	"SUMPRODUCT("
			    		+ "--(MOD(COLUMN(" + firstCellReference + ":" + lastCellReference + ") - COLUMN(A1), 3) = 1)," 
			    				+ firstCellReference + ":" + lastCellReference + ") / "
			    						+ "SUM(--(MOD(COLUMN(" + firstCellReference + ":" + lastCellReference + ") - COLUMN(A1), 3) = 1))"				    				
			    	, new CellRangeAddress(row.getRowNum(), row.getRowNum(), meanColumn, meanColumn)
			    );
			    
			    CellReference stdevCell = new CellReference(row.getRowNum(), meanColumn + 1);
			    seriesSheet.setArrayFormula(
			    	"SQRT(SUMPRODUCT("
			    		+ "--(MOD(COLUMN(" + firstCellReference + ":" + lastCellReference + ") - COLUMN(A1), 3) = 1)," 
			    				+ "IFERROR(" + firstCellReference + ":" + lastCellReference + " - " + meanCell.formatAsString() + ", 0) ^ 2) / "
			    						+ "(SUM(--(MOD(COLUMN(" + firstCellReference + ":" + lastCellReference + ") - COLUMN(A1), 3) = 1)) - 1))"				    				
			    	, new CellRangeAddress(row.getRowNum(), row.getRowNum(), meanColumn + 1, meanColumn + 1)
			    );
			    
			    Cell rsdCell = row.createCell(meanColumn + 2); 
			    rsdCell.setCellFormula(stdevCell.formatAsString() + "/" + meanCell.formatAsString());
			    rsdCell.setCellStyle(percentStyle);
			    
				dataRow++;
			}
		}
		
		File outputFile = new File(m_directory, "outputData.xlsx");
		try 
		{
			workbook.write(new FileOutputStream(outputFile));
			workbook.close();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		System.out.println("Done.");
	}
	
	public static void main(String[] args) 
	{
		// Open a directory chooser on the event dispatch thread
		Runnable chooserTask = new Runnable()
		{

			@Override
			public void run() 
			{
				JFileChooser fileChooser = new JFileChooser();		
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				int returnValue = fileChooser.showOpenDialog(null);		
				if (returnValue == JFileChooser.APPROVE_OPTION)
				{
					File selectedDirectory = fileChooser.getSelectedFile();
					new DataParser(selectedDirectory);
				}
				else
				{
					System.exit(0);
				}
			}			
		};
		
		SwingUtilities.invokeLater(chooserTask);
	}
}
