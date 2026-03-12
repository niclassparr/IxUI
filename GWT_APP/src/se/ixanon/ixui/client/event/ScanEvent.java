package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ScanEvent extends GwtEvent<ScanEventHandler> {
  public static Type<ScanEventHandler> TYPE = new Type<ScanEventHandler>();
  
  @Override
  public Type<ScanEventHandler> getAssociatedType() {
    return TYPE;
  }

	@Override
	protected void dispatch(ScanEventHandler handler) {
		handler.onScan(this);
	}
}
