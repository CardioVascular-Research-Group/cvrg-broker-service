package edu.jhu.cvrg.services.nodeDataService;

import java.io.Serializable;

/** The raw ECG samples, in millivolts, for displaying the ecg chart in graphical form.
 * 
 * @author M.Shipway, W.Gertin
 *
 */
public class VisualizationData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** Count of samples in RDT data. (rows)*/
	int rdtDataLength;
	
	/** Count of leads in RDT data. (columns)*/
	int rdtDataLeads = 3;

	/** The raw ECG samples **/
	double[][] rdtData;
	
	/** Offset, in samples, from beginning of the ecg data set (SubjectData). Zero offset means first reading in data set.**/
	int offset;
	
	/**Number of samples to skip after each one returned. To adjust for graph resolution.**/
	int skippedSamples;
	
	/** duration of the ECG in milliseconds. **/
	int msDuration;
	
	/**Get number of samples to skip after each one returned. To adjust for graph resolution.**/
	public int getSkippedSamples() {
		return skippedSamples;
	}
	/**Set number of samples to skip after each one returned. To adjust for graph resolution.**/
	public void setSkippedSamples(int skippedSamples) {
		this.skippedSamples = skippedSamples;
	}

	/** Get offset, in samples, from beginning of the ecg data set (SubjectData).**/
	public int getOffset() { return offset; }
	/** Set offset, in samples, from beginning of the ecg data set (SubjectData). **/
	public void setOffset(int offset) { this.offset = offset; }
	
	/** Get the count of leads in RDT data. (columns)*/
	public int getRdtDataLeads() {return rdtDataLeads; }
	/** Set the count of leads in RDT data. (columns)*/
	public void setRdtDataLeads(int rdtDataLeads) { this.rdtDataLeads = rdtDataLeads; }

	/** Get the count of samples in RDT data.(rows)*/
	public int getRdtDataLength() {return rdtDataLength;}
	/** Get the count of samples in RDT data.(rows)*/
	public void setRdtDataLength(int rdtDataLength) { this.rdtDataLength = rdtDataLength; }

	/** Get the duration of the ECG in milliseconds. 
	 * @return the msDuration
	 */
	public int getMsDuration() {
		return msDuration;
	}
	/** Set the duration of the ECG in milliseconds. 
	 * @param msDuration the msDuration to set
	 */
	public void setMsDuration(int msDuration) {
		this.msDuration = msDuration;
	}

	/** ECG samples in millivolts, one column per lead, one row per sample displayed.
	 * 
	 * @return
	 */
	public double[][] getRdtData() {return rdtData;}
	public void setRdtData(double[][] rdtData) {
		this.rdtData = rdtData;
	}

}