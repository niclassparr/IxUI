package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class TunerStatus implements Serializable {

	private boolean locked;
	private int frequency;
	private int signalstrength;
	private int snr;
	
	private int ci_status;
	private boolean ca_emm;
	private String ca_text;
	private String ca_osd;
	private String menu_title;
	private HashMap<Integer, String> menu_items;
	
	public TunerStatus() {
		
	}
	
	public TunerStatus(boolean locked) {
		this.locked = locked;
	}
	
	public TunerStatus(boolean locked, int frequency, int signalstrength, int snr) {
		this.locked = locked;
		this.frequency = frequency;
		this.signalstrength = signalstrength;
		this.snr = snr;
	}
	
	public TunerStatus(int ci_status, boolean ca_emm, String ca_text, String ca_osd, String menu_title, HashMap<Integer, String> menu_items) {
		this.ci_status = ci_status;
		this.ca_emm = ca_emm;
		this.ca_text = ca_text;
		this.ca_osd = ca_osd;
		this.menu_title = menu_title;
		this.menu_items = new HashMap<Integer, String>(menu_items);
	}

	public boolean isLocked() {
		return locked;
	}

	public int getFrequency() {
		return frequency;
	}

	public int getSignalstrength() {
		return signalstrength;
	}

	public int getSnr() {
		return snr;
	}

	public int getCiStatus() {
		return ci_status;
	}
	
	public boolean isCaEmm() {
		return ca_emm;
	}

	public String getCaText() {
		return ca_text;
	}

	public String getCaOsd() {
		return ca_osd;
	}
	
	public String getMenuTitle() {
		return this.menu_title;
	}
	
	
	public HashMap<Integer, String> getMenuItems() {
		return this.menu_items;
	}
}
