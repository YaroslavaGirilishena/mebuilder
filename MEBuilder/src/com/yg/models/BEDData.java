/**
 * 
 */
package com.yg.models;

/**
 * Loci in reference that supports MEI event
 * 
 * @author Yaroslava Girilishena
 *
 */
public class BEDData {
	
	private String chrom;
	private long chromStart;
	private long chromEnd;
	private String name;
	private int score;
	private char strand;
	
	/**
	 * Constructor 
	 * @param chrom
	 * @param start
	 * @param end
	 * @param name
	 * @param score
	 * @param strand
	 */
	public BEDData(String chrom, long start, long end, String name, int score, char strand) {
		this.chrom = chrom;
		this.chromStart = start;
		this.chromEnd = end;
		this.score = score;
		this.name = name;
		this.strand = strand;
	}
	
	public String toString() {
		String res = "";
		res = this.chrom + '\t' +
			  this.chromStart + '\t' +
			  this.chromEnd + '\t' +
			  this.name + '\t' +
			  this.score + '\t' +
			  this.strand;
		return res;
	}
	
 	public String fullDescString() {
		String res = "";
		res = "chrom: " + this.chrom +
			  " chromStart: " + this.chromStart + 
			  " chromEnd: " + this.chromEnd + 
			  " name: " + this.name + 
			  " score: " + this.score + 
			  " strand: " + this.strand;
		return res;
	}
 	
 	/**
 	 * 
 	 * Getters and setters
 	 * 
 	 */

	public String getChrom() {
		return chrom;
	}

	public void setChrom(String chrom) {
		this.chrom = chrom;
	}

	public long getChromStart() {
		return chromStart;
	}

	public void setChromStart(long chromStart) {
		this.chromStart = chromStart;
	}

	public long getChromEnd() {
		return chromEnd;
	}

	public void setChromEnd(long chromEnd) {
		this.chromEnd = chromEnd;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public char getStrand() {
		return strand;
	}

	public void setStrand(char strand) {
		this.strand = strand;
	}
}
