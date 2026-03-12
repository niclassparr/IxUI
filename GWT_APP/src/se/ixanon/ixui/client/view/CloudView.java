package se.ixanon.ixui.client.view;

import java.util.HashMap;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.EventButton;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.presenter.CloudPresenter;

public class CloudView extends Composite implements CloudPresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel buttonPanel;
	private EventButton connect = new EventButton("Connect", "play-circle", "green");
	private EventButton disconnect = new EventButton("Disconnect", "stop-circle", "red");
	private FlexTable flexTable;
	
	public CloudView() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("main");
		initWidget(mainPanel);
		
		flexTable = new FlexTable();
		flexTable.addStyleName("flexTable");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
		
		buttonPanel = new FlowPanel();
		
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
		mainPanel.add(flexTable);
		mainPanel.add(buttonPanel);
	}
	
	public void buildTable(HashMap<String, String> details) {
		
		flexTable.removeAllRows();
		
		if(details.get("ixcloud_enable").equals("false")) {
			flexTable.setText(0, 0, "The cloud function is disabled.");
		} else {
			flexTable.setText(0, 0, "Connection details:");
			flexTable.setText(0, 1, "");
			
			String status = "Offline";
			if(details.get("ixcloud_online").equals("true")) {
				status = "Online";
			}
			
			flexTable.setText(1, 0, "Status");
			flexTable.setText(1, 1, status);
			
			flexTable.setText(2, 0, "Date");
			flexTable.setText(2, 1, details.get("ixcloud_validate_date"));
			
			flexTable.setText(3, 0, "Message");
			flexTable.setText(3, 1, details.get("ixcloud_validate_message"));
			
			flexTable.setText(4, 0, "BeaconId");
			flexTable.setText(4, 1, details.get("ixcloud_beaconid"));
			
		}
		
	}
	
	public void addButtons(boolean isCloud) {
		
		buttonPanel.clear();
		
		if(isCloud) {
			buttonPanel.add(connect);
			buttonPanel.add(disconnect);
		}
		
		
	}
	
	public Widget asWidget() {
		return this;
	}
}
