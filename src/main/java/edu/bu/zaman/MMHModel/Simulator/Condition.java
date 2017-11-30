package edu.bu.zaman.MMHModel.Simulator;

public class Condition
{
    /**
     * Describes the list of conditions that are supported.
     */
	public enum Type
    {
        DELIVERY, 
        PREECLAMPSIA,
        ECLAMPSIA,
        SEPSIS,
        PPH,
        ANAEMIA,
        APH,
        RUPTURED_UTERUS,
        ABORTION,
        CARDIOMYOPATHY
    }
    
    /**
     * The condition type describing this condition.
     */
    private Type m_type;
    
    /**
     * The current probability of mortaility associated with the condition.
     */
    private double m_probabilityOfMortality;
    
    /**
     * Describes the rate at which the condition worsens in the absence of appropriate treatment.
     * A number close to zero causes rapid deterioration, while larger numbers slow this rate.
     * The deterioration rate specifically represents {@code r} in the equation 
     * {@code f(c) = p^[r / (c + r)]}, where {@code p} is the probability of mortality and 
     * {@code c} is the current cycle.
     */
    private double m_deteriorationRate;
    
    /**
     * Stores the age of this condition in units of cycles.
     */
    private int m_cycle = 0;
    
    /**
     * Initializes a new condition.
     *
     * @param type                      the type of condition that should be instantiated
     * @param probabilityOfMortality    the initial probability of mortality associated with this condition
     * @param deteriorationRate         the rate at which the probability of mortality increases in the
     *                                  absence of appropriate treatment {@link Condition#m_deteriorationRate
     */
    public Condition(Condition.Type type, double probabilityOfMortality, double deteriorationRate)
    {
        m_type = type;
        m_probabilityOfMortality = probabilityOfMortality;
        m_deteriorationRate = deteriorationRate;
    }
    
    public Condition.Type getType()
    {
        return m_type;
    }
    
    /**
     * {@link Condition#m_probabilityOfMortality
     */
    public double getProbabilityOfMortality()
    {
        return m_probabilityOfMortality;
    }
    
    /**
     * {@link Condition#m_probabilityOfMortality
     */
    public void setProbabilityOfMortality(double probability)
    {
        // Check to make sure the probability is between 0 and 1, exclusively
        if (probability < 0 || probability > 1)
        {
            return;
        }
        
        m_probabilityOfMortality = probability;
        
        // Reset the relative age of this condition when it set manually adjusted. This ensures that
        // if the condition is worsened, it is worsened from this new probabilty.
        m_cycle = 0;
    }
    
    /**
     * Worsens the current condition by increasing its probability of mortality based on the condition's
     * inherent deterioration rate.
     *
     * @see Condition#m_deteriorationRate
     */
    public void worsen()
    {
        m_cycle++;
        m_probabilityOfMortality = Math.pow(getProbabilityOfMortality(), m_deteriorationRate / (m_cycle + m_deteriorationRate));
    }
    
    public String toString()
    {
        return "Condition name=" + getType().name() + ", pom=" + getProbabilityOfMortality();
    }
}
