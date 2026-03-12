package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.CheckFieldClickedEvent;

public class ArrowField extends HTML implements ClickHandler {
	
	private String direction;
	
	public ArrowField(String direction) {
		
		this.direction = direction;
		
		if(direction.equals("arrow-left")) {
			this.setHTML("<i class='fa fa-arrow-circle-o-left' aria-hidden='true'></i>");
		} else {
			this.setHTML("<i class='fa fa-arrow-circle-o-right' aria-hidden='true'></i>");
		}
		
		this.addClickHandler(this);
	}
	
	@Override
	public void onClick(ClickEvent event) {
		Session.getInstance().getHandlerManager().fireEvent(new ButtonClickEvent(direction));
	}

}
