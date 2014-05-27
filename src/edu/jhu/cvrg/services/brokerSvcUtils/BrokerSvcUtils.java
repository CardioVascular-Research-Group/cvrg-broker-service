package edu.jhu.cvrg.services.brokerSvcUtils;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.net.ftp.FTP;
import org.apache.log4j.Logger;

import edu.jhu.cvrg.waveform.model.ApacheCommonsFtpWrapper;


public class BrokerSvcUtils {
	
	Logger log = Logger.getLogger(BrokerSvcUtils.class);
	
	/** remote ftp server's root directory for ftp, <BR>e.g. /export/icmv058/cvrgftp **/
	private String sep = File.separator;
	private boolean verbose = false;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	// folders to be created:
	private String publicRoot; //       = remoteFtpRoot + sep  + userId + sep + "public"; //  + sep + subjectId + sep + "input";
	private String input; //            = remoteFtpRoot + sep  + userId + sep + "public"  + sep + subjectId + sep + "input";
	private String privateInput; //     = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "input";
	private String privatechesnokov; // = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "chesnokov";
	private String privateberger; //    = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "berger";
	private String privateqrsscore; //  = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "qrsscore";
			
	private String original; // = remoteFtpRoot + sep  + userId + sep + publicPrivate + subjectId + sep + "input" + sep + originalFileName;
	private String target; //   = remoteFtpRoot + sep  + userId + sep + publicPrivate + subjectId + sep + "input" + sep;

	/** Constructor
	 * @param verbose - if true, generate debugging messages to the console.
	 */
	public BrokerSvcUtils(boolean verbose){
		this.verbose = verbose;
		debugPrintLocalln("Initializing brokerSvcUtils() in Verbose mode.");
	}
	
	
	/** Creates a unique string based on the current timestamp, plus a pseudorandom number between zero and 1000
	 * 	In the form YYYYyMMmDDdHH_MM_SSxRRRR
	 * @return - a mostly unique string
	 */
	public String generateTimeStamp() {		
			Calendar now = Calendar.getInstance();
			int year = now.get(Calendar.YEAR);
			int month = now.get(Calendar.MONTH) + 1;
			int day = now.get(Calendar.DATE);
			int hour = now.get(Calendar.HOUR_OF_DAY); // 24 hour format
			int minute = now.get(Calendar.MINUTE);
			int second = now.get(Calendar.SECOND);
			
			String date = new Integer(year).toString() + "y";
			
			if(month<10)date = date + "0"; // zero padding single digit months to aid sorting.
			date = date + new Integer(month).toString() + "m"; 

			if(day<10)date = date + "0"; // zero padding to aid sorting.
			date = date + new Integer(day).toString() + "d";

			if(hour<10)date = date + "0"; // zero padding to aid sorting.
			date = date + new Integer(hour).toString() + "_"; 

			if(minute<10)date = date + "0"; // zero padding to aid sorting.
			date = date + new Integer(minute).toString() + "_"; 

			if(second<10)date = date + "0"; // zero padding to aid sorting.
			date = date + new Integer(second).toString() + "x";
			
			// add the random number just to be sure we avoid name collisions
			Random rand = new Random();
			date = date + rand.nextInt(1000);
			return date;
		}
	
	/** Make final destination directories and transfer the file into it.<br>
	 * Parses userId, subjectId and originalFileName from fileName.<br>
	 * <br>
	 * Six created directories:<br>
	 * <i>
	 *  remoteFtpRoot/userId/public/subjectId/input<br>
	 *  remoteFtpRoot/userId/private/subjectId/input<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/chesnokov<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/chesnokov<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/berger<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/berger<br>
	 *  <br>
	 *  </i>
	 *  The original filename is restored, and the directory path is:<br>
	 *  <i>remoteFtpRoot/userId/public/subjectId/input<br>
	 *    or<br>
	 *  remoteFtpRoot/userId/private/subjectId/input</i><br>
	 *  <br>
	 * If the file exists there, is it replaced.<br>
	 *
	 * @param parentFolder
	 * @param fileName - of local file to be moved to ftp server, contains other info so it must be in the form: userId_subjectId_originalFileName.ext
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassword
	 * @param remoteFtpRoot - root directory on the ftp server, final directories will be built on this.
	 * @param isPublic
	 * @throws IOException 
	 */
	public boolean routeToFolderOld(String parentFolder, String fileName,
			String ftpHost, String ftpUser, String ftpPassword, String remoteFtpRoot, 
			boolean isPublic) throws IOException {
		boolean success = true;
		String publicPrivate = isPublic ?  "public": "private";
		
		// parse the temporary file name.
		StringTokenizer tokenizer = new StringTokenizer(fileName, "_");
		String userId = tokenizer.nextToken();
		String subjectId = tokenizer.nextToken().toLowerCase();
		String originalFileName = tokenizer.nextToken("").substring(1); 
		
		ApacheCommonsFtpWrapper ftpWrap = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
		
		createDirectoryNames(remoteFtpRoot, userId, subjectId, publicPrivate, originalFileName);
		success = makeInOutDirectories(ftpWrap, originalFileName, fileName);

		try {
	    	String inputPath="";
	        if(isPublic) {
	        	inputPath = input;
	        } else {
	        	inputPath = privateInput;
	        }
	        
	        success =  moveFileFTP(ftpWrap, inputPath, parentFolder, originalFileName, fileName);
	        
	    } catch (Exception ex) {
	        log.error("Failed to write file: " + originalFileName + " isPublic: " + isPublic);
	    	ex.printStackTrace();
	    	success = false;
	    }
	    
	    return success;
	}
	

	/** Make final destination directories and transfer the file into it.<br>
	 * Parses userId, subjectId and originalFileName from fileName.<br>
	 * <br>
	 * Six created directories:<br>
	 * <i>
	 *  remoteFtpRoot/userId/public/subjectId/input<br>
	 *  remoteFtpRoot/userId/private/subjectId/input<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/chesnokov<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/chesnokov<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/berger<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/berger<br>
	 *  <br>
	 *  </i>
	 *  The original filename is restored, and the directory path is:<br>
	 *  <i>remoteFtpRoot/userId/public/subjectId/input<br>
	 *    or<br>
	 *  remoteFtpRoot/userId/private/subjectId/input</i><br>
	 *  <br>
	 * If the file exists there, is it replaced.<br>
	 *
	 * @param parentFolder
	 * @param fileName - of local file to be moved to ftp server, contains other info so it must be in the form: userId_subjectId_originalFileName.ext
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassword
	 * @param remoteFtpRoot - root directory on the ftp server, final directories will be built on this.
	 * @param isPublic
	 * @return true on successful copying
	 * @throws IOException 
	 */
	public boolean routeToFolder(String parentFolder, String fileName,
			String ftpHost, String ftpUser, String ftpPassword, String remoteFtpRoot, 
			boolean isPublic) throws IOException {
		boolean success = true;
		
		// parse the temporary file name.
		StringTokenizer tokenizer = new StringTokenizer(fileName, "_");
		String userId = tokenizer.nextToken();
		String subjectId = tokenizer.nextToken().toLowerCase();
		String originalFileName = tokenizer.nextToken("").substring(1); 
		
		success = routeToFolder(parentFolder, fileName, userId, subjectId, originalFileName,
				ftpHost, ftpUser, ftpPassword, remoteFtpRoot,
				isPublic);
	    
	    return success;
	}
	
	/** Make final destination directories and transfer the file into it.<br>
	 * Parses userId, subjectId and originalFileName from fileName.<br>
	 * <br>
	 * Six created directories:<br>
	 * <i>
	 *  remoteFtpRoot/userId/public/subjectId/input<br>
	 *  remoteFtpRoot/userId/private/subjectId/input<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/chesnokov<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/chesnokov<br>
	 *  <br>
	 *  remoteFtpRoot/userId/public/subjectId/output/berger<br>
	 *  remoteFtpRoot/userId/private/subjectId/output/berger<br>
	 *  <br>
	 *  </i>
	 *  The original filename is restored, and the directory path is:<br>
	 *  <i>remoteFtpRoot/userId/public/subjectId/input<br>
	 *    or<br>
	 *  remoteFtpRoot/userId/private/subjectId/input</i><br>
	 *  <br>
	 * If the file exists there, is it replaced.<br>
	 *
	 * @param parentFolder
	 * @param fileName - of local file to be moved to ftp server, contains other info so it must be in the form: userId_subjectId_originalFileName.ext
	 * @param userId 
	 * @param subjectId 
	 * @param originalFileName 
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassword
	 * @param remoteFtpRoot - root directory on the ftp server, final directories will be built on this.
	 * @param isPublic
	 * @return true on successful copying
	 * @throws IOException 
	 */
	public boolean routeToFolder(String parentFolder, String fileName, String userId, String subjectId, String originalFileName,
			String ftpHost, String ftpUser, String ftpPassword, String remoteFtpRoot, 
			boolean isPublic) throws IOException {
		boolean success = true;
//		boolean isCreated = false;
		String publicPrivate = isPublic ?  "public": "private";
		
		ApacheCommonsFtpWrapper ftpWrap = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
		
		createDirectoryNames(remoteFtpRoot, userId, subjectId, publicPrivate, originalFileName);
		success = makeInOutDirectories(ftpWrap, originalFileName, fileName);

		try {
			String inputPath = isPublic ?  input: privateInput;
	        success =  moveFileFTP(ftpWrap, inputPath, parentFolder, originalFileName, fileName);
	    } catch (Exception ex) {
	        log.error("Failed to write file: " + originalFileName + " isPublic: " + isPublic);
	    	ex.printStackTrace();
	    	success = false;
	    }
	    
	    return success;
	}

	
	/** Transfer the file into the input folder.<br>
	 * Parses userId, subjectId and originalFileName from fileName.<br>
	 * <br>
 	 *  The original filename is restored, and the directory path is:<br>
	 *  <i>remoteFtpRoot/userId/public/subjectId/input<br>
	 *  or<br>
	 *  remoteFtpRoot/userId/private/subjectId/input</i><br>
	 *  <br>
	 * If the file exists there, is it replaced.<br>
	 *
	 * @param localInPath
	 * @param localInFilename - of local file to be moved to ftp server, contains other info so it must be in the form: userId_subjectId_originalFileName.ext
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassword
	 * @param remoteFtpRoot - root directory on the ftp server, final directories will be built on this.
	 * @param isPublic
	 * @throws IOException 
	 */
	public boolean routeToExistingFolder(String localInPath, String localInFilename,
			String ftpHost, String ftpUser, String ftpPassword, String remoteFtpRoot, 
			boolean isPublic) throws IOException {
		boolean success = true;
		boolean isCreated = false;
		String ftpOutFilename="", userId="",subjectId="";
		try {
			debugPrintLocalln("routeToExistingFolder: "); 
			debugPrintLocalln("# parentFolder: " + localInPath); 
			debugPrintLocalln("# fileName: " + localInFilename); 
			debugPrintLocalln("# remoteFtpRoot: " + remoteFtpRoot); 
			debugPrintLocalln("# isPublic: " + isPublic); 
			
			String publicPrivate = isPublic ?  "public": "private";
			debugPrintLocalln("## publicPrivate: " + publicPrivate);
	
			// parse the temporary file name.
			StringTokenizer tokenizer = new StringTokenizer(localInFilename, "_");
			userId = tokenizer.nextToken();
			subjectId = tokenizer.nextToken().toLowerCase();
			ftpOutFilename = tokenizer.nextToken("").substring(1); 
	
			debugPrintLocalln("## userId: " + userId); 
			debugPrintLocalln("## subjectId: " + subjectId); 
			debugPrintLocalln("## originalFileName: " + ftpOutFilename); 
			
			ApacheCommonsFtpWrapper ftpWrap = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			createDirectoryNames(remoteFtpRoot, userId, subjectId, publicPrivate, ftpOutFilename);
		
			String ftpOutPath = isPublic ?  input: privateInput;
			debugPrintLocalln("## inputPath: " + ftpOutPath); 
	        success =  moveFileFTP(ftpWrap, ftpOutPath, localInPath, ftpOutFilename, localInFilename);
	    } catch (Exception ex) {
	        log.error("Failed to write file: " + ftpOutFilename + " isPublic: " + isPublic);
	    	ex.printStackTrace();
	    	success = false;
	    }
	    
	    return success;
	}
	
	/** Copy the file into the input folder.<br>
	 * Does not delete the orignal file..<br>
	 * <br>
 	 *  The same filename is used at ftp destination, and the directory path is:<br>
	 *  <i>remoteFtpRoot/userId/public/subjectId/input<br>
	 *  or<br>
	 *  remoteFtpRoot/userId/private/subjectId/input</i><br>
	 *  <br>
	 * If the file exists there, is it replaced.<br>
	 *
	 * @param localInPath - complete local path the file is in.
	 * @param localInFilename - of local file to be copied to ftp server. File name only, no path fragments
	 * @param userId
	 * @param subjectId
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassword
	 * @param remoteFtpRoot - root directory on the ftp server, final directories will be built on this.
	 * @param isPublic - boolean 
	 * @return success/fail of copying.
	 * @throws IOException 
	 */
	public boolean routeToExistingFolder(String localInPath, String localInFilename, String userId, String subjectId, 
			String ftpHost, String ftpUser, String ftpPassword, String remoteFtpRoot, 
			boolean isPublic) throws IOException {
		boolean success = false;
		String ftpOutFilename="";

		try {
			debugPrintLocalln("routeToExistingFolder: "); 
			debugPrintLocalln("# localInPath: " + localInPath); 
			debugPrintLocalln("# localInFilename: " + localInFilename); 
			debugPrintLocalln("# userId: " + userId); 
			debugPrintLocalln("# subjectId: " + subjectId); 
			debugPrintLocalln("# remoteFtpRoot: " + remoteFtpRoot); 
			debugPrintLocalln("# isPublic: " + isPublic); 
			
			String publicPrivate = isPublic ?  "public": "private";
			debugPrintLocalln("## publicPrivate: " + publicPrivate);
	
			// output file name is now the same as the input filename, no parsing required.
			ftpOutFilename = localInFilename; 	
			debugPrintLocalln("## ftpOutFilename: " + ftpOutFilename); 
			
			ApacheCommonsFtpWrapper ftpWrap = new ApacheCommonsFtpWrapper(ftpHost, ftpUser, ftpPassword);
			createDirectoryNames(remoteFtpRoot, userId, subjectId, publicPrivate, ftpOutFilename);
		
			String ftpOutPath = isPublic ?  input: privateInput;
			debugPrintLocalln("## ftpOutPath: " + ftpOutPath); 
	        success =  moveFileFTP(ftpWrap, ftpOutPath, localInPath, ftpOutFilename, localInFilename);
	    } catch (Exception ex) {
	        log.error("Failed to write file: " + ftpOutFilename + " isPublic: " + isPublic);
	    	ex.printStackTrace();
	    	success = false;
	    }
	    
	    return success;
	}

	
	private void createDirectoryNames(String remoteFtpRoot, String userId, String subjectId, String publicPrivate, String originalFileName){
		debugPrintLocalln("createDirectoryNames()");
		publicRoot       = remoteFtpRoot + sep  + userId + sep + "public"; //  + sep + subjectId + sep + "input";
		input            = remoteFtpRoot + sep  + userId + sep + "public"  + sep + subjectId + sep + "input";
		privateInput     = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "input";
		privatechesnokov = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "chesnokov";
		privateberger    = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "berger";
		privateqrsscore  = remoteFtpRoot + sep  + userId + sep + "private" + sep + subjectId + sep + "output" + sep + "qrsscore";
				
		original = remoteFtpRoot + sep  + userId + sep + publicPrivate + subjectId + sep + "input" + sep + originalFileName;
		target   = remoteFtpRoot + sep  + userId + sep + publicPrivate + subjectId + sep + "input" + sep;

	}
	
	public boolean makeInOutDirectories(ApacheCommonsFtpWrapper ftpWrap, String originalFileName, String fileName) throws IOException {
		boolean success = true, isCreated=false;
		debugPrintLocalln("makeInOutDirectories()");
		
		String warning = "*** WARNING: could not create the new folder ";
		
		if(new File(original).exists()) {
			// file already exists, destroy from target area, prep for replacement
			debugPrintLocalln("File \"" + originalFileName + "\" already exists on ftp server, removing so it can be replaced with newer version");
			removeFromStagingArea(target, fileName);
		} else {
			try {// create input directories path.
				if(!(new File(publicRoot).exists())) { //make the public dir as a place keeper, not used at the moment. will be used when "make public" function is added.
					isCreated = ftpWrap.mkdirs(publicRoot);
					
					if(!isCreated) log.info(warning + publicRoot);
				}
				isCreated = ftpWrap.mkdirs(privateInput);
				if(!isCreated) { 
					log.info(warning + privateInput);
					success = false;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				success = false;
			}
			
			try {// create chesnokov directories path.
				isCreated = ftpWrap.mkdirs(privatechesnokov);
				if(!isCreated){
					log.info(warning + privatechesnokov);
					success = false;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				success = false;
			}
	
			try {// create berger directories path.
				isCreated = ftpWrap.mkdirs(privateberger);
				if(!isCreated){
					log.info(warning + privateberger);
					success = false;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				success = false;
			}
	
			try {// create QRS Score directories path.
				isCreated = ftpWrap.mkdirs(privateqrsscore);
				if(!isCreated){
					log.info(warning + privateqrsscore);
					success = false;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
				success = false;
			}
			//*******************************************
			debugPrintLocalln("Directory creation completed.");
		}

		return success;
	}

	/** Copies the file to the FTP server, does not remove the original file.
	 * 
	 * @param ftpWrap
	 * @param ftpOutPath
	 * @param localInPath
	 * @param ftpOutFileName
	 * @param localInFilename
	 * @return true on successful coping
	 */
	public boolean moveFileFTP(ApacheCommonsFtpWrapper ftpWrap, String ftpOutPath, String localInPath, String ftpOutFileName, String localInFilename){
		boolean success = true; 
		debugPrintLocalln("moveFileFTP()");
		
		try {
	        ftpWrap.setFileType(FTP.BINARY_FILE_TYPE);
	        log.info("writing file " + localInPath + "/" + localInFilename + " to ftp as: " + ftpOutPath + "/" + ftpOutFileName);
	        success = ftpWrap.uploadFile(localInPath, localInFilename, ftpOutPath, ftpOutFileName);

	        if(success){
	        	debugPrintLocalln("...Done");
	        }else{
	        	debugPrintLocalln("...failed!");	        	
	        }
	    } catch (Exception ex) {
	    	debugPrintLocalln("Failed to write file: " + ftpOutFileName + " inputPath: " + ftpOutPath);
	    	ex.printStackTrace();
	    	success = false;
	    }
	    debugPrintLocalln("File move completed.");
		return success;
	}
		
		
	/** Deletes the file from the specified directory.
	 * 
	 * @param path - path to prepend, must include file separator (e.g. "/")
	 * @param file - file to check for, without path.
	 */
	public void removeFromStagingArea(String path, String file) {
		debugPrintLocalln("removeFromStagingArea()");
		try {
			new File(path + sep + file).delete();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		debugPrintLocalln("Remove From Staging completed");
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

	 
	 /** Set standard OME options and create a new ServiceClient, and creates the required EndpointReference.
	  * 
	  * @param brokerURL - URL of the service to send the request to.
	  * @return a ServiceClient instance.
	  */
	 public ServiceClient getSender(String brokerURL){
			EndpointReference targetEPR = new EndpointReference(brokerURL);
			return getSender(targetEPR, brokerURL);
	 }
	 
	 /** Set standard OME options and create a new ServiceClient.
	  * 
	  * @param targetEPR
	  * @param brokerURL - URL of the service to send the request to.
	  * @return a ServiceClient instance.
	  */
	 private ServiceClient getSender(EndpointReference targetEPR, String brokerURL){
			Options options = new Options();
			options.setTo(targetEPR);
			options.setProperty(HTTPConstants.SO_TIMEOUT,new Integer(18000000));
			options.setProperty(HTTPConstants.CONNECTION_TIMEOUT,new Integer(18000000));
			options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
			options.setAction(brokerURL);
			
			ServiceClient sender = null;
			try {
				sender = new ServiceClient();
			} catch (AxisFault e) {
				e.printStackTrace();
			}
			sender.setOptions(options);
			
			return sender;
	 }


	public void debugPrintLocalln(String text){
		if(verbose)	log.info("+ bSvcUtils: " + text);
	}
	public void debugPrintln(String text){
		if(verbose)	log.info("+ " + text);
	}
	public void debugPrint(String text){
		if(verbose)	log.info("+ " + text);
	}
}
