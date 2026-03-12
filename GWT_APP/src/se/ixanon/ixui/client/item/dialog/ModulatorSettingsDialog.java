package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.AlphanumComparator;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.PopupClosedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ModulatorSettingsDialog extends Composite {
	
	private FlowPanel mainPanelInner = new FlowPanel();
	private FlexTable flexTable;
	
	public ModulatorSettingsDialog() {
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		FlowPanel mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog mod-settings");
		wrapperPanel.add(mainPanel);
		
		mainPanelInner.setStyleName("dialog-inner");
		
		HTML html = new HTML("<i class='fa fa-sliders' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Mod Net Settings");
				
		headerLabel.setStyleName("header");
				
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(mainPanelInner);	
		
		initWidget(wrapperPanel);
		
		Button okButton = new Button("OK");
		okButton.setStyleName("btn blue login");
		mainPanel.add(okButton);
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				save();				
			}
			
		});
		
		Button cancelButton = new Button("Cancel");
		cancelButton.setStyleName("btn orange login");
		mainPanel.add(cancelButton);
		
		cancelButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
		
		getMods();
		
	}
	
	private HashMap<String, Integer> getModsUI() {
		
		HashMap<String, Integer> mods = new HashMap<String, Integer>();
		
		for (int i = 1; i < flexTable.getRowCount(); ++i) {
			
			Label label = (Label) flexTable.getWidget(i, 0);
			
			for (int j = 1; j < flexTable.getCellCount(i); ++j) {
				
				RadioButton rb = (RadioButton) flexTable.getWidget(i, j);
				
				if(rb.getValue()) {
					mods.put(label.getText(), j-1);
				}
				
			}
		}
		
		return mods;
	}
		
	private void save() {
		
		Session.getInstance().getRpcService().saveModulatorsConfig(getModsUI(), new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Void result) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
	}
	
	private void getMods() {
		
		Session.getInstance().getRpcService().getModulators(Session.getInstance().getSessionKey(), new AsyncCallback<HashMap<String, Integer>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(HashMap<String, Integer> result) {
				
				flexTable = new FlexTable();
				flexTable.addStyleName("flexTable");
				flexTable.setWidth("100%");
				flexTable.setCellSpacing(0);
				flexTable.setCellPadding(5);
								
				flexTable.setText(0, 0, "Interface");
				flexTable.setText(0, 1, "None");
				flexTable.setText(0, 2, "Net 1");
				flexTable.setText(0, 3, "Net 2");
				
				flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
				
				ArrayList<String> list = new ArrayList<String>();
				
				for (Map.Entry<String, Integer> mod : result.entrySet()) {
					list.add(mod.getKey());
				}
				
				Collections.sort(list, new AlphanumComparator());
				
				for (int i = 0; i < list.size(); ++i) {
					
					int row = flexTable.getRowCount();
					
					flexTable.setWidget(row, 0, new Label(list.get(i)));
										
					flexTable.setWidget(row, 1, new RadioButton("group-"+flexTable.getRowCount()));
					flexTable.setWidget(row, 2, new RadioButton("group-"+flexTable.getRowCount()));
					flexTable.setWidget(row, 3, new RadioButton("group-"+flexTable.getRowCount()));
					
					
					
					RadioButton rb = (RadioButton) flexTable.getWidget(row, result.get(list.get(i))+1);
					rb.setValue(true);
					
				}
				
				mainPanelInner.add(flexTable);
				
			}
		});
		
	}
}
