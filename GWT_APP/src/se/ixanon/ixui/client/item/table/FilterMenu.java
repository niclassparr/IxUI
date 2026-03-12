package se.ixanon.ixui.client.item.table;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class FilterMenu extends Composite {

	private FlowPanel mainPanel = new FlowPanel();
	
	public FilterMenu() {
		
		mainPanel.setStyleName("filter-menu");
		initWidget(mainPanel);
		
	}
	
	public void setItems(ArrayList<String> items) {
		
		for(int i = 0; i < items.size(); i++) {
			
			FilterLabel fl = new FilterLabel(items.get(i));
			
			mainPanel.add(fl);
			
			if(i == 0) {
				fl.addStyleName("first");
			}
			
			if(i == items.size()-1) {
				fl.addStyleName("last");
			}
			
		}
		
	}
	
	public void setActive(String title) {
		
		for(int i = 0; i < mainPanel.getWidgetCount(); i++) {
			
			FilterLabel fl = (FilterLabel) mainPanel.getWidget(i);
			
			if(fl.getText().equals(title)) {
				fl.addStyleName("active");
			} else {
				fl.removeStyleName("active");
			}
			
		}
		
	}

}
