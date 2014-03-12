package edu.jhu.cvrg.services.nodeConversionService.annotation.muse;

import java.util.LinkedHashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * This object will contain the element values for a given LeadData tag in the MUSE XML.  The elements
 * stored here are based off of a DTD received long ago.  Values that are not found in any particular ECG will be
 * set to null.
 * 
 * 
 * The LeadData annotations are for the entire lead.
 * 
 * @author Brandon Benitez
 *
 */
public class LeadMeta {
	
	String leadByteCountTotal;
	String leadTimeOffset;
	String leadAmplitudeUnitsPerBit;
	String leadAmplitudeUnits;
	String leadHighLimit;
	String leadLowLimit;
	String leadID;
	String leadOffsetFirstSample;
	String leadSampleSize;
	String leadOff;
	String baselineSway;
	String excessiveACNoise;
	String muscleNoise;
	String leadDataCRC32;
	
	

	public LeadMeta() {
		
	}
	
	public void parseLeadData(Element leadData) {
		// get each child element and store it in the appropriate variable
		List allChildren = leadData.getChildren();
				
		if(!(allChildren.isEmpty())) {
			for (Object child : allChildren) {
				Element childElement = (Element) child;
			    if(childElement != null) {	
				    	// determine which tag it is and add it to the appropriate variable
					if(childElement.getName().equals("LeadByteCountTotal")) {
						leadByteCountTotal = childElement.getText();
					}
					else if(childElement.getName().equals("LeadTimeOffset")) {
						leadTimeOffset = childElement.getText();
					}
					else if(childElement.getName().equals("LeadAmplitudeUnitsPerBit")) {
						leadAmplitudeUnitsPerBit = childElement.getText();
					}
					else if(childElement.getName().equals("LeadAmplitudeUnits")) {
						leadAmplitudeUnits = childElement.getText();
					}
					else if(childElement.getName().equals("LeadHighLimit")) {
						leadHighLimit = childElement.getText();
					}
					else if(childElement.getName().equals("LeadLowLimit")) {
						leadLowLimit = childElement.getText();
					}
					else if(childElement.getName().equals("LeadID")) {
						leadID = childElement.getText();
					}
					else if(childElement.getName().equals("LeadOffsetFirstSample")) {
						leadOffsetFirstSample = childElement.getText();
					}
					else if(childElement.getName().equals("LeadOff")) {
						leadOff = childElement.getText();
					}
					else if(childElement.getName().equals("BaselineSway")) {
						baselineSway = childElement.getText();
					}
					else if(childElement.getName().equals("ExcessiveACNoise")) {
						excessiveACNoise = childElement.getText();
					}
					else if(childElement.getName().equals("MuscleNoise")) {
						muscleNoise = childElement.getText();
					}
					else if(childElement.getName().equals("LeadDataCRC32")) {
						leadDataCRC32 = childElement.getText();
					}
			    }
			}
		}
	}
	
	public LinkedHashMap<String, String> getAllAnnotations() {
		LinkedHashMap<String, String> finalMap = new LinkedHashMap<String, String>();
		if(leadByteCountTotal != null) {
			finalMap.put("Lead_Byte_Count_Total", leadByteCountTotal);
		}
		if(leadTimeOffset != null) {
			finalMap.put("Lead_Time_Offset", leadTimeOffset);
		}
		if(leadAmplitudeUnitsPerBit != null) {
			finalMap.put("Lead_Amplitude_Units_Per_Bit", leadAmplitudeUnitsPerBit);
		}
		if(leadAmplitudeUnits != null) {
			finalMap.put("Lead_Amplitude_Units", leadAmplitudeUnits);
		}
		if(leadHighLimit != null) {
			finalMap.put("Lead_High_Limit", leadHighLimit);
		}
		if(leadLowLimit != null) { 
			finalMap.put("Lead_Low_Limit", leadLowLimit);
		}
		if(leadOffsetFirstSample != null) { 
			finalMap.put("Lead_Offset_First_Sample", leadOffsetFirstSample);
		}
		if(leadOff != null) { 
			finalMap.put("Lead_Offset", leadOff);
		}
		if(baselineSway != null) { 
			finalMap.put("Baseline_Sway", baselineSway);
		}
		if(excessiveACNoise != null) { 
			finalMap.put("Excessive_AC_Noise", excessiveACNoise);
		}
		if(muscleNoise != null) { 
			finalMap.put("Muscle_Noise", muscleNoise);
		}
		if(leadDataCRC32 != null) { 
			finalMap.put("Lead_Data_CRC_32", leadDataCRC32);
		}
		
		return finalMap;
	}
	
	public String getLead() {
		return leadID;
	}

}
