package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.cvrgrid.schiller.jaxb.beans.ComXiriuzSemaXmlSchillerEDISchillerEDI;
import org.cvrgrid.schiller.jaxb.beans.Examdescript;
import org.cvrgrid.schiller.jaxb.beans.Patdata;
import org.cvrgrid.schiller.jaxb.beans.Eventdata;
import org.cvrgrid.schiller.jaxb.beans.Event;
import org.cvrgrid.schiller.jaxb.beans.Wavedata;
import org.cvrgrid.schiller.jaxb.beans.Channel;
import org.cvrgrid.schiller.jaxb.beans.AnnotationGlobal;

import edu.jhu.cvrg.dbapi.dto.AnnotationDTO;
//import edu.jhu.cvrg.dbapi.factory.exists.model.AnnotationData;
import edu.jhu.cvrg.waveform.utility.WebServiceUtility;

/**
 * This class will take the annotation data that has been gathered and put it into a form which complies
 * with our XML schema
 * 
 * @author bbenite1
 *
 */

public class ProcessSchiller {
	private ComXiriuzSemaXmlSchillerEDISchillerEDI comxiriuzsemaxmlschilleredischilleredi;
	private ArrayList<AnnotationDTO> examdescriptList;
	private ArrayList<AnnotationDTO> patdataList;
	private ArrayList<AnnotationDTO> crossleadAnnotationsList;
	private ArrayList<AnnotationDTO[]> groupAnnotationsList;
	private LinkedHashMap<String, ArrayList<AnnotationDTO>> leadAnnotationsList;
	private SchillerAnnotations annotationRetriever;
	private String studyID;
	private long userID;
	private long docID;
	private String recordName;
	private String subjectID;
	private final String createdBy = "Schiller Upload";
	
	private Logger log = Logger.getLogger(ProcessSchiller.class);
	
	public ProcessSchiller(ComXiriuzSemaXmlSchillerEDISchillerEDI newECG, String newStudyID, long newUserID, long newDocID, String newRecordName, String newSubjectID) {
		comxiriuzsemaxmlschilleredischilleredi = newECG;
		annotationRetriever = new SchillerAnnotations();
		examdescriptList = new ArrayList<AnnotationDTO>();
		patdataList = new ArrayList<AnnotationDTO>();
		crossleadAnnotationsList = new ArrayList<AnnotationDTO>();
		groupAnnotationsList = new ArrayList<AnnotationDTO[]>();
		leadAnnotationsList = new LinkedHashMap<String, ArrayList<AnnotationDTO>>();
		studyID = newStudyID;
		userID = newUserID;
		docID = newDocID;
		recordName = newRecordName;
		subjectID = newSubjectID;
	}

	public void setComXiriuzSemaXmlSchillerEDISchillerEDI(ComXiriuzSemaXmlSchillerEDISchillerEDI newECG) {
		comxiriuzsemaxmlschilleredischilleredi = newECG;
	}
	
	public ComXiriuzSemaXmlSchillerEDISchillerEDI getComXiriuzSemaXmlSchillerEDISchillerEDI() {
		return comxiriuzsemaxmlschilleredischilleredi;
	}
	
	public ArrayList<AnnotationDTO> getExamdescriptInfo() {
		return examdescriptList;
	}
	
	public ArrayList<AnnotationDTO> getPatDataInfo() {
		return patdataList;
	}
	
	public ArrayList<AnnotationDTO> getCrossleadAnnotations() {
		return crossleadAnnotationsList;
	}
	
	public ArrayList<AnnotationDTO[]> getGroupAnnotations() {
		return groupAnnotationsList;
	}
	
	public LinkedHashMap<String, ArrayList<AnnotationDTO>> getLeadAnnotations() {
		return leadAnnotationsList;
	}
	
	public void populateAnnotations() {
		
		log.info("Entering populateAnnotations");
		
		this.extractExamdescript();
		this.extractPatdata();
		this.processLeadAnnotations();
		this.processCrossleadAnnotations();
	}

	private void extractExamdescript() {
		Examdescript examdescriptAnn = comxiriuzsemaxmlschilleredischilleredi.getExamdescript();
		
		if(examdescriptAnn != null) {
			LinkedHashMap<String, Object> orderMappings = annotationRetriever.extractExamdescript(examdescriptAnn);
			
			String annType = "COMMENT";
    		log.debug("Size of hashmap = " + orderMappings.size());
			
			for(String key : orderMappings.keySet()) {
				if((orderMappings.get(key) != null)) {
					
					AnnotationDTO annData = new AnnotationDTO();
					annData.setNewStudyID(studyID);
					annData.setNewSubjectID(subjectID);
					annData.setUserID(Long.valueOf(userID));
					annData.setRecordID(docID);
					annData.setNewRecordName(recordName);
					annData.setValue(orderMappings.get(key).toString());
					annData.setName(key);
					annData.setCreatedBy(createdBy);
					annData.setAnnotationType(annType);
					annData.setTimestamp(new GregorianCalendar());
					
					examdescriptList.add(annData);
				}
			}
		}
	}

	private void extractPatdata() {
		Patdata patdataAnn = comxiriuzsemaxmlschilleredischilleredi.getPatdata();
		
		if(patdataAnn != null) {
			LinkedHashMap<String, Object> orderMappings = annotationRetriever.extractPatdata(patdataAnn);
			String annType = "COMMENT";
    		log.debug("Size of hashmap = " + orderMappings.size());
			
			for(String key : orderMappings.keySet()) {
				if((orderMappings.get(key) != null)) {
					AnnotationDTO annData = new AnnotationDTO();
					annData.setNewStudyID(studyID);
					annData.setNewSubjectID(subjectID);
					annData.setUserID(Long.valueOf(userID));
					annData.setRecordID(docID);
					annData.setNewRecordName(recordName);
					annData.setValue(orderMappings.get(key).toString());
					annData.setName(key);
					annData.setCreatedBy(createdBy);
					annData.setAnnotationType(annType);
					annData.setTimestamp(new GregorianCalendar());
					
					patdataList.add(annData);
				}
			}
		}
	}

	private void processLeadAnnotations() {
		Event allLeadAnnotations = comxiriuzsemaxmlschilleredischilleredi.getEventdata().getEvent();
		
		if(allLeadAnnotations != null) {
			List<Wavedata> leadAnnotationGroup = allLeadAnnotations.getWavedata();
			
			// iterate through multiple wavedata elements within file
			for(Wavedata annotation: leadAnnotationGroup) {

				if (annotation.getType().equalsIgnoreCase("ecg_averages")){	
					List<Channel> channel = annotation.getChannel();
		    		Integer leadIndex = null;
		    		
		    		//iterate through multiple channels within wavedata element
			    	for (Channel subChannel : channel) {
						String leadValue = subChannel.getName();
						
						leadIndex = containsEnum(leadValue);
						if (leadIndex == 20){
							leadIndex = null;
						}
						
						String annType = "ANNOTATION";
						
			    		LinkedHashMap<String, Object> leadMappings = annotationRetriever.extractLeadMeasurements(subChannel);
			    		
			    		ArrayList<AnnotationDTO> annotationsToAdd = new ArrayList<AnnotationDTO>();

						for(String key : leadMappings.keySet()) {

							String conceptId = "";
							String fullAnnotation = "";
							String prefLabel = "";
							
							AnnotationDTO annData = new AnnotationDTO(Long.valueOf(userID), 0L, 0L, docID, createdBy, annType, prefLabel, 
									 conceptId != null ? AnnotationDTO.ECG_TERMS_ONTOLOGY : null , conceptId,
									 null, leadIndex, null, null, fullAnnotation , Calendar.getInstance(), 
									 null, null, null, null, studyID, recordName, subjectID);
							
							if (AnnotationMaps.ecgOntoMap.containsKey(key)){
								conceptId = "http://www.cvrgrid.org/files/" + AnnotationMaps.ecgOntoMap.get(key);
							}
							
							annData.setNewStudyID(studyID);
							annData.setNewSubjectID(subjectID);
							annData.setUserID(Long.valueOf(userID));
							annData.setRecordID(docID);
							annData.setNewRecordName(recordName);
							annData.setValue(leadMappings.get(key).toString());
							annData.setName(key);
							annData.setCreatedBy(createdBy);
							annData.setBioportalClassId(conceptId);
							annData.setTimestamp(new GregorianCalendar());
							annotationsToAdd.add(annData);
							
						}
						leadAnnotationsList.put(Integer.toString(leadIndex), annotationsToAdd);
			    	}
				}
			}			
		}
	}

	private void processCrossleadAnnotations() {
		Event globalAnnotations = comxiriuzsemaxmlschilleredischilleredi.getEventdata().getEvent();
		if(globalAnnotations != null) {
			LinkedHashMap<String, Object> annotationMappings = annotationRetriever.extractCrossleadElements(globalAnnotations);
			String annType = "Global Annotation";

			// iterate through multiple annotation_global elements within file
    		for(String key : annotationMappings.keySet()) {
				if((annotationMappings.get(key) != null)) {
					
					String conceptId = "";
					
					if (AnnotationMaps.ecgOntoMap.containsKey(key)){
						conceptId = "http://www.cvrgrid.org/files/" + AnnotationMaps.ecgOntoMap.get(key);
					}
					
					AnnotationDTO annData = new AnnotationDTO();
					annData.setNewStudyID(studyID);
					annData.setNewSubjectID(subjectID);
					annData.setUserID(Long.valueOf(userID));
					annData.setRecordID(docID);
					annData.setNewRecordName(recordName);
					annData.setValue(annotationMappings.get(key).toString());
					annData.setName(key);
					annData.setCreatedBy(createdBy);
					annData.setBioportalClassId(conceptId);
					annData.setAnnotationType(annType);
					annData.setTimestamp(new GregorianCalendar());
					
					crossleadAnnotationsList.add(annData);
				}	
			}	
		}
	}	
	public static int containsEnum(String test) {
		int i = 0;
		for (LeadEnum l : LeadEnum.values()){
			if (l.name().equalsIgnoreCase(test)){
				return i;
			}
			i++;
		}
		i=20;    // can't nullify the int b/c method can't return null so assign it here and nullify it once it's returned.
		return i;
	}
}