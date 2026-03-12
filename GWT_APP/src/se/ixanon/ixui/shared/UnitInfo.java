package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UnitInfo implements Serializable {

	private String serial;
	private String version;
	private String hostname;
	private boolean cloud;
	private boolean forcedContent;
	private boolean softwareUpdate;
	private boolean hlsoutput;
	private boolean portal;
	
	public UnitInfo() {
		
	}
	
	public UnitInfo(String serial, String version, String hostname, boolean cloud, boolean forcedContent, boolean softwareUpdate, boolean hlsoutput, boolean portal) {
		this.serial = serial;
		this.version = version;
		this.hostname = hostname;
		this.cloud = cloud;
		this.forcedContent = forcedContent;
		this.softwareUpdate = softwareUpdate;
		this.hlsoutput = hlsoutput;
		this.portal = portal;
	}
	
	public String getSerial(){
		return this.serial;
	}
	
	public String getVersion(){
		return this.version;
	}
	
	public String getHostname(){
		return this.hostname;
	}

	public boolean isCloud() {
		return cloud;
	}

	public boolean isForcedContent() {
		return forcedContent;
	}
	
	public boolean isSoftwareUpdate() {
		return softwareUpdate;
	}

	public boolean isHlsoutput() {
		return hlsoutput;
	}

	public boolean isPortal() {
		return portal;
	}
	
}
