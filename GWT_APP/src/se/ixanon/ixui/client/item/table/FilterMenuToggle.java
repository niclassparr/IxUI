package se.ixanon.ixui.client.item.table;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class FilterMenuToggle extends Composite {

	private FlowPanel mainPanel = new FlowPanel();
	
	public FilterMenuToggle() {
		
		mainPanel.setStyleName("filter-menu");
		initWidget(mainPanel);
		
	}
	
	public void setItems(List<String> items) {
		
		mainPanel.clear();
		
		for(int i = 0; i < items.size(); i++) {
			
			FilterToggle fl = new FilterToggle(items.get(i));
			
			mainPanel.add(fl);
			
			if(i == 0) {
				fl.addStyleName("first");
			}
			
			if(i == items.size()-1) {
				fl.addStyleName("last");
			}
			
		}
		
	}
	

}
