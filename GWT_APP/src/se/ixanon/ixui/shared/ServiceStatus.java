package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ServiceStatus implements Serializable {

	private String name;
	private boolean scrambled;
	private String destination;
	private int bitrate;
	private int discontinuity;
	private String source;
	private int muxLoad;
	private int maxMuxLoad;
	
	private int download_bitrate;
	private int selected_bitrate;
	private int segmentCounter;
	private int num_stream_switches;
	private int num_segments_missed;
	
	//private int bitrate;
	private int bufferlevel;
	
	public ServiceStatus() {
		
	}
	
	public ServiceStatus(String name) {
		this.name = name;
		this.scrambled = false;
	}
	
	public ServiceStatus(String name, int bitrate) {
		this.name = name;
		this.bitrate = bitrate;
		this.scrambled = false;
	}
	
	public ServiceStatus(int bitrate, int bufferlevel) {
		this.bitrate = bitrate;
		this.bufferlevel = bufferlevel;
	}

	public ServiceStatus(int download_bitrate, int selected_bitrate, int segmentCounter, int num_stream_switches, int num_segments_missed, int bitrate, int bufferlevel) {
		this.download_bitrate = download_bitrate;
		this.selected_bitrate = selected_bitrate;
		this.segmentCounter = segmentCounter;
		this.num_stream_switches = num_stream_switches;
		this.num_segments_missed = num_segments_missed;
		this.bitrate = bitrate;
		this.bufferlevel = bufferlevel;

	}
	
	public ServiceStatus(String name, boolean scrambled, String destination, int bitrate, int discontinuity, String source, int muxLoad, int maxMuxLoad) {
		this.name = name;
		this.scrambled = scrambled;
		this.destination = destination;
		this.bitrate = bitrate;
		this.discontinuity = discontinuity;
		this.source = source;
		this.muxLoad = muxLoad;
		this.maxMuxLoad = maxMuxLoad;
	}

	public String getName() {
		return name;
	}

	public boolean isScrambled() {
		return scrambled;
	}

	public String getDestination() {
		return destination;
	}

	public int getBitrate() {
		return bitrate;
	}
	
	public void setBitrate(int bitrate) {
		this.bitrate = bitrate;
	}

	public int getDiscontinuity() {
		return discontinuity;
	}

	public String getSource() {
		return source;
	}

	public int getMuxLoad() {
		return muxLoad;
	}

	public int getMaxMuxLoad() {
		return maxMuxLoad;
	}

	public int getDownload_bitrate() {
		return download_bitrate;
	}

	public int getSelected_bitrate() {
		return selected_bitrate;
	}

	public int getSegmentCounter() {
		return segmentCounter;
	}

	public int getNum_stream_switches() {
		return num_stream_switches;
	}

	public int getNum_segments_missed() {
		return num_segments_missed;
	}

	public int getBufferlevel() {
		return bufferlevel;
	}


	
}
