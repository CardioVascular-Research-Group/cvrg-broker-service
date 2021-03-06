package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.cvrgrid.schiller.jaxb.beans.Event;
import org.cvrgrid.schiller.jaxb.beans.Examdescript;
import org.cvrgrid.schiller.jaxb.beans.Patdata;
import org.cvrgrid.schiller.jaxb.beans.Channel;
import org.cvrgrid.schiller.jaxb.beans.AnnotationGlobal;
import org.cvrgrid.schiller.jaxb.beans.AnnotationLead;

// This class contains methods for retrieving annotations from the XML file.
// The class is left at the default (package level) visibility as they are not intended to
// be used by classes outside of this package.
//
// Author: Brandon Benitez

class SchillerAnnotations {
	
	LinkedHashMap<String, Object> extractExamdescript(Examdescript examdescriptAnn) {
		LinkedHashMap<String, Object> examdescriptAnnsMap = new LinkedHashMap<String, Object>();
		
		examdescriptAnnsMap.put("Exam Start Date", examdescriptAnn.getStartdatetime().getDate());
		examdescriptAnnsMap.put("Exam Start Time", examdescriptAnn.getStartdatetime().getTime());
		examdescriptAnnsMap.put("Rec Type", examdescriptAnn.getRectype());
		String acqDev = examdescriptAnn.getAquiringdevice().getHardware().getVendor() + " " + examdescriptAnn.getAquiringdevice().getHardware().getModel();
		examdescriptAnnsMap.put("Acquiring Device", acqDev);
		if (examdescriptAnn.getJobId().trim().length() != 0){
			examdescriptAnnsMap.put("Job ID", examdescriptAnn.getJobId());
		}
		examdescriptAnnsMap.put("User ID", examdescriptAnn.getUserId());
		if (examdescriptAnn.getCaseId().trim().length() != 0){
			examdescriptAnnsMap.put("Case ID", examdescriptAnn.getCaseId());
		}
		return examdescriptAnnsMap;
	}
	
	LinkedHashMap<String, Object> extractPatdata(Patdata patdataAnn) {
		LinkedHashMap<String, Object> patdataAnnsMap = new LinkedHashMap<String, Object>();
		
		patdataAnnsMap.put("Patient ID", patdataAnn.getId());
		patdataAnnsMap.put("Patient Lastname", patdataAnn.getLastname());
		patdataAnnsMap.put("Patient Firstname", patdataAnn.getFirstname());
		patdataAnnsMap.put("Patient Birthdate", patdataAnn.getBirthdate());
		patdataAnnsMap.put("Patient Gender", patdataAnn.getGender());
		patdataAnnsMap.put("Patient Ethnic", patdataAnn.getEthnic());
		String weight = patdataAnn.getWeight().getValue() + patdataAnn.getWeight().getUnit();
		patdataAnnsMap.put("Patient Weight", weight);
		String height = patdataAnn.getHeight().getValue() + patdataAnn.getHeight().getUnit();
		patdataAnnsMap.put("Patient Height", height);
		if (patdataAnn.getPacemaker().getValue() != null){
			patdataAnnsMap.put("Pacemaker", patdataAnn.getPacemaker().getValue());
		}
		return patdataAnnsMap;
	}

	LinkedHashMap<String, Object> extractCrossleadElements(Event globalAnnotations) {
		
		LinkedHashMap<String, Object> annotationMappings = new LinkedHashMap<String, Object>();
		List<AnnotationGlobal> anGlo = globalAnnotations.getAnnotationGlobal();
		for(AnnotationGlobal anGloAnnot: anGlo) {
			if (anGloAnnot.getValue() != null){
				String schilAnoName = anGloAnnot.getName();
				String ecgOntoName = "";
				if (AnnotationMaps.schilAnoMap.containsKey(schilAnoName)){
					ecgOntoName = AnnotationMaps.schilAnoMap.get(schilAnoName);
				}else{
					ecgOntoName = schilAnoName;
				}
				String checked = checkForDash(anGloAnnot.getValue());
				annotationMappings.put( ecgOntoName, checked );
			}
		}
		return annotationMappings;
	}
	
	LinkedHashMap<String, Object> extractLeadMeasurements(Channel list) {
		
		LinkedHashMap<String, Object> annotationMappings = new LinkedHashMap<String, Object>();
		List<AnnotationLead> ano = list.getAnnotationLead(); 	
    	for (AnnotationLead anoLead : ano ) {
			if (anoLead.getValue() != null){
				String schilAnoName = anoLead.getName();
				String ecgOntoName = "";
				if (AnnotationMaps.schilAnoMap.containsKey(schilAnoName)){
					ecgOntoName = AnnotationMaps.schilAnoMap.get(schilAnoName);
				}else{
					ecgOntoName = schilAnoName;
				}
				String checked = checkForDash(anoLead.getValue());
				annotationMappings.put( ecgOntoName, checked );
			}
    	}
		return annotationMappings;		
	}
	

	public static String checkForDash(String test) {
		if (test.trim().equalsIgnoreCase("-")){
			test = "0";
		}
		return test;
	}
	
}