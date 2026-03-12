package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class IpStatus implements Serializable {

	private String ip;
	private boolean status;
		
	public IpStatus() {
		
	}

	public IpStatus(String ip, boolean status) {
		this.ip = ip;
		this.status = status;
	}

	public String getIp() {
		return ip;
	}

	public boolean isStatus() {
		return status;
	}	
	
}
