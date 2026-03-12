package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class TextFieldBlurEvent extends GwtEvent<TextFieldBlurEventHandler> {
	public static Type<TextFieldBlurEventHandler> TYPE = new Type<TextFieldBlurEventHandler>();
  
	private final String type;
  
	public TextFieldBlurEvent(String type) {
		this.type = type;
	}
  
	public String getType() { 
		return type;
	}
  
	@Override
	public Type<TextFieldBlurEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(TextFieldBlurEventHandler handler) {
		handler.onTextFieldBlur(this);
	}
}
