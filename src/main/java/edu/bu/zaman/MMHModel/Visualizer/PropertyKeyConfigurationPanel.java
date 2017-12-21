package edu.bu.zaman.MMHModel.Visualizer;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.bu.zaman.MMHModel.Visualizer.ArrayPropertyCondition.Type;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

public class PropertyKeyConfigurationPanel extends JPanel
{
	public enum DataOption
	{
		NONE,
		COUNT
	}
	
	/**
	 * Serial version UID used for object serialization.
	 */
	private static final long serialVersionUID = 6797228877909920012L;
	
	private static final String INVALID_PATH_CONFIG = "Incomplete configuration";
	private static final String VALID_PATH_CONFIG = "Configuration compelete";
	
	private static final Color INVALID_PATH_FOREGROUND = new Color(0x701010);
	private static final Color INVALID_PATH_BACKGROUND = new Color(0xf6bfbf);
	private static final Border INVALID_PROPERTY_BORDER = BorderFactory.createLineBorder(INVALID_PATH_FOREGROUND); 
	private static final Border INVALID_PATH_BORDER	= new CompoundBorder(
			INVALID_PROPERTY_BORDER,
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
	);
	
	private static final Color VALID_PATH_FOREGROUND = new Color(0x103A00);
	private static final Color VALID_PATH_BACKGROUND = new Color(0xf2fff2);
	private static final Border VALID_PROPERTY_BORDER = BorderFactory.createLineBorder(VALID_PATH_FOREGROUND); 
	private static final Border VALID_PATH_BORDER = new CompoundBorder(
			VALID_PROPERTY_BORDER,
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
	);
	
	private static final Border INVALID_COMPONENT_BORDER = new CompoundBorder(
			INVALID_PROPERTY_BORDER,
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
	);
	private static final Border INVALID_COMPONENT_BORDER_SELECTED = new CompoundBorder(			
			BorderFactory.createMatteBorder(0, 0, 1, 0, INVALID_PATH_FOREGROUND),
			INVALID_COMPONENT_BORDER
	);	
	private static final Border VALID_COMPONENT_BORDER = new CompoundBorder(
			VALID_PROPERTY_BORDER,
			BorderFactory.createEmptyBorder(5, 10, 5, 10)
	);
	private static final Border VALID_COMPONENT_BORDER_SELECTED = new CompoundBorder(			
			BorderFactory.createMatteBorder(0, 0, 1, 0, VALID_PATH_FOREGROUND),
			VALID_COMPONENT_BORDER
	);	
	
	/**
	 * The property key associated with this configuration panel.
	 */
	private String m_propertyKey;
	
	private String[] m_propertyKeyComponents;
	private HashMap<String, ArrayList<String>> m_componentProperties;
	
	private JPanel m_pathComponents;
	private JPanel m_conditionsPanel;
	private JTextField m_seriesNameField;
	private JLabel m_pathConfigStatus;
	private ButtonGroup m_dataOptions;
	private JRadioButton m_optionCount;
	
	private HashMap<String, ConditionsPanel> m_conditionsPanels = new HashMap<>();
	private HashMap<String, ArrayPropertyConditionSet> m_conditionSets = new HashMap<>();
	
	private ArrayList<PropertyKeyConfigurationChangedListener> m_listeners = new ArrayList<>();
	
	public PropertyKeyConfigurationPanel(String key, HashMap<String, ArrayList<String>> componentProperties)
	{
		super();
		
		m_propertyKey = key;
		m_componentProperties = componentProperties;
		m_propertyKeyComponents = key.split("\\.");
		
		initializeViews();
	}
	
	/**
	 * Initializes the view components.
	 */
	private void initializeViews()
	{
		TitledBorder border = BorderFactory.createTitledBorder("Property Configuration: " + getPropertyKey());
		border.setTitleFont(Visualizer.labelFont);
		border.setTitlePosition(TitledBorder.TOP);
		border.setTitleJustification(TitledBorder.LEFT);
		border.setTitleColor(Visualizer.labelColor);
		
		setLayout(new MigLayout("fillx, ins 10, nogrid"));
		setBorder(border);
		
		JLabel seriesNameLabel = new JLabel("Series name:");
		seriesNameLabel.setFont(Visualizer.labelFont);
		seriesNameLabel.setForeground(Visualizer.labelColor);
		
		JPanel labelBorder = new JPanel(new MigLayout("fillx, ins 0, nogrid"));
		labelBorder.setBorder(INVALID_PROPERTY_BORDER);
		
		m_seriesNameField = new JTextField();
		m_seriesNameField.setBorder(new EmptyBorder(5, 5, 5, 5));
		m_seriesNameField.addKeyListener(new KeyAdapter() 
		{		
			String oldText;

			@Override
			public void keyReleased(KeyEvent e) 
			{
				String newText = m_seriesNameField.getText();
				if (!oldText.equals(newText))
				{
					fireSeriesNameChanged(newText);
				}
				
				if (newText.equals(""))
				{
					labelBorder.setBorder(INVALID_PROPERTY_BORDER);
				}
				else
				{
					labelBorder.setBorder(VALID_PROPERTY_BORDER);
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) 
			{
				oldText = m_seriesNameField.getText();
			}
		});
		
		labelBorder.add(m_seriesNameField, "wmin 320, span");
		add(seriesNameLabel, "growy, split 2");
		add(labelBorder, "aligny center, gapleft 10, wrap");
		
		JLabel dataOptionsLabel = new JLabel("Data Options:");
		dataOptionsLabel.setFont(Visualizer.labelFont);
		dataOptionsLabel.setForeground(Visualizer.labelColor);
		
		m_dataOptions = new ButtonGroup();
		
		JRadioButton optionNone = new JRadioButton("None");
		m_optionCount = new JRadioButton("Count");
		
		ActionListener notifyOptionChanged = new ActionListener() 
		{		
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				firePropertyKeyConfigurationChanged();
			}
		};
		
		optionNone.addActionListener(notifyOptionChanged);
		m_optionCount.addActionListener(notifyOptionChanged);
		
		m_dataOptions.add(optionNone);
		m_dataOptions.add(m_optionCount);
		optionNone.setSelected(true);
		
		add(dataOptionsLabel, "gaptop 10");
		add(optionNone);
		add(m_optionCount, "wrap");
		
		JLabel propertyPathLabel = new JLabel("Path Configuration:");
		propertyPathLabel.setFont(Visualizer.labelFont);
		propertyPathLabel.setForeground(Visualizer.labelColor);
		
		m_pathConfigStatus = new JLabel();
		m_pathConfigStatus.setOpaque(true);
		m_pathConfigStatus.setFont(new Font("System", Font.PLAIN, 11));
		
		add(propertyPathLabel, "gaptop 10");
		add(m_pathConfigStatus, "gapleft 10, gaptop 10, gapbottom 15, wrap");
		
		m_pathComponents = new JPanel(new MigLayout("ins 0 0 0 15, nogrid"));
		add(m_pathComponents, "gapleft 15, wrap");
		
		for (String component : m_propertyKeyComponents)
		{
			if (component.startsWith(Visualizer.ARRAY_DELIMITER))
			{
				String buttonTitle = component.substring(1);
				JButton arrayButton = new JButton(buttonTitle);
				
				arrayButton.setBorder(INVALID_COMPONENT_BORDER);
				arrayButton.setBackground(INVALID_PATH_BACKGROUND);
				arrayButton.setOpaque(true);				
				arrayButton.setActionCommand(component);
				arrayButton.addActionListener(new ActionListener() 
				{					
					@Override
					public void actionPerformed(ActionEvent e) 
					{
						String component = e.getActionCommand();
						loadConditionsPanel(component);					
						
						updateArrayComponentButtons();
					}
				});
				
				m_pathComponents.add(arrayButton);
			}
			else
			{
				JLabel propertyLabel = new JLabel("." + component);
				m_pathComponents.add(propertyLabel);
			}
		}
		
		m_conditionsPanel = new JPanel(new MigLayout("fill, ins 0"));
		add(m_conditionsPanel, "south, gapleft 25");
		
		updateConfigurationStatus();
	}
		
	private void updateConfigurationStatus()
	{
		// Check and update overall configuration
		if (hasValidPathConfiguration())
		{
			m_pathConfigStatus.setText(VALID_PATH_CONFIG);
			m_pathConfigStatus.setBackground(VALID_PATH_BACKGROUND);
			m_pathConfigStatus.setForeground(VALID_PATH_FOREGROUND);
			m_pathConfigStatus.setBorder(VALID_PATH_BORDER);
		}
		else
		{
			m_pathConfigStatus.setText(INVALID_PATH_CONFIG);
			m_pathConfigStatus.setBackground(INVALID_PATH_BACKGROUND);
			m_pathConfigStatus.setForeground(INVALID_PATH_FOREGROUND);
			m_pathConfigStatus.setBorder(INVALID_PATH_BORDER);
		}
		
		m_pathConfigStatus.repaint(); 
	}
	
	private void updateArrayComponentButtons()
	{
		for (Component component : m_pathComponents.getComponents())
		{
			if (component instanceof JButton)
			{
				JButton button = (JButton)component;
				ConditionsPanel panel = m_conditionsPanels.get(button.getActionCommand());
				
				if (panel != null && panel.hasValidConditions())
				{
					if (panel == m_conditionsPanel.getComponent(0))
					{
						button.setBorder(VALID_COMPONENT_BORDER_SELECTED);
						button.setBackground(VALID_PATH_BACKGROUND);
					}
					else
					{
						button.setBorder(VALID_COMPONENT_BORDER);
						button.setBackground(VALID_PATH_BACKGROUND);
					}
				}
				else
				{
					if (m_conditionsPanel.getComponents().length == 0 || panel == m_conditionsPanel.getComponent(0))
					{
						button.setBorder(INVALID_COMPONENT_BORDER_SELECTED);
						button.setBackground(INVALID_PATH_BACKGROUND);
					}
					else
					{
						button.setBorder(INVALID_COMPONENT_BORDER);
						button.setBackground(INVALID_PATH_BACKGROUND);
					}
				}
			}
		}
		
		m_conditionsPanel.revalidate();
		m_conditionsPanel.repaint();
	}
	
	/**
	 * Returns whether the panel has been properly configured with a series name and fully configured
	 * property path, which is true when every array component in the key path has at least one condition 
	 * and or has been selected as the x-axis property for plotting.
	 * 
	 * @return whether the panel is properly configured
	 */
	public boolean hasValidConfiguration()
	{
		if (getSeriesName().equals("") || !hasValidPathConfiguration())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns whether the property key path for this configuration panel has been properly configured. This is true
	 * when every array component in the key path has at least one condition and or has been selected as the x-axis
	 * property for plotting.
	 * 
	 * @return whether there is a valid path configuration
	 */
	private boolean hasValidPathConfiguration()
	{
		// Make sure that the number of conditions matches the number of array properties
		if (m_conditionSets.size() < m_componentProperties.size())
		{
			return false;
		}
		
		// Check to make sure all of the panels indepdently have a valid configuration
		for (ConditionsPanel panel : m_conditionsPanels.values())
		{
			if (!panel.hasValidConditions())
			{
				return false;
			}
		}
		
		// Make sure that one component has been marked as an x-axis property		
		boolean xAxisProperty = false;		
		for (ArrayPropertyConditionSet conditionSet : m_conditionSets.values())
		{
			xAxisProperty |= conditionSet.isXAxisProperty();
		}
		
		return xAxisProperty;
	}
	
	/**
	 * Returns the series name that should be used in the plot of this data series.
	 * 
	 * @return the series name
	 */
	public String getSeriesName()
	{
		return m_seriesNameField.getText();
	}
	
	/**
	 * Returns any data option that was selected for this series.
	 * 
	 * @return the data option
	 */
	public DataOption getDataOption()
	{
		if (m_optionCount.isSelected())
		{
			return DataOption.COUNT;
		}
		
		return DataOption.NONE;
	}
	
	/**
	 * Returns an immutable map of array property condition sets that are keyed by the component name
	 * within the key path.
	 * 
	 * @return the map of condition sets
	 */
	public Map<String, ArrayPropertyConditionSet> getConditionSets()
	{
		return Collections.unmodifiableMap(m_conditionSets);
	}
		
	/**
	 * Returns the property key associated with this configuration panel.
	 * 
	 * @return the property key
	 */
	public String getPropertyKey()
	{
		return m_propertyKey;
	}
	
	/**
	 * Adds a listener to be notified when a property key configration has been changed. 
	 * 
	 * @param listener
	 */
	public void addPropertyKeyConfigurationChangedListener(PropertyKeyConfigurationChangedListener listener)
	{
		m_listeners.add(listener);
	}
	
	/**
	 * Notifies property key configuration listeners that the configuration has been changed.
	 */
	private void firePropertyKeyConfigurationChanged()
	{
		updateConfigurationStatus();
		updateArrayComponentButtons();
		
		for (PropertyKeyConfigurationChangedListener listener : m_listeners)
		{
			listener.configurationChanged(this);
		}
	}
	
	private void fireSeriesNameChanged(String newName)
	{
		for (PropertyKeyConfigurationChangedListener listener : m_listeners)
		{
			listener.seriesNameChanged(this, newName);
		}
	}
	
	/**
	 * Loads a configuration panel for an array component of the panel's property key path.
	 * 
	 * @param arrayComponent the array component whose configuration panel should be loaded
	 */
	private void loadConditionsPanel(String arrayComponent)
	{
		ConditionsPanel conditionPanel = m_conditionsPanels.get(arrayComponent);
		if (conditionPanel == null)
		{			
			ArrayPropertyConditionSet conditionSet = new ArrayPropertyConditionSet(); 
			conditionPanel = new ConditionsPanel(conditionSet, m_componentProperties.get(arrayComponent));
			
			m_conditionsPanels.put(arrayComponent, conditionPanel);
			m_conditionSets.put(arrayComponent, conditionSet);
		}
		
		m_conditionsPanel.removeAll();
		m_conditionsPanel.add(conditionPanel);
		
		m_conditionsPanel.revalidate();
		m_conditionsPanel.repaint();
	}
	
	private class ConditionsPanel extends JPanel implements DocumentListener, ActionListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -6189414283658498296L;

		private ArrayPropertyConditionSet m_conditionSet;
		private String[] m_componentProperties;
		
		private JCheckBox m_xAxisCheckbox;
		private JPanel m_conditionsPanel;
		
		private ArrayList<JComboBox<String>> m_conditionProperties = new ArrayList<>();
		private ArrayList<JComboBox<String>> m_conditionComparators = new ArrayList<>();
		private ArrayList<JTextField> m_conditionValues = new ArrayList<>();
		
		public ConditionsPanel(ArrayPropertyConditionSet conditionSet, ArrayList<String> componentProperties)
		{
			m_conditionSet = conditionSet;
			m_componentProperties = componentProperties.parallelStream().toArray(String[]::new);
			
			initializeViews();
		}
		
		private void initializeViews()
		{
			if (m_conditionSet == null)
			{
				return;
			}
			
			setLayout(new MigLayout("fillx, ins 0"));
			
			m_xAxisCheckbox = new JCheckBox("x-axis property");
			m_xAxisCheckbox.addActionListener(new ActionListener()
			{			
				@Override
				public void actionPerformed(ActionEvent e)
				{
					boolean checked = m_xAxisCheckbox.isSelected();
					setConditionsEnabled(!checked);
					
					if (checked)
					{
						// Unset property for all other condition sets and then set it for this one
						Collection<ArrayPropertyConditionSet> conditionSets = m_conditionSets.values();
						for (ArrayPropertyConditionSet set : conditionSets)
						{
							set.setXAxisProperty(false);
						}
					
						m_conditionSet.setXAxisProperty(true);
					}
					else
					{
						m_conditionSet.setXAxisProperty(false);
					}
					
					firePropertyKeyConfigurationChanged();
				}
			});
			
			add(m_xAxisCheckbox, "span");
			
			addAncestorListener(new AncestorListener() {
				
				@Override
				public void ancestorRemoved(AncestorEvent event) {}
				
				@Override
				public void ancestorMoved(AncestorEvent event) {}
				
				@Override
				public void ancestorAdded(AncestorEvent event) 
				{					
					m_xAxisCheckbox.setSelected(m_conditionSet.isXAxisProperty());
					setConditionsEnabled(!m_xAxisCheckbox.isSelected());
				}
			});
			
			m_conditionsPanel = new JPanel(new MigLayout("fillx, nogrid, ins 0"));
			
			JComboBox<String> properties = new JComboBox<>(m_componentProperties);
			properties.addActionListener(this);
			m_conditionsPanel.add(properties);
			
			JComboBox<String> comparators = new JComboBox<>(new String[] {"=", ">", ">=", "<", "<=", "in"});
			comparators.addActionListener(this);
			m_conditionsPanel.add(comparators);
			
			JTextField value = new JTextField();
			value.setPreferredSize(new Dimension(80, value.getPreferredSize().height));
			value.getDocument().addDocumentListener(this);			
			m_conditionsPanel.add(value, "wrap");
			
			add(m_conditionsPanel, "wrap");
			
			m_conditionProperties.add(properties);
			m_conditionComparators.add(comparators);
			m_conditionValues.add(value);			
		}
		
		public boolean hasValidConditions()
		{
			if (m_conditionSet.isXAxisProperty())
			{
				return true;
			}
			
			for (JTextField field : m_conditionValues)
			{
				if (field.getText().equals(""))
				{
					return false;
				}
			}
			
			return true;
		}
		
		private void updateConditions()
		{
			for (int index = 0; index < m_conditionProperties.size(); index++)
			{
				String key = m_conditionProperties.get(index).getSelectedItem().toString();
				String comparator = m_conditionComparators.get(index).getSelectedItem().toString();
				String value = m_conditionValues.get(index).getText();
				
				m_conditionSet.setCondition(key, Type.fromString(comparator), value);
			}
			
			firePropertyKeyConfigurationChanged();
		}
		
		private void setConditionsEnabled(boolean enabled)
		{
			for (Component component : m_conditionsPanel.getComponents())
			{
				component.setEnabled(enabled);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			updateConditions();
		}
		
		@Override
		public void removeUpdate(DocumentEvent e) 
		{
			updateConditions();
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) 
		{
			updateConditions();
		}
		
		@Override
		public void changedUpdate(DocumentEvent e) {}
	}
}
