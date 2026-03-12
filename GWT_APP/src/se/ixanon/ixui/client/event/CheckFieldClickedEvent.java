package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class CheckFieldClickedEvent extends GwtEvent<CheckFieldClickedEventHandler> {
  public static Type<CheckFieldClickedEventHandler> TYPE = new Type<CheckFieldClickedEventHandler>();
  
  	private final int index;
  
	public CheckFieldClickedEvent(int index) {
		this.index = index;
	}

	public int getIndex() { 
		return index;
	}
  
  @Override
  public Type<CheckFieldClickedEventHandler> getAssociatedType() {
    return TYPE;
  }

	@Override
	protected void dispatch(CheckFieldClickedEventHandler handler) {
		handler.onCheckFieldClicked(this);
	}
}
