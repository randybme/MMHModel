package edu.bu.zaman.MMHModel.Visualizer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.json.JSONObject;

import edu.bu.zaman.MMHModel.Visualizer.ArrayPropertyCondition.Type;

public class ArrayPropertyConditionSet implements Serializable
{
	private boolean m_isXAxisProperty = false;
	
	LinkedHashMap<String, ArrayPropertyCondition> m_conditions = new LinkedHashMap<>(); 
	
	public ArrayPropertyConditionSet()
	{
	}
	
	/**
	 * Sets a new condition or overrides an existing condition for the specified key. A condition may
	 * only be set if the array property is not an x-axis property.
	 *  
	 * @param key	the property key associated with the condition
	 * @param type	the type of comparator to use for the condition
	 * @param value	the value that should be compared against the value associated with the property key
	 */
	public void setCondition(String key, Type type, String value)
	{	
		if (!isXAxisProperty() && key != null && type != null && value != null)
		{
			m_conditions.put(key, new ArrayPropertyCondition(key, type, value));
		}
	}
	
	/**
	 * Clears any condition associated with the provided key.
	 * 
	 * @param key	the property key for which all conditions should be cleared 
	 */
	public void clearCondition(String key)
	{
		m_conditions.remove(key);
	}
	
	/**
	 * Determines whether the data object provided satisfies the condition set.
	 * 
	 * @param data	the data object
	 * @return		whether the data object satisfies the condition set
	 */
	public boolean isSatisfied(JSONObject data)
	{
		boolean satisfied = true;
		
		for (ArrayPropertyCondition condition : m_conditions.values())
		{
			satisfied &= condition.satisfiesCondition(data);
		}
		
		return satisfied;
	}
	
	/**
	 * Returns whether this condition set will always result in a unique match.
	 * 
	 * @return
	 */
	public boolean isUnique()
	{
		if (m_conditions.size() <= 0)
		{
			return false;
		}
		
		if (m_conditions.values().stream().findFirst().get().getConditionType() == Type.EQUAL)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Provides a read-only collection of the current conditions.
	 * 
	 * @return	the collection of current property conditions
	 */
	public Collection<ArrayPropertyCondition> getConditions()
	{
		return Collections.unmodifiableCollection(m_conditions.values());
	}
	
	/**
	 * Returns whether this condition set represents an array property that is marked for use
	 * as an x-axis property.
	 * 
	 * @return	whether this is an x-axis property
	 */
	public boolean isXAxisProperty()
	{
		return m_isXAxisProperty;
	}
	
	public void setXAxisProperty(boolean state)
	{
		m_isXAxisProperty = state;
		if (state)
		{
			m_conditions.clear();
		}
	}
}
