package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PDFRow implements Serializable {

	private String lcn;
	private String service_name;
	private String descrambler_pos;
	private String modulator_pos;
	private String modulator_pos_net2;
	private String sid_out;
	private String ip_out;
	
	public PDFRow() {
		
	}
	
	public PDFRow(String lcn, String service_name, String descrambler_pos, String modulator_pos, String modulator_pos_net2, String sid_out, String ip_out) {
		this.lcn = lcn;
		this.service_name = service_name;
		this.descrambler_pos = descrambler_pos;
		this.modulator_pos = modulator_pos;
		this.modulator_pos_net2 = modulator_pos_net2;
		this.sid_out = sid_out;
		this.ip_out = ip_out;	
	}
	
	public String getColumnText(int column, boolean dvbc_enable, boolean dvbc_enable_net2, boolean ip_enable) {
		if(column == 0) {
			return lcn;
		} else if(column == 1) {
			return service_name;
		} else if(column == 2) {
			return descrambler_pos;
		} else if(column == 3) {
			
			if(dvbc_enable) {
				return modulator_pos;
			} else {
				return null;
			}
			
			
		} else if(column == 4) {
			
			if(dvbc_enable_net2) {
				return modulator_pos_net2;
			} else {
				return null;
			}
			
			
		} else if(column == 5) {
			
			if(dvbc_enable) {
				return sid_out;
			} else {
				return null;
			}
			
			
		} else {

			if(ip_enable) {
				return ip_out;
			} else {
				return null;
			}
			
		}
	}
	
}