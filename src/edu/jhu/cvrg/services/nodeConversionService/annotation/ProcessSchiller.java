package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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


import edu.jhu.cvrg.dbapi.factory.exists.model.AnnotationData;

/**
 * This class will take the annotation data that has been gathered and put it into a form which complies
 * with our XML schema
 * 
 * @author bbenite1
 *
 */
public class ProcessSchiller {
	private ComXiriuzSemaXmlSchillerEDISchillerEDI comxiriuzsemaxmlschilleredischilleredi;
	private ArrayList<AnnotationData> examdescriptList;
	private ArrayList<AnnotationData> patdataList;
	private ArrayList<AnnotationData> crossleadAnnotationsList;
	private ArrayList<AnnotationData[]> groupAnnotationsList;
	private ArrayList<AnnotationData[]> leadAnnotationsList;
	private SchillerAnnotations annotationRetriever;
	private String studyID;
	private String userID;
	private String recordName;
	private String subjectID;
	private final String createdBy = "Schiller Upload";
	
	private Logger log = Logger.getLogger(ProcessSchiller.class);
	
	public ProcessSchiller(ComXiriuzSemaXmlSchillerEDISchillerEDI newECG, String newStudyID, String newUserID, String newRecordName, String newSubjectID) {
		comxiriuzsemaxmlschilleredischilleredi = newECG;
		annotationRetriever = new SchillerAnnotations();
		examdescriptList = new ArrayList<AnnotationData>();
		patdataList = new ArrayList<AnnotationData>();
		crossleadAnnotationsList = new ArrayList<AnnotationData>();
		groupAnnotationsList = new ArrayList<AnnotationData[]>();
		leadAnnotationsList = new ArrayList<AnnotationData[]>();
		studyID = newStudyID;  //metadata
		userID = newUserID;  //metadata
		recordName = newRecordName;  //metadata
		subjectID = newSubjectID;  //metadata
		
	}


	public void setComXiriuzSemaXmlSchillerEDISchillerEDI(ComXiriuzSemaXmlSchillerEDISchillerEDI newECG) {
		comxiriuzsemaxmlschilleredischilleredi = newECG;
	}
	
	public ComXiriuzSemaXmlSchillerEDISchillerEDI getComXiriuzSemaXmlSchillerEDISchillerEDI() {
		return comxiriuzsemaxmlschilleredischilleredi;
	}
	
	public ArrayList<AnnotationData> getExamdescriptInfo() {
		return examdescriptList;
	}
	
	public ArrayList<AnnotationData> getPatDataInfo() {
		return patdataList;
	}
	
	public ArrayList<AnnotationData> getCrossleadAnnotations() {
		return crossleadAnnotationsList;
	}
	
	public ArrayList<AnnotationData[]> getGroupAnnotations() {
		return groupAnnotationsList;
	}
	
	public ArrayList<AnnotationData[]> getLeadAnnotations() {
		return leadAnnotationsList;
	}
	
	public void populateAnnotations() {
		
		log.info("Entering populateAnnotations");
		
		this.extractExamdescript();
		this.extractPatdata();
		this.processLeadAnnotations();
		this.processCrossleadAnnotations();
		/*if(comxiriuzsemaxmlschilleredischilleredi.getExamdescript() != null) {
			this.processGroupAnnotations();
			this.processLeadAnnotations();
		}*/
	}
	
	
	
	@SuppressWarnings("deprecation")
	private void extractExamdescript() {
		Examdescript examdescriptAnn = comxiriuzsemaxmlschilleredischilleredi.getExamdescript();
		
		if(examdescriptAnn != null) {
			LinkedHashMap<String, Object> orderMappings = annotationRetriever.extractExamdescript(examdescriptAnn);
			
			log.debug("Size of hashmap = " + orderMappings.size());
			
			for(String key : orderMappings.keySet()) {
				if((orderMappings.get(key) != null)) {
					AnnotationData annData = new AnnotationData();
					annData.setIsComment(true); // TODO:  Rename this to isNonLeadAnnotation instead
					annData.setIsSinglePoint(true);
					annData.setStudyID(studyID);
					annData.setSubjectID(subjectID);
					annData.setUserID(userID);
					annData.setDatasetName(recordName);
					annData.setAnnotation(orderMappings.get(key).toString());
					annData.setConceptLabel(key);
					annData.setCreator(createdBy);
					
					Random randomNum = new Random();
					
					long randomID = java.lang.System.currentTimeMillis() * (long)randomNum.nextInt(10000);
					String ms = String.valueOf(randomID);  // used for GUID
					annData.setUniqueID(ms);
					//System.out.println(annData.getSubjectID() + " - subject id");
					
					examdescriptList.add(annData);
				}
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private void extractPatdata() {
		Patdata patdataAnn = comxiriuzsemaxmlschilleredischilleredi.getPatdata();
		
		if(patdataAnn != null) {
			LinkedHashMap<String, Object> orderMappings = annotationRetriever.extractPatdata(patdataAnn);
			
			log.debug("Size of hashmap = " + orderMappings.size());
			
			for(String key : orderMappings.keySet()) {
				if((orderMappings.get(key) != null)) {
					AnnotationData annData = new AnnotationData();
					annData.setIsComment(true); // TODO:  Rename this to isNonLeadAnnotation instead
					annData.setIsSinglePoint(true);
					annData.setStudyID(studyID);
					annData.setSubjectID(subjectID);
					annData.setUserID(userID);
					annData.setDatasetName(recordName);
					annData.setAnnotation(orderMappings.get(key).toString());
					annData.setConceptLabel(key);
					annData.setCreator(createdBy);
					
					Random randomNum = new Random();
					
					long randomID = java.lang.System.currentTimeMillis() * (long)randomNum.nextInt(10000);
					String ms = String.valueOf(randomID);  // used for GUID
					annData.setUniqueID(ms);
					//System.out.println(orderMappings.get(key).toString() + " - key name" + key + " - key value");
					
					patdataList.add(annData);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void processLeadAnnotations() {
		Event allLeadAnnotations = comxiriuzsemaxmlschilleredischilleredi.getEventdata().getEvent();
		
		if(allLeadAnnotations != null) {
			List<Wavedata> leadAnnotationGroup = allLeadAnnotations.getWavedata();
			
			int leadIndex = 0;
			
			for(Wavedata annotation: leadAnnotationGroup) {

				if (annotation.getType().equalsIgnoreCase("ecg_averages")){	
					List<Channel> channel = annotation.getChannel();
			    	for (Channel subChannel : channel) {
				
						LinkedHashMap<String, Object> leadMappings = annotationRetriever.extractLeadMeasurements(subChannel);
						AnnotationData[] annotationsToAdd = new AnnotationData[leadMappings.size()];
						
						int arrayIndex = 0;
						
						for(String key : leadMappings.keySet()) {
							//log.debug("Annotation Name = " + key + " and value = " + leadMappings.get(key).toString());
							AnnotationData annData = new AnnotationData();
							annData.setIsComment(true); // TODO:  Rename this to isNonLeadAnnotation instead
							annData.setIsSinglePoint(true);
							annData.setStudyID(studyID);
							annData.setSubjectID(subjectID);
							annData.setUserID(userID);
							annData.setDatasetName(recordName);
							annData.setAnnotation(leadMappings.get(key).toString());
							annData.setConceptLabel(key);
							annData.setCreator(createdBy);
							annData.setLeadIndex(leadIndex);
							
							Random randomNum = new Random();
							
							long randomID = java.lang.System.currentTimeMillis() * (long)randomNum.nextInt(10000);
							String ms = String.valueOf(randomID);  // used for GUID
							annData.setUniqueID(ms);
							
							annotationsToAdd[arrayIndex] = annData;
							
							arrayIndex++;
						}
					
						leadAnnotationsList.add(annotationsToAdd);
						leadIndex++;
			    	}
					
				}
			}			
		}
	}
	
	@SuppressWarnings("deprecation")
	private void processCrossleadAnnotations() {
		Event globalAnnotations = comxiriuzsemaxmlschilleredischilleredi.getEventdata().getEvent();
		if(globalAnnotations != null) {
			LinkedHashMap<String, Object> annotationMappings = annotationRetriever.extractCrossleadElements(globalAnnotations);
			
			System.out.println("Size of hashmap = " + annotationMappings.size());
			
			for(String key : annotationMappings.keySet()) {
				if((annotationMappings.get(key) != null)) {
					AnnotationData annData = new AnnotationData();
					annData.setIsComment(true); // TODO:  Rename this to isNonLeadAnnotation instead
					annData.setIsSinglePoint(true);
					annData.setStudyID(studyID);
					annData.setSubjectID(subjectID);
					annData.setUserID(userID);
					annData.setDatasetName(recordName);
					annData.setAnnotation(annotationMappings.get(key).toString());
					annData.setConceptLabel(key);
					annData.setCreator(createdBy);
					
					Random randomNum = new Random();
					
					long randomID = java.lang.System.currentTimeMillis() * (long)randomNum.nextInt(10000);
					String ms = String.valueOf(randomID);  // used for GUID
					annData.setUniqueID(ms);
					//System.out.println(orderMappings.get(key).toString() + " - key name" + key + " - key value");

					//crossleadAnnotationsList.add(annData);
					crossleadAnnotationsList.add(annData);
				}	
			}	
		}
	}
	
	/*private void processGroupAnnotations() {
		Groupmeasurements allGroupAnnotations = comxiriuzsemaxmlschilleredischilleredi.getInternalmeasurements().getGroupmeasurements();
		
		if(allGroupAnnotations != null) {
			List<Groupmeasurement> groupAnnotation = allGroupAnnotations.getGroupmeasurement();
			
			for(Groupmeasurement annotation : groupAnnotation) {
				LinkedHashMap<String, Object> groupMappings = annotationRetriever.extractGroupMeasurements(annotation);
				AnnotationData[] annotationsToAdd = new AnnotationData[groupMappings.size()];
				int index = 0;
				
				for(String key : groupMappings.keySet()) {
					AnnotationData annData = new AnnotationData();
					annData.setIsComment(true); // TODO:  Rename this to isNonLeadAnnotation instead
					annData.setIsSinglePoint(true);
					annData.setStudyID(studyID);
					annData.setSubjectID(subjectID);
					annData.setUserID(userID);
					annData.setDatasetName(recordName);
					annData.setAnnotation(groupMappings.get(key).toString());
					annData.setConceptLabel(key);
					annData.setCreator(createdBy);
					
					Random randomNum = new Random();
					
					long randomID = java.lang.System.currentTimeMillis() * (long)randomNum.nextInt(10000);
					String ms = String.valueOf(randomID);  // used for GUID
					annData.setUniqueID(ms);
					
					
					annotationsToAdd[index] = annData;
					
					index++;
				}
				
				groupAnnotationsList.add(annotationsToAdd);
			}
		}
	}
	
	}*/
	
}
