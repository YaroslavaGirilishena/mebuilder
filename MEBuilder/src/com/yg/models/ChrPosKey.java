package com.yg.models;

/**
 * 
 * @author Yaroslava Girilishena
 *
 */
public class ChrPosKey {
	public final String chromosome;
    public final Long position;

    public ChrPosKey(String chr, long pos) {
        this.chromosome = chr;
        this.position = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChrPosKey)) return false;
        ChrPosKey key = (ChrPosKey) o;
        return chromosome == key.chromosome && position == key.position;
    }

    @Override
    public int hashCode() {
        int result = chromosome.hashCode();
        result = (int) (31 * result + position);
        return result;
    }
}
