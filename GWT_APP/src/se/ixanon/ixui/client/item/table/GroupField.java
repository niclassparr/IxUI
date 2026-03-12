package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class GroupField extends Composite {

	FlowPanel innerPanel = new FlowPanel();
	
	public GroupField(String title) {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("group-fields");
		innerPanel.setStyleName("group-fields-inner");
		mainPanel.add(innerPanel);
		initWidget(mainPanel);
		
		Label label = new Label(title);
		label.setStyleName("group-fields-title");
		
		innerPanel.add(label);
		
	}
	
	public void add(Widget widget) {
		innerPanel.add(widget);
	}
	
}
