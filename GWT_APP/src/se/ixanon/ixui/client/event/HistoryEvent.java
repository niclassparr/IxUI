package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

import se.ixanon.ixui.shared.SessionKeys;

public class HistoryEvent extends GwtEvent<HistoryEventHandler>{

	public static Type<HistoryEventHandler> TYPE = new Type<>();
	private final SessionKeys keys;
	private final boolean back;

	public HistoryEvent(SessionKeys keys) {
		this.keys = keys;
		this.back = (keys == null) ? true : false;
	}

	public SessionKeys getKeys() {
		return keys;
	}

	public boolean isBack() {
		return back;
	}

	@Override
	public Type<HistoryEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(HistoryEventHandler handler) {
		handler.onHistory(this);
	}

}
