package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Emm implements Serializable {

	private int current;
	private ArrayList<Integer> free;
	
	public Emm() {
		
	}

	public Emm(int current) {
		this.current = current;
	}

	public ArrayList<Integer> getFree() {
		return free;
	}

	public void setFree(ArrayList<Integer> free) {
		this.free = new ArrayList<Integer>(free);
	}

	public int getCurrent() {
		return current;
	}
	
	
	
}
