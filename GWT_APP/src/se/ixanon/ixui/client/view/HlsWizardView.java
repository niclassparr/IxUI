package se.ixanon.ixui.client.view;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.item.table.ArrowField;
import se.ixanon.ixui.client.item.table.CheckField;
import se.ixanon.ixui.client.item.table.FilterMenu;
import se.ixanon.ixui.client.item.table.FilterMenuToggle;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.ScanButton;
import se.ixanon.ixui.client.item.table.UsageItem;
import se.ixanon.ixui.client.presenter.HlsWizardPresenter;
import se.ixanon.ixui.shared.Service;
import se.ixanon.ixui.shared.Usage;

public class HlsWizardView extends Composite implements HlsWizardPresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel wrapperArrowTables = new FlowPanel();	
	private FlowPanel wrapperTags = new FlowPanel();	
	private FlexTable flexTable = new FlexTable();
	private FlexTable flexTable1 = new FlexTable();
	private FlexTable flexTable2 = new FlexTable();
	private Button saveServicesButton = new Button("Save Services");
	private Label link = new Label("All");
	private FilterMenuToggle filterMenu = new FilterMenuToggle();
	
	public HlsWizardView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
		build();
	}
	
	private void build() {
		
		wrapperArrowTables.setStyleName("wrapper-arrow-tables");
		
		FlowPanel wrapper = new FlowPanel();
		wrapper.setStyleName("one-two-three");
		wrapper.add(new InlineLabel("Scan"));
		wrapper.add(new InlineLabel("Select Services"));
		wrapper.add(new InlineLabel("Save Services"));
		mainPanel.add(wrapper);
		
		Label note = new Label("Note! this operation will erase all previous configuration of HLS interfaces.");
		note.setStyleName("wiz-note");
		mainPanel.add(note);
		
		FlowPanel wrapperButtons = new FlowPanel();
		wrapperButtons.setStyleName("wrapper-hls-buttons");
		mainPanel.add(wrapperButtons);
		
		ScanButton scan_button = new ScanButton();
		wrapperButtons.add(scan_button);
		
		flexHeaders();
		flexHeaders1();
		flexHeaders2();
		
		wrapperArrowTables.add(flexTable);
		arrows();
		wrapperArrowTables.add(flexTable1);
		
		mainPanel.add(wrapperTags);
		mainPanel.add(wrapperArrowTables);
		
		wrapperButtons.add(flexTable2);
		
		saveServicesButton.setStyleName("btn blue");
		wrapperButtons.add(saveServicesButton);
		
		
	}
	
	public void buildFilterMenu(List<String> tags) {
		
		//wrapperTags.clear();
		
		filterMenu.setItems(tags);
		wrapperTags.add(filterMenu);
		
	}
	
	public void buildTables(ArrayList<Service> services, ArrayList<Service> selected_services) {
		
		flexTable.removeAllRows();
		flexHeaders();
	
		for (int i = 0; i < services.size(); i++) {
			
			flexTable.setText(i+1, 0, services.get(i).getName());
			flexTable.setWidget(i+1, 1, new CheckField(services.get(i).isEnabled(), null, i+1));
			//flexTable.setText(i+1, 2, services.get(i).getHlsUrl());
		}
		
		if(selected_services != null) {
			buildTable1(selected_services);
		}
		
		
	}
	
	public void buildTable1(ArrayList<Service> selected_services) {
		
		flexTable1.removeAllRows();
		flexHeaders1();
		
		for (int i = 0; i < selected_services.size(); i++) {
			
			//services.get(i).setEnabled(false);
			
			flexTable1.setText(i+1, 0, selected_services.get(i).getName());
			flexTable1.setWidget(i+1, 1, new CheckField(selected_services.get(i).isEnabled(), null, i+1));
		}
		
	}
	
	private void flexHeaders() {
		
		flexTable.addStyleName("flexTable epg");
		flexTable.setWidth("45%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
		
		flexTable.setText(0, 0, "Available Channels");
		flexTable.setText(0, 1, "Select");
		
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
	}
	
	private void flexHeaders1() {
		
		FlowPanel wrapper = new FlowPanel();
		wrapper.setStyleName("wrapper-all-link");		
		InlineLabel text = new InlineLabel("Select");
		link.setStyleName("inline link");
		
		wrapper.add(text);
		wrapper.add(link);
		
		flexTable1.addStyleName("flexTable epg");
		flexTable1.setWidth("45%");
		flexTable1.setCellSpacing(0);
		flexTable1.setCellPadding(5);
		
		flexTable1.setText(0, 0, "Selected Channels");
		flexTable1.setWidget(0, 1, wrapper);
		
		flexTable1.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
	}
	
	private void arrows() {
		FlowPanel wrapper = new FlowPanel();
		wrapper.setStyleName("wrapper-arrows");
		
		wrapper.add(new ArrowField("arrow-left"));
		wrapper.add(new ArrowField("arrow-right"));
		
		wrapperArrowTables.add(wrapper);
		
	}
	
	/*
	public void setSelectedChannels(int count) {
		flexTable1.setText(0, 0, count + " Selected Channels");
	}
	*/
	
	private void flexHeaders2() {
		
		flexTable2.addStyleName("flexTable");
		flexTable2.setWidth("50%");
		flexTable2.setCellSpacing(0);
		flexTable2.setCellPadding(5);
		
		flexTable2.setText(0, 0, "Type");
		flexTable2.setText(0, 1, "Services");
		flexTable2.setText(0, 2, "Calculated Usage");
		
		flexTable2.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
	}
	
	public void buildTable2(int max, int count) {
		
		Usage usage_hls = new Usage("Hls", "-");
		usage_hls.setCountServices(count);
		
		flexTable2.removeAllRows();
		flexHeaders2();
		
		flexTable2.setText(1, 0, usage_hls.getType());
		flexTable2.setText(1, 1, ""+usage_hls.getCountServices());
		flexTable2.setWidget(1, 2, new UsageItem(usage_hls.getCountServices(), max));
		
	}
	/*
	public int countHls() {
		
		int count = 0;
		int cell = 1;
			
		for (int i = 1; i < flexTable.getRowCount(); i++) {
			CheckField hls_enable = (CheckField) flexTable.getWidget(i, cell);
			
			if(hls_enable.getValue()) {
				count++;
			}
		}
		
		return count;
	}
	
	public void setHlsActive(boolean active) {
		
		int cell = 1;
		
		for (int i = 1; i < flexTable.getRowCount(); i++) {
			CheckField hls_enable = (CheckField) flexTable.getWidget(i, cell);
			
			if(!hls_enable.getValue()) {
				hls_enable.setActive(active);
			}
		}
		
	}
	*/
	public boolean isSelected(int table, int index) {
		
		if(table == 0) {
			CheckField c = (CheckField) flexTable.getWidget(index+1, 1);
			return c.getValue();
		}
		
		if(table == 1) {
			CheckField c = (CheckField) flexTable1.getWidget(index+1, 1);
			return c.getValue();
		}
		
		return false;
		
	}
	
	public void setSelectedAll(boolean value) {
		
		for (int i = 1; i < flexTable1.getRowCount(); i++) {
			CheckField c = (CheckField) flexTable1.getWidget(i, 1);
			c.setEnabled(value);
		}
		
	}
	
	public HasClickHandlers getSaveServicesButton() {
		return saveServicesButton;
	}
	
	public HasClickHandlers getAllLink() {
		return this.link;
	}
	
	public Widget asWidget() {
		return this;
	}
}
