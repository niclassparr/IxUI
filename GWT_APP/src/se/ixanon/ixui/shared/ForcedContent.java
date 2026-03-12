package se.ixanon.ixui.shared;

import java.io.Serializable;
import java.util.Comparator;

@SuppressWarnings("serial")
public class ForcedContent implements Serializable {

	private int id;
	private boolean enable;
	private String name;
	private int networks; // 0 = none, 3 = both
	private String ts_filename;
	private int operation_mode; // 0 = continuity, 1 = single
	private int signal_type; // 0 Normally Open, 1 = Normally Closed
	private int signal_override; // 0 = none, 1 ON 2 OFF
	private int signal_status; // 0 = none, 1 ON 2 OFF
	private boolean com_status;
	private int volume;
	
	public ForcedContent() {
		
	}

	public ForcedContent(int id, boolean enable, String name, int networks, String ts_filename, int operation_mode, int signal_type, int volume) {
		this.id = id;
		this.enable = enable;
		this.name = name;
		this.networks = networks;
		this.ts_filename = ts_filename;
		this.operation_mode = operation_mode;
		this.signal_type = signal_type;
		this.volume = volume;
	}

	
	
	public ForcedContent(int id, String name, int signal_override, int signal_status, boolean com_status) {
		this.id = id;
		this.name = name;
		this.signal_override = signal_override;
		this.signal_status = signal_status;
		this.com_status = com_status;
	}

	public int getId() {
		return id;
	}

	public boolean isEnable() {
		return enable;
	}

	public String getName() {
		return name;
	}

	public int getNetworks() {
		return networks;
	}

	public String getTsFilename() {
		return ts_filename;
	}

	public int getOperationMode() {
		return operation_mode;
	}

	public int getSignalType() {
		return signal_type;
	}

	public int getSignalOverride() {
		return signal_override;
	}

	public int getSignalStatus() {
		return signal_status;
	}

	public boolean isComStatus() {
		return com_status;
	}

	public int getVolume() {
		return volume;
	}
	
	public static class Comparators {
		
		
		public static Comparator<ForcedContent> ENABLED = new Comparator<ForcedContent>() {
			
			@Override
			public int compare(ForcedContent o1, ForcedContent o2) {
				
				Boolean b1 = new Boolean(o1.enable);
				Boolean b2 = new Boolean(o2.enable);
				
				return b1.compareTo(b2);
				
			}
			
		};
		
		public static Comparator<ForcedContent> NETWORK = new Comparator<ForcedContent>() {
			
			@Override
			public int compare(ForcedContent o1, ForcedContent o2) {
				
				Integer i1 = new Integer(o1.networks); 
		        Integer i2 = new Integer(o2.networks);
				
				return i1.compareTo(i2);
				
			}
			
		};
		
	}
	
}
