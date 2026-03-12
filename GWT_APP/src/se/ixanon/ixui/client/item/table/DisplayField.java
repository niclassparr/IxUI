package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class DisplayField extends Composite {
	
	private FlowPanel mainPanel;
	private Label label = new Label();
	private HTML display = new HTML();
	
	public DisplayField(String title, String text) {
				
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
		
		label.setText(title);
		label.setStyleName("edit-label");
		mainPanel.add(label);
		
		boolean is_br = text.contains("<br>");
		
		display.setHTML(text);
		display.setStyleName("edit-field inline br-" + is_br);
		mainPanel.add(display);
		
		
	}
}
