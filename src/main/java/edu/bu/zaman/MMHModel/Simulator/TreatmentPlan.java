package edu.bu.zaman.MMHModel.Simulator;

import java.util.ArrayList;
import java.util.HashMap;

public class TreatmentPlan
{
    /**
     * The patient associated with this treatment plan.
     */
	private Patient m_patient;

	/**
	 * The ID used to uniquely identify this treatment plan.
	 */
	private int m_id;
	
    /**
     * List of condition types that this treatment treats.
     */
	private ArrayList<Condition.Type> m_conditionTypes;

    /**
     * Stores the efficacy of this treatment plan toward the treatment of each specific condition.
     */
    private HashMap<Condition.Type, Double> m_treatmentEfficacies;
    
    /**
     * Stores the dosags used for each material resource when they administered in accordance with
     * the treatment plan.
     */
    private HashMap<Hospital.MaterialResource, Double> m_materialResourceDosages;
    
    /**
     * Describes in cycles how often each material resource is used throughout the treatment plan.
     * A value of zero indicates that the resource should only be used once at the beginning of the
     * treatment plan. A value of 1 indicates that it should be used every cycle; a value of 2, every
     * other cycle; etc.
     */
    private HashMap<Hospital.MaterialResource, Integer> m_materialResourceFrequencies;
    
    /**
     * The number of nurses needed during active periods of the treatment plan.
     */
	private int m_nursesNeeded;
    
    /**
     * The number of consecutive cycles that a nurse is needed during active periods of the
     * treatment plan.
     */
	private int m_nurseOnTime;
    
    /**
     * The number of consecutive cycles a nurse is not needed during inactive periods of the
     * treatment plan.
     */
	private int m_nurseOffTime;
    
    /**
     * The number of doctors needed during active periods of the treatment plan.
     */
	private int m_doctorsNeeded;
    
    /**
     * The number of consecutive cycles that a doctor is needed during active periods of the
     * treatment plan.
     */
	private int m_doctorOnTime;
    
    /**
     * The number of consecutive cycles a doctor is not needed during inactive periods of the
     * treatment plan.
     */
	private int m_doctorOffTime;   
	
    /**
     * The total length of the treatment plan in units of cycles.
     */
	private int m_totalCycles;

    /**
     * Records how many cycles of treatment have been administered
     */
    private int m_cycle = 0;
    
    /**
     * Creates a new treatment plan.
     *
     * @param patient   the patient associated with this treatment plan
     */
	public TreatmentPlan(
        Patient patient,
        int id,
        ArrayList<Condition.Type> conditionTypes,
        Hospital.MaterialResource[] materialResources,
        double[] treatmentEfficacies,
        double[] materialResourceDosages,
        int[] materialResourceFrequencies,
        int nursesNeeded,
        int nurseOnTime,
        int nurseOffTime,
        int doctorsNeeded,
        int doctorOnTime,
        int doctorOffTime,
        int totalCycles
    )
    {
		m_patient = patient;
		m_id = id;
        m_conditionTypes = conditionTypes;
        
        m_nursesNeeded = nursesNeeded;
        m_nurseOnTime = nurseOnTime;
        m_nurseOffTime = nurseOffTime;
        
        m_doctorsNeeded = doctorsNeeded;
        m_doctorOnTime = doctorOnTime;
        m_doctorOffTime = doctorOffTime;
        
        m_totalCycles = totalCycles;
        
        m_treatmentEfficacies = new HashMap<>();
        m_materialResourceFrequencies = new HashMap<>();
        m_materialResourceDosages = new HashMap<>();
        
        // Store the treatment efficacies internally as a HashMap rather than an array
        if (conditionTypes != null)
        {
	        for (int index = 0; index < conditionTypes.size(); index++)
	        {
	            m_treatmentEfficacies.put(conditionTypes.get(index), treatmentEfficacies[index]);
	        }
	        
	        // Stores the dosages and frequencies as a HashMap rather than as arrays
	        for (int index = 0; index < materialResources.length; index++)
	        {
	            m_materialResourceDosages.put(materialResources[index], materialResourceDosages[index]);
	            m_materialResourceFrequencies.put(materialResources[index], materialResourceFrequencies[index]);
	        }
        }
	}
	
	/**
	 * Accessor for the treatment plan ID.
	 * 
	 * @return the treatment plan ID.
	 */
	public int getID()
	{
		return m_id;
	}
    
    /**
     * Returns a HashMap of material resources and associated dosages that are required for 
     * treatment within the current treatment cycle.
     */
	public HashMap<Hospital.MaterialResource, Double> requiredMaterialResources()
    {
        HashMap<Hospital.MaterialResource, Double> cycleResources = new HashMap<>();
        for (Hospital.MaterialResource resource : m_materialResourceDosages.keySet())
        {
            int frequency = m_materialResourceFrequencies.get(resource);
			if (
                (frequency == 0 && m_cycle == 0) ||
                (frequency != 0 && m_cycle % frequency == 0)
            )
            {
                cycleResources.put(resource, m_materialResourceDosages.get(resource));
			}
		}
        
        return cycleResources;
	}
    
    /**
     * Returns the number of nurses needed from the hospital for the current treatment cycle.
     */
	public int requiredNurses()
    {
        if (humanResourcesRequired(m_cycle, m_nurseOnTime, m_nurseOffTime))
        {
        	return m_nursesNeeded;
        }
        
        return 0;
	}
    
    /**
     * Returns the number of doctors needed from the hospital for the current treatment cycle.
     */
	public int requiredDoctors()
    {
        if (humanResourcesRequired(m_cycle, m_doctorOnTime, m_doctorOffTime))
        {
        	return m_doctorsNeeded;
        }
        
        return 0;      
	}
    
    /**
     * Returns whether a human resource is required with the patient for treatment in the current cycle.
     *
     * @param cycle     the cycle for which the human resource requirements are being requested
     * @param onTime    the number of consecutive cycles the human resource is active for during treatment
     * @param offTime   the number of consecutive cycles the human resource in inactive for after treatment
     */
	private boolean humanResourcesRequired(int cycle, int onTime, int offTime)
    {
		int period = onTime + offTime;
        if (period == 0)
        {
            return false;
        }
        
		int periodCycle = cycle % period;
		if (periodCycle < onTime)
        {
            return true;
        }
            
        return false;
	}
            
    /**
     * Treats the patient for the current treatment cycle and updates the patient's probability of
     * mortality based on the treatment efficacies for each individual condition.
     */
	public void treatPatient()
    {
		double newProbability;           
        for (Condition.Type type: m_conditionTypes)
        {
            Condition patientCondition = m_patient.getCondition(type);
            if (patientCondition == null)
            {
                continue;
            }
            
			newProbability = treatedProbabilityOfMortality(
					patientCondition.getProbabilityOfMortality(), 
					m_treatmentEfficacies.get(type)
			);
            patientCondition.setProbabilityOfMortality(newProbability);
		}
            
        m_cycle++; // Increase the relative cycle, now that treatment has been adminsitered for this patient
    }
	
	/**
	 * Returns the length of the treatment plan in units of cycles.
	 * @return
	 */
	public int getLength()
	{
		return m_totalCycles;
	}
	
	/**
	 * Returns the new probability of mortality after a treatment based on the current probability
	 * of mortality and a treatment efficacy.
	 *  
	 * @param current			the current probability of mortality
	 * @param treatmentEfficacy the treatment efficacy
	 * 
	 * @return the new probability of mortality after treatment
	 */
	private double treatedProbabilityOfMortality(double current, double treatmentEfficacy)
	{
		return (current * (1 - treatmentEfficacy)); // implements a geometric decay;
	}
}
