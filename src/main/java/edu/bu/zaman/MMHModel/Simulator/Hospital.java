package edu.bu.zaman.MMHModel.Simulator;

import java.util.*;

public class Hospital
{
    public enum MaterialResource
    {
        NONE,    	
    	ABSORBENT_GAUZE,
    	ADRENALINE,
    	AMOXICILLIN,
        ATROPINE_SO4,
        BALLOON_CATHETER,
        BT_SET,
        CANULAR,
        CEFTRIAXONE,
        CHLORINE,
        CHROMIC_CUTGUT_2_0,
        CHROMIC_CUTGUT_2,
        CHROMIC_CUTGUT_1,
        CHROMIC_CUTGUT_2_TAPER,
        COATED_PGA,
        COTTON_WOOL,
        DETTOL,
        DEXAMETHASONE,
        DIAZEPAM,     
        DICLOFENAC_NA_LIQ,
        DICLOFENAC_NA_TAB,
        ERYTHROMYCIN,
        FESO4_FOLIC,
        GENTAMYCIN,
        GLOVES_LATEX,
        GLOVES_SURGICAL,
        HYDRALAZINE, 
        HYDROCORTISONE,
        IV_SET,
        LIGNOCAINE,
        METH_SPIRIT,
        METHYLDOPA,
        METRONIDAZOLE_LIQ,       
        METRONIDAZOLE_TAB,        
        MG_SO4,
        MISOPROSTOL,
        NACL_DEXTROSE,
        NACL,
        NIFEDIPINE,
        OXYTOCIN,
        PARACETAMOL,
        POVIDINE,
        SCALP_BLADE,
        SILK_BRAIDED_2,
        SILK_BRAIDED_0,
        SODIUM_LACTATE_RINGER,
        SPINAL_NEEDLE,
        SYRINGES_2CC,
        SYRINGES_5CC,
        SYRINGES_10CC,
        TRAMADOL,
        UMB_CLAMP,
        URINE_COLLECTION_BAG,
        WATER,        
        ZNO_PLASTER    
    }
    
    /**
     * The total number of nurses that are currently on shift.
     */
    private static int m_totalNurses = 0;
    
    /**
     * The total number of doctors that are currently on shift.
     */
    private static int m_totalDoctors = 0;
    
    /**
     * The number of nurses currently available for monitoring and procedures
     */
    private static int m_nurses = 0;
    
    /**
     * The number of doctors currently avaiable for procedures
     */
    private static int m_doctors = 0;
    
    /**
     * Stores the current inventory of disposable material resources
     */
    private static HashMap<MaterialResource, Double> m_totalDisposableResources;
    
    /**
     * Store the most recent treatment status for each admitted patient
     */
    private static HashMap<Integer, Boolean> m_patientTreatmentStatus;
    
    @SuppressWarnings("unused")
    private static Hospital m_instance = new Hospital(); // Eager creation of singleton Hospital object
    private Hospital()
    {
        m_totalDisposableResources = new HashMap<>();
        m_patientTreatmentStatus = new HashMap<>();
        
        // Load initial inventory of disposable resources
        stockDisposableResource(MaterialResource.ABSORBENT_GAUZE, 2000);
        stockDisposableResource(MaterialResource.ADRENALINE, 0);
        stockDisposableResource(MaterialResource.AMOXICILLIN, 500000);
        stockDisposableResource(MaterialResource.ATROPINE_SO4, 250);
        stockDisposableResource(MaterialResource.BALLOON_CATHETER, 600);
        stockDisposableResource(MaterialResource.BT_SET, 0);        
        stockDisposableResource(MaterialResource.CANULAR, 500);
        stockDisposableResource(MaterialResource.CEFTRIAXONE, 506);
        stockDisposableResource(MaterialResource.CHLORINE, 5);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2_0, 600);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2, 0);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_1, 360);
        stockDisposableResource(MaterialResource.CHROMIC_CUTGUT_2_TAPER, 420);
        stockDisposableResource(MaterialResource.COATED_PGA, 600);
        stockDisposableResource(MaterialResource.COTTON_WOOL, 0);
        stockDisposableResource(MaterialResource.DETTOL, 10);
        stockDisposableResource(MaterialResource.DEXAMETHASONE, 300);        
        stockDisposableResource(MaterialResource.DIAZEPAM, 0);
        stockDisposableResource(MaterialResource.DICLOFENAC_NA_LIQ, 4200);
        stockDisposableResource(MaterialResource.DICLOFENAC_NA_TAB, 50000);
        stockDisposableResource(MaterialResource.ERYTHROMYCIN, 0);
        stockDisposableResource(MaterialResource.FESO4_FOLIC, 200000);
        stockDisposableResource(MaterialResource.GENTAMYCIN, 0);
        stockDisposableResource(MaterialResource.GLOVES_LATEX, 12000);
        stockDisposableResource(MaterialResource.GLOVES_SURGICAL, 4000);
        stockDisposableResource(MaterialResource.HYDRALAZINE, 2200);
        stockDisposableResource(MaterialResource.HYDROCORTISONE, 0);
        stockDisposableResource(MaterialResource.IV_SET, 1000);
        stockDisposableResource(MaterialResource.LIGNOCAINE, 2500);
        stockDisposableResource(MaterialResource.METH_SPIRIT, 15);
        stockDisposableResource(MaterialResource.METHYLDOPA, 700000);
        stockDisposableResource(MaterialResource.METRONIDAZOLE_LIQ, 300000);
        stockDisposableResource(MaterialResource.METRONIDAZOLE_TAB, 2000000);
        stockDisposableResource(MaterialResource.MG_SO4, 15000);
        stockDisposableResource(MaterialResource.MISOPROSTOL, 2000000);
        stockDisposableResource(MaterialResource.NACL, 312000);
        stockDisposableResource(MaterialResource.NACL_DEXTROSE, 60000);
        stockDisposableResource(MaterialResource.NIFEDIPINE, 0);
        stockDisposableResource(MaterialResource.OXYTOCIN, 15000);
        stockDisposableResource(MaterialResource.PARACETAMOL, 2000000);
        stockDisposableResource(MaterialResource.POVIDINE, 40);
        stockDisposableResource(MaterialResource.SCALP_BLADE, 500);
        stockDisposableResource(MaterialResource.SILK_BRAIDED_2, 60);
        stockDisposableResource(MaterialResource.SILK_BRAIDED_0, 120);
        stockDisposableResource(MaterialResource.SODIUM_LACTATE_RINGER, 600000);
        stockDisposableResource(MaterialResource.SPINAL_NEEDLE, 250);
        stockDisposableResource(MaterialResource.SYRINGES_2CC, 1800);
        stockDisposableResource(MaterialResource.SYRINGES_5CC, 2800);
        stockDisposableResource(MaterialResource.SYRINGES_10CC, 0);
        stockDisposableResource(MaterialResource.TRAMADOL, 100000);
        stockDisposableResource(MaterialResource.UMB_CLAMP, 1000);
        stockDisposableResource(MaterialResource.URINE_COLLECTION_BAG, 250);
        stockDisposableResource(MaterialResource.WATER, 250);
        stockDisposableResource(MaterialResource.ZNO_PLASTER, 0);
    }
    
    /**
     * Provides the specified amount of a disposable resource to the hospital to stock
     * in it's disposable resource inventory.
     * 
     * @param resource  the disposable resource to stock
     * @param amount    the amount (in mg) to stock for the disposable resource
     */
    public static void stockDisposableResource(MaterialResource resource, double amount)
    {
        // Check to make sure the amount is positive
        if (amount <= 0)
        {
            return;
        }
        
        // Update the inventory for the specified resource depending on whether or not
        // the specified resource is already in stock
        if (m_totalDisposableResources.containsKey(resource))
        {
            double currentAmount = m_totalDisposableResources.get(resource);
            m_totalDisposableResources.put(resource, currentAmount + amount);
        }
        else
        {
            m_totalDisposableResources.put(resource, amount);
        }
    }
    
    /**
     * Attempts to treat the specified patients based on the available hospital resources
     * and their patient treatment plans.
     * 
     * @param patients 	the list of patients to treat
     */
    public static void attemptTreatments(ArrayList<Patient> patients)
    {
    	// Split the list of patients into those that already have active nurses and doctors assigned
    	// to them and those that do not. Patients that have active nurses and doctors shoudl be
    	// treated first, as if the material resources to treat those patients are not avaiable, they
    	// they shoud be returned to the hospital pool to be used to treat other patients.
    	
    	ArrayList<Patient> patientsWithStaff = new ArrayList<>();
    	ArrayList<Patient> otherPatients = new ArrayList<>();
    	
    	for (Patient patient: patients)
    	{
    		int staff = patient.getActiveNurses() + patient.getActiveDoctors();
    		if (staff > 0)
    		{
    			patientsWithStaff.add(patient);
    		}
    		else
    		{
    			otherPatients.add(patient);
    		}
    	}
    	
    	// Sort the patients according to their current probability of mortality from most
        // severe to least severe
    	Comparator<Patient> severityComparator = new Comparator<Patient>()
        {
            @Override
            public int compare(Patient patient1, Patient patient2)
            {
            	return Double.compare(patient2.probabilityOfMortality(), patient1.probabilityOfMortality());
            }
        };
        
		Collections.sort(patientsWithStaff, severityComparator);		
		Collections.sort(otherPatients, severityComparator);
		
		// Attempt to treat patients with active nurses and/or doctors first
		for (Patient patient : patientsWithStaff)
        {    	
			boolean treated = attemptTreatment(patient);
			
			// Update patient treatment status based on whether the patient was actually treated
			m_patientTreatmentStatus.put(patient.getPatientId(), treated);
        }
				
		// Attempt to treat remaining patients
		for (Patient patient : otherPatients)
        {    	
			boolean treated = attemptTreatment(patient);
			
			// Update patient treatment status based on whether the patient was actually treated
			m_patientTreatmentStatus.put(patient.getPatientId(), treated);
        }
		
		// Return hospital human resources once all patients have been treated
		for (Patient patient : patients)
        { 
			StageManager.Stage stage = patient.getStage();
            TreatmentPlan plan;
			
            if ((stage = patient.getStage()) != null && (plan = stage.getTreatmentPlan()) != null)
            {
            	int activeNurses = patient.getActiveNurses();
            	int activeDoctors = patient.getActiveDoctors();
            	int nursesNextCycle = plan.requiredNurses();
            	int doctorsNextCycle = plan.requiredDoctors();
            	
            	// Return nurses to hospital pool if the same number of nurses are not required
            	// for the patient's next treatment cycle.
            	if (nursesNextCycle != activeNurses)
            	{
            		m_nurses += activeNurses;
            		patient.setActiveNurses(0);
            	}
            	
            	// Return doctors to hospital pool if the same number of doctors are not required
            	// for the patient's next treatment cycle.
            	if (doctorsNextCycle != activeDoctors)
            	{
            		m_doctors += activeDoctors;
            		patient.setActiveDoctors(0);
            	}
            }
        }
		
		// Make sure that the maximum number of available doctors and nurses does not exceed the
		// number that should currently be on shift due to any shift changes.
		
		m_nurses = Math.min(m_nurses, m_totalNurses);
		m_doctors = Math.min(m_doctors, m_totalDoctors);
    }
    
    /**
     * Attempts to treat a specific patient based on the required hospital staff and material resources
     * specified by the patient's treatment plan. If the required resources are available, the patient is
     * treated. If not, the patient's conditions are worsened, and any active nurses and/or doctors are
     * returned to the hospital resource pool.
     * 
     * @param patient	the patient to treat
     * @return			whether or not the patient was ssuccessfully treated
     */
    private static boolean attemptTreatment(Patient patient)
    {
    	// Flag used to indicate whether the hospital has the appropriate resources to treat the patient
		boolean resourcesAvailable = true;
		
		// Get the stage the patient should be in from the StageManager
		StageManager.Stage stage = StageManager.getStage(patient);
		if (stage != null)
        {
            // Update the stage if needed
			if (!stage.equals(patient.getStage()))
            {
				patient.setStage(stage);
			}
			
			// Check it see if the patient is ready for discharge and skip treatment if so
			if (stage.equals(StageManager.Stage.Complete))
			{
				return true;
			}
        
            // Get treatment plan for the patient's current stage and determine if the required
            // resources for treatment in the current cycle are available
            TreatmentPlan plan = patient.getStage().getTreatmentPlan();
        	            	            
            HashMap<Hospital.MaterialResource, Double> requiredMaterialResources = plan.requiredMaterialResources();	
            for (Hospital.MaterialResource resource : requiredMaterialResources.keySet())
            {
                if (resource != Hospital.MaterialResource.NONE && !Hospital.isAvailable(resource, 
                		requiredMaterialResources.get(resource)))
                {
                    resourcesAvailable = false;
                    break;
                }
            }
            
            int activeNurses = patient.getActiveNurses();
            int activeDoctors = patient.getActiveDoctors();	            
            int requiredNurses = plan.requiredNurses() - activeNurses;
            int requiredDoctors = plan.requiredDoctors() - activeDoctors;
            
            if (Hospital.m_nurses < requiredNurses || Hospital.m_doctors < requiredDoctors)
            {
                resourcesAvailable = false;
            }
        
            // Request resources from the hospital based on the treatments for the current stage
            // if the resources are available	            
            if (resourcesAvailable)
            {
                // Consume the required material resources for the treatment plan from the hospital
                // inventory
            	for (Hospital.MaterialResource resource : requiredMaterialResources.keySet())
                {  
            		Hospital.consumeResource(resource, requiredMaterialResources.get(resource));
                }
            		                    
                // Check out the number of nurses and doctors required for treatment in the current cycle
                m_nurses -= requiredNurses;	               
                m_doctors -= requiredDoctors;
                
                // Assign the appropriate number of nurses and doctors to the patient for
                // the current treatment.
                patient.setActiveNurses(requiredNurses + activeNurses);
                patient.setActiveDoctors(requiredDoctors + activeDoctors);
                
                // Treat the patient, updating his or her probability of mortality and treatment
                // status to indicate they were treated
                plan.treatPatient();
                
                //System.out.println("Treating patient " + patient.getPatientId());
                
                return true;
            }
        }
        
        // Worsen the patient's current conditions as the patient could not be treated
    	// and return any human resources the patient already has allocated
    	m_nurses += patient.getActiveNurses();
    	m_doctors += patient.getActiveDoctors();
    	
    	patient.setActiveNurses(0);
    	patient.setActiveDoctors(0);	        	
    	
        for (Condition condition : patient.getConditions())
        {
            condition.worsen();
        }
                
        //System.out.println("Unable to treat patient " + patient.getPatientId());
        
        return false;
    }
    
    /**
     * Notifies the hospital that the provided patient has passed away, allowing the
     * hospital to reclaim any human resources that were still supporting the patient
     * until his/her death.
     * 
     * @param patient	the patient that passed away
     */
    public static void notifyPatientDeath(Patient patient)
    {
    	m_nurses += patient.getActiveNurses();
    	m_doctors += patient.getActiveDoctors();
    	
    	patient.setActiveNurses(0);
    	patient.setActiveDoctors(0);
    }
    
    /**
     * Return a patient's most recent treatment status in the hospital. This is updated
     * every time a treatment is attempted for the patient.
     * 
     * @param patient	the patient in question
     * @return
     */
    public static boolean didTreat(Patient patient)
    {
    	Boolean result = m_patientTreatmentStatus.get(patient.getPatientId());
    	if (result == null)
    	{
    		return false; 
    	}
    	
    	return result; 
    }
    
    /**
     * Returns whether the specified resource is available at the requested dose
     *
     * @param resource  the disposable resource being requested
     * @param dose      the amount (in mg) that is being requested
     */
    private static boolean isAvailable(MaterialResource resource, double dose)
    {
        if (!m_totalDisposableResources.containsKey(resource))
        {
            return false;
        }
        
        return (m_totalDisposableResources.get(resource) >= dose);
    }
    
    /**
     * Requests and consumes the specified dose of a particular material resource from the
     * hospital stock.
     *
     * @param resource  the disposable resource being requested
     * @param dose      the amount (in mg) that is being requested and consumed
     */
    private static void consumeResource(MaterialResource resource, double dose)
    {
        // Check to make sure the disposable resource being requested is in stock
        if (!isAvailable(resource, dose))
        {
            return;
        }
        
        double currentAmount = m_totalDisposableResources.get(resource);
        currentAmount -= dose;
        
        m_totalDisposableResources.put(resource, currentAmount);
    }
    
    /**
     * Set the total number of nurses that should currently be on shift.
     * @param nurses
     */
    public static void setNursesOnShift(int nurses)
    {
    	int additionalNurses = nurses - m_totalNurses;
    	if (additionalNurses > 0)
    	{
    		// Only add additional doctors the current number of available doctors
    		// if new doctors are coming on shift.
    		m_nurses += additionalNurses;
    	}
    	
    	m_totalNurses = nurses;
    }
    
    /**
     * Set the total number of doctors that shoudl currently be on shift.
     * @param doctors
     */
    public static void setDoctorsOnShift(int doctors)
    {
    	int additionalDoctors = doctors - m_totalDoctors;
    	if (additionalDoctors > 0)
    	{
    		// Only add additional doctors the current number of available doctors
    		// if new doctors are coming on shift.
    		m_doctors += additionalDoctors;
    	}
    	
    	m_totalDoctors = doctors;
    }
    
    /**
     * Returns the current number of nurses available to see patients.
     * 
     * @return
     */
    public static int availableNurses()
    {
    	return m_nurses;
    }
    
    /**
     * Returns the current number of doctors available to see patients.
     * @return
     */
    public static int availableDoctors()
    {
    	return m_doctors;
    }
    
    /**
     * Returns the current stock of hospital disposable resources. 
     * @return
     */
    public static HashMap<MaterialResource, Double> getHospitalStock() 
    {
    	return m_totalDisposableResources;
    }
}
