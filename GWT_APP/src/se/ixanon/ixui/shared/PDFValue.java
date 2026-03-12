package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PDFValue implements Serializable {

	private String name;
	private String value;
	
	public PDFValue() {
		
	}
	
	public PDFValue(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getValue(){
		return this.value;
	}
	
}
