package edu.bu.zaman.ModelVisualizer;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.json.JSONObject;

import edu.bu.zaman.ModelVisualizer.ArrayPropertyCondition.Type;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

public class PropertyKeyConfigurationPanel extends JPanel
{
	/**
	 * Serial version UID used for object serialization.
	 */
	private static final long serialVersionUID = 6797228877909920012L;
	
	/**
	 * The property key associated with this configuration panel.
	 */
	private String m_propertyKey;
	
	private String[] m_propertyKeyComponents;
	private HashMap<String, ArrayList<String>> m_componentProperties;
	
	private JPanel m_conditionsPanel;
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
		setLayout(new MigLayout());
		
		JLabel propertyKeyLabel = new JLabel(m_propertyKey);
		add(propertyKeyLabel, "span");
		
		for (String component : m_propertyKeyComponents)
		{
			if (component.startsWith(App.ARRAY_DELIMITER))
			{
				String buttonTitle = component.substring(1);
				JButton arrayButton = new JButton(buttonTitle);
				
				arrayButton.setActionCommand(component);
				arrayButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) 
					{
						String component = e.getActionCommand();
						loadConditionsPanel(component);					
					}
				});
				
				add(arrayButton);
			}
			else
			{
				JLabel propertyLabel = new JLabel("." + component);
				add(propertyLabel);
			}
		}
		
		m_conditionsPanel = new JPanel(new BorderLayout());
		add(m_conditionsPanel, "south");
	}
		
	/**
	 * Returns whether the property key path for this configuration panel has been properly configured. This is true
	 * when every array component in the key path has at least one condition and or has been selected as the x-axis
	 * property for plotting.
	 * 
	 * @return whether there is a valid configuration
	 */
	public boolean hasValidConfiguration()
	{
		// Make sure that the number of conditions matches the number of array properties
		if (m_conditionSets.size() < m_componentProperties.size())
		{
			return false;
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
		for (PropertyKeyConfigurationChangedListener listener : m_listeners)
		{
			listener.configurationChanged(this);
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
		m_conditionsPanel.add(conditionPanel, BorderLayout.CENTER);
		
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
		private JPanel m_conditions;
		
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
			
			setLayout(new MigLayout());
			
			m_xAxisCheckbox = new JCheckBox("x-axis property");
			m_xAxisCheckbox.addActionListener(new ActionListener()
			{			
				@Override
				public void actionPerformed(ActionEvent e)
				{
					boolean checked = m_xAxisCheckbox.isSelected();
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
				}
			});
			
			JComboBox<String> properties = new JComboBox<>(m_componentProperties);
			properties.addActionListener(this);
			add(properties);
			
			JComboBox<String> comparators = new JComboBox<>(new String[] {"=", ">", ">=", "<", "<=", "in"});
			comparators.addActionListener(this);
			add(comparators);
			
			JTextField value = new JTextField();
			value.setPreferredSize(new Dimension(80, value.getPreferredSize().height));
			value.getDocument().addDocumentListener(this);			
			add(value, "wrap");
			
			m_conditionProperties.add(properties);
			m_conditionComparators.add(comparators);
			m_conditionValues.add(value);			
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
