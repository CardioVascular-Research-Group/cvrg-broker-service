package edu.jhu.cvrg.services.nodeConversionService.annotation.muse;

public class QRSTime {
	String number = "";
	String type = ""; 
	String time = "";
	
	public QRSTime(String newNumber, String newType, String newTime) {
		number = newNumber;
		type = newType;
		time = newTime;
	}

	public QRSTime() {
		
	}
	
	public String getNumber() {
		return number;
	}
	
	public String getType() {
		return type;
	}
	
	public String getTime() {
		return time;
	}
	
	public void setNumber(String newValue) {
		number = newValue;
	}
	
	public void setType(String newValue) {
		type = newValue;
	}
	
	public void setTime(String newValue) {
		time = newValue;
	}

}
