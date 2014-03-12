package edu.jhu.cvrg.services.nodeConversionService.annotation.muse;

import org.jdom.Element;
import org.jdom.JDOMException;

import edu.jhu.cvrg.dbapi.dto.AnnotationDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This object will contain the element values for the Waveform tag in the MUSE XML.  The elements
 * stored here are based off of a DTD received long ago.  Values that are not found in any particular ECG will be
 * set to null.
 * 
 * The elements containing annotations for each lead are also stored here.
 * 
 * The Waveform annotations are for the entire ECG, while the LeadData annotations are for individual leads.
 * 
 * @author Brandon Benitez
 *
 */
public class WaveformMeta {
	
	private LinkedHashMap<String, LeadMeta> leadData;
	private String waveformType;
	private String waveformStartTime;
	private String sampleType;
	private String sampleBase;
	private String sampleExponent;
	private String highPassFilter;
	private String lowPassFilter;
	private ArrayList<String> ACFilters;
	private PaceSpikes paceSpike;

	public WaveformMeta() {
		leadData = new LinkedHashMap<String, LeadMeta>();
		ACFilters = new ArrayList<String>();
	}
	
	public void parseWaveformMetadata(Element waveformData) {
		// get each child element and store it in the appropriate variable
		List allChildren = waveformData.getChildren();
		
		if(!(allChildren.isEmpty())) {
			for (Object child : allChildren) {
				Element childElement = (Element) child;
			    if(childElement != null) {	
			    	// determine which tag it is and add it to the appropriate variable
					if(childElement.getName().equals("WaveformType")) {
						waveformType = childElement.getText();
					}
					else if(childElement.getName().equals("WaveformStartTime")) {
						waveformStartTime = childElement.getText();
					}
					else if(childElement.getName().equals("SampleType")) {
						sampleType = childElement.getText();
					}
					else if(childElement.getName().equals("SampleBase")) {
						sampleBase = childElement.getText();
					}
					else if(childElement.getName().equals("SampleExponent")) {
						sampleExponent = childElement.getText();
					}
					else if(childElement.getName().equals("HighPassFilter")) {
						highPassFilter = childElement.getText();
					}
					else if(childElement.getName().equals("LowPassFilter")) {
						lowPassFilter = childElement.getText();
					}
					else if(childElement.getName().equals("ACFilter")) {
						// TODO:  Get each AC Filter and add it to the list of total AC Filters
						ACFilters.add(childElement.getText());
					}
					else if(childElement.getName().equals("LeadData")) {
						// TODO:  Get a specific Lead and then add it to the LeadData hashmap
						LeadMeta newLead = new LeadMeta();
						newLead.parseLeadData(childElement);
						leadData.put(newLead.getLead(), newLead);
					}
					else if(childElement.getName().equals("PaceSpikes")) {
						// TODO:  Get everything under the PaceSpikes element and place it in an object
						
					}
			    }
			}
		}
	}
	
	public LinkedHashMap<String, String> getNonLeadAnnotations() {
		LinkedHashMap<String, String> finalMap = new LinkedHashMap<String, String>();
		if(waveformType != null) {
			finalMap.put("Waveform_Type", waveformType);
		}
		if(waveformStartTime != null) {
			finalMap.put("Waveform_Start_Time", waveformStartTime);
		}
		if(sampleType != null) {
			finalMap.put("Sample_Type", sampleType);
		}
		if(sampleBase != null) {
			finalMap.put("Sample_Base", sampleBase);
		}
		if(sampleExponent != null) {
			finalMap.put("Sample_Exponent", sampleExponent);
		}
		if(highPassFilter != null) { 
			finalMap.put("High_Pass_Filter", highPassFilter);
		}
		if(lowPassFilter != null) {
			finalMap.put("Low_Pass_Filter", lowPassFilter);
		}
		if(!(ACFilters.isEmpty())) {
			int counter = 1;
			
			for(String filter : ACFilters) {
				finalMap.put("AC_Filter #" + counter, filter);
			}
		}
		if(paceSpike != null) {
			// TODO:  Figure out a way to handle this complex type
			
		}
		
		return finalMap;
	}
	
	public LinkedHashMap<String, LeadMeta> getLeadAnnotations() {
		return leadData;
	}

}
