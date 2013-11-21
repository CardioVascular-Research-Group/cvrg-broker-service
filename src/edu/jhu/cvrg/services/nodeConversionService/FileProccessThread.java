package edu.jhu.cvrg.services.nodeConversionService;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.cvrg.services.nodeConversionService.annotation.ProcessPhilips103;
import edu.jhu.cvrg.services.nodeConversionService.annotation.ProcessPhilips104;
import edu.jhu.cvrg.waveform.model.AnnotationData;
import edu.jhu.cvrg.waveform.service.ServiceProperties;
import edu.jhu.cvrg.waveform.service.ServiceUtils;
import edu.jhu.cvrg.waveform.utility.AnnotationUtility;
import edu.jhu.cvrg.waveform.utility.MetaContainer;
import edu.jhu.icm.ecgFormatConverter.ECGformatConverter;
import edu.jhu.icm.ecgFormatConverter.ECGformatConverter.fileFormat;

public class FileProccessThread extends Thread {

	private String sep = File.separator;
	
	private MetaContainer metaData;
	private ECGformatConverter.fileFormat inputFormat;
	private ECGformatConverter.fileFormat outputFormat; 
	private String inputPath;
	private long groupId;
	private long folderId;
	private String outputPath;
	private ECGformatConverter conv; 
	private String recordName;
	
	Logger log = Logger.getLogger(FileProccessThread.class);
	
	private AnnotationUtility dbAnnUtility;

	public FileProccessThread(MetaContainer metaData, fileFormat inputFormat,	fileFormat outputFormat, String inputPath, 
							  long groupId, long folderId, String outputPath, ECGformatConverter conv, String recordName) {
		super();
		this.metaData = metaData;
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.inputPath = inputPath;
		this.groupId = groupId;
		this.folderId = folderId;
		this.outputPath = outputPath;
		this.conv = conv;
		this.recordName = recordName;
		this.setName(metaData.getUserID()+ " - " + recordName);
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
		
		tranferFileToLiferay(outputFormat, inputFormat, metaData.getFileName(), inputPath, groupId, folderId);
		
		intermediateEndTime = java.lang.System.currentTimeMillis();
		conversionTimeElapsed = intermediateEndTime - backgroundProcessTime;
		annotationTimeElapsed = java.lang.System.currentTimeMillis();
		
		dbAnnUtility = getDbUtility();
		// Now do annotations from Muse or Philips files
		if(fileFormat.PHILIPS103.equals(inputFormat)) {
			
			org.sierraecg.schema.Restingecgdata ecgData = (org.sierraecg.schema.Restingecgdata) conv.getPhilipsRestingecgdata();
			
			ProcessPhilips103 phil103Ann = new ProcessPhilips103( ecgData, metaData.getStudyID(), metaData.getUserID(), metaData.getRecordName(), metaData.getSubjectID());
			phil103Ann.populateAnnotations();
			
			ArrayList<AnnotationData> orderList = phil103Ann.getOrderInfo();
			ArrayList<AnnotationData> dataList = phil103Ann.getDataAcquisitions();
			ArrayList<AnnotationData> globalList = phil103Ann.getGlobalAnnotations();
			ArrayList<AnnotationData[]> leadList = phil103Ann.getLeadAnnotations();
			
			convertNonLeadAnnotations(globalList, "");
			convertLeadAnnotations(leadList);
			convertNonLeadAnnotations(orderList, "");
			convertNonLeadAnnotations(dataList, "");
			
		}else if(fileFormat.PHILIPS104.equals(inputFormat)) {
			
			org.cvrgrid.philips.jaxb.beans.Restingecgdata ecgData = (org.cvrgrid.philips.jaxb.beans.Restingecgdata) conv.getPhilipsRestingecgdata();
			
			ProcessPhilips104 phil104Ann = new ProcessPhilips104(ecgData, metaData.getStudyID(), metaData.getUserID(), metaData.getRecordName(), metaData.getSubjectID());
			phil104Ann.populateAnnotations();
			
			ArrayList<AnnotationData> orderList = phil104Ann.getOrderInfo();
			ArrayList<AnnotationData> dataList = phil104Ann.getDataAcquisitions();
			ArrayList<AnnotationData> globalList = phil104Ann.getCrossleadAnnotations();
			ArrayList<AnnotationData[]> leadList = phil104Ann.getLeadAnnotations();
			
			convertNonLeadAnnotations(globalList, "");
			convertLeadAnnotations(leadList);
			convertNonLeadAnnotations(orderList, "");
			convertNonLeadAnnotations(dataList, "");
		}
		dbAnnUtility.close();
		
		intermediateEndTime = java.lang.System.currentTimeMillis();
		annotationTimeElapsed = intermediateEndTime - annotationTimeElapsed;
		backgroundProcessTime = intermediateEndTime - backgroundProcessTime;
		
		log.info("The runtime for all background process is = " + backgroundProcessTime + " milliseconds");
		log.info("The runtime for converting the data is = " + conversionTimeElapsed + " milliseconds");
		log.info("The runtime for analyse data and entering it into the database is = " + annotationTimeElapsed + " milliseconds");
		
	}
	
	private String tranferFileToLiferay(fileFormat outputFormat, fileFormat inputFormat, String inputFilename, String inputPath, long groupId, long folderId){
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
			
			ServiceUtils.sendToLiferay(groupId, folderId, inputPath, outputFileName, orign.length(), fis);
			
			String name = inputFilename.substring(0, inputFilename.lastIndexOf(".")); // file name minus extension.

			File heaFile = new File(inputPath + name + ".hea");
			if (inputFormat != ECGformatConverter.fileFormat.WFDB) {
				if (heaFile.exists()) {
					orign = new File(inputPath + heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1));
					fis = new FileInputStream(orign);
					ServiceUtils.sendToLiferay(groupId, folderId, inputPath, heaFile.getName().substring(heaFile.getName().lastIndexOf(sep) + 1), orign.length(), fis);
				}
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			errorMessage =  e.toString();
		}
		return errorMessage;
	}

	private void convertLeadAnnotations(ArrayList<AnnotationData[]> allLeadAnnotations) {
		for(int i=0; i<allLeadAnnotations.size(); i++) {
			if(allLeadAnnotations.get(i).length != 0) {
				log.debug("There are annotations in this lead.  The size is " + allLeadAnnotations.get(i).length);
				convertAnnotations(allLeadAnnotations.get(i), true, "");
			}
		}
	}
	
	private void convertNonLeadAnnotations(ArrayList<AnnotationData> allAnnotations, String groupName){
		AnnotationData[] annotationArray = new AnnotationData[allAnnotations.size()];
		annotationArray = allAnnotations.toArray(annotationArray);
		
		convertAnnotations(annotationArray, false, groupName);
	}
	
	private boolean convertAnnotations(AnnotationData[] annotationArray, boolean isLeadAnnotation, String groupName) {
		boolean success = true;
		
		for(AnnotationData annData : annotationArray) {			
			
			
			if(isLeadAnnotation) {
				log.debug("Storing lead index " + annData.getLeadIndex());
				success = dbAnnUtility.storeLeadAnnotationNode(annData);
				
			}else {
				success = dbAnnUtility.storeComment(annData);
			}
			
			if(!success){
				break;	
			}
		}
		return success;
	}


	private AnnotationUtility getDbUtility() {
		ServiceProperties properties = ServiceProperties.getInstance();
		AnnotationUtility dbAnnUtility = new AnnotationUtility(properties.getProperty("dbUser"),
															   properties.getProperty("dbPassword"), 
															   properties.getProperty("dbURI"),	
															   properties.getProperty("dbDriver"),
															   properties.getProperty("dbMainDatabase"));

		dbAnnUtility.initialize();
		return dbAnnUtility;
	}
}
