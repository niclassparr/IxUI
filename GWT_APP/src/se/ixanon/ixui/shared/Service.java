package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import se.ixanon.ixui.client.AlphanumComparator;

@SuppressWarnings("serial")
public class Service implements Serializable, Comparable<Service> {

	private int id;
	private String interface_pos;
	private String name;
	private int sid;
	private String type;
	private String lang;
	private boolean enabled;
	private ArrayList<String> all_langs;
	private String radio_url;
	private boolean show_pres;
	private boolean scrambled;
	private String epg_url;
	private String key;
	private String hls_url;
	private String webradio_url;
	private int prefered_lcn;
	private List<String> filters;
	private boolean found;
	
	public Service() {
		
	}
		
	public Service(int id, String interface_pos, String name, int sid, String type, String lang, boolean enabled, ArrayList<String> all_langs, String radio_url, 
			boolean show_pres, boolean scrambled, String epg_url, String hls_url, String webradio_url, int prefered_lcn) {
		this.id = id;
		this.interface_pos = interface_pos;
		this.name = name;
		this.sid = sid;
		this.type = type;
		this.lang = lang;
		this.enabled = enabled;
		this.all_langs = new ArrayList<String>(all_langs);
		this.radio_url = radio_url;
		this.show_pres = show_pres;
		this.scrambled = scrambled;
		this.epg_url = epg_url;
		this.key = interface_pos + ", " + sid;
		this.hls_url = hls_url;
		this.webradio_url = webradio_url;
		this.prefered_lcn = prefered_lcn;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getInterfacePos(){
		return this.interface_pos;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getSid(){
		return this.sid;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getLang(){
		return this.lang;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public boolean isEnabled(){
		return this.enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public ArrayList<String> getAllLangs() {
		return this.all_langs;
	}

	public String getRadioUrl() {
		return radio_url;
	}

	public void setRadioUrl(String radio_url) {
		this.radio_url = radio_url;
	}

	public boolean isShowPres() {
		return show_pres;
	}

	public void setShowPres(boolean show_pres) {
		this.show_pres = show_pres;
	}

	public boolean isScrambled() {
		return scrambled;
	}

	public void setScrambled(boolean scrambled) {
		this.scrambled = scrambled;
	}

	public String getKey() {
		return key;
	}
	
	public String getEpgUrl() {
		return epg_url;
	}
	
	public void setEpgUrl(String epg_url) {
		this.epg_url = epg_url;
	}

	public String getHlsUrl() {
		return hls_url;
	}

	public void setHlsUrl(String hls_url) {
		this.hls_url = hls_url;
	}

	public String getWebradioUrl() {
		return webradio_url;
	}

	public void setWebradioUrl(String webradio_url) {
		this.webradio_url = webradio_url;
	}

	public int getPreferedLcn() {
		return prefered_lcn;
	}
	
	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}
	
	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}
	
	
	

	@Override
	public int compareTo(Service o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static class Comparators {
		
		
		public static Comparator<Service> NAME = new Comparator<Service>() {
			
			@Override
			public int compare(Service o1, Service o2) {
				
				AlphanumComparator ac = new AlphanumComparator();
				return ac.compare(o1.name, o2.name);
			}
		};
	}
	
}
