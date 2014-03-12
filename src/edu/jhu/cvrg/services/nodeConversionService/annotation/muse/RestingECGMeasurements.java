package edu.jhu.cvrg.services.nodeConversionService.annotation.muse;

import java.util.LinkedHashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * This object will contain the element values for the RestingECGMeasurements tag in the MUSE XML.  The elements
 * stored here are based off of a DTD received long ago.  Values that are not found in any particular ECG will be
 * set to null.
 * 
 * These are annotations for the entire ECG
 * 
 * @author Brandon Benitez
 *
 */
public class RestingECGMeasurements {
	
	private String systolicBP;
	private String diastolicBP;
	private String ventricularRate;
	private String atrialRate;
	private String PRInterval;
	private String QRSDuration;
	private String QTInterval;
	private String QTCorrected;
	private String PAxis;
	private String RAxis;
	private String TAxis;
	private String QRSCount;
	private String QOnset;
	private String POnset;
	private String POffset;
	private String TOffset;
	private String ECGSampleBase;
	private String ECGSampleExponent;
	private String QTcFrederica;

	public RestingECGMeasurements() {
		
	}
	
	public void parseRestingECGMeasurements(Element restingECGData) {
		// get each child element and store it in the appropriate variable
		List allChildren = restingECGData.getChildren();
		
		if(!(allChildren.isEmpty())) {
			for (Object child : allChildren) {
				Element childElement = (Element) child;
			    if(childElement != null) {	
			    	// determine which tag it is and add it to the appropriate variable
					if(childElement.getName().equals("SystolicBP")) {
						systolicBP = childElement.getText();
					}
					else if(childElement.getName().equals("DiastolicBP")) {
						diastolicBP = childElement.getText();
					}
					else if(childElement.getName().equals("VentricularRate")) {
						ventricularRate = childElement.getText();
					}
					else if(childElement.getName().equals("AtrialRate")) {
						atrialRate = childElement.getText();
					}
					else if(childElement.getName().equals("PRInterval")) {
						PRInterval = childElement.getText();
					}
					else if(childElement.getName().equals("QRSDuration")) {
						QRSDuration = childElement.getText();
					}
					else if(childElement.getName().equals("QTInterval")) {
						QTInterval = childElement.getText();
					}
					else if(childElement.getName().equals("QTCorrected")) {
						QTCorrected = childElement.getText();
					}
					else if(childElement.getName().equals("PAxis")) {
						PAxis = childElement.getText();
					}
					else if(childElement.getName().equals("RAxis")) {
						RAxis = childElement.getText();
					}
					else if(childElement.getName().equals("TAxis")) {
						TAxis = childElement.getText();
					}
					else if(childElement.getName().equals("QRSCount")) {
						QRSCount = childElement.getText();
					}
					else if(childElement.getName().equals("QOnset")) {
						QOnset = childElement.getText();
					}
					else if(childElement.getName().equals("POnset")) {
						POnset = childElement.getText();
					}
					else if(childElement.getName().equals("POffset")) {
						POffset = childElement.getText();
					}
					else if(childElement.getName().equals("TOffset")) {
						TOffset = childElement.getText();
					}
					else if(childElement.getName().equals("ECGSampleBase")) {
						ECGSampleBase = childElement.getText();
					}
					else if(childElement.getName().equals("ECGSampleExponent")) {
						ECGSampleExponent = childElement.getText();
					}
					else if(childElement.getName().equals("QTcFrederica")) {
						QTcFrederica = childElement.getText();
					}
			    }
			}
		}
	}
	
	public String getSystolicBP() {
		return systolicBP;
	}
	
	public String getDiastolicBP() {
		return diastolicBP;
	}
	
	public String getVentricularRate() {
		return ventricularRate;
	}
	
	public String getAtrial() {
		return atrialRate;
	}
	
	public String getPRInterval() {
		return PRInterval;
	}
	
	public String getQRSDuration() {
		return QRSDuration;
	}
	
	public String getQTInterval() {
		return QTInterval;
	}
	
	public String getQTCorrected() {
		return QTCorrected;
	}
	
	public String getPAxis() {
		return PAxis;
	}
	
	public String getRAxis() {
		return RAxis;
	}
	
	public String getTAxis() {
		return TAxis;
	}
	
	public String getQRSCount() {
		return QRSCount;
	}
	
	public String getQOnset() {
		return QOnset;
	}
	
	public String getPOnset() {
		return POnset;
	}
	
	public String getPOffset() {
		return POffset;
	}
	
	public String getTOffset() {
		return TOffset;
	}
	
	public String getECGSampleBase() {
		return ECGSampleBase;
	}
	
	public String getECGSampleExponent() {
		return ECGSampleExponent;
	}
	
	public String getQTcFrederica() {
		return QTcFrederica;
	}
	
	public LinkedHashMap<String, String> getAllAnnotations() {
		LinkedHashMap<String, String> finalMap = new LinkedHashMap<String, String>();
		if(systolicBP != null) {
			finalMap.put("Systolic_Blood_Pressure", systolicBP);
		}
		if(diastolicBP != null) {
			finalMap.put("Diastolic_Blood_Pressure", diastolicBP);
		}
		if(ventricularRate != null) {
			finalMap.put("Ventricular_Rate", ventricularRate);
		}
		if(atrialRate != null) {
			finalMap.put("Atrial_Rate", atrialRate);
		}
		if(PRInterval != null) {
			finalMap.put("PR_Interval", PRInterval);
		}
		if(QRSDuration != null) { 
			finalMap.put("QRS_Duration", QRSDuration);
		}
		if(QTInterval != null) {
			finalMap.put("QT_Interval", QTInterval);
		}
		if(QTCorrected != null) {
			finalMap.put("QT_Corrected", QTCorrected);
		}
		if(PAxis != null) {
			finalMap.put("P_Frontal_Axis", PAxis);
		}
		if(RAxis != null) {
			finalMap.put("R_Frontal_Axis", RAxis);
		}
		if(TAxis != null) {
			finalMap.put("T_Frontal_Axis", TAxis);
		}
		if(QRSCount != null) {
			finalMap.put("QRS_Count", QRSCount);
		}
		if(QOnset != null) {
			finalMap.put("Q_Onset", QOnset);
		}
		if(POnset != null) {
			finalMap.put("P_Onset", POnset);
		}
		if(POffset != null) {
			finalMap.put("P_Offset", POffset);
		}
		if(TOffset != null) {
			finalMap.put("T_Offset", TOffset);
		}
		if(ECGSampleBase != null) {
			finalMap.put("ECG_Sample_Base", ECGSampleBase);
		}
		if(ECGSampleExponent != null) {
			finalMap.put("ECG_Sample_Exponent", ECGSampleExponent);
		}
		if(QTcFrederica != null) {
			finalMap.put("QT_Corrected_Fridericias_Formula", QTcFrederica);
		}
		
		return finalMap;
	}
	
}
