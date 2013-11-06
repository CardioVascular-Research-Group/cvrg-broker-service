package edu.jhu.cvrg.services.nodeDataService;

	import java.io.BufferedInputStream;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileNotFoundException;
	import java.io.IOException;
	import java.nio.ByteBuffer;
	import java.nio.ByteOrder;


	public class RDTParser{

		private File rdtFile;
		private short channels, samplingRate;
		private int counts;
		private long fileSize;
		private double[][] data;
		private static final ByteOrder BYTEORDER = ByteOrder.LITTLE_ENDIAN;
		private static final int HEADERBYTES = 4;
		private static final int SHORTBYTES = 2;
		private static final boolean verbose = true;

		public RDTParser(File rdtFile) {
			this.rdtFile = rdtFile;
		}
	        
		public boolean parse() {
			if(!rdtFile.exists()) {
				if(verbose) {
					System.err.println(this.rdtFile.getName() + " does not exist.");
				}
				return false;
			}

			fileSize = rdtFile.length();
			if(fileSize > Integer.MAX_VALUE) {
				System.err.println("file size exceeding maximum int value.");
				return false;
			}
			FileInputStream rdtFis;
			try {
				rdtFis = new FileInputStream(rdtFile);
			} catch(FileNotFoundException e) {
				rdtFis = null;
				System.err.println(e.toString());
				return false;
			}

			BufferedInputStream rdtBis = new BufferedInputStream(rdtFis);

			byte[] header = new byte[HEADERBYTES];
			try {
				int result = rdtBis.read(header);
				if(result != HEADERBYTES) {
					System.err.println("error occured while reading header.");
					return false;
				}
				ByteBuffer bbHead = ByteBuffer.wrap(header);
				bbHead.order(BYTEORDER);
				this.channels = bbHead.getShort();
				this.samplingRate = bbHead.getShort();
			} catch(IOException e) {
				if(verbose) {
					System.err.println(e.toString());
				}
				try {
					rdtBis.close();
				} catch(IOException e1) {
				}
				return false;
			}
			final int REALBUFFERSIZE = (int) fileSize - HEADERBYTES;
			if(REALBUFFERSIZE % (channels * SHORTBYTES) != 0) {
				System.err.println("rdt file is not aligned.");
				return false;
			}

			this.counts = REALBUFFERSIZE / (channels * SHORTBYTES);
			this.data = new double[channels][counts];
			byte[] body = new byte[REALBUFFERSIZE];
			boolean ret = false;
			try {
				int length = rdtBis.read(body);
				if(length != REALBUFFERSIZE) {
					System.err.println("error while reading data into buffer");
					try {
						rdtBis.close();
						rdtFis.close();
					} catch(IOException e2) {
					}
					return false;
				}

				ByteBuffer bbBody = ByteBuffer.wrap(body);
				bbBody.order(BYTEORDER);
				for(int index = 0; index < this.counts; index++) {
					for(int channel = 0; channel < this.channels; channel++) {
						short value = bbBody.getShort();
						this.data[channel][index] = value;
					}
				}
				ret = true;
			} catch(IOException e1) {

			} finally {
				try {
					rdtBis.close();
					rdtFis.close();
				} catch(IOException e2) {
				}
			}
			return ret;
		}

		public void viewData(int count) {
			if(this.data != null) {
				for(int index = 0; index < count; index++) {
					String line = "";
					for(int channel = 0; channel < this.channels; channel++) {
						line += this.data[channel][index] + ", ";
					}
					System.out.println(line);
	                                
				}
			}
		}

		public void viewHeader() {
			System.out.println("# of channels is " + this.channels
					+ "; sampling rate is " + this.samplingRate);
		}
	        
	    public double[][] getData() {
	          return this.data;
	    }
	    public short getChannels() {
	          return this.channels;
	    }
	    public int getCounts() {
	          return this.counts;
	    }
	    public int getSamplingRate() {
	          return this.samplingRate;
	    }
		public long getFileSize() {
			return fileSize;
		}
		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}
	
}