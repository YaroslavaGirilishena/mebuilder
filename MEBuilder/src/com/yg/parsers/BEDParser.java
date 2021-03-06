package com.yg.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.yg.models.BEDData;
import com.yg.utilities.IOGeneralHelper;
import com.yg.utilities.PatternSplitter;

/**
 * This class parses .BED files of MEIs locations and puts data into global list of MEIs
 * As well parses ref .BED file into files that contain data about one MEI subtype
 * @author Yaroslava Girilishena
 *
 */
public class BEDParser {
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // init logger
	
	public String filename;
	private List<BEDData> listOfdata;
	private boolean usingScore;
	
	public BEDParser(String filename, boolean score){
		this.filename = filename;
		this.listOfdata = new ArrayList<BEDData>();
		this.usingScore = score;
	}
	
	public List<BEDData> parse() throws IOException {
		LOGGER.info("Parsing .BED file: " + this.filename + "\n");
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(this.filename));
    		boolean hasNext = true;
    		while (hasNext) {
    			hasNext = parseBEDFile(reader);
    		}
			
	    	IOUtils.closeQuietly(reader);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		LOGGER.info("Finished parsing .BED file: " + this.filename + "\n");
    	return listOfdata;
    }
	
	private boolean parseBEDFile(BufferedReader reader) throws IOException {
		try{
    		String line = reader.readLine();
     
    		if (line == null) {
    			return false;
    		}
    		   
    		List<String> data = PatternSplitter.toList(PatternSplitter.PTRN_TAB_SPLITTER, line);
    		
    		// chrom
    		String chrom = data.get(0);
    		// Ignore chromosome with null name 
    		if (chrom == null || chrom.equals("")) {
    			return true;
    		}
    		
    		// chromStart
    		long chromStart;
			try {
				chromStart = Long.parseLong(data.get(1));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("ERROR - .BED Parsing: position " + data.get(1) + " is not numerical");
			}
			
			// chromEnd
			long chromEnd;
			try {
				chromEnd = Long.parseLong(data.get(2));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("ERROR - .BED Parsing: position " + data.get(2) + " is not numerical");
			}
			
			// name
			String name = "";
			if (data.size() > 3 && !data.get(3).equals(".")) {
				name = data.get(3);
			} 
			
			// score 
			int score = 0;
			if (this.usingScore) {
				try {
					if (data.size() > 4)
						score = Integer.parseInt(data.get(4));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("ERROR - .BED Parsing: position " + data.get(4) + " is not numerical");
				}
			}

			
			// strand
			char strand = '.';
			int strandIndex = 4;
			if (this.usingScore) {
				strandIndex = 5;
			}
			if (data.size() > strandIndex && data.get(strandIndex) != null && !data.get(strandIndex).equals(".")) {
				strand = data.get(strandIndex).charAt(0);
			}
			
			listOfdata.add(new BEDData(chrom, chromStart, chromEnd, name, score, strand));
			
    	} catch(IOException e) {
    		System.out.println("Error when reading " + this.filename);
    		throw new IOException(e);
    	}
		return true;
	}
	
	/**
	 * Send .BED file to to be parsed into files by MEI subtypes
	 * @param bedToParse
	 * @throws IOException
	 */
	public static void parseByTypes(String bedToParse) throws IOException {
		LOGGER.info("Parsing .BED file: " + bedToParse);
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(bedToParse));
    		boolean hasNext = true;
    		while (hasNext) {
    			hasNext = parseIntoFiles(reader);
    		}
			
	    	IOUtils.closeQuietly(reader);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		LOGGER.info("Finished parsing .BED file: " + bedToParse + "\n");
    }
	
	/**
	 * Parse .BED file with MEI ref data into files each containing data about one subtype
	 * @param reader
	 * @return true - if there is next line in a file
	 * @throws IOException
	 */
	private static boolean parseIntoFiles(BufferedReader reader) throws IOException {
		// Read line
		String line = reader.readLine();
 
		// Check if line is present and has been read
		if (line == null) {
			return false;
		}
		
		// Parse line
		List<String> data = PatternSplitter.toList(PatternSplitter.PTRN_TAB_SPLITTER, line);

		// Get subtype of the Alu
		String aluSubtype = "";
		// Alu data is always in the fourth column
		if (!data.get(3).equals(".")) {
			// Parse out the fourth element to get AluType from it
    		List<String> nameData = PatternSplitter.toList(PatternSplitter.PTRN_COLON_SPLITTER, data.get(3));
    		// Assign Alu subtype, which should be the third element
    		aluSubtype = nameData.get(2);
		} 
	
		try {
			// This is the directory we will save our parsed files to
			String directory = System.getProperty("user.dir") + "/src/com/yg/input/ref/ParsedAlu/";
			IOGeneralHelper.createOutDir("/src/com/yg/input/ref/ParsedAlu/");
			// File with the current Alu subtype
			String fileName = "hg19_" + aluSubtype + ".BED";
			//Check if file exists
			if (new File(directory, fileName).exists()) {
				// If exists - append to it
			    Files.write(Paths.get(directory + fileName), (line + "\n").getBytes(), StandardOpenOption.APPEND);
			} else {
				// If doesn't exist - create and write to it
			    Files.write(Paths.get(directory + fileName), (line + "\n").getBytes(), StandardOpenOption.CREATE);
			}
		} catch (IOException e) {
    		throw new IOException("Error when trying to write to a file with specific type of Alu: " + aluSubtype);
    	}

		return true;
	}
}
