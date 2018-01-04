package edu.bu.zaman.MMHModel.Simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StageManager
{	
	private class Fields
	{
		public static final int TRAJECTORY_ID = 0;
		public static final int PATIENT_CONDITIONS = 1;
		public static final int SEVERITIES = 2;
		public static final int TREATED_CONDITIONS = 3;
		public static final int MATERIAL_RESOURCES = 4;
		public static final int RESOURCE_EFFICACIES = 5;
		public static final int RESOURCE_DOSAGES = 6;
		public static final int RESOURCE_FREQUENCIES = 7;
		public static final int NURSES = 8;
		public static final int NURSE_ON_TIME = 9;
		public static final int NURSE_OFF_TIME = 10;
		public static final int DOCTORS = 11;
		public static final int DOCTOR_ON_TIME = 12;
		public static final int DOCTOR_OFF_TIME = 13;
		public static final int TOTAL_CYCLES = 14;
		public static final int NEXT_TREATMENT = 15;
	}

	/**
	 * Reference to the patient trajectories excel spreadsheet.
	 */
	private static Sheet m_trajectoriesSheet;
	
	@SuppressWarnings("unused")
	private static StageManager m_instance = new StageManager(); // Eager creation of singleton StageManager object
	private StageManager()
	{
		loadTrajectories();
	}
	 
	/**
	 * Loads the patient trajectories file for use by the StageManager to determine the
	 * appropriate trajectory for a given patient based on their conditions and severities.
	 */
	private void loadTrajectories()
	{
		String filepath = Simulator.getSetupFilepath();        
        System.out.println("Reading patient trajectories from path: " + filepath);
        
        // Open the trajectories workbook and get the first sheet
        try (
    		FileInputStream trajectoriesFile = new FileInputStream(new File(filepath));
	        Workbook workbook = new XSSFWorkbook(trajectoriesFile);
		)
        {        		       
        	// Get the third sheet in the setup workbook, which contains all of the patient
        	// trajectory information
	        m_trajectoriesSheet = workbook.getSheetAt(2);
	        
	        // Make sure to set the cell type of all cells to strings to prevent any
	        // downstream casting problems
	        for (Row row : m_trajectoriesSheet)
	        {
	        	for (Cell cell : row)
	        	{
	        		cell.setCellType(CellType.STRING);
	        	}
	        }
        }
        catch (FileNotFoundException fnfe)
        {
        	fnfe.printStackTrace();
        }
        catch (IOException ioe)
        {
        	ioe.printStackTrace();
        }
	}
	
	/**
	 * Retreives the row number in the trajectories spreadsheet that is associated with
	 * the provided trajectory ID.
	 * 
	 * @param trajectoryId the trajectory id
	 * @return the row in the spreadsheet where the trajectory is located
	 */
	private static Row getRowForId(int trajectoryId)
	{
		return m_trajectoriesSheet.getRow(trajectoryId);
	}
	
	/**
	 * Determines whether the patient's current set of conditions and associated
	 * severities meets the requirements for the specified trajectory.
	 * 
	 * @param patient the current patient
	 * @param trajectory 	the trajectory to test against, provided as a row in the 
	 * 		  				trajectories spreadsheet
	 * @return Whether the trajectory matches the patient's current status
	 */
	public static boolean patientSatisfiesRow(Patient patient, Row trajectory)
	{
		if (trajectory == null)
		{
			return false;
		}
		
		String[] trajectoryConditions = trajectory.getCell(Fields.PATIENT_CONDITIONS).getStringCellValue().split(",");
		String[] conditionSeverities = trajectory.getCell(Fields.SEVERITIES).getStringCellValue().split(",");
		
		// Check to make sure that the patient has the same number of conditions as specified
		// by this trajectory
		
		if (patient.getConditions().size() != trajectoryConditions.length)
		{
			return false;
		}
		
		boolean match = true;		
		for (int index = 0;index < trajectoryConditions.length; index++)
		{
			// Check first to see if the patient has one of the conditions listed in the 
			// current trajectory
			Condition.Type condition = Condition.Type.valueOf(trajectoryConditions[index]);
			Condition patientCondition = patient.getCondition(condition);
			
			//System.out.println("Checking condition: " + condition);
			
			if (patientCondition != null)
			{
				
				// If the patient has the condition, check to see if the patient's severity for
				// the condition matches the requirements of the trajectory
				String[] bounds = conditionSeverities[index].split("-");
				if (bounds.length == 2)
				{
					try
					{
						double min = Double.parseDouble(bounds[0]);
						double max = Double.parseDouble(bounds[1]);
						
						double pom = patientCondition.getProbabilityOfMortality();
						if (pom > min && pom <= max)
						{
							// Continue to check next condition in the trajectory as all of the
							// requirements have been met for this condition
							continue;
						}
					}
					catch (NumberFormatException nfe)
					{
						nfe.printStackTrace();
					}
				}
				else
				{
					System.out.println("Error: invalid severity condition <" + conditionSeverities[index] + ">");
				}
			}
			
			match = false;
			break;			
		}
		
		return match;
	}
	
	/**
	 * Parses a specific trajectory and creates a TreatmentPlan from it.
	 * 
	 * @param patient 		the patient for which the TreatmentPlan should be created
	 * @param trajectory 	the row in the trajectories sheet that should be parsed
	 * 
	 * @return a TreatmentPlan object for the patient
	 */
	private static TreatmentPlan getTreatmentPlan(Patient patient, Row trajectory)
	{
		try
		{
			int id = Integer.parseInt(trajectory.getCell(Fields.TRAJECTORY_ID).getStringCellValue());
			
			String[] conditions = trajectory.getCell(Fields.TREATED_CONDITIONS).getStringCellValue().split(",");
			ArrayList<Condition.Type> conditionTypes = new ArrayList<>(conditions.length);
			for (String condition : conditions)
			{
				Condition.Type type = Condition.Type.valueOf(condition);
				if (type == null)
				{
					throw new Exception("Invalid condition encountered.");
				}
				
				conditionTypes.add(type);
			}
			
			String[] resources = trajectory.getCell(Fields.MATERIAL_RESOURCES).getStringCellValue().split(",");
			Hospital.MaterialResource[] resourceTypes = new Hospital.MaterialResource[resources.length];
			for (int index = 0;index < resources.length;index++)
			{
				Hospital.MaterialResource type = Hospital.MaterialResource.valueOf(resources[index]);
				if (type == null)
				{
					throw new Exception("Invalid material resource encountered.");
				}
				
				resourceTypes[index] = type;
			}
			
			String[] dosages = trajectory.getCell(Fields.RESOURCE_DOSAGES).getStringCellValue().split(",");
			double[] resourceDosages = new double[dosages.length];
			for (int index = 0;index < dosages.length; index++)
			{
				resourceDosages[index] = Double.parseDouble(dosages[index]);
			}
			
			String[] efficacies = trajectory.getCell(Fields.RESOURCE_EFFICACIES).getStringCellValue().split(",");
			double[] resourceEfficacies = new double[efficacies.length];
			for (int index = 0;index < efficacies.length; index++)
			{
				resourceEfficacies[index] = Double.parseDouble(efficacies[index]);
			}
			
			String[] frequencies = trajectory.getCell(Fields.RESOURCE_FREQUENCIES).getStringCellValue().split(",");
			int[] resourceFrequencies = new int[frequencies.length];
			for (int index = 0;index < frequencies.length; index++)
			{
				resourceFrequencies[index] = Integer.parseInt(frequencies[index]);
			}
			
			int nurses = Integer.parseInt(trajectory.getCell(Fields.NURSES).getStringCellValue());
			int nurseOnTime = Integer.parseInt(trajectory.getCell(Fields.NURSE_ON_TIME).getStringCellValue());
			int nurseOffTime = Integer.parseInt(trajectory.getCell(Fields.NURSE_OFF_TIME).getStringCellValue());
			
			int doctors = Integer.parseInt(trajectory.getCell(Fields.DOCTORS).getStringCellValue());
			int doctorOnTime = Integer.parseInt(trajectory.getCell(Fields.DOCTOR_ON_TIME).getStringCellValue());
			int doctorOffTime = Integer.parseInt(trajectory.getCell(Fields.DOCTOR_OFF_TIME).getStringCellValue());
			
			int totalCycles = Integer.parseInt(trajectory.getCell(Fields.TOTAL_CYCLES).getStringCellValue());
		
			System.out.println("Creating treatment plan with id " + id);
			TreatmentPlan plan = new TreatmentPlan
	        (
	            patient,
	            id,
	            conditionTypes,
	            resourceTypes,
	            resourceEfficacies,
	            resourceDosages,
	            resourceFrequencies,
	            nurses,
	            nurseOnTime,
	            nurseOffTime,
	            doctors,
	            doctorOnTime,
	            doctorOffTime,
	            totalCycles
	        );
			
			return plan;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static Stage getStage(Patient patient)
    {	
		// Determine if the patient is still in the same treatment plan, and if so, return the
		// same stage object
		Stage currentStage;
		TreatmentPlan plan;
		if ((currentStage = patient.getStage()) != null && (plan = currentStage.getTreatmentPlan()) != null)
		{			
			// Check to see if the current stage is complete and select the next treatment plan if so
			if (currentStage.m_cycles >= plan.getLength())
			{
				//System.out.println("Treatment plan complete!");
				
				Row currentRow = getRowForId(plan.getID());
				int nextTreatment = Integer.parseInt(currentRow.getCell(Fields.NEXT_TREATMENT).getStringCellValue());
				
				//System.out.println("Next treatment: " + nextTreatment);
				
				if (nextTreatment > 0)
				{
					Row newRow = getRowForId(nextTreatment);
					if (newRow != null)
					{
						return new Stage(getTreatmentPlan(patient, newRow));
					}
				}
				else if(nextTreatment == 0)
				{
					return Stage.Complete; // If the next treatment has an ID of 0, the patient is ready to be discharged
				}
			}
			else if (patientSatisfiesRow(patient, getRowForId(plan.getID())))
			{
				// Update the stage's internal cycle counter to reflect the number of cycles that the
				// patient has been in the current stage
				Stage patientStage = patient.getStage();
				patientStage.m_cycles++;
				
				return patientStage;
			}
		}
		
		// Search for a new patient trajectory that matches the patient's current condition
		for (Row row : m_trajectoriesSheet)
		{
			// Skip the header row
			if (row.getRowNum() == 0)
			{
				continue;
			}
			
			if (patientSatisfiesRow(patient, row))
			{
				return new Stage(getTreatmentPlan(patient, row));
			}
		}
		
		return null;
    }
	
	static class Stage
	{
		public static final Stage Complete = new Stage(new TreatmentPlan(
				null,
	            Integer.MAX_VALUE,
	            null,
	            null,
	            null,
	            null,
	            null,
	            0,
	            0,
	            0,
	            0,
	            0,
	            0,
	            0
		));
		
		/**
		 * Internal counter that keeps track of the age of the stage in units of model cycles.
		 */
		private int m_cycles;
		
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
	    	m_cycles = 0;
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
}
