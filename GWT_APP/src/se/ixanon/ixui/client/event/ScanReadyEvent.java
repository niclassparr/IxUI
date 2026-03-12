package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ScanReadyEvent extends GwtEvent<ScanReadyEventHandler> {
  public static Type<ScanReadyEventHandler> TYPE = new Type<ScanReadyEventHandler>();
  
  @Override
  public Type<ScanReadyEventHandler> getAssociatedType() {
    return TYPE;
  }

	@Override
	protected void dispatch(ScanReadyEventHandler handler) {
		handler.onScanReady(this);
	}
}
