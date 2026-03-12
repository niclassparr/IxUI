package se.ixanon.ixui.client.view;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.item.menu.Link;
import se.ixanon.ixui.client.item.table.FilterMenu;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.ReorderColumn;
import se.ixanon.ixui.client.presenter.InterfacesPresenter;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.SessionKeys;

public class InterfacesView extends Composite implements InterfacesPresenter.Display {

	private FlowPanel mainPanel;
	private FilterMenu filterMenu = new FilterMenu();
	private FlexTable flexTable;
	
	public InterfacesView() {
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
	
	public void buildFilterMenu(ArrayList<String> types) {
		
		ArrayList<String> filter_list = new ArrayList<String>();
		
		filter_list.add("All");
		
		for (int i = 0; i < types.size(); i++) {
			filter_list.add(Helper.translate(types.get(i)));
		}
				
		filterMenu.setItems(filter_list);
		filterMenu.setActive("All");
		
		mainPanel.add(filterMenu);
		
	}
	
	public void buildTable(ArrayList<Interface> interfaces) {
		
		flexTable.removeAllRows();
		
		flexTable.setWidget(0, 0, new ReorderColumn("Position", "interfaces"));
		flexTable.setWidget(0, 1, new ReorderColumn("Type", "interfaces"));
		flexTable.setWidget(0, 2, new ReorderColumn("Name", "interfaces"));
		flexTable.setWidget(0, 3, new ReorderColumn("Status", "interfaces"));
		//flexTable.setWidget(0, 3, new ReorderColumn("Active"));
		flexTable.setText(0, 4, "Config");
		flexTable.setText(0, 5, "Log");
		
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i < interfaces.size(); i++) {
			
			String icon2 = null;
			if(interfaces.get(i).getActive()) {
				icon2 = "play";
			}
			
			SessionKeys session_keys1 = new SessionKeys("interface-status");
			session_keys1.getKeys().put(SessionKeys.Type.INTERFACE_POS, interfaces.get(i).getPosition());
			session_keys1.getKeys().put(SessionKeys.Type.INTERFACE_TYPE, interfaces.get(i).getType());
			
			Link status_link = new Link(interfaces.get(i).getPosition(), "info-circle", "table", true, session_keys1);
			
			boolean emm = interfaces.get(i).isEmm();
			
			if(interfaces.get(i).getType().equals("dsc")) {
				emm = false;
			}
			
			status_link.addIcon(icon2, emm);
			
			flexTable.setWidget(i+1, 0, status_link);
			//flexTable.setWidget(i+1, 0, new EditLink(interfaces.get(i).getPosition(), "interface-status/id=" + interfaces.get(i).getPosition() + "/id2=" + interfaces.get(i).getType(), "info-circle", icon2, interfaces.get(i).isEmm()));
			
			String multi = "";
			if(interfaces.get(i).isMultiBand()) {
				multi = "M / ";
			}
			
			flexTable.setText(i+1, 1, multi + Helper.translate(interfaces.get(i).getType()));
			flexTable.setText(i+1, 2, interfaces.get(i).getName());
			flexTable.setText(i+1, 3, interfaces.get(i).getStatus());
			//flexTable.setText(i+1, 3, ""+interfaces.get(i).getActive());
			
			if(interfaces.get(i).getType().equals("dvbudp") || interfaces.get(i).getType().equals("dvbs") || interfaces.get(i).getType().equals("dvbt") || interfaces.get(i).getType().equals("dvbc") || interfaces.get(i).getType().equals("dsc") || interfaces.get(i).getType().equals("infostreamer") || interfaces.get(i).getType().equals("dvbhdmi") || interfaces.get(i).getType().equals("hdmi2ip") || interfaces.get(i).getType().equals("hls2ip") || interfaces.get(i).getType().equals("webradio") || interfaces.get(i).getType().equals("infoch")) {
				
				
				SessionKeys session_keys2 = new SessionKeys("interface-edit");
				session_keys2.getKeys().put(SessionKeys.Type.INTERFACE_POS, interfaces.get(i).getPosition());
				session_keys2.getKeys().put(SessionKeys.Type.INTERFACE_TYPE, interfaces.get(i).getType());
				session_keys2.getKeys().put(SessionKeys.Type.MULTIBAND, ""+interfaces.get(i).isMultiBand());
				
				Link config_link = new Link("Config", "cogs", "table", true, session_keys2);
								
				flexTable.setWidget(i+1, 4, config_link);
				
				
				//flexTable.setWidget(i+1, 4, new EditLink("Config", "interface-edit/id=" + interfaces.get(i).getPosition() + "/id2=" + interfaces.get(i).getType() + "/id3=" + interfaces.get(i).isMultiBand(), "cogs"));
			} else {
				flexTable.setText(i+1, 4, "");
			}
			
			
			SessionKeys session_keys3 = new SessionKeys("interface-log");
			session_keys3.getKeys().put(SessionKeys.Type.INTERFACE_POS, interfaces.get(i).getPosition());
						
			Link log_link = new Link("Log", "file-text-o", "table", true, session_keys3);
							
			flexTable.setWidget(i+1, 5, log_link);
			
			
			
			//flexTable.setWidget(i+1, 5, new EditLink("Log", "interface-log/id=" + interfaces.get(i).getPosition(), "file-text-o"));
			
			if(interfaces.get(i).getStatus().equals("error")) {
				flexTable.getRowFormatter().addStyleName(flexTable.getRowCount()-1, "red-error");
			}
			
		}
			
			
			
			
		
		
		mainPanel.add(flexTable);
	}
	
	public FilterMenu getFilterMenu() {
		return filterMenu;
	}
	
	public Widget asWidget() {
		return this;
	}
}
