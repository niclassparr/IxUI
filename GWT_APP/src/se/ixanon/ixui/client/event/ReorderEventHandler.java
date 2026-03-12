package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface ReorderEventHandler extends EventHandler {
	void onReorder(ReorderEvent event);
}
