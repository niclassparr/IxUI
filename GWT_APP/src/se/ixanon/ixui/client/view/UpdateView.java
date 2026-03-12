package se.ixanon.ixui.client.view;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.IconText;
import se.ixanon.ixui.client.presenter.UpdatePresenter;
import se.ixanon.ixui.shared.Package;

public class UpdateView extends Composite implements UpdatePresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel infoPanel;
	private FlexTable flexTable = new FlexTable();
	private Button ok_button = new Button("OK");
	private Button update_button = new Button("Update");
	private Header header = new Header("Update", "server");
	
	public UpdateView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
		
		infoPanel = new FlowPanel();
	}
	
	public void displayError(String error, String color) {
		infoPanel.clear();
		infoPanel.add(new IconText(error, "exclamation-circle", "info-box " + color));
	}
	
	public void setInitView(boolean isSoftwareUpdate) {
		mainPanel.add(header);
		
		if(isSoftwareUpdate) {
			
			mainPanel.add(infoPanel);
			infoPanel.add(new IconText("Check for available software updates. This might take serveral minutes", "info-circle", "info-box blue"));
					
			ok_button.setStyleName("btn blue");
			mainPanel.add(ok_button);
			
		} else {
			mainPanel.add(new Label("The update software function is disabled."));
		}
		
		
	}
	
	public void setTableView() {
		flexTable.removeAllRows();
		infoPanel.clear();
		mainPanel.clear();
		mainPanel.add(header);
		mainPanel.add(flexTable);
		
		mainPanel.add(infoPanel);
		infoPanel.add(new IconText("Update selected packages. This might take serveral minutes", "info-circle", "info-box blue"));
		
		update_button.setStyleName("btn blue");
		mainPanel.add(update_button);
		
	}
	
	public void setResultView() {
		mainPanel.clear();
		mainPanel.add(header);
	}
	
	public void buildTable(ArrayList<Package> packages) {
		
		flexTable.removeAllRows();
		
		flexTable.addStyleName("flexTable");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
		
		flexTable.setText(0, 0, "Update");
		flexTable.setText(0, 1, "Name");
		flexTable.setText(0, 2, "Version");
		
		for (int i = 0; i < packages.size(); i++) {
			
			CheckBox checkbox = new CheckBox();
			checkbox.setValue(false);
			
			flexTable.setWidget(i+1, 0, checkbox);
			flexTable.setText(i+1, 1, packages.get(i).getName());
			flexTable.setText(i+1, 2, packages.get(i).getVersion());
		}
	}
	
	public boolean isPackageSelected(ArrayList<Package> packages) {
		
		for (int i = 0; i < packages.size(); i++) {
			CheckBox checkbox = (CheckBox)flexTable.getWidget(i+1, 0);
			
			if(checkbox.getValue()) {
				return true;
			}
			
		}
		
		return false;
	}
	
	public ArrayList<Package> updatePackages(ArrayList<Package> packages) {
		
		for (int i = 0; i < packages.size(); i++) {
			CheckBox checkbox = (CheckBox)flexTable.getWidget(i+1, 0);
			
			packages.get(i).setUpdate(checkbox.getValue());
		}
		
		return packages;
	}
	
	public void displayResult(String result) {
		HTML html = new HTML("<pre>"+result+"</pre>");
		mainPanel.add(html);
	}
	
	public HasClickHandlers getOkButton() {
		return ok_button;
	}
	
	public HasClickHandlers getUpdateButton() {
		return update_button;
	}
	
	public Widget asWidget() {
		return this;
	}
}
