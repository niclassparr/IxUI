package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.TextBox;

public class NumTextBox extends TextBox implements KeyPressHandler {

	public NumTextBox() {
		this.addKeyPressHandler(this);
	}
	
	@Override
	public void onKeyPress(KeyPressEvent event) {
		if (!Character.isDigit(event.getCharCode()) 
				&& event.getNativeEvent().getKeyCode() != KeyCodes.KEY_LEFT
				&& event.getNativeEvent().getKeyCode() != KeyCodes.KEY_RIGHT
				&& event.getNativeEvent().getKeyCode() != KeyCodes.KEY_TAB 
                && event.getNativeEvent().getKeyCode() != KeyCodes.KEY_DELETE 
                && event.getNativeEvent().getKeyCode() != KeyCodes.KEY_BACKSPACE){
            ((NumTextBox) event.getSource()).cancelKey();
        }
	}

}
