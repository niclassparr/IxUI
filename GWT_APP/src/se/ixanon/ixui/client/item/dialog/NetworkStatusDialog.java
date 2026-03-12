package se.ixanon.ixui.client.item.dialog;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.shared.IpStatus;

public class NetworkStatusDialog extends Composite {
	
	private FlowPanel mainPanel; 
	private FlexTable flexTable;
		
	public NetworkStatusDialog() {
				
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog");
		wrapperPanel.add(mainPanel);
						
		Label headerLabel = new Label("Network Status");
		HTML textLabel = new HTML("This might take a while.<br>Please wait...");
		
		HTML html2 = new HTML("<i class='fa fa-circle-o-notch fa-spin fa-3x fa-fw'></i>");
		html2.setStyleName("icon header dark");
		
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(html2);	
		
		getStatus();
		
		initWidget(wrapperPanel);
		
		
	}
	
	private void addRow(String type, IpStatus status, int row) {
		flexTable.setText(row, 0, type);
		flexTable.setText(row, 1, status.getIp());
		
		if(status.isStatus()) {
			flexTable.setWidget(row, 2, new HTML("<i class='fa fa-check' aria-hidden='true'></i>"));
		} else {
			flexTable.setWidget(row, 2, new HTML("<i class='fa fa-times' aria-hidden='true'></i>"));
		}
	}
	
	private void getStatus() {
		
		
		Session.getInstance().getRpcService().getNetworkStatus2(Session.getInstance().getSessionKey(), new AsyncCallback<HashMap<String, IpStatus>>() {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(HashMap<String, IpStatus> result) {
				
				mainPanel.clear();
				
				Label headerLabel = new Label("Network Status");
				headerLabel.setStyleName("header");
				mainPanel.add(headerLabel);
				
				FlowPanel mainPanelInner = new FlowPanel();
				mainPanelInner.setStyleName("dialog-inner");
				mainPanel.add(mainPanelInner);
				
				
				flexTable = new FlexTable();
				flexTable.addStyleName("flexTable");
				flexTable.setWidth("100%");
				flexTable.setCellSpacing(0);
				flexTable.setCellPadding(5);
								
				flexTable.setText(0, 0, "Type");
				flexTable.setText(0, 1, "IP");
				flexTable.setText(0, 2, "Status");
				
				flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
				
				
				addRow("Gateway", result.get("gateway"), 1);
				addRow("DNS1", result.get("dns1"), 2);
				addRow("DNS2", result.get("dns2"), 3);
				addRow("Public IP", result.get("public"), 4);
				
				mainPanelInner.add(flexTable);
				
				Button okButton = new Button("Close");
				okButton.setStyleName("btn blue login");
				mainPanel.add(okButton);
				
				okButton.addClickHandler(new ClickHandler() {

					@Override
					public void onClick(ClickEvent event) {
						RootPanel.get("overlay").clear();
						RootPanel.get("overlay").setVisible(false);		
					}
					
				});
				
				
			}

		});
	}
}
