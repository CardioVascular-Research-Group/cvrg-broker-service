package edu.jhu.cvrg.services.nodeDataService;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.net.ftp.FTP;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import edu.jhu.cvrg.services.brokerSvcUtils.BrokerSvcUtils;
import edu.jhu.cvrg.services.brokerSvcUtils.DataUtils;
import edu.jhu.cvrg.waveform.model.ApacheCommonsFtpWrapper;
import edu.jhu.cvrg.zipconverter.ZipConverter;

/** A collection of methods for saving and retrieving data from files on the FTP service.
 * 
 * @author Mike Shipway based on code by Bill Girten
 * @author Stephen Granite
 * 
 * 20130430 - SJG - added code to nodeDataService to replace pipes with commas; meant to make hyperlinks in Excel work
 *
 */
public class DataStaging {
	private static final String CHESNOKOV_HEADER_INDICATOR = "File Analyzed";
	private static final String BERGER_HEADER_INDICATOR = "begin Analysis time =";

	/** local filesystem's root directory for ftp, <BR>e.g. /export/icmv058/cvrgftp **/
	private String localFtpRoot = "/export/icmv058/cvrgftp";  

	/** remote ftp server's root directory for ftp, <BR>e.g. /export/icmv058/cvrgftp **/
	private String remoteFtpRoot = "/export/icmv058/cvrgftp";  

	/** URL of the web page's root as seen from the internet<BR>e.g. http://icmv058.icm.jhu.edu:8080 **/
	private String urlWebPageRoot = "http://icmv058.icm.jhu.edu:8080";

	/** local filesystem's root directory for web pages files, <BR>e.g. /opt/apache-tomcat-6.0.32/webapps **/
	private String localWebPageRoot = "/opt/apache-tomcat-6.0.32/webapps";

	private String downloadDirectory = "/download"; // directory relative the webpage/local root which generated files can be downloaded from.

	private String sep = File.separator;
	private boolean verbose = true;
	private static final ByteOrder BYTEORDER = ByteOrder.LITTLE_ENDIAN;
	private static final int HEADERBYTES = 4;
	private static final int SHORTBYTES = 2;

	private boolean debugMode = true;
	protected final Logger logger = Logger.getLogger(getClass().getName());
	private BrokerSvcUtils utils = new BrokerSvcUtils(false);
	private StringBuffer analysisDataBuffer;

	/** Service to make final destination directories and transfer the file into it, via routeToFolder().<BR/>
	 *  Assumes that the file was already transfered to the ftp area.
	 * 
	 * @param param0 OMElement containing the parameters:<BR/>
	 *  userId, subjectId, fileName, ftpHost, ftpUser, ftpPassword, bExposure
	 * @return ??? always returns SUCCESS ???
	 */
	public org.apache.axiom.om.OMElement stageTransferredData(org.apache.axiom.om.OMElement param0) throws Exception {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeDataService/",
		"nodeDataService");
		OMElement stageTransferredDataStatus = fac.createOMElement("stageTransferredData", omNs);
		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();
		String subjectId = ((OMElement)iterator.next()).getText();
		String fileName = ((OMElement)iterator.next()).getText();
		String ftpHost = ((OMElement)iterator.next()).getText();
		String ftpUser = ((OMElement)iterator.next()).getText();
		String ftpPassword = ((OMElement)iterator.next()).getText();
		String sExposure = ((OMElement)iterator.next()).getText();

		boolean bExposure = new Boolean(sExposure).booleanValue();

		try {
			File xferFile = new File(localFtpRoot + sep + fileName);
			if (debugMode) System.out.println(">>>>>>>>>>>>>   staging " + xferFile);

			if(xferFile.exists()) {
				if (fileName.substring(fileName.lastIndexOf(".") + 1).equalsIgnoreCase("zip")) {

					int zipAttempt = 0;
					System.out.println("zip attempt: " + zipAttempt);
					boolean extractIndicator = extractZipFile(localFtpRoot, userId, fileName, ftpHost, ftpUser, ftpPassword, bExposure);
				}else {
					// Strip path from filename.
					String originalFileName="", ftpSubdir="";
					System.out.print("Getting position of '" + sep + "' in " + fileName + " length is :" + fileName.length());
					int pos = fileName.lastIndexOf(sep);
					System.out.println(" pos :" + pos);
					if(pos != -1) {
						ftpSubdir = fileName.substring(0, pos+1);
						System.out.println(" ftpSubdir :" + ftpSubdir);

						originalFileName = fileName.substring(pos+1);
						System.out.println(" originalFileName :" + originalFileName);
					}
					debugPrintln("localFtpRoot, fileName:" + localFtpRoot + ", " + fileName );
					debugPrintln("ftpSubdir, originalFileName:" + ftpSubdir + ", " + originalFileName );

					utils.routeToFolder(localFtpRoot, fileName, userId, subjectId, originalFileName,
							ftpHost, ftpUser, ftpPassword, remoteFtpRoot,
							bExposure);					
				}
			}
			stageTransferredDataStatus.addChild(fac.createOMText("" + "SUCCESS"));
		}
		catch (Exception e) { 
			e.printStackTrace();
			stageTransferredDataStatus.addChild(fac.createOMText("Error: " + e.toString()));
			throw e;
		}
		finally { return stageTransferredDataStatus; }
	}

	/** Service to make final destination directories and transfer the file into it, via routeToFolder().<BR/>
	 *  Assumes that the file was already transfered to the ftp area.
	 * 
	 * @param param0 OMElement containing the parameters:<BR/>
	 *  userId, subjectId, fileName, ftpHost, ftpUser, ftpPassword, bExposure
	 * @return ??? always returns SUCCESS ???
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")
	public org.apache.axiom.om.OMElement consolidateCsvs(org.apache.axiom.om.OMElement param0) throws Exception {
		System.out.println("# consolidateCsvs() #");
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeDataService/",
		"nodeDataService");
		OMElement stageTransferredDataStatus = fac.createOMElement("stageTransferredData", omNs);
		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();
		String chesSubjectIds = ((OMElement)iterator.next()).getText();
		String bergSubjectIds = ((OMElement)iterator.next()).getText();
		String chesFileNames = ((OMElement)iterator.next()).getText();
		String bergFileNames = ((OMElement)iterator.next()).getText();

		Boolean isPublic = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
		String ftpHost = ((OMElement)iterator.next()).getText();
		String ftpUser = ((OMElement)iterator.next()).getText();
		String ftpPassword = ((OMElement)iterator.next()).getText(); 
		String service = ((OMElement)iterator.next()).getText(); 
		long logindatetime = new Long ( ((OMElement)iterator.next()).getText()).longValue();
		String qrsScoreSubjectIds = ((OMElement)iterator.next()).getText();
		String qrsScoreFileNames = ((OMElement)iterator.next()).getText();
		try{
			verbose = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
		}catch(NoSuchElementException nseEx){
			System.out.println("'iteration has no more elements' Exception while parsing 'verbose' in consolidateCsvs()");
		}
		String publicOrPrivate = null;
		if (isPublic) { 
			publicOrPrivate = "public"; 
		}else {
			publicOrPrivate = "private"; 
		}

		debugPrintln("userId: \"" + userId+ "\"");
		debugPrintln("chesSubjectIds: \"" + chesSubjectIds+ "\"");
		debugPrintln("bergSubjectIds: \"" + bergSubjectIds+ "\"");
		debugPrintln("chesFileNames: \"" + chesFileNames+ "\"");
		debugPrintln("bergFileNames: \"" + bergFileNames+ "\"");
		debugPrintln("isPublic: \"" + isPublic+ "\"");
		debugPrintln("ftpHost: \"" + ftpHost+ "\"");
		debugPrintln("ftpUser: \"" + ftpUser+ "\"");
		debugPrintln("ftpPassword: \"" + ftpPassword+ "\"");
		debugPrintln("service: \"" + service+ "\"");
		debugPrintln("logindatetime: \"" + logindatetime+ "\"");

		debugPrintln("qrsScoreSubjectIds: \"" + qrsScoreSubjectIds+ "\"");
		debugPrintln("qrsScoreFileNames: \"" + qrsScoreFileNames+ "\"");

		debugPrintln("verbose: \"" + verbose + "\"");

		String timestamp = utils.generateTimeStamp();
		String allResultsFileName = "allResultsFiles_" + userId + "_" + timestamp + ".xls";
		String localAllResultsDirectory = findRelativePath("./", localWebPageRoot + sep + downloadDirectory); // put files to be allResults spreadsheet in here.
		String urlAllResultsDirectory = urlWebPageRoot + downloadDirectory; // URL as seen from the internet that is equivalent to localOutputDirectory above. e.g. http://icmv058.icm.jhu.edu:8080/download

		try {
			ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			ftpClient.verbose = verbose;

			System.out.println(userId + " Ches File Names: " + chesFileNames + "Berg File Names " + bergFileNames);
			System.out.println(userId + " Ches Subjects: " + chesSubjectIds + "Berg Subjects " + bergSubjectIds);
			FileOutputStream out = new FileOutputStream(localAllResultsDirectory + sep + allResultsFileName);
			HSSFWorkbook wbo = new HSSFWorkbook( );    	
			int numSheets = 0;

			if (!chesSubjectIds.equals("")) {
				HSSFSheet chesSheet = wbo.createSheet();
				chesSheet = consolidateAlgorithmFiles(0, chesSheet, chesSubjectIds, userId, chesFileNames,  isPublic, ftpClient);
				wbo.setSheetName(numSheets,"Chesnokov Results");
				++numSheets;
			}
			if (!bergSubjectIds.equals("")) {
				HSSFSheet bergerSheet = wbo.createSheet();
				bergerSheet = consolidateAlgorithmFiles(1, bergerSheet, bergSubjectIds, userId, bergFileNames, isPublic, ftpClient);
				wbo.setSheetName(numSheets,"Berger Results");
				++numSheets;
			}
			if (!qrsScoreSubjectIds.equals("")) {
				HSSFSheet qrsScoreSheet = wbo.createSheet();
				qrsScoreSheet = consolidateAlgorithmFiles(1, qrsScoreSheet, qrsScoreSubjectIds, userId, qrsScoreFileNames, isPublic, ftpClient);
				wbo.setSheetName(numSheets,"QRS-Score Results");
				++numSheets;
			}
			wbo.write(out);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		debugPrintln(bergSubjectIds);
		debugPrintln(chesSubjectIds);
		debugPrintln(qrsScoreSubjectIds);

		stageTransferredDataStatus.addChild(fac.createOMText(urlAllResultsDirectory + sep + allResultsFileName));
		debugPrintln("Returning results URL: \"" + urlAllResultsDirectory + sep + allResultsFileName + "\"");
		return stageTransferredDataStatus;
	}

	/** Service to make final destination directories and transfer the file into it, via routeToFolder().<BR/>
	 *  Assumes that the file was already transfered to the ftp area.
	 * 
	 * @param param0 OMElement containing the parameters:<BR/>
	 *  userId, subjectId, fileName, ftpHost, ftpUser, ftpPassword, bExposure
	 * @return ??? always returns SUCCESS ???
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")
	public org.apache.axiom.om.OMElement consolidateCsvsForWaveform(org.apache.axiom.om.OMElement param0) throws Exception {
		System.out.println("# consolidateCsvsForWaveform() #");
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeDataService/",
		"nodeDataService");
		OMElement stageTransferredDataStatus = fac.createOMElement("stageTransferredData", omNs);
		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();
		String chesSubjectIds = ((OMElement)iterator.next()).getText();
		String bergSubjectIds = ((OMElement)iterator.next()).getText();
		String chesFileNames = ((OMElement)iterator.next()).getText();
		String bergFileNames = ((OMElement)iterator.next()).getText();

		Boolean isPublic = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
		String ftpHost = ((OMElement)iterator.next()).getText();
		String ftpUser = ((OMElement)iterator.next()).getText();
		String ftpPassword = ((OMElement)iterator.next()).getText(); 
		String service = ((OMElement)iterator.next()).getText(); 
		long logindatetime = new Long ( ((OMElement)iterator.next()).getText()).longValue();
		String qrsScoreSubjectIds = ((OMElement)iterator.next()).getText();
		String qrsScoreFileNames = ((OMElement)iterator.next()).getText();
		try{
			verbose = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
		}catch(NoSuchElementException nseEx){
			System.out.println("'iteration has no more elements' Exception while parsing 'verbose' in consolidateCsvs()");
		}
		
		String publicOrPrivate = null;
		if (isPublic) { 
			publicOrPrivate = "public"; 
		} else { 
			publicOrPrivate = "private"; 
		}

		debugPrintln("userId: \"" + userId+ "\"");
		debugPrintln("chesSubjectIds: \"" + chesSubjectIds+ "\"");
		debugPrintln("bergSubjectIds: \"" + bergSubjectIds+ "\"");
		debugPrintln("chesFileNames: \"" + chesFileNames+ "\"");
		debugPrintln("bergFileNames: \"" + bergFileNames+ "\"");
		debugPrintln("isPublic: \"" + isPublic+ "\"");
		debugPrintln("ftpHost: \"" + ftpHost+ "\"");
		debugPrintln("ftpUser: \"" + ftpUser+ "\"");
		debugPrintln("ftpPassword: \"" + ftpPassword+ "\"");
		debugPrintln("service: \"" + service+ "\"");
		debugPrintln("logindatetime: \"" + logindatetime+ "\"");

		debugPrintln("qrsScoreSubjectIds: \"" + qrsScoreSubjectIds+ "\"");
		debugPrintln("qrsScoreFileNames: \"" + qrsScoreFileNames+ "\"");

		debugPrintln("verbose: \"" + verbose + "\"");

		String timestamp = utils.generateTimeStamp();
		String allResultsFileName = "allResultsFiles_" + userId + "_" + timestamp + ".xls";

		String localAllResultsDirectory = localFtpRoot; // put files to be allResults spreadsheet in here.

		try {
			ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			ftpClient.verbose = verbose;

			System.out.println(userId + " Ches File Names: " + chesFileNames + "Berg File Names " + bergFileNames);
			System.out.println(userId + " Ches Subjects: " + chesSubjectIds + "Berg Subjects " + bergSubjectIds);
			FileOutputStream out = new FileOutputStream(localAllResultsDirectory + sep + allResultsFileName);
			HSSFWorkbook wbo = new HSSFWorkbook( );    	
			int numSheets = 0;

			if (!chesSubjectIds.equals("")) {
				HSSFSheet chesSheet = wbo.createSheet();
				chesSheet = consolidateAlgorithmFiles(0, chesSheet, chesSubjectIds, userId, chesFileNames,  isPublic, ftpClient);
				wbo.setSheetName(numSheets,"Chesnokov Results");
				++numSheets;
			}
			if (!bergSubjectIds.equals("")) {
				HSSFSheet bergerSheet = wbo.createSheet();
				bergerSheet = consolidateAlgorithmFiles(1, bergerSheet, bergSubjectIds, userId, bergFileNames, isPublic, ftpClient);
				wbo.setSheetName(numSheets,"Berger Results");
				++numSheets;
			}
			if (!qrsScoreSubjectIds.equals("")) {
				HSSFSheet qrsScoreSheet = wbo.createSheet();
				qrsScoreSheet = consolidateAlgorithmFiles(1, qrsScoreSheet, qrsScoreSubjectIds, userId, qrsScoreFileNames, isPublic, ftpClient);
				wbo.setSheetName(numSheets,"QRS-Score Results");
				++numSheets;
			}
			wbo.write(out);
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		debugPrintln(bergSubjectIds);
		debugPrintln(chesSubjectIds);
		debugPrintln(qrsScoreSubjectIds);

		stageTransferredDataStatus.addChild(fac.createOMText(localAllResultsDirectory + sep + allResultsFileName));
		debugPrintln("Returning results path: \"" + localAllResultsDirectory + sep + allResultsFileName + "\"");
		return stageTransferredDataStatus;
	}

	/** Retrieves the original files which where uploaded, plus any format converted versions of ECGs for each check subject.
	 * Consolidates them all into a zip file in the webserver's download directory
	 * 
	 * @param param0
	 * @return
	 * @throws Exception
	 */
	public org.apache.axiom.om.OMElement consolidatePrimaryAndDerivedData(org.apache.axiom.om.OMElement param0) throws Exception {
		boolean success = true;
		System.out.println("consolidatePrimaryAndDerivedData() called ");
		
		OMFactory fac = OMAbstractFactory.getOMFactory(); 	 
		OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeDataService/","nodeDataService"); 	 
		OMElement stageTransferredDataStatus = fac.createOMElement("stageTransferredData", omNs); 	 
		Iterator iterator = param0.getChildren(); 
		
		String userId = ((OMElement)iterator.next()).getText(); 	 
		String subjectIds = ((OMElement)iterator.next()).getText(); 	 
		String fileNames = ((OMElement)iterator.next()).getText(); 	 
		Boolean isPublic = new Boolean(((OMElement)iterator.next()).getText()).booleanValue(); 	// not used anymore.
		String ftpHost = ((OMElement)iterator.next()).getText(); 	 
		String ftpUser = ((OMElement)iterator.next()).getText(); 	 
		String ftpPassword = ((OMElement)iterator.next()).getText(); 	 
		String service = ((OMElement)iterator.next()).getText(); 	 
		String logindatetime = ((OMElement)iterator.next()).getText(); 	 
		try{
			verbose = new Boolean(((OMElement)iterator.next()).getText()).booleanValue();
			System.out.println("'verbose' in consolidatePrimaryAndDerivedData():" + verbose);
		}catch(NoSuchElementException nseEx){
			System.out.println("'iteration has no more elements' Exception while parsing 'verbose' in consolidatePrimaryAndDerivedData()");
		}

		System.out.println("userId: " + userId);
		System.out.println("subjectIds: " + subjectIds);
		System.out.println("fileNames: " + fileNames);
		System.out.println("isPublic: " + isPublic);
		System.out.println("ftpHost: " + ftpHost);
		System.out.println("ftpUser: " + ftpUser);
		System.out.println("ftpPassword: " + ftpPassword);
		System.out.println("service: " + service);
		System.out.println("logindatetime: " + logindatetime);

		ApacheCommonsFtpWrapper ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
		ftpClient.verbose = verbose;

		String timestamp = utils.generateTimeStamp();
		String zippingName = userId + "_" + timestamp;
		String zippingDirectory = findRelativePath("./", localFtpRoot) + zippingName; // put files to be zipped in here.
		String zipFileName = "ECGFiles_" + zippingName + ".zip";

		String localOutputDirectory = localWebPageRoot + downloadDirectory; // directory on the local file system that is equivalent to urlDirectory above. e.g. /opt/apache-tomcat-6.0.32/webapps/download
		String urlOutputDirectory = urlWebPageRoot + downloadDirectory; // URL as seen from the internet that is equivalent to localOutputDirectory above. e.g. http://icmv058.icm.jhu.edu:8080/download
		String outputFilePermissions = "664";  // Unix permission to give the zip file in it's final output location

		System.out.println("timestamp: " + timestamp);
		System.out.println("zippingName: " + zippingName);
		System.out.println("zippingDirectory: " + zippingDirectory);
		System.out.println("zipFileName: " + zipFileName);
		System.out.println("localOutputDirectory: " + localOutputDirectory);
		System.out.println("urlOutputDirectory: " + urlOutputDirectory);
		System.out.println("outputFilePermissions: " + outputFilePermissions);
		
		
		getResultfromFTP(subjectIds, fileNames, zippingDirectory, ftpClient);

		boolean useManifest = true;

		// .nat for Bruce Nearing's 24 hour GE holter files *.nat (3 lead) format.  
		// .gtm for Bruce Nearing's 24 hour GE holter files *.GTM (12 lead) format.
		// .txt for GE-MUSE ECG files.
		String extOptions = "dat|ini|rdt|hea|nat|gtm|txt";
		System.out.println("Directory being sent to zipconverter: " + zippingDirectory);
		ZipConverter z = new ZipConverter(zippingDirectory, zippingDirectory + sep + zipFileName, useManifest, extOptions);

		deleteAllFilesExcept(zippingDirectory, zipFileName);
		success = moveFileToOtherDir(zippingDirectory, zipFileName, localOutputDirectory, outputFilePermissions);
		if (success){
			stageTransferredDataStatus.addChild(fac.createOMText(urlOutputDirectory + sep + zipFileName));
		}else{
			stageTransferredDataStatus.addChild(fac.createOMText(""));
		}
		return stageTransferredDataStatus;
	}


	/** Moves a file to another directory.
	 * 
	 * @param originDirectory - directory path the file starts in. 
	 * @param FileName - name with extension but not path of the file to move.
	 * @param destinationDirectory - directory path to move the file to.
	 * @param destinationFilePermissions - not implemented yet. Unix file permissions to apply to the file after the move, e.g. "664".
	 */
	private boolean moveFileToOtherDir(String originDirectory, String FileName, String destinationDirectory, String destinationFilePermissions){
		boolean success = false;

		debugPrintln("moveFileToOutputDir() called");
		debugPrintln("- originDirectory: " + originDirectory);
		debugPrintln("- FileName: " + FileName);
		debugPrintln("- destinationDirectory: " + destinationDirectory);
		debugPrintln("- destinationFilePermissions: " + destinationFilePermissions);

		InputStream inStream = null;
		OutputStream outStream = null;

		try{
			File afile =new File(originDirectory+sep+FileName);
			File bfile =new File(destinationDirectory+sep+FileName);

			inStream = new FileInputStream(afile);
			outStream = new FileOutputStream(bfile);

			byte[] buffer = new byte[1024];

			int length;
			//copy the file content in bytes 
			while ((length = inStream.read(buffer)) > 0){
				outStream.write(buffer, 0, length);
			}

			inStream.close();
			outStream.close();

			//delete the original file
			afile.delete();

			System.out.println("File is copied successful!");
			success=true;
		}catch(IOException e){
			e.printStackTrace();
		}

		if (success) {
			debugPrintln(" SUCCEEDED.");
		}else{
			// File was not successfully moved
			debugPrintln(" FAILED!");
		}

		return success;
	}

	/** Deletes all the files and empty directories in the directory except the one specified.
	 * 
	 * @param directory - to clear files from
	 * @param fileToKeep - name and extension of a single file to keep (not delete) e.g. zip to be downloaded.
	 * @throws Exception
	 */
	private void deleteAllFilesExcept(String directory, String fileToKeep) throws Exception{
		String doomedFile="";
		try {
			File dir = new File(directory);
			String[] zipFiles = dir.list();
			for (int i=0; i < zipFiles.length; i++) {
				if (!zipFiles[i].matches(fileToKeep)) {
					doomedFile = (String)zipFiles[i];
					debugPrintln("Deleting: "+ doomedFile);
					deleteTempFile(directory + sep + doomedFile);
				}		
			}			
		}
		catch (Exception e) {
			throw e;
		}
	}
	/*
	 * JBoss, Home of Professional Open Source
	 * Copyright 2005, JBoss Inc., and individual contributors as indicated
	 * by the @authors tag. See the copyright.txt in the distribution for a
	 * full listing of individual contributors.
	 *
	 * This is free software; you can redistribute it and/or modify it
	 * under the terms of the GNU Lesser General Public License as
	 * published by the Free Software Foundation; either version 2.1 of
	 * the License, or (at your option) any later version.
	 *
	 * This software is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
	 * Lesser General Public License for more details.
	 *
	 * You should have received a copy of the GNU Lesser General Public
	 * License along with this software; if not, write to the Free
	 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
	 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
	 */
	/**
	 * Build a relative path to the given base path.
	 * @param base - the path used as the base
	 * @param path - the path to compute relative to the base path
	 * @return A relative path from base to path
	 * @throws IOException
	 */ 
	public static String findRelativePath(String base, String path)
	throws IOException
	{
		String a = new File(base).getCanonicalFile().toURI().getPath();
		String b = new File(path).getCanonicalFile().toURI().getPath();
		String[] basePaths = a.split("/");
		String[] otherPaths = b.split("/");
		int n = 0;
		for(; n < basePaths.length && n < otherPaths.length; n ++)
		{
			if( basePaths[n].equals(otherPaths[n]) == false )
				break;
		}
		System.out.println("Common length: "+n);
		StringBuffer tmp = new StringBuffer("../");
		for(int m = n; m < basePaths.length - 1; m ++)
			tmp.append("../");
		for(int m = n; m < otherPaths.length; m ++)
		{
			tmp.append(otherPaths[m]);
			tmp.append("/");
		}

		return tmp.toString();
	}


	/** performs the core functions of consolidatePrimaryAndDerivedData(), 
	 * Gets the listed files from the ftp server and puts them in a local directory
	 * 
	 * @param subjectIds - Not actually used anymore. "^" separated list of subject IDs to download files for. 
	 * @param fileNames - "^" separated list of file name to fetch, full paths.
	 * @param destinationDir - Temporary local directory that the files will be copied to so they can be zipped. 
	 * @param ftpClient - ftp client that is already logged in and ready to transfer files.
	 */
	private void getResultfromFTP(String subjectIds, String fileNames, String destinationDir, ApacheCommonsFtpWrapper ftpClient){
		debugPrintln(" > getResultfromFTP(): Fetching files from FTP:" + fileNames);
		StringTokenizer fileTokenizer = new StringTokenizer(fileNames, "^");
		String currentFile;
		while (fileTokenizer.hasMoreTokens()) {

			currentFile = fileTokenizer.nextToken();

			// Determine the folder this file is in.
			String currentFolder = currentFile.substring(0, currentFile.lastIndexOf(sep));

			try {
				File f = new File(destinationDir); // create destination directory 	        
				f.mkdir();
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			} catch (IOException e1) {
				e1.printStackTrace();
			}


			try {

				debugPrintln(" > FTP Folder:" + currentFolder);
				ftpClient.changeWorkingDirectory(currentFolder);

				String[] s = ftpClient.listNames(currentFolder);
				debugPrintln(" > Copying " + s.length + " files via ftp into directory: " + destinationDir);
				for (int i=0; i<s.length; i++) {
					debugPrintln(" > " + i + ") " + currentFile);
					ftpClient.downloadFile(s[i], destinationDir + sep + s[i].substring(s[i].lastIndexOf(sep) + 1));
				}

			} catch (FileNotFoundException e) {  
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private HSSFSheet consolidateAlgorithmFiles(int algorithmCode, HSSFSheet sheet, String subjectIds, String userId, String fileNames, boolean isPublic, ApacheCommonsFtpWrapper ftpClient) {

		boolean firstToken = true;
		StringTokenizer tokenizer = new StringTokenizer(subjectIds, "^");
		StringTokenizer fileTokenizer = new StringTokenizer(fileNames, "^");
		String currentSubject = null;
		int lineNumber = 0;
		String headerIndicator = null;
		switch (algorithmCode) {
			case 0: 
				headerIndicator = CHESNOKOV_HEADER_INDICATOR; 
				break;
			case 1: 
				headerIndicator = BERGER_HEADER_INDICATOR; 
				break;
		}
		String directory = localFtpRoot + sep + userId + utils.generateTimeStamp();
		while (tokenizer.hasMoreTokens() && fileTokenizer.hasMoreTokens()) {
			currentSubject = tokenizer.nextToken();
			System.out.println(currentSubject);
			String file = fileTokenizer.nextToken();
			String bareFile = file.substring(file.lastIndexOf("/") + 1);
			try {
				ftpClient.downloadFile(file, directory + bareFile);    	
				FileReader fi = new FileReader(directory + bareFile);
				BufferedReader br = new BufferedReader(fi);
				String thisLine = null, value = null;
				int inputLineNumber = 0;
								
				while ((thisLine = br.readLine()) != null) {
					boolean isHeader = false;
					isHeader = thisLine.contains(headerIndicator);
					if (thisLine != null && thisLine.length() > 0 && (firstToken || !isHeader) ) {
						HSSFRow rowOut = sheet.createRow(lineNumber);
						StringTokenizer rowTokenizer = new StringTokenizer(thisLine, ",");
						HSSFCell cellOut;
						int colNumber = 0;
						while (rowTokenizer.hasMoreTokens()) {

							cellOut = rowOut.createCell(colNumber);
							String replacePipes = rowTokenizer.nextToken(); 
							if (isHeader) {
								
								replacePipes = replacePipes.replaceAll("\\|", ",");
								value = replacePipes;
								StringTokenizer temp = new StringTokenizer(replacePipes, ",");
								if (temp.countTokens() > 1) {
								
									HSSFHyperlink link = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
									link.setAddress(temp.nextToken());
									cellOut.setHyperlink(link);
									value = temp.nextToken().trim();
								
								} 								
								cellOut.setCellValue(value);
								sheet.setColumnWidth(colNumber, (value.length()+3)*256);
								
							} else {
								
								cellOut.setCellValue(replacePipes);
								if (sheet.getColumnWidth(colNumber)/256 < replacePipes.length())
									sheet.setColumnWidth(colNumber, replacePipes.length()*256);
							}
							colNumber++;
						}
						lineNumber++;
					}
					inputLineNumber++;  

				}
				fi.close();
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			//}
			firstToken = false;

		}

		return sheet;
	}



/** Extracts the files from a zip and puts them in sub-directories based on the subjectID .csv file found in the zip.
 * if a file is not listed in the csv, the filename is converted to lowercase and used as the subjectID.
 * 
 * @param parentFolder
 * @param uId
 * @param fileName
 * @param ftpHost
 * @param ftpUser
 * @param ftpPassword
 * @param bExposure
 * @return
 * @throws Exception
 */
	private boolean extractZipFile(String parentFolder, String uId, String fileName,  
			String ftpHost, String ftpUser, String ftpPassword, boolean bExposure) throws Exception {
		
		boolean nullCsv = true;
		String fileDir = fileName.substring(0,fileName.lastIndexOf(sep));
		if (fileName.substring(0,1).equals(sep)) {
			fileName = fileName.substring(1,fileName.length());
		}
		String bareFileName = fileName.substring(fileName.lastIndexOf(sep)+1);
		System.out.println("beginning file name " + fileName);
		
		System.out.println(localFtpRoot + sep + fileName);

		//unzip the file
		File zipFile = new File(localFtpRoot + sep + fileName);
		System.out.println("First Zip file length at 0 seconds: " + zipFile.length());
		long firstZipLength = zipFile.length();
		try {
			Thread.currentThread().sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long secondZipLength = zipFile.length();
		System.out.println("Second Zip file length at 0 seconds: " + zipFile.length());
		int attempt = 0;
		while ((firstZipLength != secondZipLength) && (attempt < 10)) {
			firstZipLength = zipFile.length();
			try {
				Thread.currentThread().sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			secondZipLength = zipFile.length();
			System.out.println("First and second zip file lengths: " + firstZipLength + " " + secondZipLength);
		}
		try {
			org.apache.tools.zip.ZipFile file = new ZipFile(zipFile);

			// find and save the .csv file.
			Enumeration<ZipEntry> enumer = file.getEntries();
			ZipEntry csvFile = null;
			String bareFileNameWithCsv = bareFileName.substring(0, bareFileName.lastIndexOf(".") +1)+"csv";
			System.out.println("bareFileName entry: " + bareFileNameWithCsv);
			while (enumer.hasMoreElements()) {
				ZipEntry entry = enumer.nextElement(); 
				String bareEntryFileName = entry.getName().substring(entry.getName().lastIndexOf(sep)+1);
				System.out.println("zip entry: " + bareEntryFileName);
				if ((bareEntryFileName.equals(bareFileNameWithCsv))) {
					nullCsv = false;
					csvFile = entry;
				}
			}
			enumer = null;
			enumer = file.getEntries();
			
			//unzip csv file 
			String csvFileName = null;
			System.out.println(nullCsv);
			if (nullCsv) { }
			else {
				nullCsv = false;
				java.io.InputStream in = file.getInputStream(csvFile);
				int sepPosition = csvFile.getName().lastIndexOf("\\");
				if (sepPosition == -1) { sepPosition = csvFile.getName().lastIndexOf("/"); } 					
				csvFileName = csvFile.getName().substring(sepPosition + 1);
				System.out.println(csvFileName + " input stream " + in.toString());
				FileOutputStream out = new FileOutputStream(localFtpRoot + fileDir + sep + csvFile.getName().substring(sepPosition + 1));
				byte[] buffer = new byte[10240];
				int counter = 0;
				while(true) {
					int bytes = in.read(buffer);
					if(bytes < 0) break;
					out.write(buffer, 0, bytes);
					counter += bytes;
				}
				out.close();
				in.close();
			}

			//unzip rest of files
			while (enumer.hasMoreElements()) {
				ZipEntry entry = enumer.nextElement();
				if ((entry.getName().substring(entry.getName().lastIndexOf(".")+1).toLowerCase().matches("dat|rdt|hea|ini|txt|xml")) ) {
					System.out.println(entry.getName());	
					java.io.InputStream in = file.getInputStream(entry);
					int sepPosition = entry.getName().lastIndexOf("\\");
					if (sepPosition == -1) { sepPosition = entry.getName().lastIndexOf("/"); } 
					String dataFileName = entry.getName().substring(sepPosition + 1);
					String subId = entry.getName().substring(sepPosition + 1,entry.getName().lastIndexOf(".")).toLowerCase();
					
					//see if file has a subject ID defined in csv header
					if (!nullCsv) { 
						FileInputStream fstream = new FileInputStream(localFtpRoot + sep + fileDir + sep + csvFileName);
						// Get the object of DataInputStream
						DataInputStream inCsv = new DataInputStream(fstream);
						BufferedReader br = new BufferedReader(new InputStreamReader(inCsv));
						String strLine;
						//Read CSV File Line By Line looking for a match to the dataFileName
						while ((strLine = br.readLine()) != null)   {
							// Print the content on the console
							StringTokenizer tokenizer = new StringTokenizer(strLine,",");
							String subjectId = tokenizer.nextToken().toLowerCase();
							String headerFileName = tokenizer.nextToken();
							if (dataFileName.equals(headerFileName)) {
								subId = subjectId;// use the subjectId listed in the csv file instead of the default one 
								System.out.println("subject ID found in csv file.");
							}
						}
					}	
					
					// make sub-dir
					File subdir = new File(localFtpRoot + fileDir + sep + subId);
					subdir.mkdir();
					
					String subjectUserFileName = dataFileName;
					System.out.println("Writing: " + localFtpRoot + fileDir + sep + subId + sep + subjectUserFileName);
					FileOutputStream out = new FileOutputStream(localFtpRoot + fileDir + sep + subId + sep + subjectUserFileName);
					byte[] buffer = new byte[10240];
					int counter = 0;
					while(true) {
						int bytes = in.read(buffer);
						if(bytes < 0) break;
						out.write(buffer, 0, bytes);
						counter += bytes;
					}
					out.close();
					in.close();

					utils.routeToFolder(localFtpRoot + fileDir + sep + subId, subjectUserFileName, uId, subId, subjectUserFileName,
							ftpHost, ftpUser, ftpPassword, remoteFtpRoot,
							bExposure);	   
					System.out.println("---------------------------------------");
				}
			}


		}catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw e1;
		}

		return true;

	}

	/** Service to retrieve the public meta-data for each subject associated with a userID.
	 * Currently uses File class to access the local file system for  the information.
	 * 
	 * @param param0 OMElement containing the parameters:<BR/>
	 * userId and logindatetime (the later is currently unused)
	 *  
	 * @return OMElements containing a collection of meta-data for each subject:<BR/> 
	 *  status, subjectid, rdtfile, channels, samplingrate, numberofpoints, filename, filesize
	 */
	public org.apache.axiom.om.OMElement collectSubjectData(org.apache.axiom.om.OMElement param0) {
		if (verbose) System.out.println("collectSubjectData() called.");
		int x = 0;
		String sId = null;
		OMElement subject = null, subjectId = null, ecgFile = null, channels = null,
		samplingRate = null, numberOfPoints = null, filename = null, filesize = null;
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace dsNs = factory.createOMNamespace("http://www.cvrgrid.org/nodeDataService/", "dataStaging");
		OMElement collectSubjects = factory.createOMElement("collectSubjects", dsNs);

		OMElement status = factory.createOMElement("status", dsNs);
		status.addChild(factory.createOMText("success"));
		collectSubjects.addChild(status);

		OMElement subjects = factory.createOMElement("subjects", dsNs);

		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();

		//------------------------------------------------------------        
		//TODO: replace this with XML database to speed up loading.

		File publicRootFolder = new File(localFtpRoot + sep  + userId + sep + "public");
		File privateRootFolder = new File(localFtpRoot + sep  + userId + sep + "private");
		File[] publicFolders = publicRootFolder.listFiles();
		File[] privateFolders = privateRootFolder.listFiles();

		try {
			if(publicFolders != null){
				// sort public folders
				if(publicFolders.length > 0){
					debugPrintln("Sorting public folders");
					Arrays.sort(publicFolders, 
							new Comparator<File>(){    
								public int compare(File f1, File f2)    
								{        
									return f1.compareTo(f2);
								} 
							}
					);
				}
				// Parse the Public files for ecg meta-data
				//				if (verbose) System.out.println("Public files");
				parseFolderArray (publicFolders, subjects, factory, dsNs);

			}
			if(privateFolders != null){
				// sort private folders.
				if(privateFolders.length > 0){
					debugPrintln("Sorting private folders");
					Arrays.sort(privateFolders, 
							new Comparator<File>(){    
								public int compare(File f3, File f4)    
								{        
									return f3.compareTo(f4);
								} 
							}
					);
				}
				// Parse the Private files for ecg meta-data
				//				if (verbose) System.out.println("Private files");
				parseFolderArray (privateFolders, subjects, factory, dsNs);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		collectSubjects.addChild(subjects);

		debugPrintln("collectSubjectData() finished, found " + privateFolders.length + " private folders.");
		return collectSubjects;
	}


	//TODO: replace this with XML database to speed up loading.
	/** Parse the files in the folders array for ecg meta-data.
	 * 
	 * @param folders - array of folders to check e.g. "private/"
	 * @param subjects
	 * @param factory
	 * @param dsNs
	 */
	private void parseFolderArray(File[] folders, OMElement subjects, OMFactory factory, OMNamespace dsNs){
		String sId = null;
		File[] fileArray = new File[0];
		OMElement rdtSubject = null,  geMuseSubject = null, subjectId = null;
		boolean hasINI = false, hasRDT = false, hasWFDB = false, hasTXT  = false;
		boolean verbOffHere = verbose;
		verbose = false;

		int max = folders.length;
		for(int i = 0; i < max; i++) {
			if(folders[i].isDirectory()) {
				// parse all the files in the public input folder.
				int position = folders[i].toString().lastIndexOf(sep);
				rdtSubject = factory.createOMElement("subject", dsNs);

				subjectId = factory.createOMElement("subjectid", dsNs);
				sId = folders[i].toString().substring(++position,
						folders[i].toString().length());
				subjectId.addChild(factory.createOMText(sId));
				rdtSubject.addChild(subjectId);
				geMuseSubject = rdtSubject.cloneOMElement();

				File inputFolder = new File(folders[i].getAbsolutePath()+ sep + "input");
				debugPrintln(inputFolder.getAbsolutePath());

				String listWCarets = "";
				fileArray = inputFolder.listFiles();					
				if(fileArray != null) {
					for(int y = 0, maxy = fileArray.length; y < maxy; y++) {
						String ext = fileArray[y].getName().substring(fileArray[y].getName().lastIndexOf(".")+1).toLowerCase(); // get the extension (in lower case)
						if(ext.equals("rdt")) {

							hasRDT = true;
							debugPrintln(inputFolder + sep + fileArray[y].getName());
							debugPrintln(" --- Has RDT");
							String fnINI = fileArray[y].getName();
							fnINI = fnINI.substring(0, fnINI.length()-4); // trip off the .rdt extension.
							fnINI = fnINI + ".ini"; // add the expected .ini
							hasINI = false;
							for(int x = 0; x < maxy; x++) { // check if matching .ini file exisits.
								debugPrintln("*** test: '" + fnINI + "' ?= '" +  fileArray[x].getName() + "'");
								if(fileArray[x].getName().compareTo(fnINI)==0){
									debugPrintln("Has INI");
									hasINI = true;
									break;
								}
							}
							getrdtData(rdtSubject, factory, dsNs, fileArray[y], hasINI);
						} 
						if(ext.equals("hea")) {
							hasWFDB = true;
						} 
						if(ext.equals("txt")) {
							getGEMuseData(geMuseSubject, factory, dsNs, fileArray[y]);
							debugPrintln(" --- Has TXT");
							hasTXT = true;
						}

						if(ext.equals("nat")) {// for Bruce Nearing's 24 hour GE holter files *.nat (3 lead) format.
							getGEMuseData(geMuseSubject, factory, dsNs, fileArray[y]);
							debugPrintln(" --- Has nat");
							hasTXT = true;
						}
						if(ext.equals("gtm")) { // for Bruce Nearing's 24 hour GE holter files *.GTM (12 lead) format.
							getGEMuseData(geMuseSubject, factory, dsNs, fileArray[y]);
							debugPrintln(" --- Has GTM");
							hasTXT = true;
						}
						if(ext.equals("slot")) { // for Bruce Nearing's 24 hour GE holter files *.GTM (12 lead) format.
							getGEMuseData(geMuseSubject, factory, dsNs, fileArray[y]);
							debugPrintln(" --- Has slot");
							hasTXT = true;
						}
						if(ext.equals("ecg")) { // for Bruce Nearing's 24 hour GE holter files *.GTM (12 lead) format.
							getGEMuseData(geMuseSubject, factory, dsNs, fileArray[y]);
							debugPrintln(" --- Has ecg");
							hasTXT = true;
						}

						// compile a caret separated list of all file names in this folder
						listWCarets += fileArray[y].getName() + "^";
					}

					
					// Distinguish between a GE-Muse raw ECG file and GE-Magellan ecg attributes file, both of which have .txt extension.
					if (hasRDT == true) { 
						subjects.addChild(rdtSubject);
						addOMEChild("namelist", listWCarets, rdtSubject, factory, dsNs);
					}else if(hasTXT){
						debugPrintln(" --- Only Has TXT but not RDT, must be a GE-MUSE file");
						subjects.addChild(geMuseSubject);						
						addOMEChild("namelist", listWCarets, rdtSubject, factory, dsNs);
					}
				}
			}
		}
		verbose = verbOffHere;
	}

	/** Gets the ecg meta-data for a rdt file.
	 *  is a sub-method of the collectSubjectData() service.
	 * @param subject - a partially filled OMElement, which all the meta-data are added to.
	 * @param factory - OMAbstractFactory used to create OMElements and OMText.
	 * @param dsNs - OMNamespace used by collectSubjectData().
	 * @param file - The file containing the ecg data.
	 * @return OMElements containing the meta-data are added to "subject" to wit:<BR/> 
	 *         wfdbfile, channels, samplingrate, numberofpoints, filename, filesize
	 */
	private OMElement getrdtData(OMElement subject, OMFactory factory, OMNamespace dsNs, File file, boolean hasINI ){
		OMElement ecgFile = null, channels = null, samplingRate = null;
		OMElement numberOfPoints = null, filename = null, filesize = null;

		if(verbose) {
			if (subject==null){
				debugPrintln("Parameter ERROR: subject is null");
				return subject;
			}
			if (factory==null){
				debugPrintln("Parameter ERROR: factory is null");
				return subject;
			}
			if (dsNs==null){
				debugPrintln("Parameter ERROR: dsNs is null");
				return subject;
			}
			if (file==null){
				debugPrintln("Parameter ERROR: file is null");
				return subject;
			}
		}
		debugPrintln("parsing RDT in getrdtData(): " + file.getPath());

		RDTParser rdtParser = new RDTParser(file);
		rdtParser.parse();

		// build subject OMElement
		ecgFile = factory.createOMElement("rdtfile", dsNs );
		ecgFile.addChild(factory.createOMText("false"));
		subject.addChild(ecgFile);

		channels = factory.createOMElement("channels", dsNs);
		channels.addChild(factory.createOMText(
				new Short(rdtParser.getChannels()).toString()));
		subject.addChild(channels);

		samplingRate = factory.createOMElement("samplingrate", dsNs);
		samplingRate.addChild(factory.createOMText(
				new Integer(rdtParser.getSamplingRate()).toString()));
		subject.addChild(samplingRate);

		numberOfPoints = factory.createOMElement("numberofpoints", dsNs);
		numberOfPoints.addChild(factory.createOMText(
				new Integer(rdtParser.getCounts()).toString()));
		subject.addChild(numberOfPoints);

		filename = factory.createOMElement("filename", dsNs);
		filename.addChild(factory.createOMText(
				file.getPath()));
		subject.addChild(filename);

		filesize = factory.createOMElement("filesize", dsNs);
		filesize.addChild(factory.createOMText(
				new Long(rdtParser.getFileSize()).toString()));
		subject.addChild(filesize);


		// build rdtfile (isRDTFile, used to allow berger if INI exists) OMElement
		ecgFile = factory.createOMElement("rdtfile", dsNs );
		if(hasINI){
			ecgFile.addChild(factory.createOMText("true"));
		}else{
			ecgFile.addChild(factory.createOMText("false"));
		}
		subject.addChild(ecgFile);

		debugPrintln(" channels: " + rdtParser.getChannels() + " samplingRate: " + rdtParser.getSamplingRate() + " numberOfPoints: " + rdtParser.getCounts() + " filesize: " + rdtParser.getFileSize() );
		rdtParser = null;
		return subject;
	}

	/** Gets the GE-Magellan meta-data for a txt file, Magellan files are not raw ECG.<BR>
	 * GE-Muse files will have already been converted to rdt format, so that file's data will be used if it exists.
	 *  is a sub-method of the collectSubjectData() service.<BR>
	 * **  Now also used for .nat and .GMT Holter files, so they can be listed for downloading.
	 * @param subject - a partially filled OMElement, which all the meta-data are added to.
	 * @param factory - OMAbstractFactory used to create OMElements and OMText.
	 * @param dsNs - OMNamespace used by collectSubjectData().
	 * @param file - The file containing the ecg data.
	 * @return OMElements containing the meta-data are added to "subject" to wit:<BR/> 
	 *         wfdbfile, channels, samplingrate, numberofpoints, filename, filesize
	 */
	private OMElement getGEMuseData(OMElement subject, OMFactory factory, OMNamespace dsNs, File file){
		if (subject==null){
			debugPrintln("Parameter ERROR: subject is null");
			return subject;
		}
		if (factory==null){
			debugPrintln("Parameter ERROR: factory is null");
			return subject;
		}
		if (dsNs==null){
			debugPrintln("Parameter ERROR: dsNs is null");
			return subject;
		}
		if (file==null){
			debugPrintln("Parameter ERROR: file is null");
			return subject;
		}

		debugPrintln("parsing GE-Muse file in getGEMuseData(): " + file.getPath());
		// build subject OMElement
		addOMEChild("rdtfile", 			"false",		subject,factory,dsNs);
		addOMEChild("channels", 		"-1",			subject,factory,dsNs);
		addOMEChild("samplingrate", 	"-1",			subject,factory,dsNs);
		addOMEChild("numberofpoints", 	"-1",			subject,factory,dsNs);
		addOMEChild("filename", 		file.getPath(),	subject,factory,dsNs);
		addOMEChild("filesize", 
				new Long(file.length()).toString(),		subject,factory,dsNs);
		addOMEChild("rdtfile", 			"false",		subject,factory,dsNs);		

		debugPrintln(" GE-Muse file filesize: " + file.length() );
		return subject;
	}

	/** Service that returns a short subset of an ecg stored in a specified file, suitable for graphical display.
	 * 
	 * @param param0 - Contains elements brokerURL, mySqlURL, fileName... etc
	 *  brokerURL - web address of the data file repository 
	 *  mySqlURL - address of the mySQL database containing the annotations
	 *  fileName - file containing the ECG data in RDT format.
	 *  fileSize - used to size the file reading buffer.
	 *  offsetMilliSeconds - number of milliseconds from the beginning of the ECG at which to start the graph.
	 *  durationMilliSeconds - The requested length of the returned data subset, in milliseconds.
	 *  graphWidthPixels - Width of the zoomed graph in pixels(zoom factor*unzoomed width), hence the maximum points needed in the returned VisualizationData.
	 * @return - OMElement containing the values in the results
	 */
	public org.apache.axiom.om.OMElement collectVisualizationData(org.apache.axiom.om.OMElement param0) {
		debugPrintln("** collectVisualizationData() 2.1 called; "); 
		long startTime = System.currentTimeMillis(), stopTime = 0, elapsed=0;

		boolean success = true, oldverbose = verbose;
		// create output parent OMElement
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace dsNs = factory.createOMNamespace("http://www.cvrgrid.org/nodeDataService/", "dataStaging");

		String resultfileName="";
		String parameterfileName = "";
		//String[] headers;
		String[] cols;
		//		String parameters="";
		//		String sFeatureColumns = "";
		int lineCount=0;

		String ftpHost="", mySqlURL="", fileName="";
		String ftpUser = "";
		String ftpPassword = "";
		String tempFile ="";
		ApacheCommonsFtpWrapper ftpClient = null;
		long fileSize=0;
		int offsetMilliSeconds=0, durationMilliSeconds=0, graphWidthPixels=0;
		String[] saLeadCSV = null; // array of comma separated ECG values, one string per lead.
		VisualizationData visData=null;
		try{// Parse parameters
			Iterator iterator = param0.getChildren();
			ftpHost = ((OMElement)iterator.next()).getText();
			ftpUser = ((OMElement)iterator.next()).getText();
			ftpPassword = ((OMElement)iterator.next()).getText();
			mySqlURL = ((OMElement)iterator.next()).getText();
			fileName = ((OMElement)iterator.next()).getText();
			fileSize =  Long.parseLong(((OMElement)iterator.next()).getText());
			offsetMilliSeconds = Integer.parseInt(((OMElement)iterator.next()).getText());
			durationMilliSeconds = Integer.parseInt(((OMElement)iterator.next()).getText());
			graphWidthPixels = Integer.parseInt(((OMElement)iterator.next()).getText());
			verbose = Boolean.parseBoolean(((OMElement)iterator.next()).getText());
			utils.setVerbose(verbose);
			
			debugPrintln("** verbose: " + verbose + " offsetMilliSeconds: " + offsetMilliSeconds + " durationMilliSeconds: " + durationMilliSeconds);

			// The filename and extension without any path.
			String fileNameOnly = fileName.substring(fileName.lastIndexOf(sep) + 1);
			// The relative directory this file is in.
			String relativeDir = fileName.substring(0, fileName.lastIndexOf(sep));
			tempFile = localFtpRoot + sep + fileNameOnly + System.currentTimeMillis();

			debugPrintln("[=====================================================================]");
			debugPrintln("fileName: " + fileName);
			debugPrintln(" fileSize: " + fileSize);
			debugPrintln(" offsetMilliSeconds: " + offsetMilliSeconds);
			debugPrintln(" durationMilliSeconds: " + durationMilliSeconds);
			debugPrintln(" graphWidthPixels: " + graphWidthPixels);
			
			debugPrintln("ftpHost: " + ftpHost);
			debugPrintln("ftpUser: " + ftpUser);
			debugPrintln("ftpPassword: " + ftpPassword);
			debugPrintln("fileNameOnly: \"" + fileNameOnly + "\"");
			debugPrintln("relativeDir: \"" + relativeDir + "\"");
			debugPrintln("tempFile: \"" + tempFile + "\"");
			debugPrintln("[=====================================================================]");

			ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			ftpClient.verbose = verbose;
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			//	        ftpClient.cd(parentFolderNode);
			debugPrintln("changeWorkingDirectory(" + remoteFtpRoot  + relativeDir + ")");
			success = ftpClient.changeWorkingDirectory(remoteFtpRoot + relativeDir);
			if(success){
					debugPrintln("Downloading: " + fileNameOnly );
					success = ftpClient.downloadFile(fileNameOnly, tempFile);
			}else{
				debugPrintln("Directory not found on FTP server: " + remoteFtpRoot);
			}
			
		} catch (OMException e) {
			System.err.println("collectVisualizationData failed while parsing parameters.");
			e.printStackTrace();
			success=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success=false;
		}
		if(success){
			try{//load data from ecg file 
				visData = fetchSubjectVisualization(tempFile, fileSize, offsetMilliSeconds, durationMilliSeconds, graphWidthPixels);

				saLeadCSV = new String[visData.getRdtDataLeads()];
				//initialize all to an empty string.
				for(int lead=0;lead < visData.getRdtDataLeads();lead++){
					saLeadCSV[lead] = ""; 
				}

				//build a comma delimited list for each column
				for(int row=0;row < visData.getRdtDataLength();row++){
					for(int lead=0;lead < visData.getRdtDataLeads();lead++){
						saLeadCSV[lead] = saLeadCSV[lead] +  visData.getRdtData()[row][lead] + ",";
					}
				}						

				// trim trailing comma 
				for(int lead=0;lead < visData.getRdtDataLeads();lead++){
					saLeadCSV[lead] = saLeadCSV[lead].substring(0, saLeadCSV[lead].length()-1);
				}
			} catch (Exception e) {
				System.err.println("collectVisualizationData failed while loading data from ecg file.");
				e.printStackTrace();
				success=false;
			}finally{
				// remove tempFile
				deleteTempFile(tempFile);
			}
		}else{
			System.err.println("collectVisualizationData() failed while FTPing " + fileName + " to " + tempFile);
		}


		// build return xml
		OMElement collectVisualizationData = factory.createOMElement("collectVisualizationData", dsNs);
		if(success){
			addOMEChild("status", 			"success",									collectVisualizationData,factory,dsNs);
			addOMEChild("DataLength", 		String.valueOf(visData.getRdtDataLength()),	collectVisualizationData,factory,dsNs);
			addOMEChild("DataLeads", 		String.valueOf(visData.getRdtDataLeads()),	collectVisualizationData,factory,dsNs);
			addOMEChild("Offset", 			String.valueOf(visData.getOffset()),		collectVisualizationData,factory,dsNs);
			addOMEChild("SkippedSamples", 	String.valueOf(visData.getSkippedSamples()),collectVisualizationData,factory,dsNs);
			addOMEChild("MsDuration", 		String.valueOf(visData.msDuration),			collectVisualizationData,factory,dsNs);
			for(int lead=0;lead < visData.getRdtDataLeads();lead++){
				addOMEChild("lead_"+lead, 	saLeadCSV[lead],							collectVisualizationData,factory,dsNs);
			}	
		}else{
			addOMEChild("status", 			"fail",										collectVisualizationData,factory,dsNs);			
		}

		verbose = oldverbose; // reset global to previous setting, in case it matters
		stopTime = System.currentTimeMillis();
		elapsed = (stopTime - startTime);
		debugPrintln(" finished, execution time(ms): " + elapsed);

		return collectVisualizationData;
	}

	/** Service that returns a short subset of an ecg stored in a specified file, suitable for graphical display.
	 * 
	 * @param param0 - Contains elements brokerURL, mySqlURL, fileName... etc<BR>
	 *  brokerURL - web address of the data file repository <BR>
	 *  fileName - file containing the ECG data in RDT format.<BR>
	 *  fileSize - used to size the file reading buffer.<BR>
	 *  offsetMilliSeconds - number of milliseconds from the beginning of the ECG at which to start the graph.<BR>
	 *  durationMilliSeconds - The requested length of the returned data subset, in milliseconds.<BR>
	 *  graphWidthPixels - Width of the zoomed graph in pixels(zoom factor*unzoomed width), hence the maximum points needed in the returned VisualizationData.<BR>
	 * @return - OMElement containing the values in the results
	 */
	public org.apache.axiom.om.OMElement collectWFDBdataSegment(org.apache.axiom.om.OMElement param0) {
		debugPrintln("** collectWFDBdataSegment() 1.0 called; "); 
		long startTime = System.currentTimeMillis(), stopTime = 0, elapsed=0;
		int iLeadCount=0;
		boolean success = true, oldverbose = verbose;
		boolean bTestPattern=false;
		
		// create output parent OMElement
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace dsNs = factory.createOMNamespace("http://www.cvrgrid.org/nodeDataService/", "dataStaging");

		String ftpHost="", mySqlURL="", fileName="";
		String ftpUser = "";
		String ftpPassword = "";
		String tempFile ="";
		ApacheCommonsFtpWrapper ftpClient = null;
		long fileSize=0;
		int offsetMilliSeconds=0, durationMilliSeconds=0, graphWidthPixels=0;
		String[] saLeadCSV = null; // array of comma separated ECG values, one string per lead.
		VisualizationData visData=null;
		try{// Parse parameters
			debugPrintln("- parsing the web service's parameters without regard to order.");
			// parse the input parameter's OMElement XML into a Map.
			Map<String, Object> paramMap = DataUtils.buildParamMap(param0);
			// Assign specific input parameters to local variables.
			fileName			= (String) paramMap.get("fileName");
			ftpHost				= (String) paramMap.get("ftpHost");
			ftpUser				= (String) paramMap.get("ftpUser");
			ftpPassword			= (String) paramMap.get("ftpPassword");
			fileSize			= Long.parseLong((String) paramMap.get("fileSize"));
			offsetMilliSeconds	= Integer.parseInt((String) paramMap.get("offsetMilliSeconds"));
			durationMilliSeconds= Integer.parseInt((String) paramMap.get("durationMilliSeconds"));
			graphWidthPixels	= Integer.parseInt((String) paramMap.get("graphWidthPixels"));
			bTestPattern		= Boolean.parseBoolean((String) paramMap.get("testPattern"));

			verbose				= Boolean.parseBoolean((String) paramMap.get("verbose"));
			
			//**************************************************
			utils.setVerbose(verbose);
			
			debugPrintln("** verbose: " + verbose + " offsetMilliSeconds: " + offsetMilliSeconds + " durationMilliSeconds: " + durationMilliSeconds);

			// The filename and extension without any path.
			String fileNameOnly = fileName.substring(fileName.lastIndexOf(sep) + 1);
			// The relative directory this file is in.
			String relativeDir = fileName.substring(0, fileName.lastIndexOf(sep));
			tempFile = localFtpRoot + sep + fileNameOnly + System.currentTimeMillis();

			String sIgnoreMess="";
			debugPrintln("[=====================================================================]");
			debugPrintln(" bTestPattern: " + bTestPattern);
			debugPrintln(" fileSize: " + fileSize);
			debugPrintln(" offsetMilliSeconds: " + offsetMilliSeconds);
			debugPrintln(" durationMilliSeconds: " + durationMilliSeconds);
			debugPrintln(" graphWidthPixels: " + graphWidthPixels);

			if(bTestPattern) sIgnoreMess = "Ignored: ";			
			debugPrintln(sIgnoreMess + "fileName: " + fileName);
			debugPrintln(sIgnoreMess + "ftpHost: " + ftpHost);
			debugPrintln(sIgnoreMess + "ftpUser: " + ftpUser);
			debugPrintln(sIgnoreMess + "ftpPassword: " + ftpPassword);
			debugPrintln(sIgnoreMess + "fileNameOnly: \"" + fileNameOnly + "\"");
			debugPrintln(sIgnoreMess + "relativeDir: \"" + relativeDir + "\"");
			debugPrintln(sIgnoreMess + "tempFile: \"" + tempFile + "\"");
			debugPrintln("[=====================================================================]");

			if(!bTestPattern){
				ftpClient = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
				ftpClient.verbose = verbose;
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				debugPrintln("changeWorkingDirectory(" + remoteFtpRoot  + relativeDir + ")");
				success = ftpClient.changeWorkingDirectory(remoteFtpRoot + relativeDir);
				if(success){
					debugPrintln("Downloading: " + fileNameOnly );
					success = ftpClient.downloadFile(fileNameOnly, tempFile);
					debugPrintln("Download finished.");
				}else{
					debugPrintln("Directory not found on FTP server: " + remoteFtpRoot);
				}
			}else{
				debugPrintln("Skipping FTP in favor to the test pattern.");
				success=true;
			}
		} catch (OMException e) {
			System.err.println("collectVisualizationData failed while parsing parameters.");
			e.printStackTrace();
			success=false;
		} catch (IOException e) {
			e.printStackTrace();
			success=false;
		}
		if(success){
			debugPrintln("Creating VisualizationData bean.");
			try{//load data from ecg file 
				if(bTestPattern){
					visData = fetchSubjectVisualizationTestPattern(fileSize, offsetMilliSeconds, durationMilliSeconds, graphWidthPixels);
					success=true;
				}else{
					visData = fetchWFDBdataSegment(tempFile, fileSize, offsetMilliSeconds, durationMilliSeconds, graphWidthPixels);
				}
				iLeadCount = visData.getRdtDataLeads();
				saLeadCSV = new String[visData.getRdtDataLeads()];
				//initialize all to an empty string.
				for(int lead=0;lead < iLeadCount;lead++){
					debugPrintln("Initializing CSV for lead: " + lead + " of " + iLeadCount);
					saLeadCSV[lead] = ""; 
				}

				//build a comma delimited list for each column
				for(int row=0;row < visData.getRdtDataLength();row++){
					for(int lead=0;lead < iLeadCount;lead++){
						saLeadCSV[lead] = saLeadCSV[lead] +  visData.getRdtData()[row][lead] + ",";
					}
				}						

				// trim trailing comma 
				for(int lead=0;lead < iLeadCount;lead++){
					debugPrintln("Finishing CSV for lead: " + lead + " of " + iLeadCount);
					saLeadCSV[lead] = saLeadCSV[lead].substring(0, saLeadCSV[lead].length()-1);
				}
			} catch (Exception e) {
				System.err.println("collectVisualizationData failed while loading data from ecg file.");
				e.printStackTrace();
				success=false;
			}finally{
				// remove tempFile
				if(!bTestPattern){
					deleteTempFile(tempFile);
				}
			}
		}else{
			System.err.println("collectVisualizationData() failed while FTPing " + fileName + " to " + tempFile);
		}


		// build return xml
		OMElement collectVisualizationData = factory.createOMElement("collectVisualizationData", dsNs);
		if(success){
			debugPrintln("Building OMElement from Web Service return values");
			addOMEChild("Status", 			"success",									collectVisualizationData,factory,dsNs);
			addOMEChild("SampleCount", 		String.valueOf(visData.getRdtDataLength()),	collectVisualizationData,factory,dsNs);
			addOMEChild("LeadCount", 		String.valueOf(iLeadCount),	collectVisualizationData,factory,dsNs);
			addOMEChild("Offset", 			String.valueOf(visData.getOffset()),		collectVisualizationData,factory,dsNs);
			addOMEChild("SkippedSamples", 	String.valueOf(visData.getSkippedSamples()),collectVisualizationData,factory,dsNs);
			addOMEChild("SegmentDuration", 		String.valueOf(visData.msDuration),			collectVisualizationData,factory,dsNs);
			for(int lead=0;lead < iLeadCount;lead++){
				addOMEChild("lead_"+lead, 	saLeadCSV[lead],							collectVisualizationData,factory,dsNs);
			}	
		}else{
			addOMEChild("Status", 			"fail",										collectVisualizationData,factory,dsNs);			
		}

		verbose = oldverbose; // reset global to previous setting, in case it matters
		stopTime = System.currentTimeMillis();
		elapsed = (stopTime - startTime);
		debugPrintln(" finished, execution time(ms): " + elapsed);

		return collectVisualizationData;
	}


	/** Deletes the specified file or directory from the local file system.
	 * Will not delete empty directories.
	 * 
	 * @param tempFile - full file path, name and extension
	 */
	private void deleteTempFile(String tempFile) {
		// A File object to represent the filename
		File f = new File(tempFile);

		// Make sure the file or directory exists and isn't write protected
		if (!f.exists())
			throw new IllegalArgumentException(
					"Delete: no such file or directory: " + tempFile);

		if (!f.canWrite())
			throw new IllegalArgumentException("Delete: write protected: "
					+ tempFile);

		// If it is a directory, make sure it is empty
		if (f.isDirectory()) {
			String[] files = f.list();
			if (files.length > 0)
				throw new IllegalArgumentException(
						"Delete: directory not empty: " + tempFile);
		}

		// Attempt to delete it
		boolean success = f.delete();

		if (!success)
			throw new IllegalArgumentException("Delete: deletion failed");
	}



	/** Reads the file from the brokerURL and stores it as the RdtData of a VisualizationData.
	 * It is assuming that the file is in RDT format, with 3 leads.
	 *
	 * @param tempFile - name of a local RDT file containing ECG data. 
	 * @param fileSize - used to size the file reading buffer.
	 * @param offsetMilliSeconds - number of milliseconds from the beginning of the ECG at which to start the graph.
	 * @param durationMilliSeconds - The requested length of the returned data subset, in milliseconds.
	 * @param graphWidthPixels - Width of the zoomed graph in pixels(zoom factor*unzoomed width), hence the maximum points needed in the returned VisualizationData.
	 * @param callback - call back handler class.
	 * 	 
	 * @see org.cvrgrid.widgets.node.client.BrokerService#fetchSubjectVisualization(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, int, int)
	 */
	private VisualizationData fetchSubjectVisualization(String tempFile,
			long fileSize, int offsetMilliSeconds, int durationMilliSeconds, int graphWidthPixels) {
		BufferedInputStream rdtBis = null;
		VisualizationData visualizationData = new VisualizationData();
		try {
			//******************************************
			try {
				FileInputStream isFile = new FileInputStream(tempFile);
				//*******************************************

				int samplesPerPixel, skippedSamples, durationInSamples;
				rdtBis = new BufferedInputStream(isFile);

				//Read the 4 header bytes
				byte[] header = new byte[HEADERBYTES];
				int result = rdtBis.read(header,0,HEADERBYTES);
				
				if(result == HEADERBYTES) {
					ByteBuffer bbHead = ByteBuffer.wrap(header);
					bbHead.order(BYTEORDER);
	
					short channels = bbHead.getShort();
					short samplingRate = bbHead.getShort(); // replaced with subjectData.setSamplingRate() 
					float fRateMsec = (float)(samplingRate/1000.0);
					if (offsetMilliSeconds<0)offsetMilliSeconds=0; // cannot read before the beginning of the file.
					int vizOffset = (int) (offsetMilliSeconds*fRateMsec);
	
					//-------------------------------------------------
					// Calculate and Set Visualization parameters
					final int REALBUFFERSIZE = (int) fileSize - HEADERBYTES;
					if(REALBUFFERSIZE % (channels * SHORTBYTES) != 0) {
						System.err.println("rdt file is not aligned.");
					}
					int counts = REALBUFFERSIZE / (channels * SHORTBYTES);
					byte[][] body = new byte[counts][(channels * SHORTBYTES)];
					byte[] sample = new byte[(channels * SHORTBYTES)]; /** A single reading from all leads. **/ 
					try {
						@SuppressWarnings("unused") // used to test rdtBis.read for exceptions
	
						int requestedMaxPoints;
						durationInSamples = (int) (fRateMsec*durationMilliSeconds);
						if(durationInSamples>graphWidthPixels){
							samplesPerPixel=durationInSamples/graphWidthPixels;
							requestedMaxPoints = graphWidthPixels;
						}else{
							samplesPerPixel=1;
							requestedMaxPoints = durationInSamples;
						}
						skippedSamples = samplesPerPixel-1;
	
						int availableSamples = counts - vizOffset; // total number of remaining samples from this offset.
						int availablePoints = availableSamples/samplesPerPixel; // total number of graphable points from this offset.
						int maxPoints = 0; // maximum data points that can be returned.
						// ensure that the copying loop doesn't try to go past the end of the data file.
						if(availablePoints > requestedMaxPoints) {
							maxPoints = requestedMaxPoints;
						} else {  // Requested duration is longer than the remainder after the offset.
							if(durationInSamples < counts){ // Requested duration is less than the file contains.
								// move the offset back so the requested amount of samples can be returned.
								vizOffset = counts - durationInSamples;
								maxPoints = requestedMaxPoints;
							}else{	// Requested duration is longer than the file contains.
								maxPoints = availablePoints;
							}
						}
						visualizationData.setRdtDataLength(maxPoints);
						visualizationData.setRdtDataLeads(channels);
						visualizationData.setOffset(vizOffset);
						visualizationData.setSkippedSamples(skippedSamples);
						int msDuration = (counts*1000)/samplingRate;
						visualizationData.setMsDuration(msDuration);
	
	
						//------------------------------------------------
						// Read the rest of the file to get the data.
						ByteBuffer bbSample;
						double[][] tempData = new double[maxPoints][channels];
						int fileOffset = vizOffset*channels*SHORTBYTES; //offset in bytes from the beginning of the file.
	
						int index1, index2, s, outSample=0;
						index2 =  vizOffset; // index of the first sample to return data for, index is in samples not bytes.
						int length, bisOffset, bisLen = sample.length;
						// read entire file into the local byte array "body"
						for (index1 =  0; index1 < counts; index1++){
							bisOffset = HEADERBYTES + (index1*bisLen);
							s=0;
							for(int c=0;c<(bisLen*4);c++){ // make up to 4 attempts to read 
								length = rdtBis.read(sample, s, 1);// read one byte into the byte array "sample", explicitly specifying which byte to read.
								if(length==1) s++; // successfully read the byte, go to the next one.
								if(s==bisLen) break; // last byte has been read.
							}
	
							if(index1==index2){ // add this sample the output data
								bbSample = ByteBuffer.wrap(sample);
								bbSample.order(BYTEORDER);
	
								for(int ch = 0; ch < channels; ch++) {
									short value =  bbSample.getShort(); // reads a Short, increments position() by 2 bytes.
									tempData[outSample][ch] = (double)value;
								}
	
								bbSample.clear();
								index2 = index2 + 1 + skippedSamples;
								outSample++;
								if(outSample==maxPoints) break;
							}
						}
	
						visualizationData.setRdtData(tempData);
	
						//*******************************************
						isFile.close();
						//				 br.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					System.err.println("fetchSubjectVisualization failed, error occured while reading header of the RDT file:" + tempFile);
				}
				//*******************************************
			} catch(IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					rdtBis.close();
				} catch(IOException e2) {
					e2.printStackTrace();
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return visualizationData;
	}


	/** Reads the file from the brokerURL and stores it as the RdtData of a VisualizationData.
	 * Then it stores that in a NodeBrokerData, which it returns via the callback.
	 *
	 * @param fileSize - used to size the file reading buffer.
	 * @param offsetMilliSeconds - number of milliseconds from the beginning of the ECG at which to start the graph.
	 * @param durationMilliSeconds - The requested length of the returned data subset, in milliseconds.
	 * @param graphWidthPixels - Width of the zoomed graph in pixels(zoom factor*unzoomed width), hence the maximum points needed in the returned VisualizationData.
	 * @param callback - call back handler class.
	 * 	 
	 * @see org.cvrgrid.widgets.node.client.BrokerService#fetchSubjectVisualization(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, int, int)
	 */
	private VisualizationData fetchSubjectVisualizationTestPattern(long fileSize, int offsetMilliSeconds, int durationMilliSeconds, int graphWidthPixels) {
		debugPrintln("+ fetchSubjectVisualizationTestPattern(), returning 3 leads with sine waves, with 1/3 of cycle phase shift between.");
		VisualizationData visualizationData = new VisualizationData();
		try {
			//	 * @param skippedSamples - number of samples to skip after each one returned. To adjust for graph resolution.
			int samplesPerPixel, skippedSamples, durationInSamples;
			short channels = 3;
			short samplingRate =1000; // replaced with subjectData.setSamplingRate() 
			float fRateMsec = (float)(samplingRate/1000.0);
			if (offsetMilliSeconds<0)offsetMilliSeconds=0; // cannot read before the beginning of the file.
			int vizOffset = (int) (offsetMilliSeconds*fRateMsec);

			//-------------------------------------------------
			// Calculate and Set Visualization parameters
			final int REALBUFFERSIZE = (int) fileSize - HEADERBYTES;
			if(REALBUFFERSIZE % (channels * SHORTBYTES) != 0) {
				System.err.println("rdt file is not aligned.");
			}
			int counts = REALBUFFERSIZE / (channels * SHORTBYTES);
			int requestedMaxPoints;
			durationInSamples = (int) (fRateMsec*durationMilliSeconds);
			if(durationInSamples>graphWidthPixels){
				samplesPerPixel=durationInSamples/graphWidthPixels;
				requestedMaxPoints = graphWidthPixels;
			}else{
				samplesPerPixel=1;
				requestedMaxPoints = durationInSamples;
			}
			skippedSamples = samplesPerPixel-1;

			int availableSamples = counts - vizOffset; // total number of remaining samples from this offset.
			int availablePoints = availableSamples/samplesPerPixel; // total number of graphable points from this offset.
			int maxPoints = 0; // maximum data points that can be returned.
			// ensure that the copying loop doesn't try to go past the end of the data file.
			if(availablePoints > requestedMaxPoints) {
				maxPoints = requestedMaxPoints;
			} else {  // Requested duration is longer than the remainder after the offset.
				if(durationInSamples < counts){ // Requested duration is less than the file contains.
					// move the offset back so the requested amount of samples can be returned.
					vizOffset = counts - durationInSamples;
					maxPoints = requestedMaxPoints;
				}else{	// Requested duration is longer than the file contains.
					maxPoints = availablePoints;
				}
			}
			visualizationData.setRdtDataLength(maxPoints);
			visualizationData.setRdtDataLeads(channels);
			visualizationData.setOffset(vizOffset);
			visualizationData.setSkippedSamples(skippedSamples);
			int msDuration = (counts*1000)/samplingRate;
			visualizationData.setMsDuration(msDuration);


			//------------------------------------------------
			// generate the data from trig functions.
			double[][] tempData = new double[maxPoints][channels];

			double index;
			index = (double)vizOffset; // index of the first sample to return data for, index is in samples not bytes.

			for(int point = 0; point < maxPoints; point++) {
				index = index + 1 ;

				tempData[point][0] = Math.sin(index/160.0)*500.0;
				tempData[point][1] = Math.sin((index/160.0)+2.094)*500.0; // plus 1/3 cycle 
				tempData[point][2] = Math.sin((index/160.0)+4.189)*500.0; // plus 2/3 cycle
			}
			visualizationData.setRdtData(tempData);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return visualizationData;
	}



	/** Reads the WFDB file from the brokerURL and stores it as the RdtData of a VisualizationData.
	 * It is assuming that the file is in RDT format, with 3 leads.
	 *
	 * @param tempFile - name of a local RDT file containing ECG data. 
	 * @param fileSize - used to size the file reading buffer.
	 * @param offsetMilliSeconds - number of milliseconds from the beginning of the ECG at which to start the graph.
	 * @param durationMilliSeconds - The requested length of the returned data subset, in milliseconds.
	 * @param graphWidthPixels - Width of the zoomed graph in pixels(zoom factor*unzoomed width), hence the maximum points needed in the returned VisualizationData.
	 * @param callback - call back handler class.
	 * 	 
	 * @see org.cvrgrid.widgets.node.client.BrokerService#fetchSubjectVisualization(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, int, int)
	 */
	private VisualizationData fetchWFDBdataSegment(String tempFile,
			long fileSize, int offsetMilliSeconds, int durationMilliSeconds, int graphWidthPixels) {
		BufferedInputStream rdtBis = null;
		VisualizationData visualizationData = new VisualizationData();
		try {
			//******************************************
			try {
				FileInputStream isFile = new FileInputStream(tempFile);
				//*******************************************
				//	 * @param skippedSamples - number of samples to skip after each one returned. To adjust for graph resolution.
				int samplesPerPixel, skippedSamples, durationInSamples;
				rdtBis = new BufferedInputStream(isFile);

				//Read the 4 header bytes
				byte[] header = new byte[HEADERBYTES];
				int result = rdtBis.read(header,0,HEADERBYTES);
				
				if(result == HEADERBYTES) {
					ByteBuffer bbHead = ByteBuffer.wrap(header);
					bbHead.order(BYTEORDER);
	
					short channels = bbHead.getShort();
					short samplingRate = bbHead.getShort(); // replaced with subjectData.setSamplingRate() 
					float fRateMsec = (float)(samplingRate/1000.0);
					if (offsetMilliSeconds<0)offsetMilliSeconds=0; // cannot read before the beginning of the file.
					int vizOffset = (int) (offsetMilliSeconds*fRateMsec);
	
					//-------------------------------------------------
					// Calculate and Set Visualization parameters
					final int REALBUFFERSIZE = (int) fileSize - HEADERBYTES;
					if(REALBUFFERSIZE % (channels * SHORTBYTES) != 0) {
						System.err.println("rdt file is not aligned.");
					}
					int counts = REALBUFFERSIZE / (channels * SHORTBYTES);
					byte[][] body = new byte[counts][(channels * SHORTBYTES)];
					byte[] sample = new byte[(channels * SHORTBYTES)]; /** A single reading from all leads. **/ 
					try {
	
						int requestedMaxPoints;
						durationInSamples = (int) (fRateMsec*durationMilliSeconds);
						if(durationInSamples>graphWidthPixels){
							samplesPerPixel=durationInSamples/graphWidthPixels;
							requestedMaxPoints = graphWidthPixels;
						}else{
							samplesPerPixel=1;
							requestedMaxPoints = durationInSamples;
						}
						skippedSamples = samplesPerPixel-1;
	
						int availableSamples = counts - vizOffset; // total number of remaining samples from this offset.
						int availablePoints = availableSamples/samplesPerPixel; // total number of graphable points from this offset.
						int maxPoints = 0; // maximum data points that can be returned.
						// ensure that the copying loop doesn't try to go past the end of the data file.
						if(availablePoints > requestedMaxPoints) {
							maxPoints = requestedMaxPoints;
						} else {  // Requested duration is longer than the remainder after the offset.
							if(durationInSamples < counts){ // Requested duration is less than the file contains.
								// move the offset back so the requested amount of samples can be returned.
								vizOffset = counts - durationInSamples;
								maxPoints = requestedMaxPoints;
							}else{	// Requested duration is longer than the file contains.
								maxPoints = availablePoints;
							}
						}
						visualizationData.setRdtDataLength(maxPoints);
						visualizationData.setRdtDataLeads(channels);
						visualizationData.setOffset(vizOffset);
						visualizationData.setSkippedSamples(skippedSamples);
						int msDuration = (counts*1000)/samplingRate;
						visualizationData.setMsDuration(msDuration);
	
	
						//------------------------------------------------
						// Read the rest of the file to get the data.
						ByteBuffer bbSample;
						double[][] tempData = new double[maxPoints][channels];
						int fileOffset = vizOffset*channels*SHORTBYTES; //offset in bytes from the beginning of the file.
	
						int index1, index2, s, outSample=0;
						index2 =  vizOffset; // index of the first sample to return data for, index is in samples not bytes.
						int length, bisOffset, bisLen = sample.length;
						// read entire file into the local byte array "body"
						for (index1 =  0; index1 < counts; index1++){
							bisOffset = HEADERBYTES + (index1*bisLen);
							s=0;
							for(int c=0;c<(bisLen*4);c++){ // make up to 4 attempts to read 
								length = rdtBis.read(sample, s, 1);// read one byte into the byte array "sample", explicitly specifying which byte to read.
								if(length==1) s++; // successfully read the byte, go to the next one.
								if(s==bisLen) break; // last byte has been read.
							}
	
							if(index1==index2){ // add this sample the output data
								bbSample = ByteBuffer.wrap(sample);
								bbSample.order(BYTEORDER);
	
								for(int ch = 0; ch < channels; ch++) {
									short value =  bbSample.getShort(); // reads a Short, increments position() by 2 bytes.
									tempData[outSample][ch] = (double)value;
								}
	
								bbSample.clear();
								index2 = index2 + 1 + skippedSamples;
								outSample++;
								if(outSample==maxPoints) break;
							}
						}
	
						visualizationData.setRdtData(tempData);
	
						//*******************************************
						isFile.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					System.err.println("fetchSubjectVisualization failed, error occured while reading header of the RDT file:" + tempFile);
				}
				//*******************************************
			} catch(IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					rdtBis.close();
				} catch(IOException e2) {
					e2.printStackTrace();
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return visualizationData;
	}



	/** Wrapper around the 3 common functions for adding a child element to a parent OMElement.
	 * 
	 * @param name - name/key of the child element
	 * @param value - value of the new element
	 * @param parent - OMElement to add the child to.
	 * @param factory - OMFactory
	 * @param dsNs - OMNamespace
	 */
	private void addOMEChild(String name, String value, OMElement parent, OMFactory factory, OMNamespace dsNs){
		OMElement child = factory.createOMElement(name, dsNs);
		child.addChild(factory.createOMText(value));
		parent.addChild(child);
	}


	public org.apache.axiom.om.OMElement collectAnalysisData(org.apache.axiom.om.OMElement param0) {
		logger.setLevel(Level.OFF);
		int x = 0;
		String sId = null;
		String token = null;
		OMElement subject = null;

		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace dsNs = factory.createOMNamespace("http://www.cvrgrid.org/nodeDataService/",
		"dataStaging");
		OMElement collectAnalysisData = factory.createOMElement("collectAnalysisData", dsNs);

		OMElement status = factory.createOMElement("status", dsNs);
		status.addChild(factory.createOMText("success"));
		collectAnalysisData.addChild(status);
		OMElement subjects = factory.createOMElement("analysis", dsNs);

		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();
		File headFile = new File(localFtpRoot + sep  + userId + sep + "public");

		subjects = collectResultFiles(headFile, factory, dsNs, subjects);

		headFile = new File(localFtpRoot + sep  + userId + sep + "private");
		subject = collectResultFiles(headFile, factory, dsNs, subjects);

		collectAnalysisData.addChild(subjects);
		
		return collectAnalysisData;
	}

	/** Collects the information about the result files in the specified folder and builds OMElements to return the the browser.
	 * 
	 * @param collectAnalysisData - parent element to which to add the new child elements.
	 * @param headFile - The folder to look in for files.
	 * @param factory - The OMFactory to use.
	 * @param dsNs - The OMNamespace to use.
	 * @return - parent element to which the new child elements where added.
	 */
	private OMElement collectResultFiles(File headFile, OMFactory factory, OMNamespace dsNs, OMElement subjects){
		int x = 0;
		String sId = null;
		String token = null;
		OMElement oneSubject = null, subjectId = null, ecgFile = null, filename = null;

		File[] folders = headFile.listFiles();
		if(null != folders) {
			try {
				for(int i = 0, max = folders.length; i < max; i++) {
					if(folders[i].isDirectory()) {
						int position = folders[i].toString().lastIndexOf(sep);
						sId = folders[i].toString().substring(++position,
								folders[i].toString().length());
						analysisDataBuffer = null;
						listRecursively(folders[i]);
						if(null != analysisDataBuffer) {
							StringTokenizer tokenizer = new StringTokenizer(
									analysisDataBuffer.toString(), "|");
							while(tokenizer.hasMoreTokens()) {
								token = tokenizer.nextToken();
								oneSubject = factory.createOMElement("subject", dsNs);

								addOMEChild("subjectid", sId, oneSubject, factory, dsNs);
								if(token.indexOf("berger") != -1) {
									addOMEChild("berger", "true", oneSubject, factory, dsNs);
								}
								if(token.indexOf("chesnokov") != -1) {
									addOMEChild("chesnokov", "true", oneSubject, factory, dsNs);
								}
								if(token.indexOf("qrsscore") != -1) {
									addOMEChild("qrsscore", "true", oneSubject, factory, dsNs);
								}
								addOMEChild("filename", token, oneSubject, factory, dsNs);

								subjects.addChild(oneSubject);
							}
						}
					}
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return subjects;
	}

	/** Lists the output directories found in the specified directory, if they are berger, chesnokov or qrsscore
	 * 
	 * @param fdir - directory to start list from
	 */
	private void listRecursively(File fdir) {
		if(fdir.isDirectory()) {
			for(File f : fdir.listFiles()) listRecursively(f);
		} else {
			if(fdir.getName().endsWith("csv")) {
				if( (fdir.getPath().indexOf("output") != -1) && ((fdir.getPath().indexOf("berger") != -1) || (fdir.getPath().indexOf("chesnokov") != -1) || (fdir.getPath().indexOf("qrsscore") != -1))  ) {
					if(null == analysisDataBuffer) {
						analysisDataBuffer = new StringBuffer(fdir.getPath());
					} else {
						analysisDataBuffer.append("|" + fdir.getPath());
					}
				}
			}
		}
	}


	public org.apache.axiom.om.OMElement verifyBergerInput(org.apache.axiom.om.OMElement param0) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace omNs = fac.createOMNamespace("http://www.cvrgrid.org/nodeDataService/", "nodeDataService");
		OMElement stageTransferredDataStatus = fac.createOMElement("verifyBergerInput", omNs);
		Iterator iterator = param0.getChildren();
		String userId = ((OMElement)iterator.next()).getText();
		String subjectId = ((OMElement)iterator.next()).getText();
		String fileName = ((OMElement)iterator.next()).getText();
		fileName = fileName.substring(0, fileName.length() - 3) + "ini";
		boolean isFileExisting = checkIfStaged(fileName);
		if(isFileExisting) {
			stageTransferredDataStatus.addChild(fac.createOMText("" + "SUCCESS"));
		} else {
			stageTransferredDataStatus.addChild(fac.createOMText("" + "FAILURE"));
		}
		return stageTransferredDataStatus;
	}
	
	/** Checks if the file is already in the staging (ftp receive) folder.
	 * 
	 * @param fileName - file to check for, without path.
	 * @return boolean
	 */
	private boolean checkIfStaged(String fileName) {
		boolean isStaged = false;
		File xferFile = new File(localFtpRoot + sep  + fileName);
		if(xferFile.exists()) isStaged = true;
		debugPrintln("[Debug mode] checkIfStaged(): " + fileName + " isStaged" + isStaged);
		return isStaged;
	}

	private void debugPrintln(String text){
		utils.debugPrintln("+ " + text);
	}
}
