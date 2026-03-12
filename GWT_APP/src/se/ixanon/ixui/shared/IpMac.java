package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class IpMac implements Serializable {

	private String ip;
	private String mac;
		
	public IpMac() {
		
	}

	public IpMac(String ip, String mac) {
		this.ip = ip;
		this.mac = mac;
	}

	public String getIp() {
		return ip;
	}

	public String getMac() {
		return mac;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}	
	
	
	
}
