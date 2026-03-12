package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import se.ixanon.ixui.server.ContextManager;

@SuppressWarnings("serial")
public class Config implements Serializable {

	private int id;
	private String interface_pos;
	private String interface_name;
	private boolean interface_active;
	private int freq;
	private String pol;
	private int symb;
	private String del;
	private int satno;
	private String lnb_type;
	private int bw;
	private int emm;
	private ArrayList<String> ch_types;
	private ArrayList<String> ch_audiourls;
	private String pres_url;
	private String hdmi_format;
	private String constellation;
	private String in_ip;
	private int in_port;
	private int max_bitrate;
	private int gain;
	private String webradio_url;
	
	public Config() {
		
	}
	
	public Config(int id, String interface_pos) {
		this.id = id;
		this.interface_pos = interface_pos;
	}
	
	public Config(String interface_pos, String interface_name, boolean interface_active) {
		this.interface_pos = interface_pos;
		this.interface_active = interface_active;
		this.interface_name = interface_name;
	}
	
	/*
	public Config(int id, String interface_pos, String url_or_format) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.pres_url = url_or_format;
		this.hdmi_format = url_or_format;
	}
	*/
	public Config(int id, String interface_pos, String in_ip, int in_port) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.in_ip = in_ip;
		this.in_port = in_port;
	}
	
	public Config(int id, String interface_pos, int freq, int bw) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.freq = freq;
		this.bw = bw;
	}
	
	public Config(int id, String interface_pos, int freq, String del, int bw) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.freq = freq;
		this.bw = bw;
		this.del = del;
	}
	
	public Config(int id, String interface_pos, int freq, int symb, String del, String constellation) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.freq = freq;
		this.symb = symb;
		this.del = del;
		this.constellation = constellation;
	}
	
	public Config(int id, String interface_pos, int freq, String pol, int symb, String del, int satno, String lnb_type) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.freq = freq;
		this.pol = pol;
		this.symb = symb;
		this.del = del;
		this.satno = satno;
		this.lnb_type = lnb_type;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getInterfacePos(){
		return this.interface_pos;
	}
	
	public String getInterfaceName(){
		return this.interface_name;
	}
	
	public void setInterfaceName(String interface_name){
		this.interface_name = interface_name;
	}
	
	public boolean getInterfaceActive(){
		return this.interface_active;
	}
	
	public void setInterfaceActive(boolean interface_active){
		this.interface_active = interface_active;
	}
	
	public int getFreq(){
		return this.freq;
	}
	
	public String getPol(){
		return this.pol;
	}
	
	public int getSymb(){
		return this.symb;
	}
	
	public String getDel(){
		return this.del;
	}
	
	public int getSatno(){
		return this.satno;
	}
	
	public String getLnbType(){
		return this.lnb_type;
	}
	
	public int getBw(){
		return this.bw;
	}

	public int getEmm() {
		return emm;
	}

	public void setEmm(int emm) {
		this.emm = emm;
	}

	public String getPresUrl() {
		return pres_url;
	}

	public ArrayList<String> getChTypes() {
		return ch_types;
	}

	public ArrayList<String> getChAudiourls() {
		return ch_audiourls;
	}

	public String getHdmiFormat() {
		return hdmi_format;
	}

	public String getConstellation() {
		return constellation;
	}

	public String getInIp() {
		return in_ip;
	}

	public int getInPort() {
		return in_port;
	}

	public int getMaxBitrate() {
		return max_bitrate;
	}

	public void setMaxBitrate(int max_bitrate) {
		this.max_bitrate = max_bitrate;
	}

	public void setPresUrl(String pres_url) {
		this.pres_url = pres_url;
	}

	public void setHdmiFormat(String hdmi_format) {
		this.hdmi_format = hdmi_format;
	}

	public int getGain() {
		return gain;
	}

	public void setGain(int gain) {
		this.gain = gain;
	}
	
	public String getWebradioUrl() {
		return webradio_url;
	}

	public void setWebradioUrl(String webradio_url) {
		this.webradio_url = webradio_url;
	}

	public static class Comparators {
		
		
		public static Comparator<Config> HLS2IP = new Comparator<Config>() {
			
			@Override
			public int compare(Config o1, Config o2) {
				
				int result = o1.interface_name.compareTo(o2.interface_name);
				if (result != 0) return result;
				
				Boolean b1 = new Boolean(o1.interface_active);
				Boolean b2 = new Boolean(o2.interface_active);
				
				result = b1.compareTo(b2);
				if (result != 0) return result;
				
				Integer i1 = new Integer(o1.max_bitrate); 
		        Integer i2 = new Integer(o2.max_bitrate); 
				
				return i1.compareTo(i2);
				
			}
		};
		
		
		public static Comparator<Config> UDP = new Comparator<Config>() {
			
			@Override
			public int compare(Config o1, Config o2) {
				
				int result = o1.interface_name.compareTo(o2.interface_name);
				if (result != 0) return result;
				
				Boolean b1 = new Boolean(o1.interface_active);
				Boolean b2 = new Boolean(o2.interface_active);
				
				result = b1.compareTo(b2);
				if (result != 0) return result;				
				
				String ip1 = o1.in_ip != null ? o1.in_ip : "";
				String ip2 = o2.in_ip != null ? o2.in_ip : "";
				
				result = ip1.compareTo(ip2);
				if (result != 0) return result;
				
				Integer i1 = new Integer(o1.in_port); 
		        Integer i2 = new Integer(o2.in_port);
		        
		        result = i1.compareTo(i2);
				if (result != 0) return result;
		        
				Integer emm1 = new Integer(o1.emm); 
		        Integer emm2 = new Integer(o2.emm); 
				
				return emm1.compareTo(emm2);
				
			}
		};
		
		
		public static Comparator<Config> SAT = new Comparator<Config>() {
			
			@Override
			public int compare(Config o1, Config o2) {
				
				int result = o1.interface_name.compareTo(o2.interface_name);
				if (result != 0) return result;
				
				Boolean active1 = new Boolean(o1.interface_active);
				Boolean active2 = new Boolean(o2.interface_active);
				
				result = active1.compareTo(active2);
				if (result != 0) return result;				
				
				
				Integer freq1 = new Integer(o1.freq); 
		        Integer freq2 = new Integer(o2.freq);
		        
		        result = freq1.compareTo(freq2);
				if (result != 0) return result;
		        
				String pol1 = o1.pol != null ? o1.pol : "";
				String pol2 = o2.pol != null ? o2.pol : "";
				
				result = pol1.compareTo(pol2);
				if (result != 0) return result;
				
				
				Integer symb1 = new Integer(o1.symb); 
		        Integer symb2 = new Integer(o2.symb);
		        
		        result = symb1.compareTo(symb2);
				if (result != 0) return result;
				
				String del1 = o1.del != null ? o1.del : "";
				String del2 = o2.del != null ? o2.del : "";
				
				result = del1.compareTo(del2);
				if (result != 0) return result;
				
				
				Integer satno1 = new Integer(o1.satno); 
		        Integer satno2 = new Integer(o2.satno);
		        
		        result = satno1.compareTo(satno2);
				if (result != 0) return result;
				
				String lnb1 = o1.lnb_type != null ? o1.lnb_type : "";
				String lbn2 = o2.lnb_type != null ? o2.lnb_type : "";
				
				result = lnb1.compareTo(lbn2);
				if (result != 0) return result;
				
				
				Integer emm1 = new Integer(o1.emm); 
		        Integer emm2 = new Integer(o2.emm); 
				
				return emm1.compareTo(emm2);
				
			}
		};
		
		
		public static Comparator<Config> TER = new Comparator<Config>() {
			
			@Override
			public int compare(Config o1, Config o2) {
				
				int result = o1.interface_name.compareTo(o2.interface_name);
				if (result != 0) return result;
				
				Boolean active1 = new Boolean(o1.interface_active);
				Boolean active2 = new Boolean(o2.interface_active);
				
				result = active1.compareTo(active2);
				if (result != 0) return result;				
				
				
				Integer freq1 = new Integer(o1.freq); 
		        Integer freq2 = new Integer(o2.freq);
		        
		        result = freq1.compareTo(freq2);
				if (result != 0) return result;
		        
				
				Integer bw1 = new Integer(o1.bw); 
		        Integer bw2 = new Integer(o2.bw);
		        
		        result = bw1.compareTo(bw2);
				if (result != 0) return result;
				
				String del1 = o1.del != null ? o1.del : "";
				String del2 = o2.del != null ? o2.del : "";
				
				result = del1.compareTo(del2);
				if (result != 0) return result;
				
				
				Integer emm1 = new Integer(o1.emm); 
		        Integer emm2 = new Integer(o2.emm); 
				
				return emm1.compareTo(emm2);
				
			}
		};
		
		
		public static Comparator<Config> CABLE = new Comparator<Config>() {
			
			@Override
			public int compare(Config o1, Config o2) {
				
				int result = o1.interface_name.compareTo(o2.interface_name);
				if (result != 0) return result;
				
				Boolean active1 = new Boolean(o1.interface_active);
				Boolean active2 = new Boolean(o2.interface_active);
				
				result = active1.compareTo(active2);
				if (result != 0) return result;				
				
				
				Integer freq1 = new Integer(o1.freq); 
		        Integer freq2 = new Integer(o2.freq);
		        
		        result = freq1.compareTo(freq2);
				if (result != 0) return result;
		        
				
				Integer symb1 = new Integer(o1.symb); 
		        Integer symb2 = new Integer(o2.symb);
		        
		        result = symb1.compareTo(symb2);
				if (result != 0) return result;
				
				String del1 = o1.del != null ? o1.del : "";
				String del2 = o2.del != null ? o2.del : "";
				
				result = del1.compareTo(del2);
				if (result != 0) return result;
				
				String con1 = o1.constellation != null ? o1.constellation : "";
				String con2 = o2.constellation != null ? o2.constellation : "";
				
				result = con1.compareTo(con2);
				if (result != 0) return result;
				
				
				Integer emm1 = new Integer(o1.emm); 
		        Integer emm2 = new Integer(o2.emm); 
				
				return emm1.compareTo(emm2);
				
			}
		};
		
		
	}
	
	
}
