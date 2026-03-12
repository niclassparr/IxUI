package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.FilterToggleEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;

public class FilterToggle extends Label implements ClickHandler {

	private String title;
	private boolean toggle = false;
	
	public FilterToggle(String title) {
		
		this.title = title;
		
		this.setText(title);
		this.setStyleName("filter-label inline");
		
		this.addClickHandler(this);
	}
	
	@Override
	public void onClick(ClickEvent event) {
		
		toggle = !toggle;
		
		if(toggle) {
			this.addStyleName("active");
		} else {
			this.removeStyleName("active");
		}
		
		
		
		Session.getInstance().getHandlerManager().fireEvent(new FilterToggleEvent(title, toggle));
	}
	

}
