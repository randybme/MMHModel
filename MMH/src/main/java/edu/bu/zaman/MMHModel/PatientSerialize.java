package edu.bu.zaman.MMHModel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import com.google.gson.Gson;


public class PatientSerialize implements Serializable{
	
	/*****************************POJO Structure***************************************************/
	
	public int Cycle;
	public int ID;
	public int age;
	public String Condition;
	public boolean Survival;
	public String Treatment;
	public int DoctorsUsed;
	public int NursesUsed;
	public Double DosesUsed;
	
	//cycle
	//     ID[]

	
	public int getCycle() {return this.Cycle;}
	public void setCycle(int Cycle) {this.Cycle = Cycle;}
	
	
	public int getPatientCollected() {return this.ID;}
	public void setPatientCollected(int ID) {this.ID = ID;}
	
	
	public int getAge() {return this.age;}
	public void setAge(int age) {this.age = age;}
	
	
	public String getCondition() {return this.Condition;}
	public void setCondition(String Condition) {this.Condition = Condition;}
	
	
	public boolean getisAlive() {return this.Survival;}
	public void setisAlive(boolean isAlive) {this.Survival = isAlive;}

	
	public int getDoctors() {return this.DoctorsUsed;}
	public void setDoctors(int DoctorsUsed) {this.DoctorsUsed = DoctorsUsed;}
	
	public int getNurses() {return this.NursesUsed;}
	public void setNurses(int NursesUsed) {this.NursesUsed = NursesUsed;}
	
	public Double getDoses() {return this.DosesUsed;}
	public void setDoses(Double DosesUsed) {this.DosesUsed = DosesUsed;}


}



