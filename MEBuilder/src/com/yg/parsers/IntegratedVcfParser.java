package com.yg.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.yg.io_handlers.IOParameters;
import com.yg.models.GenotypeEvent;
import com.yg.models.Variants;
import com.yg.models.VcfMetadata;
import com.yg.utilities.PatternSplitter;

/**
 * 
 * @author Yaroslava Girilishena
 *
 */
public class IntegratedVcfParser {
	
	private int lineNumber;				// number of current line being read
	private boolean alreadyFinished;	
	
	private BufferedReader reader;
	public VcfMetadata vcfMetadata; 
	
	/**
	 * 
	 */
	public IntegratedVcfParser(@Nonnull BufferedReader reader) {
		this.reader = reader;
	}
	
	/**
	 * Parses the entire Integrated VCF file
	 */
	public void parse() throws IOException {
		boolean hasNext = true;
		while (hasNext) {
			hasNext = parseNextLine();
		}
		IOUtils.closeQuietly(reader);
	}
	
	/**
	 * Parses metadata
	 */
	@SuppressWarnings("resource")
	public void parseMetadata() throws IOException {


		if (vcfMetadata != null) {
			throw new IllegalStateException("ERROR - Metadata has already been parsed");
		}
		
		BufferedReader headerReader;
		try {
			String filePath = System.getProperty("user.dir") + IOParameters.INTEGRATED_INPUT_HEADER;
			headerReader = new BufferedReader(new FileReader(filePath));
		
			vcfMetadata = new VcfMetadata();
			
			String line;
			while ((line = headerReader.readLine()) != null) {
				lineNumber++;
				if (line.startsWith("##")) {
					try {
						parseHeaderMetadata(line);
					} catch (RuntimeException e) {
						throw new IllegalArgumentException("Error parsing (## header) metadata on line #" + lineNumber + ": " + line, e);
					}
				} else if (line.startsWith("#")) {
					try {
						parseColumnInfo(line);
					} catch (RuntimeException e) {
						throw new IllegalArgumentException("Error parsing column (# header) on line #" + lineNumber + ": " + line, e);
					}
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	 
	/**
	 * Gets VCF metadata (if it has already been parsed)
	 */
	public @Nullable VcfMetadata getMetadata() {
		return this.vcfMetadata;
	}

	/**
	 * Parses a metadata line that starts with ##
	 * @param line
	 */
	private void parseHeaderMetadata(@Nonnull String line) {

		int idx = line.indexOf("=");
		String propName = line.substring(2, idx).trim();
		String propValue = line.substring(idx + 1).trim();

		switch (propName) {
		case "fileformat":
			vcfMetadata.setFileFormat(propValue);
			break;
		case "source":
			vcfMetadata.setSource(propValue);
			break;
		case "ALT":
			vcfMetadata.addAlt(propValue);
			break;
		case "INFO":
			vcfMetadata.addInfo(propValue);
			break;
		case "FORMAT":
			vcfMetadata.addFormat(propValue);
			break;

		default:
			vcfMetadata.addRawProperty(propName, propValue);
		}
	}
	
	/**
	 * Parses a metadata line that starts with #
	 * @param line
	 */
	private void parseColumnInfo(@Nonnull String line) {
		String lineWithValues = line.substring(1).trim();
		// The VCF file should be tab-delimited by standards 
		List<String> cols = toList(PatternSplitter.PTRN_TAB_SPLITTER, lineWithValues);
		vcfMetadata.setColumns(cols.subList(0, 9));
		vcfMetadata.setSamples(cols.subList(9, cols.size()));
	}
	
	/**
	 * Parse each line of Integrated VCF file
	 * @return boolean - does next line exist
	 * @throws IOException
	 */
	public boolean parseNextLine() throws IOException {
		// Check for metadata
		if (vcfMetadata == null) {
			parseMetadata();
		}
		// Read line with data
		String line = reader.readLine();
		if (line == null) {
			System.out.println("Integrated VCF Parser - Finished reading");
			alreadyFinished = true;
			return false; // No more data to read
		}
		
		if (alreadyFinished) {
			// Prevents user errors from causing infinite loops
			throw new IllegalStateException("Already finished reading the stream");
		}
		
		lineNumber++;
		
		try {
			List<String> data = toList(PatternSplitter.PTRN_TAB_SPLITTER, line);

			// CHROM
			String chromosome = "chr" + data.get(0);
			
			// POS
			long position;
			try {
				position = Long.parseLong(data.get(1));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("ERROR - Parsing Integrated VCF: position " + data.get(1) + " is not numerical");
			}
			
			// ID
			List<String> ids = null;
			if (!data.get(2).equals(".")) {
				ids = toList(PatternSplitter.PTRN_SEMICOLON_SPLITTER, data.get(2));
			} 
			
			// REF
			String ref = null;
			if (!data.get(3).equals(".")) {
				ref = data.get(3);
			}
			
			// ALT
			List<String> alt = null;
			if (!data.get(7).isEmpty() && !data.get(4).equals(".")) {
				alt = toList(PatternSplitter.PTRN_COMMA_SPLITTER, data.get(4));
			}
			
			// QUAL
			BigDecimal quality = null;
			if (!data.get(5).isEmpty() && !data.get(5).equals(".")) {
				quality = new BigDecimal(data.get(5));
			}
			
			// FILTER
			List<String> filters = null;
			if (!data.get(6).equals("PASS") && !data.get(6).equals(".")) {
				filters = toList(PatternSplitter.PTRN_SEMICOLON_SPLITTER, data.get(6));
			}
			
			// INFO
			ListMultimap<String, String> info = null;
			if (!data.get(7).equals("") && !data.get(7).equals(".")) {
				info = ArrayListMultimap.create();
				List<String> props = toList(PatternSplitter.PTRN_SEMICOLON_SPLITTER, data.get(7));
				for (String prop : props) {
					int idx = prop.indexOf('=');
					if (idx == -1) {
						info.put(prop, "");
					} else {
						String key = prop.substring(0, idx);
						String value = prop.substring(idx + 1);
						info.putAll(key, toList(PatternSplitter.PTRN_COMMA_SPLITTER, value));
					}	
				}
			}
			
			// FORMAT
			List<String> format = null;
			if (data.size() >= 9 && data.get(8) != null) {
				format = toList(PatternSplitter.PTRN_COLON_SPLITTER, data.get(8));
			}
			
			// Genotypes to Samples mapping
			Map<String, String> sampleGenMapping = new HashMap<String, String>();
			for (int gen = 9; gen < data.size(); gen++) {
				sampleGenMapping.put(this.vcfMetadata.getSampleName(gen-9), data.get(gen));
			}
			
			// Event
			GenotypeEvent event = new GenotypeEvent(chromosome, position, ids, ref, alt, quality, filters, info, format, sampleGenMapping);
			
			// Check if such event occurred previously in initial set of data
			if (Variants.insertionMEsTable.contains(event.getChromosome(), event.getPosition())) {				
				// Store Event
				Variants.integratedEvents.add(event);
			}
						
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("ERROR - Parsing: Integrated VCF data line #" + lineNumber + ": " + line, e);
		}
		
		return true;
	}
	
	/**
	 * Convert line with patterns into List
	 * @param pattern
	 * @param string
	 * @return List of substrings
	 */
	private List<String> toList(@Nonnull Pattern pattern, @Nullable String string) {
		String[] array = pattern.split(string);
		List<String> list = new ArrayList<>(array.length);
		Collections.addAll(list, array);
		return list;
	}

}
