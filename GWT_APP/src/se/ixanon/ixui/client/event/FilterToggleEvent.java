package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class FilterToggleEvent extends GwtEvent<FilterToggleEventHandler> {
	public static Type<FilterToggleEventHandler> TYPE = new Type<FilterToggleEventHandler>();
  
	private final String title;
	private final boolean toggle;
  
	public FilterToggleEvent(String title, boolean toggle) {
		this.title = title;
		this.toggle = toggle;
	}
  
	public String getTitle() { 
		return title;
	}
	
	public boolean isToggle() { 
		return toggle;
	}
  
	@Override
	public Type<FilterToggleEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(FilterToggleEventHandler handler) {
		handler.onFilterToggle(this);
	}
}
