package edu.bu.zaman.MMHModel;

import java.util.ArrayList;

public class Patient
{
    /**
     * A unique identifier for the patient.
     */
    private int m_patientId;
    
	/**
     * The age of the patient.
	 */
	private int m_age;
    
    /**
     * Whether or not the patient is alive.
     */
	private boolean m_isAlive;
    
    /**
     * List of conditions the patient has.
     */
	private ArrayList<Condition> m_conditions;

    /**
     * The current stage for the patient
     */
    public Stage m_stage;
    
    /**
     * Using number of Doctors
     */
    private int doctors_using;
   
    /**
     * Using number of Nurses
     */
    private int nurses_using;
    
    /**
     * Using number of Doses
     */
   private Double doses_using;
    
    
    
	/**
	 * Creates a new patient.
     *
     * @param patientId     a unique identifier for the patient
     * @param age           the age of the patient
     * @param conditions    a list of conditions the patient has
	 */
	public Patient(int patientId, int age, ArrayList<Condition> conditions)
	{
        m_patientId = patientId;
		m_age = age;
        m_isAlive = true;

		if (conditions == null)
		{
			m_conditions = new ArrayList<>();
		}
		else
		{
			m_conditions = conditions;
		}
	}

	/**
	 * Returns whether or not the patient has a condition of a particular type.
	 * 
	 * @param type the type of condition that is to be checked for
	 * @return whether the patient has the specified condition type
	 */
	public boolean hasCondition(Condition.Type type)
	{
		for (Condition condition : m_conditions)
		{
			if (condition.getType() == type)
			{
				return true;
			}
		}
		
		return false;
	}
	
    /**
     * Returns the probability of mortality for the patient, accounting for all of the patient's
     * individual conditions, assuming that each condition is independent of all other conditions.
     * This is performed by evaluating the total probability of surviving, which is given by the
     * product of the complements for the probability of mortality for each individual condition.
     *
     * As an example, consider a patient with two conditions, each with a probability of mortality
     * of 20%. In the case, for outcomes are possible: (1) the patient survives, (2) the patient
     * dies from condition A, (3) the patient dies from condition B, (4) the patient dies from
     * both conditions. To evaluate the total probability associated with the patient dying, we
     * can focus on just the probability of surviving. In this example, the probability of surviving
     * condition A is 80% and the probability of surviving condition B is also 80%; the probability
     * of suriving both conditions, assuming they are independent, is then {@code 0.8 * 0.8}, or
     * 64%. The total probability of mortality can then be calculated as 32%.
     */
	public double probabilityOfMortality()
	{
		double probabilityOfSurvival = 1;
		for (Condition condition : m_conditions)
		{
			probabilityOfSurvival *= 1 - condition.getProbabilityOfMortality();
		}

		return 1 - probabilityOfSurvival;
	}
    
    /**
     * {@link Patient#m_patientId}
     */
    public int getPatientId()
    {
        return m_patientId;
    }
    
    /**
     * {@link Patient#m_age}
     */
    public int getAge()
    {
        return m_age;
    }
    
    /**
     * Returns a list of all of the patient conditions.
     */
    public ArrayList<Condition> getConditions()
    {
        return m_conditions;
    }
    
    /**
     * Returns a specific condition for the patient or null if the patient does not have a condition
     * that matches the requested condition type.
     *
     * @param type the type of the condition that is being requested
     */
	public Condition getCondition(Condition.Type type)
    {
        for (Condition condition : m_conditions)
        {
            if (condition.getType() == type)
            {
                return condition;
            }
        }
        
		return null;
	}
    
    /**
     * {@link Patient#m_stage}
     */
    public Stage getStage()
    {
        return m_stage;
    }
    
    /**
     * {@link Patient#m_stage}
     */
    public void setStage(Stage stage)
    {
        m_stage = stage;
    }
    
    /**
     * Returns true if the patient is still alive and false otherwise.
     */
	public boolean isAlive()
    {
		return m_isAlive;
	}

    /**
     * Kills the patient.
     */
	public void die()
    {
		m_isAlive = false;
	}
	
	
	public void setDoctor(int Doctor) 
	{
		doctors_using = doctors_using + Doctor;
	}
	/******Cumulative**********/
	
	
	public int getDoctor() 
	{
		return doctors_using;
	}
	
	
	public void setNurse(int Nurse) 
	{
		nurses_using = nurses_using + Nurse;
	}
	/******Cumulative**********/
	
	
	public int getNurse() 
	{
		return nurses_using;
	}
    
	
	public void setDoses(Double Doseage) 
	{
		doses_using = Double.valueOf(doses_using) + Double.valueOf(Doseage);
	}
	
	
	public Double getDoses() 
	{
		return doses_using;
	}
	
	
	
	
	
	
	
    @Override
    public String toString()
    {
        String value = "Patient id=" + getPatientId() + ", age=" + getAge() + ", ";
        
        for (Condition condition : getConditions())
        {
            value += condition.toString() + ", ";
        }
            
        return value;
    }
    
    
    public String toStringCondition() {
    	
    	String value = "";
    	
        for (Condition condition : getConditions())
        {
            value += condition.toString() + ", ";
        }
            
        return value;
    	
    }
}
