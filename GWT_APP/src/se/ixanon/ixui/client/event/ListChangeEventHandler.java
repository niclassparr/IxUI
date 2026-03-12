package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface ListChangeEventHandler extends EventHandler {
	void onListChange(ListChangeEvent event);
}
