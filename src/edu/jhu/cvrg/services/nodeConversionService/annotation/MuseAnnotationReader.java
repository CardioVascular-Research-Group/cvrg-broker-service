package edu.jhu.cvrg.services.nodeConversionService.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.jhu.cvrg.services.nodeConversionService.annotation.muse.LeadMeta;
import edu.jhu.cvrg.services.nodeConversionService.annotation.muse.QRSTimesTypes;
import edu.jhu.cvrg.services.nodeConversionService.annotation.muse.RestingECGMeasurements;
import edu.jhu.cvrg.services.nodeConversionService.annotation.muse.WaveformMeta;
import edu.jhu.cvrg.waveform.utility.WebServiceUtility;
import edu.jhu.cvrg.dbapi.dto.AnnotationDTO;
import edu.jhu.cvrg.dbapi.factory.exists.model.AnnotationData;

/**
 * This is the controller that will parse the annotations from Muse 7, 7.1, and 8 XML files and extract them.  This will put each set of annotations in a List
 * of AnnotationData objects to send back for entry into the database.
 * 
 * DOM was used (via the JDOM library) instead of JAXB for faster development and somewhat better efficiency.  The idea is that it provides more
 * flexibility for parsing the data and allowing parts of the document to passed in piecemeal for better memory management.  In this case the entire document
 * does not need to be put into objects.
 * 
 * @author Brandon Benitez
 *
 */

public class MuseAnnotationReader {
	
	private RestingECGMeasurements restingECG;
	private QRSTimesTypes qrsTimes;
	private WaveformMeta wholeWaveformMeta;
	private String xmlInput;
	
	private String studyID;
	private String userID;
	private String recordName;
	private Long docID;
	private String subjectID;
	
	private String createdBy = "Muse Upload";

	public MuseAnnotationReader(String newInput, String newStudyID, String newUserID, Long newDocID, String newRecordName, String newSubjectID) {
		xmlInput = newInput;
		restingECG = new RestingECGMeasurements();
		qrsTimes = new QRSTimesTypes();
		wholeWaveformMeta = new WaveformMeta();
		
		studyID = newStudyID;
		userID = newUserID;
		docID = newDocID;
		recordName = newRecordName;
		subjectID = newSubjectID;
	}
	
	public MuseAnnotationReader() {
		this("", "", "", Long.valueOf(0), "", "");
	}
	
	public void setXMLInput(String newInput) {
		xmlInput = newInput;
	}
	
	public String getXMLInput() {
		return xmlInput;
	}
	
	public void parseAnnotations() throws JDOMException {
		Document xmlDoc = buildDOM(xmlInput);
		Element restingECGElement = xmlDoc.getRootElement().getChild("RestingECGMeasurements");
		
		if(restingECGElement != null) {
			restingECG.parseRestingECGMeasurements(restingECGElement);
			restingECGElement = null;
		}
		else {
			throw new JDOMException("Unable to parse the RestingECGMeasuremens element!");
		}
		
		Element qrsTimeTypesElement = xmlDoc.getRootElement().getChild("QRSTimesTypes");
		
		if(qrsTimeTypesElement != null) {
			qrsTimes.parseQRSTimesTypes(qrsTimeTypesElement);
			qrsTimeTypesElement = null;
		}
		else {
			throw new JDOMException("Unable to parse the QRSTimesTypes element!");
		}
		
		List waveformElements = xmlDoc.getRootElement().getChildren("Waveform");
		
		// traverse through each occurance of the Waveform element to find the one the Rhythm type
		if(!(waveformElements.isEmpty())) {
			Iterator waveformIter = waveformElements.iterator();
			while(waveformIter.hasNext()) {
				Element nextWaveform = (Element)waveformIter.next();
				Element waveformType = nextWaveform.getChild("WaveformType");
				
				// Check to make sure there are valid waveforms, then send the Waveform tag to the appropriate object for parsing
				if((waveformType != null) && (waveformType.getText().equals("Rhythm"))) {
					wholeWaveformMeta.parseWaveformMetadata(nextWaveform);
					break;
				}
			}
		}
		else {
			throw new JDOMException("Unable to find any Waveform elements of any kind!");
		}
	}
	
	public ArrayList<AnnotationDTO> getRestingECGMeasurements() {
		LinkedHashMap<String, String> restingECGMap = restingECG.getAllAnnotations();
		
		ArrayList<AnnotationDTO> finalList = createAnnotations(restingECGMap);
		
		return finalList;
	}
	
	public ArrayList<AnnotationDTO> getWaveformNonLeadData() {
		LinkedHashMap<String, String> waveformMap = wholeWaveformMeta.getNonLeadAnnotations();
		
		ArrayList<AnnotationDTO> finalList = createAnnotations(waveformMap);
		
		return finalList;
	}
	
	public LinkedHashMap<String, ArrayList<AnnotationDTO>> getLeadAnnotations() {
		LinkedHashMap<String, ArrayList<AnnotationDTO>> finalMap = new LinkedHashMap<String, ArrayList<AnnotationDTO>>();
		LinkedHashMap<String, LeadMeta> leadMap = wholeWaveformMeta.getLeadAnnotations();
		
		for(String key : leadMap.keySet()) {
			LeadMeta tempLead = leadMap.get(key);
			
			LinkedHashMap<String, String> allTerms = tempLead.getAllAnnotations();
			
			ArrayList<AnnotationDTO> termsList = createAnnotations(allTerms, LeadEnum.valueOf(key));
			finalMap.put(key, termsList);
		}
		
		return finalMap;
	}
	
	private ArrayList<AnnotationDTO> createAnnotations(LinkedHashMap<String, String> annotationEntries) {
		return createAnnotations(annotationEntries, null);
	}
	
	private ArrayList<AnnotationDTO> createAnnotations(LinkedHashMap<String, String> annotationEntries, LeadEnum leadIndexEnum) {
		ArrayList<AnnotationDTO> newList = new ArrayList<AnnotationDTO>();
		String annType;
		Integer iLeadIndex = null;
		
		if(leadIndexEnum != null) {
			annType = "ANNOTATION";
			switch(leadIndexEnum) {
				case I:
					iLeadIndex = 0;
					break;
				case II:
					iLeadIndex = 1;
					break;
				case III:
					iLeadIndex = 2;
					break;
				case aVR:
					iLeadIndex = 3;
					break;
				case aVL:
					iLeadIndex = 4;
					break;
				case aVF:
					iLeadIndex = 5;
					break;
				case V1:
					iLeadIndex = 6;
					break;
				case V2:
					iLeadIndex = 7;
					break;
				case V3:
					iLeadIndex = 8;
					break;
				case V4:
					iLeadIndex = 9;
					break;
				case V5:
					iLeadIndex = 10;
					break;
				case V6:
					iLeadIndex = 11;
					break;
				case VX:
					iLeadIndex = 12;
					break;
				case VY:
					iLeadIndex = 13;
					break;
				case VZ:
					iLeadIndex = 14;
					break;
				default:
					// unknown lead, best to put it on the whole ECG for now
					iLeadIndex = null;
			}
		}
		else {
			annType = "COMMENT";
		}
		
		for(String key : annotationEntries.keySet()) {
			if(annotationEntries.get(key) != null) {
				
				// TODO:  A lookup table should be used instead of this, but for now this will do since
				// we only have a few Muse annotations that have matching Bioportal terms
				String conceptId = "";
				if(annotationEntries.get(key).toString().equals("PR_Interval")){
					conceptId = "http://www.cvrgrid.org/files/ECGTermsv1.owl#ECG_000000341"; 
				}else if(annotationEntries.get(key).toString().equals("QT_Interval")){
					conceptId = "http://www.cvrgrid.org/files/ECGTermsv1.owl#ECG_000000682"; 
				}else if(annotationEntries.get(key).toString().equals("QT_Corrected")) {
					conceptId = "http://www.cvrgrid.org/files/ECGTermsv1.owl#ECG_000000701";
				}else if(annotationEntries.get(key).toString().equals("QT_Corrected_Fridericias_Formula")) {
					conceptId = "http://www.cvrgrid.org/files/ECGTermsv1.owl#ECG_000000040";
				}
				
				String prefLabel = "";
				String fullAnnotation = "";
				
				if(!(conceptId.equals(""))) {
					Map<String, String> saOntDetails = WebServiceUtility.lookupOntology(AnnotationDTO.ECG_TERMS_ONTOLOGY, conceptId, "definition", "prefLabel");
					prefLabel = saOntDetails.get("prefLabel");
					fullAnnotation = saOntDetails.get("definition");
				}
				else {
					prefLabel = key;
				}
				
				AnnotationDTO annData = new AnnotationDTO(Long.valueOf(userID)/*userid*/, 0L/*groupID*/, 0L/*companyID*/, docID, createdBy/*createdBy*/, annType, prefLabel, 
						 conceptId != null ? AnnotationDTO.ECG_TERMS_ONTOLOGY : null , conceptId,
						 null/*bioportalRef*/, iLeadIndex, null/*unitMeasurement*/, null/*description*/, fullAnnotation , Calendar.getInstance(), 
						 null, null, null, null, //start and end are the same than this is a single point annotation 
						 studyID/*newStudyID*/, recordName/*newRecordName*/, subjectID/*newSubjectID*/);
				

				annData.setNewStudyID(studyID);
				annData.setNewSubjectID(subjectID);
				annData.setUserID(Long.valueOf(userID));
				annData.setRecordID(docID);
				annData.setNewRecordName(recordName);
				annData.setValue(annotationEntries.get(key).toString());
				annData.setName(key);
				annData.setCreatedBy(createdBy);
				annData.setTimestamp(new GregorianCalendar());
				
				
				newList.add(annData);
			}
		}
		
		return newList;
	}
	
	/**
	 * Helper method to build a <code>jdom.org.Document</code> from an 
	 * XML document represented as a String
	 * @param  xmlDocAsString  <code>String</code> representation of an XML
	 *         document with a document declaration.
	 *         e.g., <?xml version="1.0" encoding="UTF-8"?>
	 *                  <root><stuff>Some stuff</stuff></root>
	 * @return Document from an XML document represented as a String
	 */
	public static Document buildDOM(String xmlDocAsString) 
	        throws JDOMException {
		Document doc = null;
	    SAXBuilder builder = new SAXBuilder();
	    Reader stringreader = new StringReader(xmlDocAsString);
	    try {
	    	doc = builder.build(stringreader);
	    } catch(IOException ioex) {
	    	ioex.printStackTrace();
	    }
	    return doc;
	}

}
