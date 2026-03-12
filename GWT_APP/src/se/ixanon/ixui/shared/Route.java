package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.Comparator;

import se.ixanon.ixui.client.AlphanumComparator;

@SuppressWarnings("serial")
public class Route implements Serializable, Comparable<Route> {

	private int id;
	private int service_id;
	private String service_name;
	private String service_type;
	private String interface_pos;
	private String interface_type;
	private int lcn;
	private String descrambler_pos;
	private String modulator_pos;
	private String modulator_pos_net2;
	private int out_sid;
	private String out_ip;
	private boolean scrambled;
	private String output_name;
	private String epg_url;
	private boolean hls_enable;
	private boolean interface_multiband;
	
	public Route() {
		
	}
	
	public Route(int id, String service_name) {
		this.id = id;
		this.service_name = service_name;
	}

	public Route(int id, int service_id, String service_name, String service_type, String interface_pos, String interface_type, boolean interface_multiband, int lcn, String descrambler_pos, String modulator_pos, String modulator_pos_net2, int out_sid, String out_ip, boolean scrambled, String output_name, String epg_url, boolean hls_enable) {
		this.id = id;
		this.service_id = service_id;
		this.service_name = service_name;
		this.service_type = service_type;
		this.interface_pos = interface_pos;
		this.interface_type = interface_type;
		this.lcn = lcn;
		this.descrambler_pos = descrambler_pos;
		this.modulator_pos = modulator_pos;
		this.modulator_pos_net2 = modulator_pos_net2;
		this.out_sid = out_sid;
		this.out_ip = out_ip;
		this.scrambled = scrambled;
		this.output_name = output_name;
		this.epg_url = epg_url;
		this.hls_enable = hls_enable;
		this.interface_multiband = interface_multiband;
	}
	
	public int getId(){
		return this.id;
	}
	
	public int getServiceId(){
		return this.service_id;
	}
	
	public String getServiceName(){
		return this.service_name;
	}
	
	public String getServiceType(){
		return this.service_type;
	}
	
	public String getInterfacePos(){
		return this.interface_pos;
	}
	
	public String getInterfaceType(){
		return this.interface_type;
	}
	
	public int getLcn(){
		return this.lcn;
	}
	
	public String getDescramblerPos(){
		return this.descrambler_pos;
	}
	
	public String getModulatorPos(){
		return this.modulator_pos;
	}
	
	public String getModulatorPosNet2(){
		return this.modulator_pos_net2;
	}
	
	public int getOutSid(){
		return this.out_sid;
	}
	
	public String getOutIp(){
		return this.out_ip;
	}
	
	public String getOutputName(){
		return this.output_name;
	}
	
	public boolean isScrambled() {
		return scrambled;
	}
	
	public String getEpgUrl() {
		return epg_url;
	}

	public boolean isHls() {
		return hls_enable;
	}
	
	public boolean isInterfaceMultiband() {
		return interface_multiband;
	}

	@Override
	public int compareTo(Route o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static class Comparators {
		
		
		public static Comparator<Route> SERVICE_NAME = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				return o1.service_name.compareTo(o2.service_name);
			}
		};
		
		public static Comparator<Route> SERVICE_NAME_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				return o2.service_name.compareTo(o1.service_name);
			}
		};
		
		public static Comparator<Route> INTERFACE_POS = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.interface_pos, o2.interface_pos);
			}
		};
		
		public static Comparator<Route> INTERFACE_POS_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o2.interface_pos, o1.interface_pos);
			}
		};
		
		public static Comparator<Route> LCN = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				
				if(o1.lcn > o2.lcn) {
					return 1;
				} else if(o1.lcn < o2.lcn) {
					return -1;
				}
				    
				return 0;
			}
		};
		
		public static Comparator<Route> LCN_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				
				if(o2.lcn > o1.lcn) {
					return 1;
				} else if(o2.lcn < o1.lcn) {
					return -1;
				}
				    
				return 0;
			}
		};
		
		public static Comparator<Route> OUT_SID = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				
				if(o1.out_sid > o2.out_sid) {
					return 1;
				} else if(o1.out_sid < o2.out_sid) {
					return -1;
				}
				    
				return 0;
			}
		};
		
		public static Comparator<Route> OUT_SID_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				
				if(o2.out_sid > o1.out_sid) {
					return 1;
				} else if(o2.out_sid < o1.out_sid) {
					return -1;
				}
				    
				return 0;
			}
		};
		
		public static Comparator<Route> OUT_IP = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.out_ip, o2.out_ip);
			}
		};
		
		public static Comparator<Route> OUT_IP_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o2.out_ip, o1.out_ip);
			}
		};
		
		public static Comparator<Route> DESCRAMBLER_POS = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.descrambler_pos, o2.descrambler_pos);
			}
		};
		
		public static Comparator<Route> DESCRAMBLER_POS_REVERSE = new Comparator<Route>() {
			
			@Override
			public int compare(Route o1, Route o2) {
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o2.descrambler_pos, o1.descrambler_pos);
			}
		};
	}
}
