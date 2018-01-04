package edu.bu.zaman.MMHModel.Simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Singleton class that manages the creation and reassessment of patients and their
 * conditions.
 * 
 * @author Darash Desai
 *
 */
public class Shiva
{	
	/**
	 * Internal Random instance used to draw random numbers.
	 */
	private static Random m_random = new Random();
	
	/**
	 * Reference to the condition probabilities excel spreadsheet.
	 */
	private static Sheet m_conditionProbabilitiesSheet;	
	
	/**
	 * Contains list of probabilities associated with the incidence of each condition or set
	 * of conditions that patients arrive with.
	 */
	private static ArrayList<Double> m_conditionProbabilities = new ArrayList<>();
	
	/**
	 * Stores the mean initial probability of mortality for each condition observed among
	 * patients when they are admitted to the hospital. This is populated at startup from
	 * the setup.xlsx file. 
	 */
	private static HashMap<Condition.Type, Double> m_pomMean = new HashMap<>();
	
	/**
	 * Stores the standard deviation of the initial probabilty of mortality for each condition
	 * observed among patients when they are admitted to the hospital. This is used together
	 * with <code>m_pomMean</code> to define a gaussian probability distribution to use for
	 * drawing a random variate when randomly assigning a condition to a new patient at admission.  
	 */
	private static HashMap<Condition.Type, Double> m_pomStdev = new HashMap<>();
	
	/**
	 * Stores the deterioration rates associated with each condition.
	 */
    private static HashMap<Condition.Type, Double> m_deteriorationRates = new HashMap<>();
	
    /**
     * Counter used to provide unique patient ids when creating a new patient
     */
    private static int m_patientCount = 0;
    
    @SuppressWarnings("unused")
	private static Shiva m_instance = new Shiva(); // Eager creation of singleton StageManager object
    private Shiva() 
    {
    	String filepath = Simulator.getSetupFilepath();        
        
        // Open the trajectories workbook and get the first sheet
        try (
    		FileInputStream trajectoriesFile = new FileInputStream(new File(filepath));
	        Workbook workbook = new XSSFWorkbook(trajectoriesFile);
		)
        {
        	// Get the first sheet, which contains deterioration rates for each condition
        	Sheet deteriorations = workbook.getSheetAt(0);
        	
        	// Parse the deterioration information and store it locally
        	for (Row row : deteriorations)
        	{
        		// Skip the header row
        		if (row.getRowNum() == 0)
        		{
        			continue;
        		}
        		
        		Condition.Type condition = Condition.Type.valueOf(row.getCell(0).getStringCellValue());
        		
        		double pomMean = row.getCell(1).getNumericCellValue();
        		double pomStdev = row.getCell(2).getNumericCellValue();
        		double deterioration = row.getCell(3).getNumericCellValue();
        		
        		if (condition != null)
        		{
        			m_pomMean.put(condition, pomMean);
        			m_pomStdev.put(condition, pomStdev);
        			m_deteriorationRates.put(condition, deterioration);
        		}
        	}
        	
        	// Get the second sheet, which contains probabilties for each condition or set of
        	// conditions that new patients arrive with
        	m_conditionProbabilitiesSheet = workbook.getSheetAt(1);
        	
        	// Parse the condition probability information and store it locally
        	double sum = 0;
        	for (Row row : m_conditionProbabilitiesSheet)
        	{
        		// Skip the header row
        		if (row.getRowNum() == 0)
        		{
        			continue;
        		}
        		
        		sum += row.getCell(1).getNumericCellValue();
        		m_conditionProbabilities.add(sum);
        	}        
        	
        	// Test for unity taking into account rounding errors with floating point arithmetic
        	if ((Math.round(sum * 100000d) / 100000d) != 1)
        	{
        		System.out.println("Error: condition probabilities do not sum to unity.");
        		System.exit(-1);
        	}
        }
        catch(FileNotFoundException fnfe)
        {
        	fnfe.printStackTrace();
        }
        catch(IOException ioe)
        {
        	ioe.printStackTrace();
        }
    }
    
    /**
     * Creates a new patient to admit to the hospital with a random age and set of conditions.
     * The probability of being assigned any given condition is defined by the incidence rates
     * provided in the setup file, and the initial probabilty of mortality for each condition
     * is randomly drawn using a gaussian probabilty distribution with a mean and standard
     * deviation that is also defined for each condition in the setup file.
     * 
     * @param minAge	the minimum age the patient should be
     * @param maxAge	the maximum age the patient should be
     * 
     * @return	a new patient to admit to the hospital 
     */
	public static Patient createPatient(int minAge, int maxAge)
    {
        // Generates a random age for the patient
		int age = minAge + (int)(Math.random() * (maxAge-minAge+1));
        
        // Randomly assign conditions to the patient
        double variate = Math.random();
        int row = 0;
        for (int index = 0;index < m_conditionProbabilities.size(); index++)
        {
        	if (m_conditionProbabilities.get(index) >= variate)
        	{
        		row = index;
        		break;
        	}
        }
        
        String[] conditionTypes = m_conditionProbabilitiesSheet.getRow(row + 1).getCell(0).getStringCellValue().split(",");        
        ArrayList<Condition> conditions = new ArrayList<>();
        for (String condition : conditionTypes)
        {
        	Condition.Type type = Condition.Type.valueOf(condition);
        	if (type != null)
        	{
                conditions.add(new Condition(
                    type,
                    getProbabilityOfMortality(type),
                    m_deteriorationRates.get(type)
                ));
        	}
        }
        
        return new Patient(++m_patientCount, age, conditions);
    }

	/**
	 * Generates a probability of mortality for a random patient based on the mean and stdev defined
	 * for that condition's probability distribution. These values are fetched from the "Conditions"
	 * sheet of the setup.xlsx file.
	 * 
	 * @param conditionType 	the condition to fetch a random probability of mortality for
	 * @return a probability of mortality
	 */
	private static double getProbabilityOfMortality(Condition.Type conditionType)
	{
		double pom = 0;
		double pomMean = m_pomMean.getOrDefault(conditionType, 0.0);
		double pomStdev = m_pomStdev.getOrDefault(conditionType, 0.0);
		
		// Make sure the condition type exists in the hash maps
		if (pomMean * pomStdev == 0)
		{
			return 0;
		}
		
		do
		{
			pom = m_random.nextGaussian() * pomStdev + pomMean;
		}
		while (pom <= 0 || pom >= 1);
		
		return pom;
	}
	
    /**
     * Assess the current patient and determines whether any conditions are spotaneously
     * cured and also whether the patient spotaneously contracts new conditions
     *
     * @param patient   the patient being assessed
     */
    public static void reassessConditions(Patient patient)
    {
        /*ADD CONDITION:condition.add() based on random probability parsing through condition incidence rates (see createPatient)
         * Co-morbidities: Parse through spreadsheet for conditional probabilities, get conditional probabilities
         * Each co-morbidity - condition probability is modulated by other condition or dictated by other condition
         * REMOVE CONDITION:
         * Threshold value to remove conditions - same threshold or different for different conditions?
         */
    }
}
