package edu.bu.zaman.MMHModel;

public class Stage
{
    /**
     * The treatment plan associated with this stage.
     */
    private TreatmentPlan m_treatmentPlan;
    
    /**
     * Creates a new hospital stage for a patient trajectory.
     *
     * @param plan  the treatment plan associated with the hospital stage
     */
    public Stage(TreatmentPlan plan)
    {
        m_treatmentPlan = plan;
    }
    
    /**
     * {@link Stage#m_treatmentPlan}
     */
    public TreatmentPlan getTreatmentPlan()
    {
        return m_treatmentPlan;
    }
    
    /**
     * Compares two stages to see if they are equal according to their treatment plans
     */
    public boolean equals(Stage stage)
    {
        if (stage == null)
        {
            return false;
        }
        
        return m_treatmentPlan.getID() == stage.getTreatmentPlan().getID();
    }
}
