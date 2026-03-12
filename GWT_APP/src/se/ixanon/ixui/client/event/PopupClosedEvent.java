package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class PopupClosedEvent extends GwtEvent<PopupClosedEventHandler>{

	public static Type<PopupClosedEventHandler> TYPE = new Type<PopupClosedEventHandler>();
	
	public PopupClosedEvent() {
		
	}
		
	@Override
	public Type<PopupClosedEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(PopupClosedEventHandler handler) {
		handler.onPopupClosed(this);
	}

}
