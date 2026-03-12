package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.Comparator;

import se.ixanon.ixui.client.AlphanumComparator;

@SuppressWarnings("serial")
public class Usage implements Serializable, Comparable<Usage> {

	private String type;
	private String interface_pos;
	private int count_services;
	private int count_bitrate;
	
	public Usage() {
		
	}
	
	public Usage(String type, String interface_pos) {
		this.type = type;
		this.interface_pos = interface_pos;
	}
	
	public String getType(){
		return this.type;
	}
	
	public String getInterfacePos(){
		return this.interface_pos;
	}
	
	public int getCountServices(){
		return this.count_services;
	}
	
	public void setCountServices(int count_services){
		this.count_services = count_services;
	}
	
	public int getCountBitrate(){
		return this.count_bitrate;
	}
	
	public void addServiceCount() {
		count_services++;
	}
	
	public void addBitrateCount(int bitrate) {
		count_bitrate += bitrate;
	}
	
	public void clearCount() {
		count_services = 0;
		count_bitrate = 0;
	}

	@Override
	public int compareTo(Usage arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static class Comparators {
		
		
		public static Comparator<Usage> INTERFACE_POS = new Comparator<Usage>() {
			
			@Override
			public int compare(Usage o1, Usage o2) {
				
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.interface_pos, o2.interface_pos);
			}
		};
	}
}
