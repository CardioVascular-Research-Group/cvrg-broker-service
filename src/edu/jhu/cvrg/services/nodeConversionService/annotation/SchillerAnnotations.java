package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.util.ArrayList;
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
		if (examdescriptAnn.getJobId().toString() != null){
			examdescriptAnnsMap.put("Job ID", examdescriptAnn.getJobId().toString());
		}
		examdescriptAnnsMap.put("User ID", examdescriptAnn.getUserId());
		if (examdescriptAnn.getCaseId() != null){
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
			
			String schilAnoName = anGloAnnot.getName();
			String ecgOntoName = "";
			if(schilAnoName.equals("AXIS_QRS")) {
				ecgOntoName = "QRS_Axis";
			}else if(schilAnoName.equals("QRS")) {
				ecgOntoName = "QRS_Duration";
			/*}else if(schilAnoName.equals("HR")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("RR")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("P")) {
				ecgOntoName = "P_Duration";
			}else if(schilAnoName.equals("PQ")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("QT")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("QTC")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("QRS")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("QRS")) {
				ecgOntoName = "";
			}else if(schilAnoName.equals("QRS")) {
				ecgOntoName = "";*/
			}else{
				ecgOntoName = schilAnoName;
			}
			if (anGloAnnot.getValue() != null){
				annotationMappings.put( ecgOntoName, anGloAnnot.getValue() );
			}
		}
		return annotationMappings;
	}
	
	LinkedHashMap<String, Object> extractLeadMeasurements(Channel list) {
		
		LinkedHashMap<String, Object> annotationMappings = new LinkedHashMap<String, Object>();
		List<AnnotationLead> ano = list.getAnnotationLead(); 	
    	for (AnnotationLead anoLead : ano ) {
			
			String schilAnoName = anoLead.getName();
			String ecgOntoName = "";
			if(schilAnoName.equals("Q_DUR")){
				ecgOntoName = "Q_Wave_Duration"; 
			}else if(schilAnoName.equals("Q_AMPL")){
				ecgOntoName = "Q_Wave_Amplitude"; 
			}else if(schilAnoName.equals("R_DUR")) {
				ecgOntoName = "R_Wave_Duration";
			}else if(schilAnoName.equals("R_AMPL")) {
				ecgOntoName = "R_Wave_Amplitude";
			}else if(schilAnoName.equals("S_DUR")) {
				ecgOntoName = "S_Wave_Duration";
			}else if(schilAnoName.equals("S_AMPL")) {
				ecgOntoName = "S_Wave_Amplitude";
			/*}else if(schilAnoName.equals("R-_DUR")) {
				ecgOntoName = "R_Minus_Wave_Duration";
			}else if(schilAnoName.equals("R-_AMPL")) {
				ecgOntoName = "R_Minus_Wave_Amplitude";
			}else if(schilAnoName.equals("S-_DUR")) {
				ecgOntoName = "S_Minus_Duration";
			}else if(schilAnoName.equals("S-_AMPL")) {
				ecgOntoName = "S_Minus_Amplitude";
			}else if(schilAnoName.equals("J_AMPL")) {
				ecgOntoName = "J_Amplitude";*/
			}else{
				ecgOntoName = schilAnoName;
			}
			if (anoLead.getValue() != null){
				annotationMappings.put( ecgOntoName, anoLead.getValue() );
				//annotationMappings.put( anoLead.getName(), anoLead.getValue() );
			}
    	}
		return annotationMappings;		
	}

}
