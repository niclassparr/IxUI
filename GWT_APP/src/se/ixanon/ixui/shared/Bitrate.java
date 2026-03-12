package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Bitrate implements Serializable {

	private String type;
	private int bitrate;
	
	public Bitrate() {
		
	}
	
	public Bitrate(String type, int bitrate) {
		this.type = type;
		this.bitrate = bitrate;
	}
	
	public String getType(){
		return this.type;
	}
	
	public int getBitrate(){
		return this.bitrate;
	}
	
}
