package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.Helper;

public class StatusField extends Composite {

	private Label status;
	
	public StatusField(String title) {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
		
		Label label = new Label(title);
		label.setStyleName("edit-label");
		mainPanel.add(label);
		
		status = new Label("none");
		status.setStyleName("edit-field");
		mainPanel.add(status);
	}
	
	public void setStatus(int value) {
		String s = Helper.getStatus(value);
		status.setText(s);
		status.setStyleName("edit-field");
		status.addStyleName(s);
	}
	
	public void setStatus(boolean value) {
		String s = Helper.getStatus(value);
		status.setText(s);
		status.setStyleName("edit-field");
		status.addStyleName(s);
	}
}
