package com.yg.assembler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.yg.exceptions.InputParametersException;
import com.yg.io_handlers.IOParameters;
import com.yg.utilities.ProcessStream;

/**
 * This class performs assembly using velvet tool
 * 
 * @author Yaroslava Girilishena
 *
 */
public class VelvetAssembler {
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // init logger
	
	public String fileWithReads;
	public String outputDirectory;
	
	/**
	 * Constructor
	 * @param reads
	 * @param out
	 */
	public VelvetAssembler(String reads, String out){
		this.fileWithReads = reads;
		this.outputDirectory = out;
	}
	
	/**
	 * Perform local assembly using Velvet tool
	 * @param chromosome
	 * @param position
	 * @param estimatedCoverage
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InputParametersException
	 */
	public static String doLocalAssembly(String chromosome, long position, Integer estimatedCoverage) throws IOException, InterruptedException, InputParametersException {
		String fileWithReads = System.getProperty("user.dir") + "/disc_reads/" + IOParameters.ME_TYPE + "/" + IOParameters.ME_TYPE + "." + chromosome + "_" + position + IOParameters.OUTPUT_FORMAT;

		String outputDirectory = System.getProperty("user.dir") + "/intermediate_output/velvet_assembly/" + IOParameters.ME_TYPE + "/" + IOParameters.ME_TYPE + "." + chromosome + "_" + position;
		
		// Check if input file exist
		File input = new File(fileWithReads);
		if (!input.exists() || input.isDirectory()) {
			return null;
		}
		// Create output directory if it doesn't exist
		Path path = Paths.get(outputDirectory);
        if (!Files.exists(path)) {
           Files.createDirectories(path);
        }
		
        // ---------------------------------------------------------------
 		// VELVETH
 		// ---------------------------------------------------------------
        
		// Commands for running velveth
		List<String> velvethCommands = new ArrayList<String>();
		velvethCommands.add(IOParameters.VELVET_TOOL_PATH + "/velveth");
		velvethCommands.add(outputDirectory);
		velvethCommands.add(IOParameters.HASH_LENGTH.toString());
		velvethCommands.add("-fasta");
		velvethCommands.add("-shortPaired");
		//velvethCommands.add("-short");
		velvethCommands.add(fileWithReads);
		
		// First establishes hash-tables consisting of all possible Kmeric sub-sequences found in the sequencing read dataset
		// This is were read type and Kmer length(s) are defined.
	    ProcessBuilder velvetPB = new ProcessBuilder(velvethCommands);
        Process velvetProcess = velvetPB.start();
        
        // Collect error messages
        ProcessStream errStream = new ProcessStream(velvetProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	System.out.println("VELVETH ERROR:\n" + errStream.getOutput());
        	throw new InputParametersException("VELVETH ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        ProcessStream outputStream = new ProcessStream(velvetProcess.getInputStream(), "OUTPUT");
        outputStream.start();
        
        velvetProcess.waitFor();
        
        outputStream.cleanBuffer();
        
        // ---------------------------------------------------------------
 		// VELVETG
 		// ---------------------------------------------------------------
        
        // Commands for running velvetg
 		List<String> velvetgCommands = new ArrayList<String>();
 		velvetgCommands.add(IOParameters.VELVET_TOOL_PATH + "/velvetg");
 		velvetgCommands.add(outputDirectory); 
 		velvetgCommands.add("-min_contig_lgth"); // output contigs be longer than a certain length
 		velvetgCommands.add(IOParameters.MIN_CONTIG_LENGTH_TO_KEEP.toString()); //200
 		velvetgCommands.add("-ins_length");
 		velvetgCommands.add(IOParameters.INS_LENGTH.toString()); //300
 		velvetgCommands.add("-exp_cov"); // set expected coverage to auto
 		if (estimatedCoverage < 10) {
 	 		velvetgCommands.add("auto");
 		} else {
 	 		velvetgCommands.add(estimatedCoverage.toString());
 		}
 		velvetgCommands.add("-scaffolding"); // turn off scaffolding 
 		velvetgCommands.add("no");
 		
 		velvetgCommands.add("-read_trkg"); // produce more detailed description of the assembly
 		velvetgCommands.add("yes");
 		
		// Then builds de Bruijn graphs based on velveth step, removes errors and solves repeats to eventually yields contig sequences. 
        velvetPB = new ProcessBuilder(velvetgCommands);
        velvetProcess = velvetPB.start();
        
        // Collect error messages
        errStream = new ProcessStream(velvetProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	throw new InputParametersException("VELVETG ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        outputStream = new ProcessStream(velvetProcess.getInputStream(), "OUTPUT");
        outputStream.start();
        
        velvetProcess.waitFor();
        
        LOGGER.info("Velevetg output for " + fileWithReads + "\n" + outputStream.getOutput());
        outputStream.cleanBuffer();

        // Return path to created file with contigs
		return outputDirectory + "/contigs.fa";
	}
	
	/**
	 * Assemble concordant reads and contigs
	 * @param varType
	 * @param chromosome
	 * @param position
	 * @return
	 * @throws IOException
	 * @throws InputParametersException
	 * @throws InterruptedException
	 */
	public static String assembleContigsAndReads(String varType, String chromosome, long position) throws IOException, InputParametersException, InterruptedException {
		String fileWithConcReads = System.getProperty("user.dir") + "/conc_reads/" + varType + "/" + varType + "." + chromosome + "_" + position + IOParameters.OUTPUT_FORMAT;
		String fileWithContigs = System.getProperty("user.dir") + "/contigs/" + varType + "/" + varType + "." + chromosome + "_" + position + "/contigs.fa";

		String outputDirectory = System.getProperty("user.dir") + "/seq_assembly/" + varType + "/" + varType + "." + chromosome + "_" + position;
		
		// Check if input file exist
		File input = new File(fileWithConcReads);
		if (!input.exists() || input.isDirectory()) {
			return null;
		}
		input = new File(fileWithContigs);
		if (!input.exists() || input.isDirectory()) {
			return null;
		}
		
		// Create output directory if it doesn't exist
		Path path = Paths.get(outputDirectory);
        if (!Files.exists(path)) {
           Files.createDirectories(path);
        }
		
        // ---------------------------------------------------------------
 		// VELVETH
 		// ---------------------------------------------------------------
        
		// Commands for running velveth
		List<String> velvethCommands = new ArrayList<String>();
		velvethCommands.add(IOParameters.VELVET_TOOL_PATH + "/velveth");
		velvethCommands.add(outputDirectory);
		velvethCommands.add(IOParameters.HASH_LENGTH.toString());
		velvethCommands.add("-fasta");
		velvethCommands.add("-short");
		velvethCommands.add(fileWithConcReads);
		velvethCommands.add("-long");
		velvethCommands.add(fileWithContigs);
		
		// First establishes hash-tables consisting of all possible k-meric sub-sequences found in the sequencing read dataset
		// This is were read type and k-mer length(s) are defined.
	    ProcessBuilder velvetPB = new ProcessBuilder(velvethCommands);
        Process velvetProcess = velvetPB.start();
        
        // Collect error messages
        ProcessStream errStream = new ProcessStream(velvetProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	System.out.println("VELVETH ERROR:\n" + errStream.getOutput());
        	throw new InputParametersException("VELVETH ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        ProcessStream outputStream = new ProcessStream(velvetProcess.getInputStream(), "OUTPUT");
        outputStream.start();
        
        velvetProcess.waitFor();
        
        outputStream.cleanBuffer();
        
        // ---------------------------------------------------------------
 		// VELVETG
 		// ---------------------------------------------------------------
        
        // Commands for running velvetg
 		List<String> velvetgCommands = new ArrayList<String>();
 		velvetgCommands.add(IOParameters.VELVET_TOOL_PATH + "/velvetg");
 		velvetgCommands.add(outputDirectory); 
 		velvetgCommands.add("-min_contig_lgth"); // output contigs be longer than a certain length
 		velvetgCommands.add(IOParameters.MIN_CONTIG_LENGTH.toString()); //200
// 		velvetgCommands.add("-ins_length");
// 		velvetgCommands.add(IOParameters.INS_LENGTH.toString()); //300
// 		velvetgCommands.add("-exp_cov"); // set expected coverage to auto
// 		if (estimatedCoverage < 10) {
// 	 		velvetgCommands.add("auto");
// 		} else {
// 	 		velvetgCommands.add(estimatedCoverage.toString());
// 		}
 		velvetgCommands.add("-scaffolding"); // turn off scaffolding 
 		velvetgCommands.add("no");
 		
 		velvetgCommands.add("-read_trkg"); // produce more detailed description of the assembly
 		velvetgCommands.add("yes");
 		
		// Then builds de Bruijn graphs based on velveth step, removes errors and solves repeats to eventually yields contig sequences. 
        velvetPB = new ProcessBuilder(velvetgCommands);
        velvetProcess = velvetPB.start();
        
        // Collect error messages
        errStream = new ProcessStream(velvetProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	throw new InputParametersException("VELVETG ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        outputStream = new ProcessStream(velvetProcess.getInputStream(), "OUTPUT");
        outputStream.start();
        
        velvetProcess.waitFor();
        
        LOGGER.info("Velevetg output for " + fileWithConcReads + " and " + fileWithContigs + "\n" + outputStream.getOutput());
        outputStream.cleanBuffer();

        // Return path to created file with contigs
		return outputDirectory + "/contigs.fa";
	}
}
