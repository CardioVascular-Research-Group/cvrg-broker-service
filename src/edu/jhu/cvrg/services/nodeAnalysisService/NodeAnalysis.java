package edu.jhu.cvrg.services.nodeAnalysisService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.net.ftp.FTP;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformer;

import edu.jhu.cvrg.waveform.model.ApacheCommonsFtpWrapper;
import edu.osu.bmi.matlab.MatClient;

/** This web service contains several ecg analysis algorythms.
 * 
 * @author Michael Shipway, based on work by Bill Girten
 *
 */
public class NodeAnalysis {

	private String sep = File.separator;
	private boolean isFileExisting;
	private String matlabFunction = "analyzeqt";
	private String parentFolderNode = "/export/cvrgftp";
	private String outputFolderNodeBerger = "/export/BergerOut";
	private static Properties mFileLocations = new Properties();
	private String MATLAB_VIEWHOLT_LOCATION = "/opt/qt_usuhs";
	private boolean debugMode = true;

	@SuppressWarnings("deprecation")
	public org.apache.axiom.om.OMElement runBergerAlgorithm(org.apache.axiom.om.OMElement param0) {
		
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeAnalysisService/","nodeAnalysisService");
        OMElement bergerAlgorithmResults = fac.createOMElement("runBergerAlgorithm", omNs);
        Iterator iterator = param0.getChildren();
        String userId = ((OMElement)iterator.next()).getText();
        String subjectId = ((OMElement)iterator.next()).getText();
        String rdtFileName = ((OMElement)iterator.next()).getText();
        
        String ftpHost = ((OMElement)iterator.next()).getText();
        String ftpUser = ((OMElement)iterator.next()).getText();
        String ftpPassword = ((OMElement)iterator.next()).getText();
        boolean isPublic = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
        int separatorPosition = rdtFileName.lastIndexOf("/");
		
        String rdtFile = rdtFileName.substring(separatorPosition + 1);
        String outputRdtFile = userId + "_" + subjectId + "_" + rdtFile;
        Date curDate = new Date();
    	String date = new Integer(curDate.getYear() + 1900).toString() + new Integer(curDate.getMonth() +1).toString() + new Integer(curDate.getDate()).toString();
    	date = date + new Integer(curDate.getHours()).toString() + new Integer(curDate.getMinutes()).toString() + new Integer(curDate.getSeconds()).toString();
    	Random rand = new Random();
    	date=date + rand.nextInt(1000);
        String newPathName = outputFolderNodeBerger + sep + date + userId + subjectId;
        File newPath = new File(newPathName);
        try {
        	ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
	        System.out.println("downloading: " + rdtFileName + "from: " + ftpHost);	        
        	
        	newPath.mkdir();
	        ftpClient.downloadFile(rdtFileName,newPathName + sep + outputRdtFile);
	        String iniFile = rdtFile.substring(0, rdtFile.lastIndexOf(".")+1) + "ini";
	        String outputIniFile = outputRdtFile.substring(0, outputRdtFile.lastIndexOf(".")+1) + "ini";
	        ftpClient.downloadFile(iniFile,	newPathName + sep + outputIniFile);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        

        String outputFilename = newPathName + sep + outputRdtFile.substring(0,outputRdtFile.lastIndexOf("."));
        String inputDirectory = newPathName;
        
		System.out.println("qtvi analysis matlab function: " + matlabFunction);
		System.out.println("Executing matlab code in directory: " + MATLAB_VIEWHOLT_LOCATION);
		System.out.println("INPUT: " + inputDirectory);
		System.out.println("OUTPUT: " + outputFilename);

		String parms = outputFilename + " " + inputDirectory + " " + inputDirectory;
		mFileLocations.put(this.matlabFunction, MATLAB_VIEWHOLT_LOCATION);
		MatClient matClient = new MatClient();
		try {
			matClient.createJob("clear all");
			matClient.createJob("close all");
			matClient.createJob("addpath " + mFileLocations.get(matlabFunction));
			matClient.createJob(matlabFunction + " " + parms);
			matClient.createJob("bye");
	        isFileExisting = true;
		} catch(Exception ex) {
			ex.printStackTrace();
		}

        if(isFileExisting) {
    	    try {
    	    	ApacheCommonsFtpWrapper m_client = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);

    	    	m_client.setFileType(FTP.BINARY_FILE_TYPE);
    	    	
    	        FileInputStream in = new FileInputStream(outputFilename + ".csv");

    	        int pos = rdtFile.lastIndexOf(".");
    	        String putFilename = rdtFile.substring(0, pos);
    	    	String temp = "public";
    	        if(isPublic) {
    	        	
    	        } else {
    	        	temp = "private";
    	        }
    	        String ftpFilePath = parentFolderNode + sep + userId + sep
    			+ temp + sep + subjectId
    			+ sep + "output/berger/";
    	        
    	        debugPrintln(ftpFilePath + putFilename + ".csv");
    	        m_client.changeWorkingDirectory(ftpFilePath);
    	        m_client.uploadFile(outputFilename + ".csv", putFilename + ".csv");
    	        
    	        in.close();
    			bergerAlgorithmResults.addChild(fac.createOMText("" + "SUCCESS"));
    			
    	    } catch (Exception ex) {
    	        ex.printStackTrace();
    	        bergerAlgorithmResults.addChild(fac.createOMText("" + "FAILURE"));
    	    }

		} else {
			bergerAlgorithmResults.addChild(fac.createOMText("" + "FAILURE"));
		}
        bergerToXML(userId, subjectId, rdtFileName, ftpHost, ftpUser, ftpPassword, newPathName);
		return bergerAlgorithmResults;
	}
	
	private org.apache.axiom.om.OMElement bergerToXML(String userId, String subjectId, String fileName, String ftpHost, String ftpUser, String ftpPassword, String stageDir) {
		String row = null;
    	BufferedReader in = null;
    	StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    			+ "<qtviResults>\n");
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.example.org/nodeConversionService/",
        		"nodeConversionService");
        OMElement nodeConversionStatus = fac.createOMElement("nodeConversionStatus", omNs);
        String bareFileName = fileName.substring(fileName.lastIndexOf("/")+1);
        String outputFileName = userId + "_" + subjectId + "_" + bareFileName; 
        String csvFilename = stageDir + sep + outputFileName;
        csvFilename = csvFilename.substring(0, csvFilename.length() - 3) + "csv";
        debugPrintln("converting " + csvFilename);
   		try {
            in = new BufferedReader(new FileReader(csvFilename));
            String[] headings = new String[0];
			StringTokenizer tokenizer = null;
			int ptr = 0;
            while((row = in.readLine()) != null) {
				tokenizer = new StringTokenizer(row, ",");
				ptr = 0;
				if(headings.length == 0) {
					headings = new String[tokenizer.countTokens()];
					while(tokenizer.hasMoreTokens()) {
						headings[ptr] = tokenizer.nextToken();
						headings[ptr] = headings[ptr].replaceAll(" ", "");
						headings[ptr] = headings[ptr].replaceAll("[*]", "_");
						headings[ptr] = headings[ptr].replaceAll("[(]", "_");
						headings[ptr] = headings[ptr].replaceAll("[)]", "");
						ptr++;
					}
					ptr = 0;
				} else {
		            sb.append("<qtviResult>\n");
					while(tokenizer.hasMoreTokens()) {
						sb.append("<" + headings[ptr].trim() + ">"
								+ tokenizer.nextToken().trim()
								+ "</" + headings[ptr].trim() + ">\n");
						ptr++;
					}
		            sb.append("</qtviResult>\n");
				}
            }
            sb.append("</qtviResults>");
            String xmlOut = sb.toString();
            debugPrintln("writing " + xmlOut);
			String xmlFilename = csvFilename.replaceAll("csv", "xml");
			debugPrintln("to " + xmlFilename);
			BufferedWriter out = new BufferedWriter(new FileWriter(xmlFilename));
			out.write(xmlOut);
			out.close();
			ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			String xmlDestinationFilename = bareFileName.substring(0,bareFileName.lastIndexOf(".")+1) + "xml";
			System.out.println(xmlFilename + " to " + parentFolderNode + fileName.substring(0,fileName.lastIndexOf(sep)+1) + xmlDestinationFilename);
			String destDir = parentFolderNode + fileName.substring(0,fileName.lastIndexOf(sep)+1).replace("input", "output/berger");
			ftpClient.uploadFile(xmlFilename, destDir + xmlDestinationFilename);
			in.close();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
   		
        nodeConversionStatus.addChild(fac.createOMText("" + "SUCCESS"));
		return nodeConversionStatus;
	}
	
	
	public org.apache.axiom.om.OMElement runChesnokovAlgorithm(org.apache.axiom.om.OMElement param0) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeAnalysisService/",
        		"nodeAnalysisService");
        OMElement chesnokovAlgorithmResults = fac.createOMElement("runChesnokovAlgorithm", omNs);
        Iterator iterator = param0.getChildren();
        String userId = ((OMElement)iterator.next()).getText();
        String subjectId = ((OMElement)iterator.next()).getText();
        String wfdbFileName = ((OMElement)iterator.next()).getText();
        System.out.println("Chesnokov wfdb File Name: " + wfdbFileName);
        String ftpHost = ((OMElement)iterator.next()).getText();
        String ftpUser = ((OMElement)iterator.next()).getText();
        String ftpPassword = ((OMElement)iterator.next()).getText();
        int separatorPosition = wfdbFileName.lastIndexOf("/");
        String ftpSourceDir = wfdbFileName.substring(0, separatorPosition); // ftp path of the source file.
        String datFile = wfdbFileName.substring(separatorPosition + 1);  // ftp name of the source file.
        String local_datFile = userId + "_" + subjectId + "_" + datFile; // local name of the destination file.

        //download the rdt file to the staging area
        boolean success = true;
        try {
        	ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword, true);
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

	        debugPrintln("------------- runChesnokovAlgorithm -------------- Version 2.0 ------");
	        debugPrintln("    calling ftpClient.downloadFile(ftpSourceDir, datFile, parentFolderNode, local_datFile);");	        
	        success = ftpClient.downloadFile(ftpSourceDir, datFile, parentFolderNode, local_datFile);	        

	        ftpClient.logout();
	        ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword, true);
	        ftpClient.setFileType(FTP.ASCII_FILE_TYPE);

	        String heaFileAtFtp = datFile.substring(0, datFile.lastIndexOf(".")+1) + "hea";
	        String heaFileLocally = local_datFile.substring(0,local_datFile.lastIndexOf(".")+1)+"hea";
	        debugPrintln("----------------------------");
	        debugPrintln("    calling ftpClient.downloadFile(ftpSourceDir, heaFileAtFtp, parentFolderNode, heaFileLocally);");	        
	        success = ftpClient.downloadFile(ftpSourceDir, heaFileAtFtp, parentFolderNode, heaFileLocally);	        
	        debugPrintln("----------------------------");
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	        debugPrintln("download success = " + success);
	        debugPrintln("----------------------------");
        }
        catch (Exception e) {
        	debugPrintln("Download FAILED");
        	e.printStackTrace();
        	success = false;
        }

        // execute Chesnokov analysis.
        String datFilePath = parentFolderNode + sep + local_datFile; 
        //build the wine command 
        String chesnokovOutputFilenameXml = local_datFile.substring(0, local_datFile.lastIndexOf(".") + 1) + "xml";
        String chesnokovOutputFilePathXml = parentFolderNode + sep + chesnokovOutputFilenameXml;
        String winePrefix = "/usr/bin/wine /opt/autoqrs/Release/ecg.exe /opt/autoqrs/Release/filters";
		String cmd = winePrefix + " " + datFilePath + " " + chesnokovOutputFilePathXml; // add parameters for "input file" and "output file"
		
        if(success){
	        debugPrintln("executing shell command: ");
	        debugPrintln("  "+ cmd);
			try {
				ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
				pb.redirectErrorStream(true);
				Process shell = pb.start();
				InputStream shellIn = shell.getInputStream();
				int c;
				while((c = shellIn.read()) != -1) System.out.write(c);
				shellIn.close();
				debugPrintln("Shell command finished.");
		        debugPrintln("----------------------------");
			}
			catch (Exception e) {
				e.printStackTrace();
				success = false;
			}
        }
        String chesnokovCSVFilepath="";
        if(success){			
        	try {
		        debugPrintln("calling chesnokovToCSV(chesnokovOutputFilename)");
		        chesnokovCSVFilepath = chesnokovToCSV(chesnokovOutputFilePathXml, parentFolderNode + sep + local_datFile, datFile);
		        debugPrintln("----------------------------");
        	}catch(Exception e) {
				e.printStackTrace();
				success = false;
			}
        }
        if(success){
        	// copy result file to ftp server.
			try {
		        ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
		        String ftpOutDir = ftpSourceDir.replaceAll("input","output/chesnokov");
		        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		        debugPrintln("-------------- uploading xml result file --------------");
		        String filepathXmlToFtp = parentFolderNode + ftpOutDir + sep + datFile.replace(".dat",".xml");
		        debugPrintln("local xml file: " + chesnokovOutputFilePathXml);
		        debugPrintln("ftp destination: " + filepathXmlToFtp);

		        System.out.println("sending " + chesnokovOutputFilePathXml + " to " + filepathXmlToFtp);
		        debugPrintln("calling ftpClient.uploadFile(chesnokovOutputFilename, filetoFtp);");
		        ftpClient.uploadFile(chesnokovOutputFilePathXml, filepathXmlToFtp);
		        
		        debugPrintln("------------- uploading csv result file ---------------");

		        String filepathCsvToFtp = parentFolderNode + ftpOutDir + datFile.replace(".dat",".csv");
		        debugPrintln("local csv file: " + chesnokovCSVFilepath);
		        debugPrintln("ftp destination: " + filepathCsvToFtp);

		        System.out.println("sending " + chesnokovCSVFilepath + " to " + filepathXmlToFtp);
		        debugPrintln("calling ftpClient.uploadFile(chesnokovCSVFilepath, filepathCsvToFtp)");
		        ftpClient.uploadFile(chesnokovCSVFilepath, filepathCsvToFtp);
		        debugPrintln("----------------------------");
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        }
	        chesnokovAlgorithmResults.addChild(fac.createOMText("SUCCESS"));
		}else{
	        chesnokovAlgorithmResults.addChild(fac.createOMText("FAILURE"));			
		}
		
		return chesnokovAlgorithmResults;
	}
	

	/** Transforms the xml file output by chesnokov into a csv database file using chesnokov_datatable.xsl,
	 * saves the resulting csv file under the same path and name but with the csv extension.
	 * 
	 * @param fileName - xml file to transform.
	 * @param fileAnalyzedTempName - name of the temporary file which generated this xml output.
	 * @param fileAnalyzedFinalName - name to substitute for the fileAnalyzedTempName in the final output (csv) file. e.g. what the original data file (.dat) is named.
	 * 
	 * @return - file path/name of the resulting csv file, with the full path and extension.
	 */
	private String chesnokovToCSV(String fileName, String fileAnalyzedTempName, String fileAnalyzedFinalName) {
		Document xmlDoc = null;
        Document transformed = null;
        InputStream xsltIS = null;
        XSLTransformer xslTransformer = null;
		String row = null;
    	String xhtml = null;
    	BufferedReader in = null;
    	StringBuffer sb = new StringBuffer();
        String chesnokovFilename = fileName;
        String csvOutputFilename = "";
        debugPrintln(" ** converting " + chesnokovFilename);
   		try {
            in = new BufferedReader(new FileReader(chesnokovFilename));
            while((row = in.readLine()) != null) {
            	if(row.indexOf("<autoQRSResults") != -1) {
                	sb.append("<autoQRSResults>");
            	} else {
                	sb.append(row);
            	}
            }
            in.close();
            xmlDoc = build(sb.toString());
            xsltIS = this.getClass().getResourceAsStream("chesnokov_datatable.xsl");
			xslTransformer = new XSLTransformer(xsltIS);
			transformed = xslTransformer.transform(xmlDoc);
			xhtml = getString(transformed);
			debugPrintln(" ** xslTransformation completed using: " + xsltIS.toString());

			int truncPosition = xhtml.indexOf("<html>");
			xhtml = xhtml.substring(truncPosition, xhtml.length());
			xhtml = xhtml.replaceAll("<html>", "");
			xhtml = xhtml.replaceAll("</html>", "");
			csvOutputFilename = chesnokovFilename.replaceAll("xml", "csv");

			debugPrintln(" ** replacing : " + fileAnalyzedTempName + " with: " + fileAnalyzedFinalName);
			xhtml = xhtml.replaceAll(fileAnalyzedTempName, fileAnalyzedFinalName);
			
			debugPrintln(" ** writing " + csvOutputFilename);
			BufferedWriter out = new BufferedWriter(new FileWriter(csvOutputFilename));
			out.write(xhtml);
			out.close();
		   
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
		return csvOutputFilename;
	}
	
	
	public org.apache.axiom.om.OMElement runQRSscoreAlgorithm(org.apache.axiom.om.OMElement param0) {
		boolean temp_debugMode = debugMode;
		// Parse the parameters received from the web page.
		OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeAnalysisService/",
        		"nodeAnalysisService");
        OMElement qRSscoreAlgorithmResults = fac.createOMElement("runQRSscoreAlgorithm", omNs);
        Iterator iterator = param0.getChildren();
        String userId = ((OMElement)iterator.next()).getText();
        String subjectId = ((OMElement)iterator.next()).getText();
        String magellanFileName = ((OMElement)iterator.next()).getText();
        String ftpHost = ((OMElement)iterator.next()).getText();
        String ftpUser = ((OMElement)iterator.next()).getText();
        String ftpPassword = ((OMElement)iterator.next()).getText();
    	boolean debugMode = Boolean.parseBoolean(((OMElement)iterator.next()).getText());
        if (debugMode) System.out.println("Magellan-outputted File Name: " + magellanFileName);

        int separatorPosition = magellanFileName.lastIndexOf(sep);
        String ftpIn = magellanFileName.substring(0, separatorPosition + 1);
        String magellanFile = magellanFileName.substring(separatorPosition + 1);
        String magellanLocalTempFile = userId + "_" + System.currentTimeMillis() + "_" + subjectId + "_" + magellanFile;
        String localFile = parentFolderNode + sep + magellanLocalTempFile;
        if (debugMode) System.out.println("ftpIn: " + ftpIn);
        if (debugMode) System.out.println("magellanFile: " + magellanFile);
        if (debugMode) System.out.println("local path (parentFolderNode): " + parentFolderNode);
        if (debugMode) System.out.println("Local Name (magellanLocalTempFile): " + magellanLocalTempFile);
        if (debugMode) System.out.println("localFile: " + localFile);

        //download the magellan file to the staging area
        try {
	        ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
//	        ftpClient.binary();
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	        ftpClient.downloadFile(ftpIn, magellanFile,
		    		parentFolderNode, magellanLocalTempFile);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        
        String qRSscoreOutputFilename = magellanLocalTempFile.substring(0,
        		magellanLocalTempFile.lastIndexOf(".")) + ".csv";
        String qRSscoreOutputFull = parentFolderNode + sep + qRSscoreOutputFilename;
        if (debugMode) System.out.println("Local result file(qRSscoreOutputFull): " + qRSscoreOutputFull);

        
        // Execute the analysis 
        try {
			FileToScore parser = new FileToScore(localFile, qRSscoreOutputFull, debugMode);
//			parser.setVerbose(debugMode);
			if (debugMode) System.out.println("");
			if (debugMode) System.out.println("Done, input file contained " + parser.getLineCount() + " lines, " + parser.getDatasetCount() +  " datasets found and processed.");
			if (debugMode) System.out.println("Number of each conduction types found:");
			int total=0;
			for (Iterator it = parser.getMapTypesFound().entrySet().iterator(); it.hasNext();) {
				Map.Entry<String,Integer> entry = (Map.Entry) it.next();
				String key = entry.getKey();
				Integer value = entry.getValue();
				if (debugMode) System.out.println(key + ": " + value);
				total += value;
				// do something with the key and the value
			}
			if (debugMode) System.out.println("Total (for verification): " + total);
			if (debugMode) System.out.println("number re-classified: " + parser.getReclassified());
		} catch (Exception e1) {
			e1.printStackTrace();
		} 
		boolean isFileExisting = true;
		// Save results file to FTP
		try {
	        ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
	        String ftpOut = ftpIn.replaceAll("input","output/qrsscore");
	        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	        String qRSscoreDestinationFilename = magellanFile.substring(0,magellanFile.lastIndexOf(".")) + ".csv"; 
	        if (debugMode) System.out.println("sending " + qRSscoreOutputFull + " to " + parentFolderNode + ftpOut + qRSscoreDestinationFilename);
	        isFileExisting = ftpClient.uploadFile(qRSscoreOutputFull, parentFolderNode + ftpOut + qRSscoreDestinationFilename);
        	removeFromStagingArea(qRSscoreOutputFull);
        	removeFromStagingArea(localFile);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
		
        if(isFileExisting) {
        	qRSscoreAlgorithmResults.addChild(fac.createOMText("" + "SUCCESS"));
		} else {
			qRSscoreAlgorithmResults.addChild(fac.createOMText("" + "FAILURE"));
		}
        
        debugMode = temp_debugMode; // return it to the original setting.
		return qRSscoreAlgorithmResults;
	}
	
	
	private void removeFromStagingArea(String file) {
		try {
			System.out.println("[NodeAnalysis]: " + file + " removed from staging area.");
			new File(file).delete();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
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
    private static Document build(String xmlDocAsString) 
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
    
    /**
     * Helper method to generate a String output of a
     * <code>org.jdom.Document</code>
     * @param  xmlDoc  Document XML document to be converted to String
     * @return <code>String</code> representation of an XML
     *         document with a document declaration.
     *         e.g., <?xml version="1.0" encoding="UTF-8"?>
     *                  <root><stuff>Some stuff</stuff></root>
     */
    private static String getString(Document xmlDoc) throws JDOMException {
        try {
             XMLOutputter xmlOut = new XMLOutputter();
             StringWriter stringwriter = new StringWriter();
             xmlOut.output(xmlDoc, stringwriter);
    
             return stringwriter.toString();
        } catch (Exception ex) {
            throw new JDOMException("Error converting Document to String"+ ex);
        }
    }
    
    private void debugPrintln(String text){
		if(debugMode)	System.out.println(">>> " + text);
	}

	
}