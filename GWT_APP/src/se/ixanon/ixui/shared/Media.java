package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Media implements Serializable {

	private String title;
	private String internal_filename;
	
	public Media() {
		
	}

	public Media(String title, String internal_filename) {
		this.title = title;
		this.internal_filename = internal_filename;
	}

	public String getTitle() {
		return title;
	}

	public String getInternalFilename() {
		return internal_filename;
	}
	
}
