package edu.jhu.cvrg.services.nodeConversionService;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.cvrg.dbapi.dto.AnnotationDTO;
import edu.jhu.cvrg.dbapi.factory.Connection;
import edu.jhu.cvrg.dbapi.factory.ConnectionFactory;
import edu.jhu.cvrg.dbapi.factory.exists.model.AnnotationData;
import edu.jhu.cvrg.dbapi.factory.exists.model.MetaContainer;
import edu.jhu.cvrg.services.nodeConversionService.annotation.ProcessPhilips103;
import edu.jhu.cvrg.services.nodeConversionService.annotation.ProcessPhilips104;
import edu.jhu.cvrg.waveform.service.ServiceUtils;
import edu.jhu.icm.ecgFormatConverter.ECGformatConverter;
import edu.jhu.icm.ecgFormatConverter.ECGformatConverter.fileFormat;

public class FileProccessThread extends Thread {

	private String sep = File.separator;
	
	private MetaContainer metaData;
	private ECGformatConverter.fileFormat inputFormat;
	private ECGformatConverter.fileFormat outputFormat; 
	private String inputPath;
	private long groupId;
	private long companyId;
	private long folderId;
	private String outputPath;
	private ECGformatConverter conv; 
	private String recordName;
	private Connection dbUtility;
	private long docId;
	private long userId;
	
	Logger log = Logger.getLogger(FileProccessThread.class);
	
	public FileProccessThread(MetaContainer metaData, fileFormat inputFormat,	fileFormat outputFormat, String inputPath, 
							  long groupId, long folderId, long companyId, String outputPath, ECGformatConverter conv, String recordName, long docId, long userId) {
		super();
		this.metaData = metaData;
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.inputPath = inputPath;
		this.groupId = groupId;
		this.companyId = companyId;
		this.folderId = folderId;
		this.outputPath = outputPath;
		this.conv = conv;
		this.recordName = recordName;
		this.setName(metaData.getUserID()+ " - " + recordName);
		this.dbUtility = ConnectionFactory.createConnection();
		this.docId = docId;
		this.userId = userId;
	}


	@Override
	public void run() {
		this.fileProccess();
	}
	
	
	private void fileProccess() {

		long intermediateEndTime, annotationTimeElapsed, conversionTimeElapsed;
		long backgroundProcessTime = java.lang.System.currentTimeMillis();
		
		int rowsWritten;
		
		rowsWritten = conv.write(outputFormat, outputPath, recordName);
		
		log.debug("rowsWritten: " + rowsWritten);
		
		log.debug(" +++++ Conversion completed successfully, results will be transfered.");
		
		tranferFileToLiferay(outputFormat, inputFormat, metaData.getFileName(), inputPath, groupId, folderId, docId, userId);
		
		intermediateEndTime = java.lang.System.currentTimeMillis();
		conversionTimeElapsed = intermediateEndTime - backgroundProcessTime;
		annotationTimeElapsed = java.lang.System.currentTimeMillis();
		
		
		ArrayList<AnnotationData> nonLeadList = new ArrayList<AnnotationData>();
		ArrayList<AnnotationData[]> leadList = null;
		
		// Now do annotations from Muse or Philips files
		if(fileFormat.PHILIPS103.equals(inputFormat)) {
			
			org.sierraecg.schema.Restingecgdata ecgData = (org.sierraecg.schema.Restingecgdata) conv.getPhilipsRestingecgdata();
			
			ProcessPhilips103 phil103Ann = new ProcessPhilips103( ecgData, metaData.getStudyID(), metaData.getUserID(), metaData.getRecordName(), metaData.getSubjectID());
			phil103Ann.populateAnnotations();
			
			ArrayList<AnnotationData> orderList = phil103Ann.getOrderInfo();
			ArrayList<AnnotationData> dataList = phil103Ann.getDataAcquisitions();
			ArrayList<AnnotationData> globalList = phil103Ann.getGlobalAnnotations();
			
			leadList = phil103Ann.getLeadAnnotations();
			
			nonLeadList.addAll(orderList);
			nonLeadList.addAll(dataList);
			nonLeadList.addAll(globalList);
			
		}else if(fileFormat.PHILIPS104.equals(inputFormat)) {
			
			org.cvrgrid.philips.jaxb.beans.Restingecgdata ecgData = (org.cvrgrid.philips.jaxb.beans.Restingecgdata) conv.getPhilipsRestingecgdata();
			
			ProcessPhilips104 phil104Ann = new ProcessPhilips104(ecgData, metaData.getStudyID(), metaData.getUserID(), metaData.getRecordName(), metaData.getSubjectID());
			phil104Ann.populateAnnotations();
			
			ArrayList<AnnotationData> orderList = phil104Ann.getOrderInfo();
			ArrayList<AnnotationData> dataList = phil104Ann.getDataAcquisitions();
			ArrayList<AnnotationData> globalList = phil104Ann.getCrossleadAnnotations();
			
			leadList = phil104Ann.getLeadAnnotations();
			
			nonLeadList.addAll(orderList);
			nonLeadList.addAll(dataList);
			nonLeadList.addAll(globalList);
			
		}
		
		convertLeadAnnotations(leadList);
		convertNonLeadAnnotations(nonLeadList, "");
		
		intermediateEndTime = java.lang.System.currentTimeMillis();
		annotationTimeElapsed = intermediateEndTime - annotationTimeElapsed;
		backgroundProcessTime = intermediateEndTime - backgroundProcessTime;
		
		log.info("The runtime for all background process is = " + backgroundProcessTime + " milliseconds");
		log.info("The runtime for converting the data is = " + conversionTimeElapsed + " milliseconds");
		log.info("The runtime for analyse data and entering it into the database is = " + annotationTimeElapsed + " milliseconds");
		
	}
	
	private String tranferFileToLiferay(fileFormat outputFormat, fileFormat inputFormat, String inputFilename, String inputPath, long groupId, long folderId, long docId, long userId){
		String errorMessage="";
		try {

			String outputExt = ".dat";
			if (outputFormat == ECGformatConverter.fileFormat.RDT){ 
				outputExt = ".rdt"; }
			else if (outputFormat == ECGformatConverter.fileFormat.GEMUSE) {
				outputExt = ".txt";
			}else if (outputFormat == ECGformatConverter.fileFormat.HL7) {
				outputExt = ".xml";
			}

			String outputFileName = inputFilename.substring(0, inputFilename.lastIndexOf(".")) + outputExt;

			File orign = new File(inputPath + outputFileName);
			FileInputStream fis = new FileInputStream(orign);
			
			long[] filesId = null; 
			
			Long fileId = ServiceUtils.sendToLiferay(groupId, folderId, userId, inputPath, outputFileName, orign.length(), fis);
			
			String name = inputFilename.substring(0, inputFilename.lastIndexOf(".")); // file name minus extension.

			File heaFile = new File(inputPath + name + ".hea");
			if (inputFormat != ECGformatConverter.fileFormat.WFDB && heaFile.exists()) {
				orign = new File(inputPath + heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1));
				fis = new FileInputStream(orign);
				
				filesId = new long[2];
				filesId[0] = fileId;
				
				fileId = ServiceUtils.sendToLiferay(groupId, folderId, userId, inputPath, heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1), orign.length(), fis);
				filesId[1] = fileId;
			
			}else{
				filesId = new long[1];
				filesId[0] = fileId;
			}
			
			dbUtility.storeFilesInfo(docId, filesId, null);
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			errorMessage =  e.toString();
		}
		return errorMessage;
	}

	private void convertLeadAnnotations(ArrayList<AnnotationData[]> allLeadAnnotations) {
		ArrayList<AnnotationData> list = new ArrayList<AnnotationData>();
		for(int i=0; i<allLeadAnnotations.size(); i++) {
			if(allLeadAnnotations.get(i).length != 0) {
				log.debug("There are annotations in this lead.  The size is " + allLeadAnnotations.get(i).length);
				list.addAll(Arrays.asList(allLeadAnnotations.get(i)));
			}
		}
		convertAnnotations(list, true, "");
	}
	
	private void convertNonLeadAnnotations(ArrayList<AnnotationData> allAnnotations, String groupName){
		convertAnnotations(allAnnotations, false, groupName);
	}
	
	private boolean convertAnnotations(ArrayList<AnnotationData> annotationArray, boolean isLeadAnnotation, String groupName) {
		boolean success = true;

		Set<AnnotationDTO> annotationSet = new HashSet<AnnotationDTO>();
		for(AnnotationData annData : annotationArray) {			
			
			AnnotationDTO ann = null;
			String type = null;
			if(isLeadAnnotation) {
				type = "ANNOTATION";
			}else {
				type = "COMMENT";
			}
			
			ann = new AnnotationDTO(Long.valueOf(annData.getUserID()), groupId, companyId, docId, annData.getCreator(), type, annData.getConceptLabel(), annData.getConceptID(), annData.getConceptRestURL(),
					    annData.getLeadIndex(), annData.getUnit(), annData.getComment(), annData.getAnnotation(), new GregorianCalendar(), annData.getMilliSecondStart(), 
					    annData.getMicroVoltStart(), annData.getMilliSecondEnd(), annData.getMicroVoltEnd(), annData.getStudyID(), annData.getDatasetName(), annData.getSubjectID());
			 
			annotationSet.add(ann);
		}
		
		success = annotationSet.size() == dbUtility.storeAnnotations(annotationSet);
				
		return success;
	}

}
