package com.yg.io_handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yg.exceptions.FileException;
import com.yg.exceptions.InputParametersException;

/**
 * This class parses input parameters;
 * Creates folders for output;
 * Collects all files from specified paths
 * 
 * @author Yaroslava Girilishena
 *
 */
public class UserInputHandler {
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // init logger

	/**
	 * Parse CLI parameters and set them to global values
	 * @param args - array of parameters
	 * @return
	 * @throws InputParametersException 
	 */
	public static boolean parseCLParameters(String[] args) throws InputParametersException {
		// If no parameters specified, return help message
		if (args == null || args.length == 0) {
			printHelp();
			return false;
		}
		
		// HashMap of parameters
		Map<String, String> clParameters = new HashMap<String, String>();
		
		// Asked for help message
		if (args[0].equals("-help") || args[0].equals("-h") || args[0].equals("--h") || args[0].equals("--help")) {
			printHelp();
			return false;
		}
		
		// Config file with parameters is given
		if (args[0].equals("-config") || args[0].equals("-conf")) {
			 
			try {
				// Parse config file with parameters
				clParameters = getConfigParams(args[1]);
				
				// Parse data and setup global parameters
				setupGlobalParameters(clParameters);
			} catch (InputParametersException | IOException e) {
				throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
			}
			
			return true;
		}
		
		if (args.length % 2 != 0) {
			// Not enough values for parameters specified
			throw new InputParametersException("PARAMETERS ERROR: Not enough parameters");
		}
		
		// Parse parameters values
		for (int i=0; i < args.length - 1; i=i+2) {
			clParameters.put(args[i], args[i+1]);
			LOGGER.info("CLI parameter: " + args[i] + "=" + clParameters.get(args[i]));
		}
		
		// If it's NOT a development mode
		if (!clParameters.containsKey("-dev")) {
			
			// Check for required parameters
			if (!(clParameters.containsKey("-c") && clParameters.containsKey("-p")) ||
				!clParameters.containsKey("-i") ||
				!clParameters.containsKey("-BAMpath") || !clParameters.containsKey("-SAMTOOLSpath") ||
				!clParameters.containsKey("-BLASTpath") || !clParameters.containsKey("-BLASTdb") || !clParameters.containsKey("-BL2SEQpath") ||
				!clParameters.containsKey("-CDHITpath") || !clParameters.containsKey("-CAP3path") ||
				!clParameters.containsKey("-ME") || !IOParameters.SUPPORTED_TYPES.contains(clParameters.get("-ME"))) {
				
				throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
			}
			
			// Parse data and setup global parameters
			try {
				setupGlobalParameters(clParameters);
			} catch (InputParametersException e) {
				throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
			}
			
			// Log all parameters
			LOGGER.info("User's parameters: " + clParameters.toString());
		
		} else {
			// Setup list of .bam files with raw reads
			if (IOParameters.COLLECT_READS) {
				IOParameters.getListOfBamFiles();
			}
						
			// If only one location is specified
			if (clParameters.containsKey("-c") && clParameters.containsKey("-p")) {
				int position = Integer.parseInt(clParameters.get("-p"));
				
				// Validate parameters' values
				if (position < 0 || position > Math.pow(2, 31)-1) {
					throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
				}
				
				Matcher matcher = Pattern.compile("^chr[0-9XY]+$").matcher(clParameters.get("-c"));
            	if (!matcher.find()) {
            		throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
            	}
            			
				IOParameters.INPUT_FILE = false;
				IOParameters.DEF_CHROMOSOME = clParameters.get("-c");
				IOParameters.DEF_POSITION = Integer.parseInt(clParameters.get("-p"));
			} else {
				IOParameters.INPUT_FILE = true;
			}
			
			// If start and end positions are specified
			if (clParameters.containsKey("-startLoci") && clParameters.containsKey("-endLoci")) {
				IOParameters.SE_SPECIFIED = true;
				int startLoci = Integer.parseInt(clParameters.get("-startLoci"));
				int endLoci = Integer.parseInt(clParameters.get("-endLoci"));
				
				if (startLoci < 0 || endLoci < 0) {
					startLoci = 0;
					endLoci = 1;
				}
				
				if (startLoci > endLoci) {
					throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements startLoci > endLoci");
				}
				
				IOParameters.START_LOCI = startLoci;
				IOParameters.END_LOCI = endLoci;
			} else {
				IOParameters.SE_SPECIFIED = false;
			}
		}
		
		return true;
	}
	
	/**
	 * Print Help message with all acceptable parameters with description
	 */
	public static void printHelp() {
		String help = "-ME: type of MEI, required. Default = Alu\n" +
				"-c: chromosome name, required if input file is not specified\n" +
				"-p: position in chromosome, required if input file is not specified\n" +
				"-i: .vcf or .bed input file, required if chromosome and position are not specified\n" + 
				"-startLoci: start position in the list of locations, optional. Default = 0 \n" + 
				"-endLoci: end position in the list of locations, optional. Default = size of input file with locations\n" +
				"-min_ins_length: the minimum length of the insertion alignment to the consensus database, optional. Default = 100\n" +
				"-BAMpath: path to BAM files, required\n" + 
				"-BAMfile: BAM file name, if not specified, all BAM files in BAMpath will be used\n" +
				"-SAMTOOLSpath: path to samtools, required\n" +
				"-BLASTpath: path to blastn executable, required\n" + 
				"-BLASTdb: path and name of blast DB, required\n" +
				"-BL2SEQpath: path to bl2seq executable, required\n" + 
				"-CDHITpath: path to cd-hit executable, required\n" +
				"-CAP3path: path to cap3 executable, required\n" +
				"-config: path to the configuration faile with all parameters specified there. optional\n" +
				// genome version
				// -dev 1 
				
				"-help: print this message.\n";
		
		System.out.println("HELP:\n" + help);
	}
	

	/**
	 * Parse and setup global parameters
	 * @param clParameters
	 * @throws InputParametersException
	 */
	public static void setupGlobalParameters(Map<String, String> clParameters) throws InputParametersException {
		// MEI type to work with
		IOParameters.ME_TYPE = clParameters.get("-ME");
		
		// Input file with events
		if (clParameters.containsKey("-i") && !clParameters.get("-i").equals("")) {
			IOParameters.INPUT_FILE_WITH_LOCATIONS = clParameters.get("-i");
			IOParameters.INPUT_FILE = true;
		} else if (clParameters.containsKey("-c") && !clParameters.get("-c").equals("") && (clParameters.containsKey("-p") && !clParameters.get("-p").equals(""))) {
			// Given a single loci
			IOParameters.DEF_CHROMOSOME = clParameters.get("-c");
			IOParameters.DEF_POSITION = Integer.parseInt(clParameters.get("-p"));
			IOParameters.INPUT_FILE = false;
		} else {
			throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements"); 
		}
		
		if (clParameters.containsKey("-startLoci") && clParameters.containsKey("-endLoci")) {
			IOParameters.SE_SPECIFIED = true;
			IOParameters.START_LOCI = Integer.parseInt(clParameters.get("-startLoci"));
			IOParameters.END_LOCI = Integer.parseInt(clParameters.get("-endLoci"));
		}
		
		// Set minimum insertion length
		if (clParameters.containsKey("-min_ins_length")) {
			if (Integer.parseInt(clParameters.get("-min_ins_length")) >= 50 && Integer.parseInt(clParameters.get("-min_ins_length")) <= 200) {
				IOParameters.MIN_MEI_LENGTH.put(IOParameters.ME_TYPE, Integer.parseInt(clParameters.get("-min_ins_length")));
			} else {
				throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
			}
		}
		
		// Directory or a single BAM file
		if (clParameters.containsKey("-BAMfile")) {
			IOParameters.LIST_OF_BAM_FILES.add(clParameters.get("-BAMfile"));
		} else {
			IOParameters.BAM_INPUT_PATH = clParameters.get("-BAMpath");
			// Setup list of .bam files with raw reads
			IOParameters.getListOfBamFiles();
		}
		
		// Refence genome directory
		IOParameters.REF_SEQ_DIR = clParameters.get("-ref_dir");
		
		// Consensus DB path
		IOParameters.CONSENSUS_DB = System.getProperty("user.dir") + "/src/com/yg/input/consensus/" + IOParameters.ME_TYPE + ".fa"; ;
		
		// Tools exec paths
		if (clParameters.containsKey("-SAMTOOLSpath")) {
			IOParameters.SAMTOOLS_PATH = clParameters.get("-SAMTOOLSpath");
		}
		IOParameters.BLAST_EXEC_PATH = clParameters.get("-BLASTpath");
		IOParameters.BL2SEQ_EXEC_PATH = clParameters.get("-BL2SEQpath");
		IOParameters.CDHIT_TOOL_PATH = clParameters.get("-CDHITpath");
		IOParameters.CAP3_TOOL_PATH = clParameters.get("-CAP3path");
		
		// Number of threads
		if (clParameters.containsKey("-threads")) {
			IOParameters.THREADS = Integer.parseInt(clParameters.get("-threads"));
		}		
		
		// Check output format specification
		if (clParameters.containsKey("-fasta")) {
			IOParameters.OUTPUT_FORMAT = ".fa";
		} else if (clParameters.containsKey("-fastq")) {
			IOParameters.OUTPUT_FORMAT = ".fq";
		} else {
			IOParameters.OUTPUT_FORMAT = ".fa";
		}
	}
	
	
	/**
	 * Parse the config file with input parameters 
	 * and return Map of key-values
	 * @param propertiesFile - configuration file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> getConfigParams(String propertiesFile) throws IOException {	
		InputStream inputStream = null;
		Map<String, String> parameters = new HashMap<String, String>();
				
		try {
			Properties prop = new Properties();
			File inputFile = new File(propertiesFile);
			if (!inputFile.exists() || inputFile.isDirectory()) {
				throw new FileException("Config file with parameters " + propertiesFile + " not found!");
			}
			inputStream = new FileInputStream(inputFile);

			prop.load(inputStream);
			
			// -------------------------------
			// CHECK REQUIRED PARAMETERS
		    // -------------------------------
			
			if (prop.getProperty("-ME") == null || prop.getProperty("-ME").equals("") ||
			   prop.getProperty("-BAMpath") == null || prop.getProperty("-BAMpath").equals("") || //&& prop.getProperty("-BAMfile") == null ||  prop.getProperty("-BAMfile").equals("")) ||
			   //prop.getProperty("-SAMTOOLSpath") == null || prop.getProperty("-SAMTOOLSpath").equals("") ||
			   prop.getProperty("-BLASTpath") == null || prop.getProperty("-BLASTpath").equals("") ||
			   //prop.getProperty("-BLASTdb") == null || prop.getProperty("-BLASTdb").equals("") ||
			   prop.getProperty("-BL2SEQpath") == null || prop.getProperty("-BL2SEQpath").equals("") ||
			   prop.getProperty("-CDHITpath") == null || prop.getProperty("-CDHITpath").equals("") ||
			   prop.getProperty("-CAP3path") == null || prop.getProperty("-CAP3path").equals("")) {
				
				System.out.println("EXCEPTION - Config file: BAD ARGUMENTS"); 
				throw new InputParametersException("PARAMETERS ERROR: Parameters do not meet the requirements");
			}
			
			// -------------------------------
			// SAVE PARAMETERS
		    // -------------------------------
			
			if (prop.getProperty("-ME") != null && !prop.getProperty("-ME").equals("")) {				
				parameters.put("-ME", prop.getProperty("-ME"));
			}
			
			if (prop.getProperty("-i") != null && !prop.getProperty("-i").equals("")) {
				parameters.put("-i", prop.getProperty("-i"));
			}
			if (prop.getProperty("-c") != null && !prop.getProperty("-c").equals("")) {
				parameters.put("-c", prop.getProperty("-c"));
			}
			
			if (prop.getProperty("-p") != null && !prop.getProperty("-p").equals("")) {
				parameters.put("-p", prop.getProperty("-p"));
			}
			
			if (prop.getProperty("-min_ins_length") != null && !prop.getProperty("-min_ins_length").equals("")) {
				parameters.put("-min_ins_length", prop.getProperty("-min_ins_length"));
			}
			
			if (prop.getProperty("-BAMpath") != null && !prop.getProperty("-BAMpath").equals("")) {
				parameters.put("-BAMpath", prop.getProperty("-BAMpath"));
			}
			
			if (prop.getProperty("-BAMfile") != null && !prop.getProperty("-BAMfile").equals("")) {
				parameters.put("-BAMfile", prop.getProperty("-BAMfile"));
			}
			
			if (prop.getProperty("-ref_dir") != null && !prop.getProperty("-ref_dir").equals("")) {
				parameters.put("-ref_dir", prop.getProperty("-ref_dir"));
			}
			
			// for dev mode - path is empty
			if (prop.getProperty("-SAMTOOLSpath") != null && !prop.getProperty("-SAMTOOLSpath").equals("")) {
				parameters.put("-SAMTOOLSpath", prop.getProperty("-SAMTOOLSpath"));
			}
			
			if (prop.getProperty("-BLASTpath") != null && !prop.getProperty("-BLASTpath").equals("")) {
				parameters.put("-BLASTpath", prop.getProperty("-BLASTpath"));
			}
			
			// added into the project
//			if (prop.getProperty("-BLASTdb") != null && !prop.getProperty("-BLASTdb").equals("")) {
//				parameters.put("-BLASTdb", prop.getProperty("-BLASTdb"));
//			}
			
			if (prop.getProperty("-BL2SEQpath") != null && !prop.getProperty("-BL2SEQpath").equals("")) {
				parameters.put("-BL2SEQpath", prop.getProperty("-BL2SEQpath"));
			}
			
			if (prop.getProperty("-CDHITpath") != null && !prop.getProperty("-CDHITpath").equals("")) {
				parameters.put("-CDHITpath", prop.getProperty("-CDHITpath"));
			}
			
			if (prop.getProperty("-CAP3path") != null && !prop.getProperty("-CAP3path").equals("")) {
				parameters.put("-CAP3path", prop.getProperty("-CAP3path"));
			}
			
			if (prop.getProperty("-startLoci") != null && !prop.getProperty("-startLoci").equals("")) {
				parameters.put("-startLoci", prop.getProperty("-startLoci"));
			}
			
			if (prop.getProperty("-endLoci") != null && !prop.getProperty("-endLoci").equals("")) {
				parameters.put("-endLoci", prop.getProperty("-endLoci"));
			}
			
		} catch (Exception e) {
			System.out.println("Exception while parsing config with parameters: " + e);
		} finally {
			inputStream.close();
		}
		return parameters;
	}

}
