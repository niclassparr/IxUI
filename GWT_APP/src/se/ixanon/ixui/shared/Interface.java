package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.Comparator;

import se.ixanon.ixui.client.AlphanumComparator;

@SuppressWarnings("serial")
public class Interface implements Serializable, Comparable<Interface> {

	private String position;
	private String name;
	private String type;
	private String status;
	private boolean active;
	private boolean emm;
	private boolean multi_band;
	private int network_num;
	
	public Interface() {
		
	}
	
	public Interface(String position, boolean active) {
		this.position = position;
		this.active = active;
	}

	public Interface(String position, String name, String type) {
		this.position = position;
		this.name = name;
		this.type = type;
	}
	
	public Interface(String position, String name, String type, String status, boolean active, boolean multi_band) {
		this.position = position;
		this.name = name;
		this.type = type;
		this.status = status;
		this.active = active;
		this.multi_band = multi_band;
	}
	
	public String getPosition(){
		return this.position;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getStatus(){
		return this.status;
	}
	
	public boolean getActive(){
		return this.active;
	}
	
	public boolean isEmm() {
		return emm;
	}

	public void setEmm(boolean emm) {
		this.emm = emm;
	}
	
	public int getNetworkNum() {
		return network_num;
	}

	public void setNetworkNum(int network_num) {
		this.network_num = network_num;
	}

	@Override
	public int compareTo(Interface arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean isMultiBand() {
		return multi_band;
	}

	public static class Comparators {
		
		
		public static Comparator<Interface> POS = new Comparator<Interface>() {
			
			@Override
			public int compare(Interface o1, Interface o2) {
				
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.position, o2.position);
			}
		};
	}
}
