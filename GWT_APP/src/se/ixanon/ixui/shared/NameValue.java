package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NameValue implements Serializable {

	private int id;
	private String name;
	private String value;
	
	public NameValue() {
		
	}
	
	public NameValue(int id, String name) {
		this.id = id;
		this.name = name;
		this.value = null;
	}
	
	public NameValue(int id, String name, String value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getValue(){
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return id + " - " + name + "\n";
	}
	
	
}
