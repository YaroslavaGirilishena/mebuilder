package com.yg.io_handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.yg.assembler.Bl2seqAlignment;
import com.yg.exceptions.InputParametersException;
import com.yg.models.ConsensusLocation;
import com.yg.models.FASTASeq;
import com.yg.models.MEInsertion;
import com.yg.parsers.FastaParser;
import com.yg.utilities.IOGeneralHelper;
import com.yg.utilities.PatternSplitter;

/**
 * This class formats and writes output data
 * Deducts TSD/IMD and transductions 
 * 
 * @author Yaroslava Girilishena
 *
 */
public class OutputData {	
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	
	// -----------------------------------------------------------------------------------
	//	FULLY AND PARTIALLY CHARACTERIZED MEIS 
	// -----------------------------------------------------------------------------------
	
	/**
	 * Write successful MEI characterization
	 * @param me - mobile element
	 * @throws IOException
	 * 
	 * Format:
	 * for insertion
	 * >dbRIP|ID#|ME|Class:Family:Subfamily|Strand|+/-|Genome|hg19/hg38|Pos|chr:start-end|Allele|ins|Insertion|ref/non-ref|TSD|length:sequence|IMD|length:sequence|5TR|length:sequence|3TR|length:sequence
	 * pre-integration
	 * >dbRIP|ID#|ME|Class:Family:Subfamily|Strand|+/-|Genome|hg19/hg38|Pos|chr:start-end|Allele|pre|Insertion|ref/non-ref|TSD|length:sequence|IMD|length:sequence|5TR|length:sequence|3TR|length:sequence
	 */
	public static void writeMEOut(MEInsertion me) throws IOException {
		LOGGER.info("Writing SUCCESSFUL sequences: " + me.getChromosome() + "_" + me.getPosition() + "\n");

		// Name of the output file
		String outfilename =  System.getProperty("user.dir") + "/results/";
		if (me.isFull()) {
			outfilename += "characterized_mei";
			// Create output directory if it doesn't exist
			IOGeneralHelper.createOutDir("/results/characterized_mei/" + IOParameters.ME_TYPE);
		} else {
			outfilename += "partial_mei";
			// Create output directory if it doesn't exist
			IOGeneralHelper.createOutDir("/results/partial_mei/" + IOParameters.ME_TYPE);
		}
		outfilename += "/" + IOParameters.ME_TYPE + "/" + IOParameters.ME_TYPE + "." + me.getChromosome() + "_" + me.getPosition() + ".fa"; 

		// Construct insertion output
		String output = ">1KP" + "||ME|SINE:" + IOParameters.ME_TYPE + ":" +  me.getConsensusAlignments().get(0).getConsensus() + // ME type
						"|Strand|" + me.getStrand() + // strand
						"|Genome|hg19|Pos|" + me.getChromosome() + ":" + me.getStartPos() + "-" + me.getEndPos() + // location
						"|Allele|ins|Insertion|non-ref|TSD|" + me.getTSD().length() + ":" + me.getTSD() + //TSD
						"|IMD|" + me.getIMD().length() + ":" + me.getIMD() + // IMD 
						"|5TR|" + me.getTransduction5().length() + ":" + me.getTransduction5() + // 5' end transduction
						"|3TR|" + me.getTransduction3().length() + ":" + me.getTransduction3(); // 3' end transduction
		
		output += 	"\n" + me.getFlankingL() + // left flanking
					"\n" + me.getTSD() + // TSD
					"\n" + me.getSequence() + // insertion
					"\n" + me.getTSD() + // TSD
					"\n" + me.getFlankingR() + "\n\n"; // right flanking
			
		// Collect pre-integration output
		output += ">1KP" + "||ME|SINE:" + IOParameters.ME_TYPE + ":" +  me.getConsensusAlignments().get(0).getConsensus() + // ME type
				  "|Strand|" + me.getStrand() + // strand  
				  "|Genome|hg19|Pos|" + me.getChromosome() + ":" + me.getStartPos() + "-" + me.getEndPos() + // location
				  "|Allele|pre|Insertion|non-ref|TSD|" + me.getTSD().length() + ":" + me.getTSD() + //TSD
				  "|IMD|" + me.getIMD().length() + ":" + me.getIMD() + // IMD 
				  "|5TR|" + me.getTransduction5().length() + ":" + me.getTransduction5() + // 5' end transduction
				  "|3TR|" + me.getTransduction3().length() + ":" + me.getTransduction3(); // 3' end transduction
				
		
		String preintegrationFile = System.getProperty("user.dir") + "/intermediate_output/ref_flanking/" + IOParameters.ME_TYPE + "/" + me.getChromosome() + "_" + me.getPosition() + ".fa";
		FastaParser preintegration = new FastaParser(preintegrationFile);
		FASTASeq preintegrationAllele = preintegration.parse().get(0);
		
		output += "\n" + preintegrationAllele.getSequence().substring(0, IOParameters.FLANKING_REGION) + // write pre-integration allele
				  "\n" + me.getTSD() + 
				  "\n" + preintegrationAllele.getSequence().substring(IOParameters.FLANKING_REGION) +
				  "\n//\n";	// to separate events
					
							
		try(FileWriter fw = new FileWriter(outfilename, false);
		    BufferedWriter bw = new BufferedWriter(fw);
		    PrintWriter out = new PrintWriter(bw))
		{
		    out.println(output);
		    
		    // Close streams
		    out.close();
		    bw.close();
		} catch (IOException e) {
			throw e;
		}
	}
	
	
	// -----------------------------------------------------------------------------------
	//	FAILED MEI
	// -----------------------------------------------------------------------------------
	
	/**
	 * Print all merged contigs of failed MEI into separate file
	 * @param me
	 * @throws IOException
	 */
	public static void writeFailedMEOut(MEInsertion me) throws IOException {
		LOGGER.info("Writing FAILED sequences: " + me.getChromosome() + "_" + me.getPosition() + "\n");
		
		// Create output directory if it doesn't exist
		IOGeneralHelper.createOutDir("/results/failed_mei/" + IOParameters.ME_TYPE);
		// Name of the output file
		String outfilename =  System.getProperty("user.dir") + "/results/failed_mei/" + IOParameters.ME_TYPE + "/" + IOParameters.ME_TYPE + "." + me.getChromosome() + "_" + me.getPosition() +".failed.fa"; 
		
		String output = "";
		// Construct output
		for (int i=0; i < me.getContigs().size(); i++) {
			output += ">1KP|ME|SINE:" + IOParameters.ME_TYPE + ":"; 
			if (!me.getConsensusAlignments().isEmpty()) {
				boolean hasConsAlignment = false;
				for (ConsensusLocation cl : me.getConsensusAlignments()) {
					if (cl.getContigId().equals(me.getContigs().get(i).getDescription())) {
						output += me.getConsensusAlignments().get(0).getConsensus() + // ME type
								"|" + me.getConsensusAlignments().get(0).getStart() + "-" + me.getConsensusAlignments().get(0).getEnd() + "|Strand|" + me.getConsensusAlignments().get(0).getStrand(); // location and strand;
						hasConsAlignment = true;
						break;
					}
				}
				if (!hasConsAlignment) {
					output += "Undef|Strand|Undef";
				}
			} else {
				output += "Undef|Strand|Undef";
			}
			
			output += "|Genome|hg19|" + me.getChromosome() + ":" + me.getStartPos() + "-" + me.getEndPos() + "|Allele|ins|" + me.getContigs().get(i).getDescription() + 
					  "\n" + me.getContigs().get(i).getSequence() + "\n";
		}
		if (!output.equals("")) {
			output += "//\n"; // to separate events
		} else {
			// No contigs found
			output += ">" + me.getChromosome() + ":" + me.getStartPos() + "-" + me.getEndPos() +
					" no merged contigs found\n//\n";
		}
		
		try(FileWriter fw = new FileWriter(outfilename, false);
		    BufferedWriter bw = new BufferedWriter(fw);
		    PrintWriter out = new PrintWriter(bw))
		{
		    out.println(output);
		    
		    // Close streams
		    out.close();
		    bw.close();
		} catch (IOException e) {
			throw e;
		}
	}
	
	
	// -----------------------------------------------------------------------------------
	//	TSD / IMD / TRANSDUCTIONS
	// -----------------------------------------------------------------------------------
	
	/**
	 * 
	 * @param me
	 * @return
	 * @throws InterruptedException 
	 * @throws InputParametersException 
	 * @throws IOException 
	 */
	public static void extractTSD(String contigfile, MEInsertion me) throws IOException, InputParametersException, InterruptedException {
		LOGGER.info("Extracting TSD for " + me.getChromosome() + "_" + me.getPosition());
		
		// Get reference sequence covering given location
		String chrRefSeqFile = System.getProperty("user.dir") + "/intermediate_output/ref_flanking/" + IOParameters.ME_TYPE + "/" + me.getChromosome() + "_" + me.getPosition() + ".fa";
		//String chrRefFile = IOParameters.REF_SEQ_DIR + chromosome + ".fa"; // full chromosome 
		
		// Run bl2seq to find the TSD
		String bl2seqOutput = Bl2seqAlignment.runBL2SEQforTSD(contigfile, chrRefSeqFile, me.getChromosome(), me.getPosition()); //"/Users/YG/Dropbox (Особиста)/SVsCodeSharcnet/SVsCharacterization/tsd_alignment_merged.fa";
		
		BufferedReader reader = new BufferedReader(new FileReader(bl2seqOutput));
		
		String line = reader.readLine();
		if (line == null) {
			reader.close();
			return; // No more data to read
		}
		
		// Skip header 
		while ((line = reader.readLine()) != null && line.startsWith("#")) {
			continue;
		}
		
		List<List<String>> alignments = new ArrayList<List<String>>();
		// Get the first alignment
		List<String> data = PatternSplitter.toList(PatternSplitter.PTRN_TAB_SPLITTER, line.trim());
		// Get the second alignment 
		line = reader.readLine();
		if (line == null) {
			reader.close();
			return;
		}
		List<String> dataOther = PatternSplitter.toList(PatternSplitter.PTRN_TAB_SPLITTER, line.trim());
		if (data == null || dataOther == null) {
			reader.close();
			return;
		}
		alignments.add(data);
		alignments.add(dataOther);

		reader.close();
		
		// Validate alignment data
		if (data == null || dataOther == null) {
			LOGGER.info("TSD - No good alignment found...\n");
			return;
		}
//		if (data == null ) { //|| Math.abs(Integer.parseInt(data.get(8)) - position) > 10000 || Math.abs(Integer.parseInt(data.get(9)) - position) > 10000) {
//			LOGGER.info("No good alignment found 1");
//			return null;
//		}
//		if (dataOther == null || Math.abs(Integer.parseInt(data.get(6)) - Integer.parseInt(dataOther.get(6))) < 20
//				|| Math.abs(Integer.parseInt(data.get(7)) - Integer.parseInt(dataOther.get(7))) < 20
//				|| Math.abs(Integer.parseInt(data.get(6)) - Integer.parseInt(dataOther.get(6))) > 100000
//				|| Math.abs(Integer.parseInt(data.get(7)) - Integer.parseInt(dataOther.get(7))) > 100000) {
//			
//			LOGGER.info("No good alignment found 2");
//			return null;
//		}
		
		// -----------------------------------------------------------------------------------
		// GET TSD / IMD
	    // -----------------------------------------------------------------------------------
		
		// Parse overlapping contig (subject)
		FastaParser parseRef = new FastaParser(chrRefSeqFile);
		FASTASeq subject = parseRef.parse().get(0);
		
		// Get the alignment positions
		List<Integer> positionsList = new ArrayList<Integer>();
		for (int i=0; i<alignments.size(); i++) {
			positionsList.add(Integer.parseInt(alignments.get(i).get(8)));
			positionsList.add(Integer.parseInt(alignments.get(i).get(9)));
		}
		
		// Sort the positions
		Collections.sort(positionsList, new Comparator<Integer>() {
			@Override
			public int compare(Integer pos1, Integer pos2) {
				if (pos1 > pos2) return 1;
				if (pos1 < pos2) return -1;
				else return 0;
			}
		});
					
		// -qs1-----------qs2----qe1-----------qe2-
		//					\TSD/
		if (( Integer.parseInt(data.get(8)) < Integer.parseInt(dataOther.get(8)) && Integer.parseInt(dataOther.get(8)) < Integer.parseInt(data.get(9)) )
				|| ( Integer.parseInt(dataOther.get(8)) < Integer.parseInt(data.get(8)) && Integer.parseInt(data.get(8)) < Integer.parseInt(dataOther.get(9)) )) {
			// There is TSD
			
			// Get two middle positions as the start and the end of TSD
			int tsdStart = positionsList.get(1);
			int tsdEnd = positionsList.get(2);

			// Get the TSD sequence
			String tsd = subject.getSequence().substring(tsdStart - 1, tsdEnd);
			LOGGER.info("TSD :" + tsdStart + "-" + tsdEnd + ": " + tsd + "\n");
			// Set TSD to ME
			me.setTSD(tsd);
			
		} else {
			// There is IMD
			
			// Get two middle positions as the start and the end of TSD
			int imdStart = positionsList.get(1);
			int imdEnd = positionsList.get(2);

			// Get the IMD sequence
			String imd = subject.getSequence().substring(imdStart, imdEnd);
			LOGGER.info("IMD :" + imdStart + "-" + imdEnd + ": " + imd + "\n");

			// Set IMD to ME
			me.setIMD(imd);
		}
		
		// -----------------------------------------------------------------------------------
		// GET 5' AND 3' TRANSDUCTIONS
	    // -----------------------------------------------------------------------------------
		
		List<String> leftFlankAlignment = null, rightFlankAlignment = null;	
		if (Integer.parseInt(alignments.get(0).get(6)) < Integer.parseInt(alignments.get(1).get(6))) {
			leftFlankAlignment = alignments.get(0);
			rightFlankAlignment = alignments.get(1);
		} else {
			leftFlankAlignment = alignments.get(1);
			rightFlankAlignment = alignments.get(0);
		}

		String flankSeq = "";
		// left flanking
		if (//Integer.parseInt(leftFlankAlignment.get(7)) - 1 > IOParameters.FLANKING_REGION &&
				Integer.parseInt(leftFlankAlignment.get(7)) - 1 < me.getFlankingL().length()) { // if there is an extra sequence
			
			flankSeq = me.getFlankingL();
			
			me.setFlankingL(flankSeq.substring(0, Integer.parseInt(leftFlankAlignment.get(7))));
			if (me.getStrand() == '+') {
				me.setTransduction5(flankSeq.substring(Integer.parseInt(leftFlankAlignment.get(7))));
				LOGGER.info("5' TR: " + me.getTransduction5() + " " + me.getTransduction5().length() + " bases");
			} else {
				me.setTransduction3(flankSeq.substring(Integer.parseInt(leftFlankAlignment.get(7))));
				LOGGER.info("3' TR: " + me.getTransduction3() + " " + me.getTransduction3().length() + " bases");
			}
		}
			
		// right flanking
		int lengthOfRefAlignment = Integer.parseInt(rightFlankAlignment.get(3)); // length of alignment to the reference
		if (lengthOfRefAlignment > IOParameters.FLANKING_REGION &&
				me.getFlankingR().length() > lengthOfRefAlignment) { // if there is an extra sequence
			
			flankSeq = me.getFlankingR();
			
			if (flankSeq.length() > IOParameters.FLANKING_REGION) {
				me.setFlankingR(flankSeq.substring(flankSeq.length() - lengthOfRefAlignment));
				if (me.getStrand() == '-') {
					me.setTransduction5(flankSeq.substring(0, flankSeq.length() - lengthOfRefAlignment));
					LOGGER.info("5' TR: " + me.getTransduction5() + " " + me.getTransduction5().length() + " bases");
				} else {
					me.setTransduction3(flankSeq.substring(0, flankSeq.length() - lengthOfRefAlignment));
					LOGGER.info("3' TR: " + me.getTransduction3() + " " + me.getTransduction3().length() + " bases");
				}
			}
		}
	}
	
	/**
	 * NOT NEEDED 
	 * Write the preintegration sequence
	 * @param me
	 * @throws IOException
	 */
	public static void writePreintegrationSeq(MEInsertion me) throws IOException {
		LOGGER.info("Writing PRE-INTEGRATION sequence: " + me.getChromosome() + "_" + me.getPosition() + "\n");

		// Create output directory if it doesn't exist
		IOGeneralHelper.createOutDir("/results/preintegration_seq/");
		// Name of the output file
		String outfilename =  System.getProperty("user.dir") + "/results/preintegration_seq/" + IOParameters.ME_TYPE + ".pre.fa"; 
		
		String output = ">" + me.getChromosome() + ":" + me.getStartPos() + "-" + me.getEndPos() + 
				"|pre\n" + me.getFlankingL() + "\n" + me.getTSD() + "\n" + me.getFlankingR() + "\n";
		
		try(FileWriter fw = new FileWriter(outfilename, true);
		    BufferedWriter bw = new BufferedWriter(fw);
		    PrintWriter out = new PrintWriter(bw))
		{
		    out.println(output);
		    
		    // Close streams
		    out.close();
		    bw.close();
		} catch (IOException e) {
			throw e;
		}
		
	}
}
