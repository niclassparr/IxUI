package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class CiMenu implements Serializable {

	private String title;
	private String sub_title;
	private HashMap<String, Integer> items;
	
	
	public CiMenu() {
		
	}
	
	public CiMenu(String title, String sub_title, HashMap<String, Integer> items) {
		this.title = title;
		this.sub_title = sub_title;
		this.items = items;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getSubTitle() {
		return this.sub_title;
	}
	
	public HashMap<String, Integer> getItems() {
		return this.items;
	}
	
}
