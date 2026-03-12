package se.ixanon.ixui.client.item.menu;

import se.ixanon.ixui.client.item.dialog.PushDialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class PushButton extends Composite implements ClickHandler {
	
	private HTML html;
	
	public PushButton() {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("inline");
		initWidget(mainPanel);
		
		FocusPanel button_panel = new FocusPanel();
		
		FlowPanel button = new FlowPanel();
		button_panel.setStyleName("btn blue");
		button_panel.add(button);
		mainPanel.add(button_panel);
		
		html = new HTML("<i class='fa fa-upload' aria-hidden='true'></i>");
		html.setStyleName("icon button");
		
		Label label = new Label("Push Config");
		label.setStyleName("inline");
		
		button.add(html);
		button.add(label);
		
		button_panel.addClickHandler(this);
	}
	
	public void toggleFlash(boolean value) {
		
		if(value) {
			html.addStyleName("faa-flash animated");
		} else {
			html.removeStyleName("faa-flash animated");
		}
		
	}
		
	@Override
	public void onClick(ClickEvent event) {
		
		RootPanel.get("overlay").add(new PushDialog());
		
	}
	

}
