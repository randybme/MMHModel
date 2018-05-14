package edu.bu.zaman.MMHModel.Simulator;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 
 *
 */

public class Simulator 
{
	
	public static HashMap<String, Object> patientList = new HashMap<String, Object>();
	
	static List<String> jsonList = new ArrayList<String>();
	
	/**
	 * Filename of the setup excel spreadsheet stored in the project root folder.
	 */
	private static final String SETUP_FILENAME = "setup.xlsx";
	
	/**
	 * Filename of the json output data stored in the outputs folder.
	 */
	private static final String OUTPUT_FILENAME = "model_data.json";
	
	/**
	 * The number of replicate simulations that should be run for each simulation condition.
	 */
	private static final int NUM_REPLICATES = 50;
	
	/**
	 * Provides an absolute filepath to the setup file containing all of the model parameters
	 * and trajectory information.
	 * 
	 * @return the absolute filepath to the setup file
	 */
	public static String getSetupFilepath()
	{
		String currentDir = System.getProperty("user.dir");
        String filepath = Paths.get(currentDir, SETUP_FILENAME).toString();
        
        return filepath;
	}
	
	/**
	 * Provides an absolute path to the output directory where all of the model output files
	 * are stored.
	 * 
	 * @return the absolute path to the model's output directory
	 */
	public static String getOutputDirectory()
	{
		String currentDir = System.getProperty("user.dir");		
        String outputDir = Paths.get(currentDir, "outputs").toString();
        
        return outputDir;
	}
	
	/**
	 * Provides an absolute filepath to use for the output file containing all of the model
	 * data for the current run.
	 * 
	 * @return the absolute filepath to the setup file
	 */
	public static String getOutputFilepath() 
	{
		String timestamp = new SimpleDateFormat("YYMMdd_HHmmss").format(new Date());
        String filepath = Paths.get(getOutputDirectory(), timestamp + "_" + OUTPUT_FILENAME).toString();
        
        return filepath;
	}
		
	/***********************************************************MODEL**************************************************************
	 * The main run loop for the model.
	 * @param args
	 */
    public static void main(String[] args)
    {
    	// Make sure the output directory for the model data exists
		File outputDirectory = new File(getOutputDirectory());
		if (!outputDirectory.exists())
		{
			outputDirectory.mkdir();
		}
    			
		for (int replicate = 0; replicate < NUM_REPLICATES; replicate++)
		{
	    	// Stock the hospital with the initial number of human resources
	    	Hospital.setNursesOnShift(10);
			Hospital.setDoctorsOnShift(10);
	    	
	        ArrayList<Patient> deceasedPatients = new ArrayList<>();
	
	        // Initiate data logging for initial cycle
	        ModelLogger.newCycle();
	        
			double probabilityNewPatient = 0.1; // The probability of acquiring a new patient
			int totalCycles = 8640; // Number of cycles to run simulation, 1 cycle is 15 minutes		
			
	        // Iterates through cycles of 15 minutes
			for (int cycle = 0; cycle < totalCycles; cycle++)
	        {           
	            /////////////////////////////////////////////////////////////////////////////////
	            // 1. Take in additional patients
	            //
	            // New patients are randomly admitted to the hospital. Each patient is created
	            // by Shiva with a random age and set of conditions.
	            /////////////////////////////////////////////////////////////////////////////////
	                        
				System.out.println("Cycle " + cycle + ", " + Hospital.currentPatients.size() + " patients");
				
				// Determine if a new patient has arrived
				double newPatient = Math.random();		
				if (newPatient <= probabilityNewPatient)
				{
	                // Create a new patient with a random age and set of conditions
	                Patient patient = Shiva.createPatient(18, 45);
	                Hospital.currentPatients.add(patient);
	
					// Log the new patient
					ModelLogger.logPatient(patient);
					
	                // TODO: Implement ability to add multiple patients at once
				} 
	
	            /////////////////////////////////////////////////////////////////////////////////
	            // 2. Allocate resources to patients and treat them
	            //
	            // The current list of patients are sorted from most severe to least and are
	            // assigned human and material resources required for treatment. The patient's
	            // treatment is based on their current stage and associated treatment plan.
	            // The patient's current stage is updated every cycle by the StageManager, who
	            // evaluates the patient's current state and list of conditions. This logic is all
				// handled by the Hospital object.
	            /////////////////////////////////////////////////////////////////////////////////
				
				Hospital.attemptTreatments(Hospital.currentPatients); 
	            
	            /////////////////////////////////////////////////////////////////////////////////
	            // 3. Evaluate patient health and update current patient list
	            //
				// Determine if any patient deaths will occur based on the patient probability
	            // of mortality and a random number draw. Remove any deceased patients from the list 
				// of current patients and notify the hospital of the deaths so that any human
				// resources associated with the patients can be made available for other treatments.
	            /////////////////////////////////////////////////////////////////////////////////
	
				// Signal the beginning of a new logging cycle to the ModelLogger
				ModelLogger.newCycle();
				
	            Iterator<Patient> iterator = Hospital.currentPatients.iterator();
	            while (iterator.hasNext())
	            {
	                Patient patient = iterator.next();
	                StageManager.Stage stage = patient.getStage();
	                
	                // Determine whether the patient is ready for discharge and remove them from
	                // the current patients array if they are
	                if (stage.equals(StageManager.Stage.Complete))
	                {
	                	iterator.remove(); // Remove the patient if they are ready for discharge
	                }
	                else
	                {
	                	double variate = Math.random();
	                	double pom = patient.probabilityOfMortality();
	                	int remainingStay = StageManager.getRemainingStay(patient);
	                	
	                	// As the probability of mortality is typically considered the probability of dying
	                	// over a patient's full hospital stay, the probability of dying at any given cycle, pᵢ,
	                	// is actually much smaller and is governed by the binomial distribution. In other
	                	// words, the probability of mortality is actually a cumulative probability of dying
	                	// at all cycles.
	                	//
	                	// pom = sum(1 ≤ k ≤ total cycles) : Pr(k;n,pᵢ) = permutations(n,k) * (pᵢ^k) * (1-pᵢ)^(n-k)
	                	//
	                	// This is equivalent to 1 - Pr(0;n,pᵢ). This simplifies to pom = 1 - (1 - pᵢ)^n.
	                	// Solving for pᵢ gives pᵢ = 1 - (1 - pom)^(1/n).
	                	double cyclePOM = 1 - Math.pow(1 - pom, 1.0 / remainingStay);
	                	
	                	// TODO: Incorporate results from Benoit's near miss study
	                	// Determine whether current patient remains alive
	                	if (remainingStay > 0 && variate <= cyclePOM)
		                {
	                		System.out.println("Patient died with cyclePOM of " + cyclePOM + " and POM of " + pom + " remaining stay: " + remainingStay);
	                		
		                    patient.die();
		                    Hospital.notifyPatientDeath(patient);
		                    
		                    // Add patient to running list of deceased patients and remove from the list of
		                    // current patients
		                    deceasedPatients.add(patient);
		                    iterator.remove();
					 	}
					 	else
		                {
						 	Shiva.reassessConditions(patient);
		                }
	                }
				 	
				 	// Log the patient state at the end of the cycle
				 	ModelLogger.logPatient(patient);
				 }
	
				 /*
	
				 4 CHECK TIME + UPDATE RESOURCES
	
				 */
	
				 // TODO: Update staff resources based on day/night shifts
			}
			
			// Write the log data to file
			try
			{
				ModelLogger.writeLog(getOutputFilepath());
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			
			ModelLogger.reset();
		}
    }
}
