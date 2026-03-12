package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface ScanReadyEventHandler extends EventHandler {
	void onScanReady(ScanReadyEvent event);
}
