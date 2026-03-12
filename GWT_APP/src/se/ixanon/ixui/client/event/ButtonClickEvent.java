package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ButtonClickEvent extends GwtEvent<ButtonClickEventHandler> {
	public static Type<ButtonClickEventHandler> TYPE = new Type<ButtonClickEventHandler>();
  
	private final String type;
  
	public ButtonClickEvent(String type) {
		this.type = type;
	}
  
	public String getType() { 
		return type;
	}
  
	@Override
	public Type<ButtonClickEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(ButtonClickEventHandler handler) {
		handler.onButtonClick(this);
	}
}
