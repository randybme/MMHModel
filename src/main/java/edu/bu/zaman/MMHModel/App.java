package edu.bu.zaman.MMHModel;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 *
 */


public class App 
{
	
	public static HashMap<String, Object> patientList = new HashMap<String, Object>();
	
	static List<String> jsonList = new ArrayList<String>();
	



	
	
	/**
	 * Filename of the setup excel spreadsheet stored in the project root folder.
	 */
	private static final String SETUP_FILENAME = "setup.xlsx";
	
	private static final String json_FILENAME = "patientlist.json";
	
	private static final String Spreadsheet_FILENAME = "patientlist.csv";
	
	private static final String ser_FILENAME = "data.ser";
	
	/**
	 * Provides an absolute filepath to the setup file containing all of the model parameters
	 * and trajectory information.
	 * 
	 * @return the absolute filepath to the setup file
	 * 
	 * json/csv/ser output files
	 *
	 */
	
	
	
	public static String getSetupFilepath()
	{
		String currentDir = System.getProperty("user.dir");
        String filepath = Paths.get(currentDir, SETUP_FILENAME).toString();
        
        return filepath;
	}
	
	
	
	public static String pointJSONfilepath() {
		
		String currentDir = System.getProperty("user.dir");
        String filepath = Paths.get(currentDir, json_FILENAME).toString();
        
        return filepath;
	}
	
	
	
	public static String getSpreadsheetFilepath()
	{
		String currentDir = System.getProperty("user.dir");
        String filepath = Paths.get(currentDir, Spreadsheet_FILENAME).toString();
        
        return filepath;
	}
	
	public static String getserFilepath()
	{
		String currentDir = System.getProperty("user.dir");
        String filepath = Paths.get(currentDir, ser_FILENAME).toString();
        
        return filepath;
	}
	
	
	/***********************************************************MODEL**************************************************************
	 * The main run loop for the model.
	 * update collection after every var
	 * @param args
	 * @throws IOException 
	 */
    public static void main( String[] args ) throws IOException
    {
    	
    	String spreadsheetPath = getSpreadsheetFilepath();
    	FileWriter writer = new FileWriter(spreadsheetPath, true);
    	
    	Collector.writeLine(writer, Arrays.asList("ID", "Age", "Condition", "POM", "Survival"));
    	   	
    	
    	
        ArrayList<Patient> currentPatients = new ArrayList<>();
        ArrayList<Patient> deceasedPatients = new ArrayList<>();
        
        
        
        
		double probabilityNewPatient = 0.41; // The probability of acquiring a new patient
		int totalCycles = 10; // Number of cycles to run simulation, 1 cycle is 15 minutes

		Hospital.nurses = 10;
		Hospital.doctors = 10;
		//TODO export
		
		
        // Iterates through cycles of 15 minutes
		for (int cycle = 0; cycle < totalCycles; cycle++)
        {
            
            /////////////////////////////////////////////////////////////////////////////////
            // 1. Take in additional patients
            //
            // New patients are randomly admitted to the hospital. Each patient is created
            // by Shiva with a random age and set of conditions.
            /////////////////////////////////////////////////////////////////////////////////
            
            // Determine if a new patient has arrived
			System.out.println("Cycle " + cycle + ", " + currentPatients.size() +  " patients");
			double newPatient = Math.random();
		
			
			
			if (newPatient <= probabilityNewPatient);
			//if (cycle == 1)
			{
                // Create a new patient with a random age and set of conditions
                // LIMITATION: Can only add 1 patient per cycle
                Patient p = Shiva.createPatient(18, 45);
				currentPatients.add(p);

				
				
									

               
                //System.out.println("Creating patient: " + p.toString());
                

			} 

            /////////////////////////////////////////////////////////////////////////////////
            // 2. Allocate resources to patients and treat them
            //
            // The current list of patients are sorted from most severe to least t and are
            // assigned human and material resources required for treatment. The patient's
            // treatment is based on their current stage and associated treatment plan.
            // The patient's current stage is updated every cycle by the StageManager, who
            // evaluates the patient's current state and list of conditions.
            /////////////////////////////////////////////////////////////////////////////////
            
            // Sort the patients according to their current probability of mortality from most
            // severe to least severe
			Collections.sort(currentPatients, new Comparator<Patient>()
            {
                @Override
                public int compare(Patient patient1, Patient patient2)
                {
                	int diff = 0;
                	if ((patient2.probabilityOfMortality() - patient1.probabilityOfMortality()) < 0)
                	{
                		diff = -1;
                	}
                	else if  (((patient2.probabilityOfMortality() - patient1.probabilityOfMortality()) > 0))
                	{
                		diff = 1;
                	}
                return diff;
                }
            });
            

            // Update the patient's stage, if needed, and administer cycle treatment
			for (Patient patient : currentPatients)
            {
				// Flag used to indicate whether the hospital has the appropriate resources to treat the patient
				boolean resourcesAvailable = true;
				
				// Get the stage the patient should be in from the StageManager
				Stage stage = StageManager.getStage(patient);
				if (stage != null)
                {
	                // Update the stage if needed
					if (!stage.equals(patient.getStage()))
	                {
						patient.setStage(stage);
					}
	            
	                // Get treatment plan for the patient's current stage and determine if the required
	                // resources for treatment in the current cycle are available
	                TreatmentPlan plan = patient.getStage().getTreatmentPlan();
                
	                HashMap<Hospital.MaterialResource, Double> requiredMaterialResources = plan.requiredMaterialResources();
	                
	                int requiredNurses = plan.requiredNurses();
	                int requiredDoctors = plan.requiredDoctors();
	
	                for (Hospital.MaterialResource resource : 
	                	requiredMaterialResources.keySet())
	                {
	                    if (resource != Hospital.MaterialResource.NONE && !Hospital.isAvailable(resource, 
	                    		requiredMaterialResources.get(resource)))
	                    {
	                        resourcesAvailable = false;
	                        //System.out.println("No material resources");
	                        break;
	                    }
	                }
	                System.out.println(patient.toString());
	                System.out.println("Resources for patient " + patient.getPatientId());
	                for (Hospital.MaterialResource resource: requiredMaterialResources.keySet()){
	
	                    String key =resource.toString();
	                    String value = requiredMaterialResources.get(resource).toString();  
	                    System.out.println("\t" + key + " " + value);  
	                }
	                
	                
	                
	                if (Hospital.nurses < requiredNurses || 
	                		Hospital.doctors < requiredDoctors)
	                {
	                    resourcesAvailable = false;
	                    //System.out.println("No staff resources for patient " + patient.getPatientId());
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
	                        //patient.setDoses(requiredMaterialResources.get(resource));
	                    }
	                	
	                    
	                    
	                    // Check out the number of nurses and doctors required for treatment in the current cycle
	                    Hospital.nurses -= requiredNurses;
	                    
	                    //System.out.println(" ");
	                    //System.out.println("Number of nurses required by patient " + patient.getPatientId() + ": " + requiredNurses);
	                    //System.out.println("Number of nurses in the hospital: " + Hospital.nurses);
	                    
	                    Hospital.doctors -= requiredDoctors;
	                    
	                   // System.out.println(" ");
	                   // System.out.println("Number of doctors required by patient " + patient.getPatientId() + ": " + requiredDoctors);
	                    //System.out.println("Number of doctors in the hospital: " + Hospital.doctors);
	                    
	                    patient.setDoctor(requiredDoctors);
	                    patient.setNurse(requiredNurses);
	                    
	           
	
	                    // Treat the patient, updating his or her probability of mortality
	                    plan.treatPatient();
	                    
	                    System.out.println("Treating patient " + patient.getPatientId());
	                }
                }
                else
                {
                	resourcesAvailable = false;
                }
                
                // Worsen the patient's current conditions as the patient could not be treated
                if (resourcesAvailable == false)
                {
                    for (Condition condition : patient.getConditions())
                    {
                        condition.worsen();
                    }
                }
            }

            /////////////////////////////////////////////////////////////////////////////////
            // 3. Evaluate patient health
            //
            // Determine if any patient deaths will occur based on the patient probability
            // of mortality and a random number draw.
            /////////////////////////////////////////////////////////////////////////////////

            
			for (Patient patient : currentPatients)
            {
            	double variate = 0.8;
            	int randDie = 1 + (int)(Math.random() * (4-1+1));
			 	if (variate <= patient.probabilityOfMortality() && (randDie == 1 || randDie == 2)) //NEW
                {
                    patient.die();
			 	}
            }
            
            /////////////////////////////////////////////////////////////////////////////////
            // 4. Update current patient list and return and available resources to the hospital
            //
            // Remove any deceased patients from the list of current patients and return any
            // human resources that are now available after the current treatment cycle.
            /////////////////////////////////////////////////////////////////////////////////


            
            
            Iterator<Patient> iterator = currentPatients.iterator();
            while (iterator.hasNext())
            {
                Patient patient = iterator.next();
                Stage stage = patient.getStage();
                
                if (stage != null)
                {
				 	TreatmentPlan plan = patient.getStage().getTreatmentPlan();
				 	
				 	if (patient.isAlive())
	                {
				 		Shiva.reassessConditions(patient);
				 	}
	                else
	                {
	                    // Add patient to running list of deceased patients and remove from the list of
	                    // current patients
	                    deceasedPatients.add(patient);
	                    iterator.remove();
	                  
				 	}
				 	
				 	
				 	
				 	
				 	
	                 
				 	int freeNurses = plan.freeNursesAfterTreatment();
				 	int freeDoctors = plan.freeDoctorsAfterTreatment();
	                 
				 	Hospital.nurses += freeNurses;
				 	Hospital.doctors += freeDoctors;
	                
				 	//System.out.println(" ");
				 	
				 	//System.out.println("Nurses in the hospital (end of cycle): " + Hospital.nurses);
				 	//System.out.println("Doctors in the hospital (end of cycle): " + Hospital.doctors);
				 	//System.out.println(" ");
                }

			 }

			 /*

			 V CHECK TIME + UPDATE RESOURCES

			 */

			 // CHECK: How many cycles until night shift?
            
            //System.out.println("Cycle " + cycle + ", " + currentPatients.size() + " patients");
            System.out.println(" ");
            System.out.println(" ");
            /**********************************************************JSON STORE!************************************/
           // gsonBuild(Cycle, ID, Age, Survival, DoctorsUsed);
            if (cycle%100 == 0)
            {
            	int cycleRemaining = totalCycles - cycle; 
            	System.out.println("Cycles remaining: = " + cycleRemaining);
            }
            else if (cycle == (totalCycles -1))
            {
            	System.out.println("Run finished.");
            }

            
            for (Patient patient : currentPatients)
            {
                //System.out.println("\t" + patient.toString());
                Collector.writeLine(writer, Arrays.asList(patient.toString(), "Survived"));
                gsonBuild2(patient, cycle);




            }
            
            for (Patient patienter : deceasedPatients) {
            	Collector.writeLine(writer, Arrays.asList(patienter.toString(), "Died"));
            	gsonBuild2(patienter, cycle);
            	
            }
            
          
            
		}
		
		
		
	      try {
	    	  String serpath = getserFilepath();
	          FileOutputStream fileOut = new FileOutputStream(serpath);
	          ObjectOutputStream out = new ObjectOutputStream(fileOut);
	          out.writeObject(patientList);
	          out.close();
	          fileOut.close();
	          System.out.printf("Serialized data is saved in " + System.getProperty("user.dir"));
	       }catch(IOException i) {
	          i.printStackTrace();
	       }
	        
	 
	      
	      
	      
		
        writer.flush();
        writer.close();
        

    }
    
	/******************************************JSON****************************************/
	static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
    
    public static void gsonBuild2(Patient patientObj, int Cycle) {
    	
    	
    	PatientSerialize patientAdd = new PatientSerialize();
    	
    	
    	//POJO Hierarchy
    	//internalize resources into patientObj
    	int IDer = patientObj.getPatientId();
    	int ager = patientObj.getAge();
    	String conditioner = patientObj.toStringCondition();
    	int DoctorsUsed = patientObj.getDoctor();
    	int NursesUsed = patientObj.getNurse();
    	Double DosesUsed = patientObj.getDoses();
    	

    	patientAdd.setCycle(Cycle);
    	patientAdd.setPatientCollected(IDer);
    	patientAdd.setAge(ager);
    	patientAdd.setCondition(conditioner);
    	patientAdd.setDoctors(DoctorsUsed);
    	patientAdd.setNurses(NursesUsed);
    	patientAdd.setDoses(DosesUsed);
    	
    	
    	String json = gson.toJson(patientAdd);
    	
    	String writeToFile = pointJSONfilepath();
    	
    	try (FileWriter writer = new FileWriter(writeToFile, true)){
    		//True to prevent new file
    		gson.toJson(patientAdd, writer);
    	
    	} catch(IOException e) {
    		e.printStackTrace();
    	}

    
    	
    	
    	
    }
	/*********************************Toodle-Loo!********************JSON END*************Toodle-Loo!****************************************/
    

}
