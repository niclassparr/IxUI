package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class EventButton extends Composite implements ClickHandler {

	private String type;
	
	public EventButton(String title, String icon, String color) {
		
		this.type = title.toLowerCase();
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("inline");
		initWidget(mainPanel);
		
		FocusPanel button_panel = new FocusPanel();
		
		FlowPanel button = new FlowPanel();
		button_panel.setStyleName("btn " + color);
		button_panel.add(button);
		mainPanel.add(button_panel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon button");
		
		Label label = new Label(title);
		label.setStyleName("inline");
		
		button.add(html);
		button.add(label);
		
		button_panel.addClickHandler(this);
	}
		
	@Override
	public void onClick(ClickEvent event) {
		Session.getInstance().getHandlerManager().fireEvent(new ButtonClickEvent(type));
	}
	

}
