package com.yg.assembler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.yg.exceptions.InputParametersException;
import com.yg.io_handlers.IOParameters;
import com.yg.utilities.IOGeneralHelper;
import com.yg.utilities.ProcessStream;

/**
 * This class does pairwise alignment between contigs (or contig and flanking) using bl2seq tool
 * 
 * @author Yaroslava Girilishena
 *
 */

public class Bl2seqAlignment {
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // init logger
	
	/**
	 *  Compare pairwise two contigs for alignment 
	 * @param contig1 - path to cintig 1
	 * @param contig2 - path to contig 2
	 * @param outDir - output directory
	 * @return a file with output data
	 * @throws IOException
	 * @throws InputParametersException
	 * @throws InterruptedException
	 */
	public static String runBL2SEQ(String contig1, String contig2, String outDir) throws IOException, InputParametersException, InterruptedException {
		// Set output file
		String bl2seqOutFile = System.getProperty("user.dir") + outDir + "/" + contig1.substring(contig1.lastIndexOf("/") + 1, contig1.lastIndexOf(".")) + "_" + 
							   contig2.substring(contig2.lastIndexOf("/") + 1, contig2.lastIndexOf(".")) + ".fa";
				
		// Build command for running bl2seq
		List<String> blastnCommands = new ArrayList<String>();
		blastnCommands.add(IOParameters.BL2SEQ_EXEC_PATH + "/bl2seq");
		blastnCommands.add("-i");
		blastnCommands.add(contig1);
		blastnCommands.add("-j");
		blastnCommands.add(contig2);
		blastnCommands.add("-p");
		blastnCommands.add("blastn");
		blastnCommands.add("-e");
		blastnCommands.add("0.1");
		blastnCommands.add("-W");
		blastnCommands.add("4"); // 9
		blastnCommands.add("-D");
		blastnCommands.add("1");
		blastnCommands.add("-q");
		blastnCommands.add("-2");
		blastnCommands.add("-F");
		blastnCommands.add("F");
		blastnCommands.add("-o");
		blastnCommands.add(bl2seqOutFile);

		// Run the tool
		ProcessBuilder blastnPB = new ProcessBuilder(blastnCommands);
        Process blastnProcess = blastnPB.start();
        
        // Collect error messages
        ProcessStream errStream = new ProcessStream(blastnProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        // Catch error
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	throw new InputParametersException("BL2SEQ ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        ProcessStream outputStream = new ProcessStream(blastnProcess.getInputStream(), "OUTPUT");
        outputStream.start();

        blastnProcess.waitFor();
        outputStream.cleanBuffer(); // clean buffer
		
		return bl2seqOutFile; // return a file with output data
	}

	/**
	 * Run bl2seq to align contig and reference sequence to find TSD/IMD, etc.
	 * @param contig
	 * @param ref
	 * @param chromosome
	 * @param position
	 * @return a file with output data
	 * @throws IOException
	 * @throws InputParametersException
	 * @throws InterruptedException
	 */
	public static String runBL2SEQforTSD(String contig, String ref, String chromosome, long position) throws IOException, InputParametersException, InterruptedException {
		// Create output directory
		String bl2seqOutFile = "/intermediate_output/tsd_alignment/" + IOParameters.ME_TYPE;
		IOGeneralHelper.createOutDir(bl2seqOutFile);
		
		// Setup output file
		bl2seqOutFile += "/" + IOParameters.ME_TYPE +"." + chromosome + "_" + position + ".fa";
		
		// Build command for running bl2seq
		List<String> blastnCommands = new ArrayList<String>();
		blastnCommands.add(IOParameters.BL2SEQ_EXEC_PATH + "/bl2seq");
		blastnCommands.add("-i");
		blastnCommands.add(contig);
		blastnCommands.add("-j");
		blastnCommands.add(ref);
		blastnCommands.add("-p");
		blastnCommands.add("blastn");
		blastnCommands.add("-e");
		blastnCommands.add("1e-8");
		blastnCommands.add("-D");
		blastnCommands.add("1");
		blastnCommands.add("-q");
		blastnCommands.add("-4");
		blastnCommands.add("-F");
		blastnCommands.add("F");
		blastnCommands.add("-o");
		blastnCommands.add(System.getProperty("user.dir") + bl2seqOutFile);

		// Run the tool
		ProcessBuilder blastnPB = new ProcessBuilder(blastnCommands);
        Process blastnProcess = blastnPB.start();
        
        // Collect error messages
        ProcessStream errStream = new ProcessStream(blastnProcess.getErrorStream(), "ERROR");            
        errStream.start();
        
        // Catch error
        if (errStream.getOutput() != null && !errStream.getOutput().equals("") && errStream.getOutput().length() != 0) {
        	throw new InputParametersException("BL2SEQ for TSD ERROR:\n" + errStream.getOutput());
        } else {
        	errStream.cleanBuffer();
        }
        
        // Collect output
        ProcessStream outputStream = new ProcessStream(blastnProcess.getInputStream(), "OUTPUT");
        outputStream.start();

        blastnProcess.waitFor();
        outputStream.cleanBuffer(); // clean buffer
		
		return System.getProperty("user.dir") + bl2seqOutFile;
	}
}

/*
 * 
 * bl2seq 2.2.26   arguments:

  -i  First sequence [File In]
  -j  Second sequence [File In]
  -p  Program name: blastp, blastn, blastx, tblastn, tblastx. For blastx 1st sequence should be nucleotide, tblastn 2nd sequence nucleotide [String]
  -g  Gapped [T/F]
    default = T
  -o  alignment output file [File Out]
    default = stdout
  -d  theor. db size (zero is real size) [Real]
    default = 0
  -a  Text ASN.1 output file [File Out]  Optional
  -G  Cost to open a gap (-1 invokes default behavior) [Integer]
    default = -1
  -E  Cost to extend a gap (-1 invokes default behavior) [Integer]
    default = -1
  -X  X dropoff value for gapped alignment (in bits) (zero invokes default behavior)
      blastn 30, megablast 20, tblastx 0, all others 15 [Integer]
    default = 0
  -W  Word size, default if zero (blastn 11, megablast 28, all others 3) [Integer]
    default = 0
  -M  Matrix [String]
    default = BLOSUM62
  -q  Penalty for a nucleotide mismatch (blastn only) [Integer]
    default = -3
  -r  Reward for a nucleotide match (blastn only) [Integer]
    default = 1
  -F  Filter query sequence (DUST with blastn, SEG with others) [String]
    default = T
  -e  Expectation value (E) [Real]
    default = 10.0
  -S  Query strands to search against database (blastn only).  3 is both, 1 is top, 2 is bottom [Integer]
    default = 3
  -T  Produce HTML output [T/F]
    default = F
  -m  Use Mega Blast for search [T/F]  Optional
    default = F
  -Y  Effective length of the search space (use zero for the real size) [Real]
    default = 0
  -t  Length of the largest intron allowed in tblastn for linking HSPs [Integer]
    default = 0
  -I  Location on first sequence [String]  Optional
  -J  Location on second sequence [String]  Optional
  -D  Output format: 0 - traditional, 1 - tabular [Integer]
    default = 0
  -U  Use lower case filtering for the query sequence [T/F]  Optional
    default = F
  -A  Input sequences in the form of accession.version [T/F]
    default = F
  -V  Force use of the legacy BLAST engine [T/F]  Optional
    default = F
*/