package se.ixanon.ixui.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.i18n.client.NumberFormat;

public class Helper {

	public static <T> Set<T> findDuplicates(Collection<T> collection) {

	    Set<T> duplicates = new LinkedHashSet<>();
	    Set<T> uniques = new HashSet<>();

	    for(T t : collection) {
	        if(!uniques.add(t)) {
	            duplicates.add(t);
	        }
	    }

	    return duplicates;
	}
	
	
	public static String getStatus(int value) {
		if(value == 1) {
			return "ON";
		} else if(value == 2) {
			return "OFF";
		} else {
			return "None";
		}
	}
	
	public static String getStatus(boolean value) {
		if(value) {
			return null;
		} else {
			return "ERROR";
		}
	}
	
	public static int getNull(String s) {
		
		if(s.equals("")) {
			return 0;
		} else {
			return Integer.parseInt(s);
		}
		
		
	}
	
	public static String translate(String type) {
		
		String value = "unknown";
		
		if(type.equals("dvbudp")) {
			value = "udp";
		} else if(type.equals("dvbs")) {
			value = "satellite";
		} else if(type.equals("dvbt")) {
			value = "terrestrial";
		} else if(type.equals("dvbc")) {
			value = "cable";
		} else if(type.equals("infostreamer")) {
			value = "infotv";
		} else if(type.equals("mod")) {
			value = "modulator";
		} else if(type.equals("dsc")) {
			value = "descrambler";
		} else if(type.equals("hdmi2ip") || type.equals("dvbhdmi")) {
			value = "hdmi";
		} else if(type.equals("hls2ip")) {
			value = "hls";
		} else if(type.equals("webradio")) {
			value = "webradio";
		} else if(type.equals("infoch")) {
			value = "infochannel";
		}
		
		return value;
	}
	
	public static String getFormatted(double value, int decimalCount) {
	    StringBuilder numberPattern = new StringBuilder(
	            (decimalCount <= 0) ? "" : ".");
	    for (int i = 0; i < decimalCount; i++) {
	        numberPattern.append('0');
	    }
	    return NumberFormat.getFormat(numberPattern.toString()).format(value);
	}
	
	public static native void log( String s ) 
	/*-{ console.log( s ); }-*/;
	
}
