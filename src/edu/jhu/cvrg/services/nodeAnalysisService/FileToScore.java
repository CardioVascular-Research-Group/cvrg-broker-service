package edu.jhu.cvrg.services.nodeAnalysisService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class FileToScore {

	private Map<String,Integer> mapTypesFound = new HashMap<String,Integer>();

	private File fFile;
	private String outFileName;
	private QRS_Score score = new QRS_Score();

	private int lineCount=0, datasetCount=0, iCount = 0;
	private StringBuffer sb;
	private boolean verbose = false;
	
	private int reclassified;
	private String conductionType;	

	/**
	 * @param inputFile - file name of the input file.
	 */
	public void setfFile(String inputFile) {
		fFile = new File(inputFile);
		auditloggerln("Full path of input file: " + inputFile);
	}
	
	
	/** Procedure to set the outFileName variable.
	 * @param outFileName - file name for the results file generated
	 */
	public void setOutFileName(String outFileName) {
		this.outFileName = outFileName;
		auditloggerln("Full path of outFileName: " + outFileName);
	}
	
	public String getOutFileName() {
		return outFileName;
	}

	/**
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * @param verbose the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/** Constructor for the fileToScore class, which runs QRS_Score.calculateQRS_score() on the data in the file
	 * Derived from Steve Granite's GEMUSESplitter class
	 * @param inFile - input file name including path where the source file is located
	 * @param outFile - output file name, with extension and path.
	 * @param verbose - prints audit/debug text to System.out
	 */
	public FileToScore(String inFile, String outFile, boolean verbose){
		setVerbose(verbose);
		System.out.println("  fileToScore()> inFile: " + inFile + "   outFile: " + outFile + "   verbose:" + verbose);
		runCalculation(inFile, outFile);
		auditloggerln("Done");
	}

	/** Executes the QRS_Score algorithm.
	 * 
	 * @param localDirPath - path where the source file is located, and the result file will be created.
	 * @param inFile - input file name including path
	 * @param outFileName - output file name, without extension or path, path will be the same as the input file.
	 */
	private void runCalculation(String inFile, String outFileName){
		try {
			auditloggerln("Running QRS_Score.calculateQRS_score(inFile, outFileName) on file: \""+ inFile + "\"");
			auditloggerln("--------------------------------------------------------------------");
			auditloggerln("inFile: " + inFile);
			auditloggerln("outFileName: " + outFileName);
			setfFile(inFile);
			setOutFileName(outFileName);
			processLineByLine();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** Procedure to process the input line by line
	 */
	public final void processLineByLine() throws FileNotFoundException {
		Scanner lineScanner;
		try {
			lineScanner = new Scanner(fFile);
			
			try {
				//first use a Scanner to get each line
				auditloggerln("Scanning file: " + fFile);
				sb = new StringBuffer();
				sb.append("patientID,conductionType,points_I,points_II,points_aVL,points_aVF,points_V1ant,points_V1post,points_V2ant,points_V2post,points_V3,points_V4,points_V5,points_V6,QRS_Score,scar 1,scar 2,scar 3,scar 4,scar 5,scar 6,scar 7,scar 8,scar 9,scar 10,scar 11,scar 12,# out of 32,filepath\n");
				while ( lineScanner.hasNextLine() ){
					processLine( lineScanner.nextLine() );
				}
			}
			finally {
				//ensure the underlying stream is always closed
				lineScanner.close();
				auditloggerln("Writing string buffer to file: " + getOutFileName());
				writeFile(sb.toString(), getOutFileName()); // write output (csv) File
			}
		} catch (FileNotFoundException e) {
			System.err.println("Input file not found: " + fFile.getAbsolutePath());
		}
	}

	/** Procedure to perform individual line processing
	 * @param aLine - line from file to be processed
	 */
	protected void processLine(String aLine){
		if(iCount==0){
			setLineCount(getLineCount() + 1);
			aLine = aLine.trim();
			if (aLine.equalsIgnoreCase("scode")) {
				iCount++;
			}
		} else {
			setLineCount(getLineCount() + 1);
			if(aLine.length() > 0){
				int[] fullresults = score.getFullResults();
				String results="";
				for(int r : fullresults){
					results += r + ",";
				}
				String scar="";
				for(int r : fullresults){
					scar += r + ",";
				}

//				conductionType;
				String ID =score.ID;
				String filepath = score.FilePath;
				sb.append(ID + "," + conductionType  + "," + results  +  scar  +  filepath + "\n");
				setDatasetCount(getDatasetCount() + 1);
			}
		}
	}

	/** Procedure to output information extracted from the input file
	 * @param aText - String variable containing the output
	 * @param outputFileName - file name for the output file generated 
	 */
	protected void writeFile(String aText, String outputFileName){
		try {
			// Create file
			FileWriter fstream = new FileWriter(new File(outputFileName));
			BufferedWriter out1 = new BufferedWriter(fstream);
			out1.write(aText);
			out1.close();
			fstream.close();
		} catch (Exception ex){
			System.err.println("Error: " + ex.getMessage());
		}    
	}


	protected int scoreLine(String aLine){
		int result;
		Scanner des = new Scanner(aLine); // data element Scanner
		des.useDelimiter(" ");

		score.Name = des.next(); //[0]);  //123442810;
		score.ID =  des.next(); //[1]);  //123442810;  //
		score.Date_Time = des.next(); //[2];  //"01/15/2010_11:41";  //
		String dummySpace = des.next(); // because this data element has 2 spaces in front of it instead of two.
		score.FilePath = des.next(); // [3];  //"C:\\magellan\\data\\RestECG\\TestECGs to GRID\\DeIdentified ECGs\\2.ECG" ;  //
		score.Age =  des.nextFloat(); //[4]);  //53;  //
		score.Sex =  des.nextFloat(); //[5]);  //0;  //
		score.vrate =  des.nextFloat(); //[6]);  //51;  //
		score.arate =  des.nextFloat(); //[7]);  //51;  //
		score.pr =  des.nextFloat(); //[8]);  //156;  // 
		score.qrsd =  des.nextFloat(); //[9]);  //88;  //
		score.qt =  des.nextFloat(); //[10]);  //448;  //
		score.qtc =  des.nextFloat(); //[11]);  //412;  //  	
		score.pax =  des.nextFloat(); //[12]);  //76;  //
		score.qrsax =  des.nextFloat(); //[13]);  //55;  //
		score.tax =  des.nextFloat(); //[14]);  //57;  //
		score.pdur =  des.nextFloat(); //[15]);  //114;  //  	

		score.qrstangle =  des.nextFloat(); //[16]);  //53;  //

		score.pa_aVF =  des.nextFloat(); //[17]);  //87;  //
		score.pa_V1 =  des.nextFloat(); //[18]);  //58;  //

		score.qa_I =  des.nextFloat(); //[19]);  //43;  //
		score.qa_II =  des.nextFloat(); //[20]);  //39;  //
		score.qa_aVL =  des.nextFloat(); //[21]);  //39;  //
		score.qa_aVF =  des.nextFloat(); //[22]);  //24;  //
		score.qa_V1 =  des.nextFloat(); //[23]);  //0; //
		score.qa_V2 =  des.nextFloat(); //[24]);  //0;  // 
		score.qa_V3 =  des.nextFloat(); //[25]);  //0;  //
		score.qa_V4 =  des.nextFloat(); //[26]);  //0;  //
		score.qa_V5 =  des.nextFloat(); //[27]);  //29;  //  	
		score.qa_V6 =  des.nextFloat(); //[28]);  //83;  //

		score.qd_I =  des.nextFloat(); //[29]);  //21;  //  	
		score.qd_II =  des.nextFloat(); //[30]);  //18;  //
		score.qd_aVL =  des.nextFloat(); //[31]);  //26;  // 
		score.qd_aVF =  des.nextFloat(); //[32]);  //18;  //
		score.qd_V1 =  des.nextFloat(); //[33]);  //0;  //
		score.qd_V2 =  des.nextFloat(); //[34]);  //0;  //
		score.qd_V3 =  des.nextFloat(); //[35]);  //0;  //
		score.qd_V4 =  des.nextFloat(); //[36]);  //0;  //
		score.qd_V5 =  des.nextFloat(); //[37]);  //15;  //
		score.qd_V6 =  des.nextFloat(); //[38]);  //21;  //

		score.ra_I =  des.nextFloat(); //[39]);  //620;  //  	
		score.ra_II =  des.nextFloat(); //[40]);  //1181;  //  	
		score.ra_aVL =  des.nextFloat(); //[41]);  //87;  //  	
		score.ra_aVF =  des.nextFloat(); //[42]);  //898;  //
		score.ra_V1 =  des.nextFloat(); //[43]);  //209;  //  	
		score.ra_V2 =  des.nextFloat(); //[44]);  //454;  //  	
		score.ra_V3 =  des.nextFloat(); //[45]);  //390;  //  	
		score.ra_V4 =  des.nextFloat(); //[46]);  //1562;  //
		score.ra_V5 =  des.nextFloat(); //[47]);  //2075;  //  	
		score.ra_V6 =  des.nextFloat(); //[48]);  //1601;  //

		score.rd_V1 =  des.nextFloat(); //[49]);  //25;  //
		score.rd_V2 =  des.nextFloat(); //[50]);  //25;  //
		score.rd_V3 =  des.nextFloat(); //[51]);  //27;  //


		score.sa_I =  des.nextFloat(); //[52]);  //0;  //  	
		score.sa_II =  des.nextFloat(); //[53]);  //170;  //
		score.sa_aVL =  des.nextFloat(); //[54]);  //78;  //
		score.sa_aVF =  des.nextFloat(); //[55]);  //185;  //
		score.sa_V1 =  des.nextFloat(); //[56]);  //1250;  //
		score.sa_V2 =  des.nextFloat(); //[57]);  //2021;  //
		score.sa_V3 =  des.nextFloat(); //[58]);  //1250;  //
		score.sa_V4 =  des.nextFloat(); //[59]);  //502;  //
		score.sa_V5 =  des.nextFloat(); //[60]);  //97;  //
		score.sa_V6 =  des.nextFloat(); //[61]);  //0;  //  	

		score.rpa_I =  des.nextFloat(); //[62]);  //0;  //
		score.rpa_II =  des.nextFloat(); //[63]);  //0;  //
		score.rpa_aVL =  des.nextFloat(); //[64]);  //122;  //  	
		score.rpa_aVF =  des.nextFloat(); //[65]);  //0;  //  	
		score.rpa_V1 =  des.nextFloat(); //[66]);  //0;  //
		score.rpa_V2 =  des.nextFloat(); //[67]);  //0;  //  	
		score.rpa_V3 =  des.nextFloat(); //[68]);  //0;  //  	
		score.rpa_V4 =  des.nextFloat(); //[69]);  //0;  //  	
		score.rpa_V5 =  des.nextFloat(); //[70]);  //29 ;  //
		score.rpa_V6 =  des.nextFloat(); //[71]);  //0;  //  	

		score.spa_I =  des.nextFloat(); //[72]);  //0;  //
		score.spa_II =  des.nextFloat(); //[73]);  //0;  //
		score.spa_aVL =  des.nextFloat(); //[74]);  //0;  //
		score.spa_aVF =  des.nextFloat(); //[75]);  //0;  //
		score.spa_V1 =  des.nextFloat(); //[76]);  //0;  //
		score.spa_V2 =  des.nextFloat(); //[77]);  //0;  //
		score.spa_V3 =  des.nextFloat(); //[78]);  //0;  //
		score.spa_V4 =  des.nextFloat(); //[79]);  //0;  //
		score.spa_V5 =  des.nextFloat(); //[80]);  //0;  //
		score.spa_V6 =  des.nextFloat(); //[81]);  //0;  //  	
		score.No_scode = des.nextInt();// Number of scodes to follow.

		score.scode.clear();
		score.scode.add(des.nextInt()); 
		des.useDelimiter("  ");// because sCodes have double spaces!?
		for(int i=1;i<score.No_scode;i++){
			score.scode.add(des.nextInt()); 
		}

		conductionType = score.classifyConductionType();
		result = score.calculateQRS_score();
		this.reclassified = score.reclassified;
		
		int count = getMapTypesFound().containsKey(conductionType) ? getMapTypesFound().get(conductionType) : 0;
		getMapTypesFound().put(conductionType, count + 1);

		return result;
	}

	/** Procedure to echo values to the System out
	 * @param aObject - Object to be converted to a String for printing
	 */
	private void auditloggerln(Object aObject){
		if(verbose) System.out.println("fTS> " + String.valueOf(aObject));
	}
	/** Procedure to echo values to the System out
	 * @param aObject - Object to be converted to a String for printing
	 */
	private void auditlogger(Object aObject){
		if(verbose) System.out.print(String.valueOf(aObject));
	}

	public void setLineCount(int lineCount) {
		this.lineCount = lineCount;
	}

	public int getLineCount() {
		return lineCount;
	}

	public void setDatasetCount(int datasetCount) {
		this.datasetCount = datasetCount;
	}

	public int getDatasetCount() {
		return datasetCount;
	}

	public void setMapTypesFound(Map<String,Integer> mapTypesFound) {
		this.mapTypesFound = mapTypesFound;
	}

	public Map<String,Integer> getMapTypesFound() {
		return mapTypesFound;
	}


	public int getReclassified() {
		return reclassified;
	}

}
