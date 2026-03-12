package se.ixanon.ixui.client.view;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.GroupField;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.ListField;
import se.ixanon.ixui.client.item.table.TextField;
import se.ixanon.ixui.client.item.table.ToggleField;
import se.ixanon.ixui.client.presenter.SettingsPresenter;

public class SettingsView extends Composite implements SettingsPresenter.Display {

	private FlowPanel mainPanel;
	
	private Button datetime = new Button("Date and Time Settings");
	private Button mod_button = new Button("Modulator Net Settings");
	private Button pw_button = new Button("Change Password");
	
	private ToggleField dvbc_enabled = new ToggleField("Enabled");
	private ListField dvbc_start_freq = new ListField("Start Frequency (MHz)");
	private ListField dvbc_symbol_rate = new ListField("Symbolrate");
	private ListField dvbc_qam_method = new ListField("QAM Constellation");
	private ListField dvbc_attenuation = new ListField("Attenuation (dB)");
	private TextField dvbc_network_id = new TextField("Network ID", false);
	private TextField dvbc_network_name = new TextField("Network Name", true);
	private TextField dvbc_orgnetid = new TextField("Original Network ID", false);
	
	private ToggleField dvbc_net2_enabled = new ToggleField("Enabled");
	private ListField dvbc_net2_start_freq = new ListField("Start Frequency (MHz)");
	private ListField dvbc_net2_symbol_rate = new ListField("Symbolrate");
	private ListField dvbc_net2_qam_method = new ListField("QAM Constellation");
	private ListField dvbc_net2_attenuation = new ListField("Attenuation (dB)");
	private TextField dvbc_net2_network_id = new TextField("Network ID", false);
	private TextField dvbc_net2_network_name = new TextField("Network Name", true);
	private TextField dvbc_net2_orgnetid = new TextField("Original Network ID", false);
	
	private ToggleField ip_enabled = new ToggleField("Enabled");
	private TextField ip_start_addr = new TextField("Multicast Start Address", true);
	private ListField ip_network_interface = new ListField("Network Interface");
	private TextField lan = new TextField("VLAN ID (Optional)", false);
	private TextField ip_ttl = new TextField("Time To Live (TTL)", true);
	private TextField ip_tos = new TextField("Type Of Service (TOS)", true);
	
	private TextField dsc_max_services = new TextField("Max Services", false);
	private TextField dsc_max_bitrate = new TextField("Max Bitrate (bps)", false);
	
	private TextField bitrate_tvsd = new TextField("TV_SD (bps)", false);
	private TextField bitrate_tvhd = new TextField("TV_HD (bps)", false);
	private TextField bitrate_radio = new TextField("RADIO (bps)", false);
	
	private ToggleField hls_ba_enabled = new ToggleField("Basic Auth Enabled");
	private TextField hls_ba_user = new TextField("User", true);
	private TextField hls_ba_password = new TextField("Password", true);
	private TextField hls_max_bitrate = new TextField("Max Bitrate (bps)", false);
	
	private ToggleField remux_enabled = new ToggleField("Enabled");
	private ListField remux_audio_format = new ListField("Audio Format");
	private TextField remux_audio_offset = new TextField("Audio Mux Offset (ms)", true);
	private TextField remux_muxrate = new TextField("Multiplexer Bitrate (bps)", false);
	
	private ToggleField hls_enabled = new ToggleField("Enabled");
	private TextField hls_server_ip = new TextField("Server IP", true);
	private TextField hls_inport = new TextField("Inport", false);
	private TextField hls_outport = new TextField("Outport", false);
	private TextField hls_services = new TextField("Max Services", false);
	private TextField hls_playback_prefix = new TextField("Playback Prefix", true);
	
	private ToggleField portal_enabled = new ToggleField("Enabled");
	private TextField portal_server_ip = new TextField("Server IP", true);
	private TextField portal_url = new TextField("Portal URL", true);
	
	private ToggleField cloud_enabled = new ToggleField("Enabled");
	private TextField cloud_url = new TextField("Cloud URL", true);
	
	private ToggleField forced_content_enabled = new ToggleField("Enabled");
	
	private Button save_button = new Button("Save");
	
	public SettingsView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
	}
	
	/*
	private String getValue(String name) {
		
		for (int i = 0; i < values.size(); ++i) {
			if(name.equals(values.get(i).getName())) {
				return values.get(i).getValue();
			}
		}
		
		return "";
		
	}
	
	private void setValue(String name, String value) {
		
		for (int i = 0; i < values.size(); ++i) {
			if(name.equals(values.get(i).getName())) {
				values.get(i).setValue(value);
			}
		}
		
	}
	*/
	
	public void enableHlsAuth(boolean enabled) {
		hls_ba_user.setEnabled(enabled);
		hls_ba_password.setEnabled(enabled);
	}
	
	public void enableRemux(boolean enabled) {
		remux_audio_format.setEnabled(enabled);
		remux_audio_offset.setEnabled(enabled);
		remux_muxrate.setEnabled(enabled);
	}
	
	public void build(HashMap<String, String> settings, boolean cloud, boolean forced_content, boolean hls_output, boolean portal) {
		
		datetime.setStyleName("btn blue");
		mainPanel.add(datetime);
		
		mod_button.setStyleName("btn blue");
		mainPanel.add(mod_button);
		
		pw_button.setStyleName("btn blue");
		mainPanel.add(pw_button);
		
		//this.values = new ArrayList<NameValue>(values);
		
		/*
		ArrayList<String> enabled_list = new ArrayList<String>();
		enabled_list.add("true");
		enabled_list.add("false");
		*/
		
		FlowPanel wrapperPanel = new FlowPanel();
		wrapperPanel.setStyleName("panel-wrapper");
		FlowPanel firstPanel = new FlowPanel();
		firstPanel.setStyleName("panel first");
		FlowPanel secondPanel = new FlowPanel();
		secondPanel.setStyleName("panel second");
		
		ArrayList<String> freq_list = new ArrayList<String>();
		
		for (int i = 114; i <= 858; i += 8) {
			freq_list.add(""+i);
		}
				
		ArrayList<String> qam_list = new ArrayList<String>();
		qam_list.add("QAM-64");
		qam_list.add("QAM-128");
		qam_list.add("QAM-256");
		
		ArrayList<String> attenuation_list = new ArrayList<String>();
		for (int i = 0; i <= 30; ++i) {
			attenuation_list.add(""+i);
		}
		
		ArrayList<String> network_list = new ArrayList<String>();
		//network_list.add("eth0");
		//network_list.add("eth1");
		
		
		
		for (int i = 0; i <= 9; i++) {
			
			if(!settings.containsKey("nw_eth"+i+"_onboot")) {
				break;
			}
			
			network_list.add("eth" + i);
		
		}
		
		ArrayList<String> symbol_rate_list = new ArrayList<String>();
		symbol_rate_list.add("6900000");
		symbol_rate_list.add("6875000");
		
		
		ArrayList<String> remux_format_list = new ArrayList<String>();
		remux_format_list.add("passthrough");
		remux_format_list.add("aac");
		remux_format_list.add("mp2");
		
		
		
		//NET1
		Label dvbc_label = new Label("DVB-C NET1 Output Settings:");
		dvbc_label.setStyleName("bold semi-header");
		firstPanel.add(dvbc_label);
		
		//dvbc_enabled.setList(enabled_list);
		dvbc_enabled.setToggle(Boolean.valueOf(settings.get("dvbc_enable")));
		firstPanel.add(dvbc_enabled);
		
		String freq_full = settings.get("dvbc_freq");
		String freq_temp = freq_full.substring(0, 3);
		
		dvbc_start_freq.setList(freq_list);
		dvbc_start_freq.setListValue(freq_temp);
		firstPanel.add(dvbc_start_freq);
		
		dvbc_symbol_rate.setList(symbol_rate_list);
		dvbc_symbol_rate.setListValue(settings.get("dvbc_symb"));
		firstPanel.add(dvbc_symbol_rate);
		
		dvbc_qam_method.setList(qam_list);
		dvbc_qam_method.setListValue(settings.get("dvbc_qam"));
		firstPanel.add(dvbc_qam_method);
		
		dvbc_attenuation.setList(attenuation_list);
		dvbc_attenuation.setListValue(settings.get("dvbc_attenuation"));
		firstPanel.add(dvbc_attenuation);
		
		dvbc_network_id.setValue(settings.get("dvbc_netid"));
		firstPanel.add(dvbc_network_id);
		
		dvbc_orgnetid.setValue(settings.get("dvbc_orgnetid"));
		firstPanel.add(dvbc_orgnetid);
		
		dvbc_network_name.setValue(settings.get("dvbc_netname"));
		firstPanel.add(dvbc_network_name);
		
		//NET2
		Label dvbc_net2_label = new Label("DVB-C NET2 Output Settings:");
		dvbc_net2_label.setStyleName("bold semi-header");
		secondPanel.add(dvbc_net2_label);
		
		//dvbc_enabled.setList(enabled_list);
		dvbc_net2_enabled.setToggle(Boolean.valueOf(settings.get("dvbc_net2_enable")));
		secondPanel.add(dvbc_net2_enabled);
		
		String freq_net2_full = settings.get("dvbc_net2_freq");
		String freq_net2_temp = freq_net2_full.substring(0, 3);
		
		dvbc_net2_start_freq.setList(freq_list);
		dvbc_net2_start_freq.setListValue(freq_net2_temp);
		secondPanel.add(dvbc_net2_start_freq);
		
		dvbc_net2_symbol_rate.setList(symbol_rate_list);
		dvbc_net2_symbol_rate.setListValue(settings.get("dvbc_net2_symb"));
		secondPanel.add(dvbc_net2_symbol_rate);
		
		dvbc_net2_qam_method.setList(qam_list);
		dvbc_net2_qam_method.setListValue(settings.get("dvbc_net2_qam"));
		secondPanel.add(dvbc_net2_qam_method);
		
		dvbc_net2_attenuation.setList(attenuation_list);
		dvbc_net2_attenuation.setListValue(settings.get("dvbc_net2_attenuation"));
		secondPanel.add(dvbc_net2_attenuation);
		
		dvbc_net2_network_id.setValue(settings.get("dvbc_net2_netid"));
		secondPanel.add(dvbc_net2_network_id);
		
		dvbc_net2_orgnetid.setValue(settings.get("dvbc_net2_orgnetid"));
		secondPanel.add(dvbc_net2_orgnetid);
		
		dvbc_net2_network_name.setValue(settings.get("dvbc_net2_netname"));
		secondPanel.add(dvbc_net2_network_name);
		
		
		Label ip_label = new Label("IP Output Settings:");
		ip_label.setStyleName("bold semi-header");
		firstPanel.add(ip_label);
		
		//ip_enabled.setList(enabled_list);
		ip_enabled.setToggle(Boolean.valueOf(settings.get("ip_enable")));
		firstPanel.add(ip_enabled);
		
		ip_start_addr.setValue(settings.get("ip_startaddr"));
		firstPanel.add(ip_start_addr);
		
		
		String temp = settings.get("ip_netdev");
		String[] arr = {temp, ""};

		if(temp.contains(".")) {
			arr = temp.split("\\.");
		}
		
		ip_network_interface.setList(network_list);
		ip_network_interface.setListValue(arr[0]);
		firstPanel.add(ip_network_interface);
				
		lan.setValue(arr[1]);
		firstPanel.add(lan);
		
		ip_ttl.setValue(settings.get("ip_ttl"));
		firstPanel.add(ip_ttl);
		
		ip_tos.setValue(settings.get("ip_tos"));
		firstPanel.add(ip_tos);
		
		
		Label remux_label = new Label("Remultiplexer Settings:");
		remux_label.setStyleName("bold semi-header");
		firstPanel.add(remux_label);
		
		remux_enabled.setEventType("remux_enabled");
		remux_enabled.setToggle(Boolean.valueOf(settings.get("remux_enable")));
		firstPanel.add(remux_enabled);
		
		remux_audio_format.setList(remux_format_list);
		remux_audio_format.setListValue(settings.get("remux_audio_format"));
		firstPanel.add(remux_audio_format);
		
		remux_audio_offset.setValue(settings.get("remux_audio_offset"));
		firstPanel.add(remux_audio_offset);
		
		remux_muxrate.setValue(settings.get("remux_muxrate"));
		firstPanel.add(remux_muxrate);
		
		
		
		
		
		Label hls_in_label = new Label("HLS Input Settings:");
		hls_in_label.setStyleName("bold semi-header");
		firstPanel.add(hls_in_label);
		
		GroupField hls_ba_group = new GroupField("HLS Basic Auth");
		firstPanel.add(hls_ba_group);
		
		hls_ba_enabled.setEventType("hls_ba_enabled");
		hls_ba_enabled.setToggle(Boolean.valueOf(settings.get("hls_ba_enable")));
		hls_ba_group.add(hls_ba_enabled);
		
		hls_ba_user.setValue(settings.get("hls_ba_user"));
		hls_ba_group.add(hls_ba_user);
		
		hls_ba_password.setValue(settings.get("hls_ba_passwd"));
		hls_ba_group.add(hls_ba_password);
		
		hls_max_bitrate.setValue(settings.get("hls_max_bitrate"));
		firstPanel.add(hls_max_bitrate);
		
		
		
		Label dsc_label = new Label("Descrambler Settings:");
		dsc_label.setStyleName("bold semi-header");
		firstPanel.add(dsc_label);
				
		dsc_max_services.setValue(settings.get("dsc_services"));
		firstPanel.add(dsc_max_services);
		
		dsc_max_bitrate.setValue(settings.get("dsc_bitrate"));
		firstPanel.add(dsc_max_bitrate);
		
		Label bitrate_label = new Label("Bitrate Settings:");
		bitrate_label.setStyleName("bold semi-header");
		firstPanel.add(bitrate_label);
		
		bitrate_tvsd.setValue(settings.get("bitrate_tvsd"));
		firstPanel.add(bitrate_tvsd);
		
		bitrate_tvhd.setValue(settings.get("bitrate_tvhd"));
		firstPanel.add(bitrate_tvhd);
		
		bitrate_radio.setValue(settings.get("bitrate_radio"));
		firstPanel.add(bitrate_radio);
		
		
		
		if(hls_output) {
			
			Label hls_out_label = new Label("HLS Output Settings:");
			hls_out_label.setStyleName("bold semi-header");
			firstPanel.add(hls_out_label);
			
			//hls_enabled.setList(enabled_list);
			hls_enabled.setToggle(Boolean.valueOf(settings.get("hls_enable")));
			firstPanel.add(hls_enabled);
			
			hls_server_ip.setValue(settings.get("hls_server_ip"));
			firstPanel.add(hls_server_ip);
			
			hls_inport.setValue(settings.get("hls_inport"));
			firstPanel.add(hls_inport);
			
			hls_outport.setValue(settings.get("hls_outport"));
			firstPanel.add(hls_outport);
			
			hls_services.setValue(settings.get("hls_services"));
			firstPanel.add(hls_services);
			
			hls_playback_prefix.setValue(settings.get("hls_playback_prefix"));
			firstPanel.add(hls_playback_prefix);
			
		}
		
		
		
		if(portal) {
			
			Label portal_label = new Label("Portal Settings:");
			portal_label.setStyleName("bold semi-header");
			firstPanel.add(portal_label);
			
			//portal_enabled.setList(enabled_list);
			portal_enabled.setToggle(Boolean.valueOf(settings.get("portal_enable")));
			firstPanel.add(portal_enabled);
			
			portal_server_ip.setValue(settings.get("portal_server_ip"));
			firstPanel.add(portal_server_ip);
			
			portal_url.setValue(settings.get("portal_url"));
			firstPanel.add(portal_url);
			
		}
		
		
		
		
		
		if(cloud) {
			Label cloud_label = new Label("Cloud Settings:");
			cloud_label.setStyleName("bold semi-header");
			firstPanel.add(cloud_label);
			
			cloud_enabled.setToggle(Boolean.valueOf(settings.get("ixcloud_enable")));
			firstPanel.add(cloud_enabled);
			
			cloud_url.setValue(settings.get("ixcloud_validate_url"));
			firstPanel.add(cloud_url);
		}
		
		if(forced_content) {
			
			Label forced_content_label = new Label("Force Content Settings:");
			forced_content_label.setStyleName("bold semi-header");
			firstPanel.add(forced_content_label);
			
			forced_content_enabled.setToggle(Boolean.valueOf(settings.get("forced_content_enable")));
			firstPanel.add(forced_content_enabled);
			
		}
		
		wrapperPanel.add(firstPanel);
		wrapperPanel.add(secondPanel);
		
		mainPanel.add(wrapperPanel);
		
		save_button.setStyleName("btn blue");
		mainPanel.add(save_button);
	}
	
	public HashMap<String, String> getSettings(boolean cloud, boolean forced_content, boolean hls_output, boolean portal) {
		
		HashMap<String, String> settings = new HashMap<String, String>();
		
		settings.put("dvbc_enable", String.valueOf(dvbc_enabled.isToggle()));
		settings.put("dvbc_freq", dvbc_start_freq.getValue() + "000000");
		settings.put("dvbc_symb", dvbc_symbol_rate.getValue());
		settings.put("dvbc_qam", dvbc_qam_method.getValue());
		settings.put("dvbc_attenuation", dvbc_attenuation.getValue());
		settings.put("dvbc_netid", dvbc_network_id.getValue());
		settings.put("dvbc_orgnetid", dvbc_orgnetid.getValue());
		settings.put("dvbc_netname", dvbc_network_name.getValue());
		
		settings.put("dvbc_net2_enable", String.valueOf(dvbc_net2_enabled.isToggle()));
		settings.put("dvbc_net2_freq", dvbc_net2_start_freq.getValue() + "000000");
		settings.put("dvbc_net2_symb", dvbc_net2_symbol_rate.getValue());
		settings.put("dvbc_net2_qam", dvbc_net2_qam_method.getValue());
		settings.put("dvbc_net2_attenuation", dvbc_net2_attenuation.getValue());
		settings.put("dvbc_net2_netid", dvbc_net2_network_id.getValue());
		settings.put("dvbc_net2_orgnetid", dvbc_net2_orgnetid.getValue());
		settings.put("dvbc_net2_netname", dvbc_net2_network_name.getValue());
		
		
		
		
		
		
		settings.put("ip_enable", String.valueOf(ip_enabled.isToggle()));
		settings.put("ip_startaddr", ip_start_addr.getValue());
		
		String temp = ip_network_interface.getValue();
		
		if(!lan.getValue().equals("")) {
			temp += "." + lan.getValue();
		}
		
		
		settings.put("ip_netdev", temp);
		settings.put("ip_ttl", ip_ttl.getValue());
		settings.put("ip_tos", ip_tos.getValue());
		
		settings.put("dsc_services", dsc_max_services.getValue());
		settings.put("dsc_bitrate", dsc_max_bitrate.getValue());
		
		settings.put("bitrate_tvsd", bitrate_tvsd.getValue());
		settings.put("bitrate_tvhd", bitrate_tvhd.getValue());
		settings.put("bitrate_radio", bitrate_radio.getValue());

		settings.put("remux_enable", String.valueOf(remux_enabled.isToggle()));
		settings.put("remux_audio_format", remux_audio_format.getValue());
		settings.put("remux_audio_offset", remux_audio_offset.getValue());
		settings.put("remux_muxrate", remux_muxrate.getValue());
		
		settings.put("hls_ba_enable", String.valueOf(hls_ba_enabled.isToggle()));
		settings.put("hls_ba_user", hls_ba_user.getValue());
		settings.put("hls_ba_passwd", hls_ba_password.getValue());
		settings.put("hls_max_bitrate", hls_max_bitrate.getValue());
		
		
		
		if(hls_output) {
			settings.put("hls_enable", String.valueOf(hls_enabled.isToggle()));
			settings.put("hls_server_ip", hls_server_ip.getValue());
			settings.put("hls_inport", hls_inport.getValue());
			settings.put("hls_outport", hls_outport.getValue());
			settings.put("hls_services", hls_services.getValue());
			settings.put("hls_playback_prefix", hls_playback_prefix.getValue());
		}
		
		if(portal) {
			settings.put("portal_enable", String.valueOf(portal_enabled.isToggle()));
			settings.put("portal_server_ip", portal_server_ip.getValue());
			settings.put("portal_url", portal_url.getValue());
		}
		
				
		
		
		if(cloud) {
			settings.put("ixcloud_enable", String.valueOf(cloud_enabled.isToggle()));
			settings.put("ixcloud_validate_url", cloud_url.getValue());
		}
		
		if(forced_content) {
			settings.put("forced_content_enable", String.valueOf(forced_content_enabled.isToggle()));
		}
		
		return settings;
	}
	
	public HasClickHandlers getDateTimeButton() {
		return this.datetime;
	}
	
	public HasClickHandlers getModButton() {
		return this.mod_button;
	}

	public HasClickHandlers getPwButton() {
		return this.pw_button;
	}
	
	public HasClickHandlers getSaveButton() {
		return this.save_button;
	}
		
	public Widget asWidget() {
		return this;
	}
}
