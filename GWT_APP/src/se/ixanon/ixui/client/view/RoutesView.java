package se.ixanon.ixui.client.view;

import java.util.ArrayList;
import java.util.Collections;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.AlphanumComparator;
import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.item.menu.Link;
import se.ixanon.ixui.client.item.table.CheckField;
import se.ixanon.ixui.client.item.table.EditLink;
import se.ixanon.ixui.client.item.table.EventButton;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.ListField;
import se.ixanon.ixui.client.item.table.ReorderColumn;
import se.ixanon.ixui.client.item.table.TextField;
import se.ixanon.ixui.client.item.table.UsageItem;
import se.ixanon.ixui.client.presenter.RoutesPresenter;
import se.ixanon.ixui.shared.Bitrate;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.Route;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.Usage;

public class RoutesView extends Composite implements RoutesPresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel flexTable_wrapper;
	private ArrayList<String> descramblers_list = new ArrayList<String>();
	private ArrayList<String> modulators_list_net1 = new ArrayList<String>();
	private ArrayList<String> modulators_list_net2 = new ArrayList<String>();
	//private ArrayList<Interface> interfaces;
	private ArrayList<Route> routes;
	private ArrayList<Usage> usages = new ArrayList<Usage>();
	private FlexTable flexTable = new FlexTable();
	//private Button hls_button = new Button("HLS Wizard");
	private Button save_button = new Button("Save");
	
	public RoutesView() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("main");
		initWidget(mainPanel);
	}
	
	public void setHeader(Header header, boolean isHls) {
		
		FlowPanel wrapper = new FlowPanel();
		wrapper.setStyleName("wrapper-routes-header");
		
		
		if(isHls) {
			EventButton hls_button = new EventButton("HLS Wizard", "magic", "blue");
			wrapper.add(hls_button);
		}
		
		wrapper.add(header);
		mainPanel.add(wrapper);
		mainPanel.add(flexTable);
		
		flexTable_wrapper = new FlowPanel();
		mainPanel.add(flexTable_wrapper);
		
		save_button.setStyleName("btn blue");
		mainPanel.add(save_button);
	}
	
	public void buildLists(ArrayList<Interface> interfaces) {
		
		//this.interfaces = new ArrayList<Interface>(interfaces);
		
		for (int i = 0; i < interfaces.size(); i++) {
			
			if(interfaces.get(i).getType().equals("dsc")) {
				descramblers_list.add(interfaces.get(i).getPosition());
			}
			
			if(interfaces.get(i).getType().equals("mod")) {
				
				if(interfaces.get(i).getNetworkNum() == 1) {
					modulators_list_net1.add(interfaces.get(i).getPosition());
				}
				
				if(interfaces.get(i).getNetworkNum() == 2) {
					modulators_list_net2.add(interfaces.get(i).getPosition());
				}
			}
			
		}
		
		Collections.sort(descramblers_list, new AlphanumComparator());
		Collections.sort(modulators_list_net1, new AlphanumComparator());
		Collections.sort(modulators_list_net2, new AlphanumComparator());
		
		descramblers_list.add(0, "None");
		modulators_list_net1.add(0, "None");
		modulators_list_net2.add(0, "None");
		
	}
	
	public ArrayList<String> checkDuplicateTextFieldValue() {
		
		ArrayList<String> duplicates = new ArrayList<String>();
		
		int column_out_sid = -1;
		int column_out_ip = -1;
		
		for (int i = 0; i < flexTable.getCellCount(0); i++) {
			
			ReorderColumn reorder_column = (ReorderColumn) flexTable.getWidget(0, i);
			
			if(reorder_column.getName() != null && reorder_column.getName().equals("Out SID")) {
				column_out_sid = i;
			}
			
			if(reorder_column.getName() != null && reorder_column.getName().equals("Out IP")) {
				column_out_ip = i;
			}
			
		}
		
		ArrayList<String> out_sids = new ArrayList<String>();
		ArrayList<String> out_ips = new ArrayList<String>();
		
		for (int i = 1; i < flexTable.getRowCount(); i++) {
			
			if(column_out_sid != -1) {
				TextField out_sid = (TextField) flexTable.getWidget(i, column_out_sid);
				
				if(!out_sid.getValue().equals("")) {
					out_sids.add(out_sid.getValue());
				}
				
			}
			
			if(column_out_ip != -1) {
				TextField out_ip = (TextField) flexTable.getWidget(i, column_out_ip);
				
				if(!out_ip.getValue().equals("")) {
					out_ips.add(out_ip.getValue());
				}
				
			}
			
		}
		
		for (int i = 1; i < flexTable.getRowCount(); i++) {
			
			if(column_out_sid != -1) {
				TextField out_sid = (TextField) flexTable.getWidget(i, column_out_sid);
				
				if(Helper.findDuplicates(out_sids).contains(out_sid.getValue())) {
					
					String duplicate = "Duplicate service with SID: " + out_sid.getValue();
					
					if(!duplicates.contains(duplicate)) {
						duplicates.add(duplicate);
					}
					
					out_sid.setHighlighted(true);
				} else {
					out_sid.setHighlighted(false);
				}
				
			}
			
			if(column_out_ip != -1) {
				TextField out_ip = (TextField) flexTable.getWidget(i, column_out_ip);
				
				if(Helper.findDuplicates(out_ips).contains(out_ip.getValue())) {
					
					String duplicate = "Duplicate service with IP: " + out_ip.getValue();
					
					if(!duplicates.contains(duplicate)) {
						duplicates.add(duplicate);
					}
					
					out_ip.setHighlighted(true);
				} else {
					out_ip.setHighlighted(false);
				}
				
				
			}
			
		}
				
		return duplicates;
		
	}
	
	public void buildTable(ArrayList<Route> routes, boolean dvbc, boolean ip, boolean hls, boolean portal, boolean dvbc_net2) {
		
		this.routes = new ArrayList<Route>(routes);
		
		if(flexTable.getRowCount() > 2) {
			for (int i = flexTable.getRowCount()-1; i >= 1; i--) {
				flexTable.removeRow(i);
			}
		}
		
		if(flexTable.getRowCount() == 0) {
			flexTable.addStyleName("flexTable layout"); //dvbc-" + dvbc + " ip-" + ip + "dvbc-net2-" + dvbc_net2 
			flexTable.setWidth("100%");
			flexTable.setCellSpacing(0);
			flexTable.setCellPadding(5);
			
			flexTable.setWidget(0, 0, new ReorderColumn("Service", "routes"));
			flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Interface", "routes"));
			flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("LCN", "routes"));
			flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Descrambler", "routes"));
			
			boolean isdvbc = false;
			
			if(dvbc && !dvbc_net2) {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Mod Net 1", "routes"));
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
				
				isdvbc = true;
			}
			
			if(!dvbc && dvbc_net2) {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
				//flexTable.setText(0, flexTable.getCellCount(0), "Mod Net 2");
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Mod Net 2", "routes"));
				
				isdvbc = true;
			}
			
			if(dvbc && dvbc_net2) {
				//flexTable.setText(0, flexTable.getCellCount(0), "Mod Net 1");
				//flexTable.setText(0, flexTable.getCellCount(0), "Mod Net 2");
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Mod Net 1", "routes"));
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Mod Net 2", "routes"));
				
				isdvbc = true;
			}
			
			if(isdvbc) {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Out SID", "routes"));
			} else {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
			}
			
			
			if(ip) {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Out IP", "routes"));
			} else {
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn(null, null));
			}
			
			//flexTable.setText(0, flexTable.getCellCount(0), "Epg");
			flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Epg", "routes"));
			
			if(hls) {
				//flexTable.setText(0, flexTable.getCellCount(0), "Hls");
				flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Hls", "routes"));
			}
			
			
			flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		}
		
		for (int i = 0; i < routes.size(); i++) {
			
			String temp_lock = "unlock";
			
			if(routes.get(i).isScrambled()) {
				temp_lock = "lock";
			}
			
			TextField output_name = new TextField(null, true);
			output_name.setValue(routes.get(i).getOutputName());
			output_name.addTitle(routes.get(i).getServiceName());
			output_name.addIcon(temp_lock);
			
			flexTable.setWidget(i+1, 0, output_name);
			//flexTable.setWidget(i+1, 0, new IconText(routes.get(i).getServiceName(), temp_lock));
			
			
			
			SessionKeys session_keys = new SessionKeys("interface-edit");
			session_keys.getKeys().put(SessionKeys.Type.INTERFACE_POS, routes.get(i).getInterfacePos());
			session_keys.getKeys().put(SessionKeys.Type.INTERFACE_TYPE, routes.get(i).getInterfaceType());
			session_keys.getKeys().put(SessionKeys.Type.MULTIBAND, ""+routes.get(i).isInterfaceMultiband());
			
			Link config_link = new Link(routes.get(i).getInterfacePos(), "cogs", "table", true, session_keys);
							
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), config_link);
			
			
			
			//flexTable.setWidget(i+1, flexTable.getCellCount(i+1), new EditLink(routes.get(i).getInterfacePos(), "interface-edit/id=" + routes.get(i).getInterfacePos() + "/id2=" + routes.get(i).getInterfaceType(), "cogs"));
			
			TextField lcn = new TextField(null, false);
			lcn.setValue(""+routes.get(i).getLcn());
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), lcn);
			
			ListField dsc = new ListField(null);
			dsc.setList(descramblers_list);
			dsc.setListValue(routes.get(i).getDescramblerPos());
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), dsc);
			
			boolean isdvbc = false;
			
			
			ListField mod = new ListField(null);
			mod.setList(modulators_list_net1);
			mod.setListValue(routes.get(i).getModulatorPos());
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), mod);
			
			if(dvbc) {	
				isdvbc = true;
			} else {
				mod.hide();
			}
			
			ListField mod2 = new ListField(null);
			mod2.setList(modulators_list_net2);
			mod2.setListValue(routes.get(i).getModulatorPosNet2());
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), mod2);
			
			if(dvbc_net2) {
				isdvbc = true;
			} else {
				mod2.hide();
			}
			
			TextField out_sid = new TextField(null, false);
			out_sid.setValue(""+routes.get(i).getOutSid());
			out_sid.addBlurEvent("out_sid");
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), out_sid);
			
			if(!isdvbc) {
				out_sid.hide();
			}
			
			TextField out_ip = new TextField(null, true);
			out_ip.setValue(routes.get(i).getOutIp());
			out_ip.addBlurEvent("out_ip");
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), out_ip);
			
			if(!ip) {
				out_ip.hide();
			}
			
			
			
			String epg_value = routes.get(i).getEpgUrl();
			
			if(epg_value == null || epg_value.equals("null")) {
				epg_value = "";
			}
			
			TextField epg_url = new TextField(null, true);
			epg_url.setValue(epg_value);
			flexTable.setWidget(i+1, flexTable.getCellCount(i+1), epg_url);
			
			if(hls) {
				CheckField hls_enable = new CheckField(routes.get(i).isHls(), null);
				flexTable.setWidget(i+1, flexTable.getCellCount(i+1), hls_enable);
			}
			
		}
	}
	
	public void buildTable2(ArrayList<Bitrate> bitrates, int max_mod, int max_dsc, int max_hls, int count_hls, boolean dvbc, boolean dvbc_net2, boolean hls) {
		
		flexTable_wrapper.clear();
		
		usages.clear();
		
		//dsc
		for (int i = 0; i < routes.size(); i++) {
			
			//dsc
			ListField dsc = (ListField) flexTable.getWidget(i+1, 3);
			Usage usage_dsc = getUsage(dsc.getValue());
			
			if(usage_dsc == null) {
				if(!dsc.getValue().equals("None")) {
					usage_dsc = new Usage("Descrambler", dsc.getValue());
					usages.add(usage_dsc);	
				}
			}
			
			if(usage_dsc != null) {
				usage_dsc.addServiceCount();
				usage_dsc.addBitrateCount(getBitrate(routes.get(i).getServiceType(), bitrates));
			}
			
			if(dvbc) {
				//mod
				ListField mod = (ListField) flexTable.getWidget(i+1, 4);
				Usage usage_mod = getUsage(mod.getValue());
				
				if(usage_mod == null) {
					if(!mod.getValue().equals("None")) {
						usage_mod = new Usage("Modulator", mod.getValue());
						usages.add(usage_mod);	
					}
				}
				
				if(usage_mod != null) {
					usage_mod.addServiceCount();
					usage_mod.addBitrateCount(getBitrate(routes.get(i).getServiceType(), bitrates));
				}
			}
			
			if(dvbc_net2) {
				//mod
				ListField mod = (ListField) flexTable.getWidget(i+1, 5);
				Usage usage_mod = getUsage(mod.getValue());
				
				if(usage_mod == null) {
					if(!mod.getValue().equals("None")) {
						usage_mod = new Usage("Modulator", mod.getValue());
						usages.add(usage_mod);	
					}
				}
				
				if(usage_mod != null) {
					usage_mod.addServiceCount();
					usage_mod.addBitrateCount(getBitrate(routes.get(i).getServiceType(), bitrates));
				}
			}
			
			
		}
		
		if(hls) {
			//HLS
			Usage usage_hls = new Usage("Hls", "-");
			
			usage_hls.setCountServices(count_hls);
			
			usages.add(usage_hls);
		}
		
		
		
		Collections.sort(usages, Usage.Comparators.INTERFACE_POS);
		
		FlexTable flexTable = new FlexTable();
		flexTable.addStyleName("flexTable");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
		
		flexTable.clear();
		
		flexTable.setText(0, 0, "Type");
		flexTable.setText(0, 1, "Interface");
		flexTable.setText(0, 2, "Services");
		flexTable.setText(0, 3, "Calculated Bitrate");
		flexTable.setText(0, 4, "Calculated Usage");
		
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i < usages.size(); i++) {
			flexTable.setText(i+1, 0, usages.get(i).getType());
			flexTable.setText(i+1, 1, usages.get(i).getInterfacePos());
			flexTable.setText(i+1, 2, ""+usages.get(i).getCountServices());
			
			if(usages.get(i).getType().equals("Hls")) {
				flexTable.setText(i+1, 3, "-");
			} else {
				flexTable.setText(i+1, 3, formatBitrate(usages.get(i).getCountBitrate()));
			}
			
			
			if(usages.get(i).getType().equals("Hls")) {
				flexTable.setWidget(i+1, 4, new UsageItem(usages.get(i).getCountServices(), max_hls));
			}
			
			if(usages.get(i).getType().equals("Descrambler")) {
				flexTable.setWidget(i+1, 4, new UsageItem(usages.get(i).getCountBitrate(), max_dsc));
			}
			
			if(usages.get(i).getType().equals("Modulator")) {
				flexTable.setWidget(i+1, 4, new UsageItem(usages.get(i).getCountBitrate(), max_mod));
			}
			
		}
		
		flexTable_wrapper.add(flexTable);
	}
	
	public ArrayList<Route> getRoutes(boolean dvbc, boolean dvbc_net2, boolean hls, boolean portal, boolean is_ip) {
		
		ArrayList<Route> save_routes = new ArrayList<Route>();
		
		for (int i = 0; i < routes.size(); i++) {
			
			int col = 2;
			
			boolean scrambled = routes.get(i).isScrambled();
			String service_name = routes.get(i).getServiceName();
			
			int id = routes.get(i).getId();
			int service_id = routes.get(i).getServiceId();
			String service_type = routes.get(i).getServiceType();
			
			//String service_name = flexTable.getText(i+1, 0);
			TextField name = (TextField) flexTable.getWidget(i+1, 0);
			String output_name = name.getValue();
			
			
			String interface_pos = routes.get(i).getInterfacePos();
			String interface_type = routes.get(i).getInterfaceType();
			boolean interface_multiband = routes.get(i).isInterfaceMultiband();
			
			TextField lcn_widget = (TextField) flexTable.getWidget(i+1, col);
			int lcn = Integer.parseInt(lcn_widget.getValue()); 
			col++;
						
			ListField dsc = (ListField) flexTable.getWidget(i+1, col);
			String descrambler_pos = dsc.getValue();
			col++;

			ListField mod = (ListField) flexTable.getWidget(i+1, col);
			String modulator_pos = mod.getValue();
			col++;
			
			ListField mod2 = (ListField) flexTable.getWidget(i+1, col);
			String modulator_pos_net2 = mod2.getValue();
			col++;
			
			TextField sid = (TextField) flexTable.getWidget(i+1, col);
			int out_sid = Integer.parseInt(sid.getValue());
			col++;
			
			TextField ip = (TextField) flexTable.getWidget(i+1, col);
			String out_ip = ip.getValue();
			col++;
			
			TextField epg = (TextField) flexTable.getWidget(i+1, col);
			String epg_url = epg.getValue();
			col++;
			
			boolean isHls = false;
			if(hls) {
				
				int last_cell = flexTable.getCellCount(1)-1;
				
				CheckField hls_enable = (CheckField) flexTable.getWidget(i+1, last_cell);
				isHls = hls_enable.getValue();
			}
			
			save_routes.add(new Route(id, service_id, service_name, service_type, interface_pos, interface_type, interface_multiband, lcn, descrambler_pos, modulator_pos, modulator_pos_net2, out_sid, out_ip, scrambled, output_name, epg_url, isHls));
		}
		
		return save_routes;
	}
	
	public int countHls(boolean hls, boolean portal) {
		
		int count = 0;
		
		if(hls) {
			
			int last_cell = flexTable.getCellCount(1)-1;
			
			for (int i = 1; i < flexTable.getRowCount(); i++) {
				
				CheckField hls_enable = (CheckField) flexTable.getWidget(i, last_cell);
				
				if(hls_enable.getValue()) {
					count++;
				}
			
			}
			
		}
		
		return count;
		
	}
	
	public void setHlsActive(boolean active, boolean hls, boolean portal) {
		
		if(hls) {
			
			int last_cell = flexTable.getCellCount(1)-1;
			
			for (int i = 1; i < flexTable.getRowCount(); i++) {
				CheckField hls_enable = (CheckField) flexTable.getWidget(i, last_cell);
				
				if(!hls_enable.getValue()) {
					hls_enable.setActive(active);
				}
			}
		}	
		
		
	}
	
	private int getBitrate(String service_type, ArrayList<Bitrate> bitrates) {
		
		String temp = service_type.replaceAll("_", "").toLowerCase();
		temp = "bitrate_" + temp;
		
		for (int i = 0; i < bitrates.size(); i++) {
			if(temp.equals(bitrates.get(i).getType())) {
				return bitrates.get(i).getBitrate();
			}
		}
		
		return 0;
	}
	
	private Usage getUsage(String interface_pos) {
		for (int i = 0; i < usages.size(); i++) {
			if(interface_pos.equals(usages.get(i).getInterfacePos())) {
				return usages.get(i);
			}
		}
		
		return null;
	}
		
	public HasClickHandlers getSaveButton() {
		return this.save_button;
	}
	
	private String formatBitrate(int value) {
		
		double d = value * 0.000001;
		String formatted = NumberFormat.getFormat("#.00").format(d);
		
		return formatted + " Mbps";
	}
	
	public Widget asWidget() {
		return this;
	}
}
