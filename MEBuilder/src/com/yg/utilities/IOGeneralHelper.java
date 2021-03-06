package com.yg.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.yg.io_handlers.IOParameters;
import com.yg.models.FASTASeq;

/**
 * Class that works with files and folders 
 * @author Yaroslava Girilishena
 *
 */
public class IOGeneralHelper {
	public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // init logger
	
    private static FileHandler fh;  

	/**
	 * Collect all .fa files' names from the specified directory
	 * @return
	 */
	public static List<String> getListOfFAFiles(String filepath) {
		File dir = new File(filepath);
		List<String> faFiles = new ArrayList<String>();
		
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith((".fa")) && !faFiles.contains(file.getAbsolutePath())) {
				faFiles.add(file.getAbsolutePath());
			}
		}
		return faFiles;
	}
	
	/**
	 * Create directory if it doesn't exist
	 * @param dirPath
	 * @throws IOException
	 */
	public static void createOutDir(String dirPath) throws IOException {
		// Create output directory if it doesn't exist
		Path path = Paths.get(System.getProperty("user.dir")  +  dirPath);
        if (!Files.exists(path)) {
        	Files.createDirectories(path);
        }
	}
	
	/**
	 * Delete specified directory
	 * @param directory
	 * @return
	 */
	public static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(files != null){
	            for(int i=0; i<files.length; i++) {
	                if (files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}
	
	/**
	 * Remove a file
	 * @param filename
	 * @param message
	 */
	public static void deleteFile(String filename, String message) {
		if (message != null) LOGGER.info(filename + " " + message + "\n");
		
		File file = new File(filename);
		file.delete();
	}
	
	/**
	 * Print merged contig into a file
	 * @param bl2seqOutDir
	 * @param contig
	 * @throws IOException
	 */
	public static void writeFASeqIntoFile(String outFile, FASTASeq seq, boolean append) throws IOException {
		try(FileWriter fw = new FileWriter(outFile, append);
		    BufferedWriter bw = new BufferedWriter(fw);
		    PrintWriter out = new PrintWriter(bw)) {
			
			// Print contig
		    out.print(seq.toPrint());
		    
		    // Close streams
		    out.close();
		    bw.close();
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Merge list of files into specified file
	 * @param files
	 * @param mergedFile
	 * @return 
	 */
	public static void mergeFiles(List<File> files, String mergedFile) {
		FileWriter fstream = null;
        BufferedWriter out = null;
		
        try {
            fstream = new FileWriter(mergedFile, true);
            out = new BufferedWriter(fstream);
        }
        catch(IOException e1) {
            e1.printStackTrace();
        }
				
        for(File f : files) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(f);
                BufferedReader in = new BufferedReader(new InputStreamReader(fis));
 
                String aLine;
                while((aLine = in.readLine()) != null)  {
                    out.write(aLine);
                    out.newLine();
                }
 
                in.close();
            }
            catch(IOException e) {
               e.printStackTrace();
            }
        }
 
        try {
        	out.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
	}
	
	/**
	 * Setup file for logging
	 * @throws SecurityException
	 * @throws IOException
	 */
	public static void setupLogger(String chromosome, long position) throws SecurityException, IOException {
		String logFileName = System.getProperty("user.dir") + "/log/logfile_" + IOParameters.ME_TYPE + "." + chromosome + "_" + position + ".log";
		File logFile = new File(logFileName);
		logFile.createNewFile(); // if file already exists will do nothing 
		
		// This block configure the logger with handler and formatter  
        fh = new FileHandler(logFileName);  
        LOGGER.addHandler(fh);
        fh.setLevel(Level.ALL);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  

        // the following statement is used to log any messages  
        LOGGER.info("LOGGER CREATED\n"); 
	}
}
