package main.database;

import java.io.Serializable;
import java.util.List;

public class Posting implements Serializable {
    // for inverted index tables
	public int id; // doc id 
	public int freq;
    public List<Integer> positions;

	public Posting(int id, int freq, List<Integer> positions) {
        this.id = id;
        this.freq = freq;
        this.positions = positions;
    }

    public int getId() {
        return id;
    }

    public int getFreq() {
        return freq;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    

	public void addPosition(int position) {
        positions.add(position);
    }
    
}