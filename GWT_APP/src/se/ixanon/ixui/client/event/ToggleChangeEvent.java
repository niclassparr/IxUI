package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ToggleChangeEvent extends GwtEvent<ToggleChangeEventHandler> {
  public static Type<ToggleChangeEventHandler> TYPE = new Type<ToggleChangeEventHandler>();
  
  private final String type;
  private final boolean toggle;
  
  public ToggleChangeEvent(String type, boolean toggle) {
		this.type = type;
		this.toggle = toggle;
	}

	public String getType() { 
		return type;
	}
	
	public boolean isToggle() { 
		return toggle;
	}
  
  @Override
  public Type<ToggleChangeEventHandler> getAssociatedType() {
    return TYPE;
  }

	@Override
	protected void dispatch(ToggleChangeEventHandler handler) {
		handler.onToggleChange(this);
	}
}
