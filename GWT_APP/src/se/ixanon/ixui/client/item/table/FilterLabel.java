package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.FilterClickEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;

public class FilterLabel extends Label implements ClickHandler {

	private String title;
	
	public FilterLabel(String title) {
		
		this.title = title;
		
		this.setText(title);
		this.setStyleName("filter-label inline");
		
		this.addClickHandler(this);
	}
		
	@Override
	public void onClick(ClickEvent event) {
		Session.getInstance().getHandlerManager().fireEvent(new FilterClickEvent(title));
	}
	

}
