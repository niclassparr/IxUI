package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class FilterClickEvent extends GwtEvent<FilterClickEventHandler> {
	public static Type<FilterClickEventHandler> TYPE = new Type<FilterClickEventHandler>();
  
	private final String title;
  
	public FilterClickEvent(String title) {
		this.title = title;
	}
  
	public String getTitle() { 
		return title;
	}
  
	@Override
	public Type<FilterClickEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(FilterClickEventHandler handler) {
		handler.onFilterClick(this);
	}
}
