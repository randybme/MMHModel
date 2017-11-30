package edu.bu.zaman.MMHModel.Visualizer;

import java.io.Serializable;

import org.json.JSONObject;

/**
 * @author Darash Desai
 *
 */
public class ArrayPropertyCondition implements Serializable 
{
	/**
	 * Defines the list of condition types.
	 *
	 */
	public enum Type
	{
		EQUAL,
		GREATER_THAN,
		GREATER_THAN_EQUAL_TO,
		LESS_THAN,
		LESS_THAN_EQUAL_TO,
		IN;
		
		public static Type fromString(String type)
		{
			if (type.equals("="))
			{
				return EQUAL;
			}
			else if (type.equals(">"))
			{
				return GREATER_THAN;
			}
			else if (type.equals(">="))
			{
				return GREATER_THAN_EQUAL_TO;
			}
			else if (type.equals("<"))
			{
				return LESS_THAN;
			}
			else if (type.equals("<="))
			{
				return LESS_THAN_EQUAL_TO;
			}
			else if (type.toLowerCase().equals("in"))
			{
				return IN;
			}
			
			return null;
		}
	}
	
	private String m_propertyKey;
	private Type m_conditionType;
	private String m_value;
	
	public ArrayPropertyCondition(String key, Type type, String value)
	{
		m_propertyKey = key;
		m_conditionType = type;
		m_value = value;
	}
	
	/**
	 * Determines whether the object provided satisfies this condition instance using the
	 * instance's property key. 
	 * 
	 * The provided object is assumed to be the root object that should contain a property 
	 * matching this condition's property key. Child properties are supported and are represented 
	 * in the property key using dot notation (e.g. property.subproperty).
	 * 
	 * @param object	the object to test
	 * @return whether the object satisfies the condition for the condition instance's property key
	 */
	public boolean satisfiesCondition(JSONObject object)
	{
		if (object == null)
		{
			return false;
		}
		
		String[] components = m_propertyKey.split("//.");
		if (components.length <= 0)
		{
			return false;
		}
		
		JSONObject root = object;
		for (String component : components)
		{
			Object value = root.get(component);
			if (value == null)
			{
				return false;
			}
			else if (value instanceof JSONObject)
			{
				root = (JSONObject)value;
			}
			else
			{
				return valueSatisfiesCondition(value);
			}
		}
		
		return false;
	}
	
	/**
	 * Helper method that determines whether the provided value satisfies the condition
	 * defined by this class.
	 * 
	 * @param value	the value to test
	 * @return whether the value satisfies this condition
	 */
	private boolean valueSatisfiesCondition(Object value)
	{
		if (m_conditionType == Type.EQUAL)
		{
			return value.toString().equals(m_value);
		}
		else if (m_conditionType == Type.IN)
		{
			String stringValue = value.toString();
			String[] values = m_value.split(",");
			
			for (String testValue : values)
			{
				if (testValue.trim().equals(stringValue))
				{
					return true;
				}
			}
		}
		else
		{
			try 
			{
				double testValue = Double.parseDouble(m_value);
				double doubleValue = (double)value;
				
				switch (m_conditionType)
				{
				
				case GREATER_THAN:
					return doubleValue > testValue;
					
				case GREATER_THAN_EQUAL_TO:
					return doubleValue >= testValue;
					
				case LESS_THAN:
					return doubleValue < testValue;
				
				case LESS_THAN_EQUAL_TO:
					return doubleValue <= testValue;
					
				case IN:
				case EQUAL:
				
				}
			}
			catch (ClassCastException cce)
			{
				cce.printStackTrace();
				return false;
			}
		}
		
		return false;
	}

	public String getPropertyKey()
	{
		return m_propertyKey;
	}
	
	public Type getConditionType()
	{
		return m_conditionType;
	}
	
	public String getValue()
	{
		return m_value;
	}
}
