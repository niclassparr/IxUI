package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class StreamerStatus implements Serializable {

	private int muxLoad;
	private int maxMuxLoad;
	private int caServices;
	private int caPids;
	private ArrayList<ServiceStatus> services;
	
	public StreamerStatus() {
		
	}
	
	public StreamerStatus(ArrayList<ServiceStatus> services) {
		this.services = new ArrayList<ServiceStatus>(services);
	}
	
	public StreamerStatus(int muxLoad, int maxMuxLoad, int caServices, int caPids, ArrayList<ServiceStatus> services) {
		this.muxLoad = muxLoad;
		this.maxMuxLoad = maxMuxLoad;
		this.caServices = caServices;
		this.caPids = caPids;
		this.services = new ArrayList<ServiceStatus>(services);
	}
	
	public int getMuxLoad() {
		return muxLoad;
	}

	public int getMaxMuxLoad() {
		return maxMuxLoad;
	}

	public int getCaServices() {
		return caServices;
	}

	public int getCaPids() {
		return caPids;
	}

	public ArrayList<ServiceStatus> getServices() {
		return services;
	}
	
}
