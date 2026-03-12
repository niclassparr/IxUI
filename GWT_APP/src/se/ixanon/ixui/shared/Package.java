package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Package implements Serializable {

	private String name;
	private String version;
	private boolean update;
	
	public Package() {
		
	}
	
	public Package(String name, String version) {
		this.name = name;
		this.version = version;
		this.update = false;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getVersion(){
		return this.version;
	}
	
	public boolean isUpdate(){
		return this.update;
	}
	
	public void setUpdate(boolean update) {
		this.update = update;
	}
	
}
