package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class EmmUpdateEvent extends GwtEvent<EmmUpdateEventHandler> {
	public static Type<EmmUpdateEventHandler> TYPE = new Type<EmmUpdateEventHandler>();
  
	@Override
	public Type<EmmUpdateEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(EmmUpdateEventHandler handler) {
		handler.onEmmUpdate(this);
	}
}
