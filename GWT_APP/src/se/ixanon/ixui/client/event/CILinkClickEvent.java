package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class CILinkClickEvent extends GwtEvent<CILinkClickEventHandler> {
	public static Type<CILinkClickEventHandler> TYPE = new Type<CILinkClickEventHandler>();
  
	private final int id;
  
	public CILinkClickEvent(int id) {
		this.id = id;
	}
  
	public int getId() { 
		return id;
	}
  
	@Override
	public Type<CILinkClickEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(CILinkClickEventHandler handler) {
		handler.onCILinkClick(this);
	}
}
