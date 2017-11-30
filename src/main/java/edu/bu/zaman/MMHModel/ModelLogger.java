package edu.bu.zaman.MMHModel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.bu.zaman.MMHModel.Hospital.MaterialResource;

public class ModelLogger 
{
	private static HashMap<Integer, JSONObject> m_initialPatientData = new HashMap<>();	
	private static HashMap<Integer, Integer> m_patientStartCycles = new HashMap<>();
	private static JSONArray m_cycleData = new JSONArray();
	private static JSONArray m_cyclePatientData;
	private static JSONArray m_deceasedPatients = new JSONArray();;
	
	private static int m_cycle = -1;
	
	/**
	 * Signal data logging for a new model cycle.
	 */
	public static void newCycle()
	{
		// Save previous cycle patient data if it exists
		if (m_cyclePatientData != null)
		{
			// Add hospital status data to the end of the previous cycle before
			// initiating logging for a new cycle
			
			int freeNurses = Hospital.availableNurses();
			int freeDoctors = Hospital.availableDoctors();
			HashMap<Hospital.MaterialResource, Double> hospitalStock = Hospital.getHospitalStock();
			
			JSONObject materialResources = new JSONObject();
			for (Entry<MaterialResource, Double> resource: hospitalStock.entrySet())
			{
				materialResources.put(resource.getKey().name(), resource.getValue());
			}
			
			JSONObject hospitalData = new JSONObject();
				hospitalData.put("freeNurses", freeNurses);
				hospitalData.put("freeDoctors", freeDoctors);
				hospitalData.put("inventory", materialResources);
			
			// Store patient data and hospital resource status for current cycle.
			JSONObject cycleData = new JSONObject();
				cycleData.put("patients", m_cyclePatientData);
				cycleData.put("hospitalResources", hospitalData);
			
			m_cycleData.put(cycleData);
		}
	
		// Initialize new array to store patient data for new cycle
		m_cyclePatientData = new JSONArray();
		m_cycle++;
	}
	
	/**
	 * Logs the patient data for the specified patient.
	 * 
	 * If this is the first log for the patient, the patient data is logged as the
	 * initial data for the patient upon admission. Otherwise, the patient data is
	 * stored as the patient's current state under hospital care.
	 * 
	 * @param patient
	 * @return Whether the patient was successfully logged.
	 */
	public static boolean logPatient(Patient patient)
	{
		if (m_cyclePatientData == null)
		{
			return false;
		}
		
		int patientId = patient.getPatientId();
		
		// Check to see if this patient has been logged before
		if (!m_initialPatientData.containsKey(patientId))
		{
			m_initialPatientData.put(patientId, PatientSerializer.intialPatientData(patient));
			m_patientStartCycles.put(patientId, m_cycle);
		}
		
		m_cyclePatientData.put(PatientSerializer.patientData(patient));
		
		// Check to see if patient died
		if (!patient.isAlive())
		{
			m_deceasedPatients.put(PatientSerializer.patientDeathData(patient));
		}
		
		return true;
	}
	
	/**
	 * Writes the model log as JSON output to a file at the specified file path.
	 * 
	 * @param path	the path where the log file should be saved.
	 * @throws IOException 
	 */
	public static void writeLog(String path) throws IOException
	{
		JSONObject outputData = new JSONObject();
			outputData.put("patients", m_initialPatientData.values());
			outputData.put("cycles", m_cycleData);
			outputData.put("deaths", m_deceasedPatients);
		
		FileWriter writer = new FileWriter(path);
		outputData.write(writer);
		
		writer.flush();
		writer.close();
	}
	
	private static class PatientSerializer
	{
		/**
		 * Returns JSONObject representation of the patient information at the time
		 * of admission, recording the patient's ID, age, and list of conditions at
		 * admission.
		 * 
		 * @param patient The patient object
		 * @return JSONObject
		 */
		private static JSONObject intialPatientData(Patient patient)
		{
			JSONObject data = new JSONObject();
			
			int patientId = patient.getPatientId();
			int age = patient.getAge();
			
			data.put("id", patientId);
			data.put("age", age);
						
			ArrayList<Condition> conditions = patient.getConditions();
			ArrayList<String> conditionStrings = new ArrayList<>(conditions.size());
			
			for (Condition condition: conditions)
			{
				conditionStrings.add(condition.getType().name());
			}
			
			data.put("conditions", conditionStrings);
			
			return data;
		}
				
		private static JSONObject patientData(Patient patient)
		{
			JSONObject data = new JSONObject();
			
			// Retreive patient data
			int patientId = patient.getPatientId();
			int age = patient.getAge();
			double probabilityOfMortality = patient.probabilityOfMortality();
			boolean isAlive = patient.isAlive();		
			int treatmentId = -1;
			boolean treated = Hospital.didTreat(patient);			
			
			StageManager.Stage stage;
			TreatmentPlan plan;
			if ((stage = patient.getStage()) != null && (plan = stage.getTreatmentPlan()) != null)
			{
				treatmentId = plan.getID();
			}
			
			data.put("patientId", patientId);			
			data.put("age", age);
			data.put("isAlive", isAlive);
			data.put("probabilityOfMortality", probabilityOfMortality);			
			data.put("treatmentId", treatmentId);
			data.put("treated", treated);
			
			JSONArray conditions = new JSONArray();
			for (Condition condition: patient.getConditions())
			{
				JSONObject conditionData = new JSONObject();
				
				conditionData.put("name", condition.getType().name());
				conditionData.put("probabiityOfMortality", condition.getProbabilityOfMortality());	
				
				conditions.put(conditionData);
			}
			
			data.put("conditions", conditions);
			
			return data;
		}
		
		private static JSONObject patientDeathData(Patient patient)
		{
			int patientId = patient.getPatientId();
			if (patient.isAlive() || !m_patientStartCycles.containsKey(patientId))
			{
				return null;
			}
			
			JSONObject data = new JSONObject();
			
			// Retreive patient data			
			int age = patient.getAge();
			double probabilityOfMortality = patient.probabilityOfMortality();
			int stay = m_cycle - m_patientStartCycles.get(patientId);

			data.put("patientId", patientId);			
			data.put("age", age);
			data.put("probabilityOfMortality", probabilityOfMortality);
			data.put("stay", stay);
			
			JSONArray conditions = new JSONArray();
			for (Condition condition: patient.getConditions())
			{
				JSONObject conditionData = new JSONObject();
				
				conditionData.put("name", condition.getType().name());
				conditionData.put("probabiityOfMortality", condition.getProbabilityOfMortality());	
				
				conditions.put(conditionData);
			}
			
			data.put("conditions", conditions);
			
			return data;
		}
	}
}
