package se.ixanon.ixui.client.view;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.ListField;
import se.ixanon.ixui.client.item.table.TextField;
import se.ixanon.ixui.client.presenter.NetworkPresenter;
import se.ixanon.ixui.shared.IpMac;
import se.ixanon.ixui.shared.NameValue;

public class NetworkView extends Composite implements NetworkPresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel wrapper_top;
	private FlexTable flexTable;
	//private ArrayList<NameValue> values;
	private Button save_button = new Button("Save");
	private TextField hostname = new TextField("Hostname", true);
	private TextField gateway = new TextField("Default Gateway", true);
	private ListField multicastdev = new ListField("Multicast Route");
	private TextField dns1 = new TextField("DNS1", true);
	private TextField dns2 = new TextField("DNS2", true);
	private Button status_button = new Button("IP Status"); 
		
	public NetworkView() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("main");
		initWidget(mainPanel);
		
		flexTable = new FlexTable();
		flexTable.addStyleName("flexTable");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
	}
	
	public void buildStatus(ArrayList<IpMac> status) {
		
		wrapper_top = new FlowPanel();
		wrapper_top.setStyleName("panel-wrapper");
		
		
		FlowPanel wrapper_right = new FlowPanel();
		wrapper_right.setStyleName("wrapper-right");
		
		Label status_label = new Label("Network Status:");
		status_label.setStyleName("bold semi-header");
		wrapper_right.add(status_label);
		
		FlexTable flexTable2 = new FlexTable();
		flexTable2.addStyleName("flexTable");
		flexTable2.setWidth("100%");
		flexTable2.setCellSpacing(0);
		flexTable2.setCellPadding(5);
		
		flexTable2.removeAllRows();
		
		flexTable2.setText(0, 0, "Type");
		flexTable2.setText(0, 1, "Mac");
		flexTable2.setText(0, 2, "IP");
		
		flexTable2.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i < status.size(); i++) {
			
			flexTable2.setText(i+1, 0, "eth"+i);
			
			if(status.get(i).getMac() != null) {
				flexTable2.setText(i+1, 1, status.get(i).getMac());
			} else {
				flexTable2.setText(i+1, 1, "");
			}
			
			if(status.get(i).getIp() != null) {
				flexTable2.setText(i+1, 2, status.get(i).getIp());
			} else {
				flexTable2.setText(i+1, 2, "");
			}
			
		}
		
		wrapper_right.add(flexTable2);
		
		status_button.setStyleName("btn blue");
		wrapper_right.add(status_button);
		
		wrapper_top.add(wrapper_right);
		mainPanel.add(wrapper_top);
		
	}
	
	public void build(HashMap<String, NameValue> settings) {
		
		FlowPanel wrapper_left = new FlowPanel();
		wrapper_left.setStyleName("wrapper-left");
		
		//this.values = new ArrayList<NameValue>(values);
		
		ArrayList<String> multicastdev_list = new ArrayList<String>();
		//multicastdev_list.add("eth0");
		
		ArrayList<String> bootproto_list = new ArrayList<String>();
		bootproto_list.add("static");
		bootproto_list.add("dhcp");
		
		ArrayList<String> onboot_list = new ArrayList<String>();
		onboot_list.add("yes");
		onboot_list.add("no");
		
		Label common_label = new Label("Common Settings:");
		common_label.setStyleName("bold semi-header");
		wrapper_left.add(common_label);
		
		hostname.setValue(settings.get("nw_hostname").getValue());
		wrapper_left.add(hostname);
		
		gateway.setValue(settings.get("nw_gateway").getValue());
		wrapper_left.add(gateway);
		
		//moved set down
		wrapper_left.add(multicastdev);
		
		dns1.setValue(settings.get("nw_dns1").getValue());
		wrapper_left.add(dns1);
		
		dns2.setValue(settings.get("nw_dns2").getValue());
		wrapper_left.add(dns2);
		
		wrapper_top.add(wrapper_left);
		
		Label device_label = new Label("Device Settings:");
		device_label.setStyleName("bold semi-header");
		mainPanel.add(device_label);
		
		flexTable.removeAllRows();
		
		flexTable.setText(0, 0, "Type");
		flexTable.setText(0, 1, "Protocol");
		flexTable.setText(0, 2, "Onboot");
		flexTable.setText(0, 3, "IP");
		flexTable.setText(0, 4, "Netmask");
		flexTable.setText(0, 5, "Mac");
				
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i <= 9; i++) {
			
			if(!settings.containsKey("nw_eth"+i+"_onboot")) {
				break;
			}
			
			multicastdev_list.add("eth" + i);
			
			flexTable.setText(i+1, 0, "eth" + i);
			
			ListField bootproto = new ListField(null);
			bootproto.setList(bootproto_list);
			bootproto.setListValue(settings.get("nw_eth"+i+"_bootproto").getValue());
			flexTable.setWidget(i+1, 1, bootproto);
			
			ListField onboot = new ListField(null);
			onboot.setList(onboot_list);
			onboot.setListValue(settings.get("nw_eth"+i+"_onboot").getValue());
			flexTable.setWidget(i+1, 2, onboot);
			
			TextField ip = new TextField(null, true);
			ip.setValue(settings.get("nw_eth"+i+"_ipaddr").getValue());
			flexTable.setWidget(i+1, 3, ip);
			
			TextField netmask = new TextField(null, true);
			netmask.setValue(settings.get("nw_eth"+i+"_netmask").getValue());
			flexTable.setWidget(i+1, 4, netmask);
			
			flexTable.setText(i+1, 5, settings.get("nw_eth"+i+"_mac").getValue());
			
		}
		
		multicastdev.setList(multicastdev_list);
		multicastdev.setListValue(settings.get("nw_multicastdev").getValue());
		
		mainPanel.add(flexTable);
		
		save_button.setStyleName("btn blue");
		mainPanel.add(save_button);
	}
	
	private NameValue updateNV(HashMap<String, NameValue> settings, String name, String value) {
		
		if(settings.containsKey(name)) {
			NameValue nv = settings.get(name);
			nv.setValue(value);
			return nv;
		} else {
			NameValue nv = new NameValue(0, name, value);
			return nv;
		}
		
		
		
	}
	
	public HashMap<String, NameValue> getSettingsValues(HashMap<String, NameValue> settings) {
		
		settings.put("nw_multicastdev", updateNV(settings, "nw_multicastdev", multicastdev.getValue()));
		settings.put("nw_gateway", updateNV(settings, "nw_gateway", gateway.getValue()));
		settings.put("nw_dns1", updateNV(settings, "nw_dns1", dns1.getValue()));
		settings.put("nw_dns2", updateNV(settings, "nw_dns2", dns2.getValue()));
		settings.put("nw_hostname", updateNV(settings, "nw_hostname", hostname.getValue()));
		
		for (int i = 0; i < flexTable.getRowCount()-1; i++) {
			
			ListField bootproto = (ListField) flexTable.getWidget(i+1, 1);
			settings.put("nw_eth"+i+"_bootproto", updateNV(settings, "nw_eth"+i+"_bootproto", bootproto.getValue()));
			
			ListField onboot = (ListField) flexTable.getWidget(i+1, 2);
			settings.put("nw_eth"+i+"_onboot", updateNV(settings, "nw_eth"+i+"_onboot", onboot.getValue()));
						
			TextField ip = (TextField) flexTable.getWidget(i+1, 3);
			settings.put("nw_eth"+i+"_ipaddr", updateNV(settings, "nw_eth"+i+"_ipaddr", ip.getValue()));
						
			TextField netmask = (TextField) flexTable.getWidget(i+1, 4);
			settings.put("nw_eth"+i+"_netmask", updateNV(settings, "nw_eth"+i+"_netmask", netmask.getValue()));
						
			//TextField mac = (TextField) flexTable.getWidget(i+1, 5);
			//setValue("nw_eth"+i+"_mac", mac.getValue());
		}
		
		return settings;
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
	*/
	/*
	private void setValue(String name, String value) {
		
		for (int i = 0; i < values.size(); ++i) {
			if(name.equals(values.get(i).getName())) {
				values.get(i).setValue(value);
			}
		}
		
	}
	*/
	public HasClickHandlers getStatusButton() {
		return this.status_button;
	}
	
	
	public HasClickHandlers getSaveButton() {
		return this.save_button;
	}
	
	public Widget asWidget() {
		return this;
	}
}
