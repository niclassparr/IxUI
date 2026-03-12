package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

public class PwField extends Composite {
	
	private FlowPanel mainPanel;
	private PasswordTextBox box = new PasswordTextBox();
	private Label label = new Label();
	
	public PwField(String title) {
		
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
		
		label.setText(title);
		label.setStyleName("edit-label");
		box.setStyleName("edit-field");
		
		mainPanel.add(label);
		mainPanel.add(box);
		
	}
	
	public String getValue() {
		return box.getText();
	}

}
