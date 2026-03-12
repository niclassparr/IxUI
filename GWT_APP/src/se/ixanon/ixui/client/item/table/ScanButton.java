package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ScanEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class ScanButton extends Composite implements ClickHandler {

	private FlowPanel mainPanel;
	private Label last_scan = new Label();
	private boolean enabled = true;
	
	public ScanButton() {
		
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("scan-wrapper");
		initWidget(mainPanel);
		
		FocusPanel button_panel = new FocusPanel();
		
		FlowPanel button = new FlowPanel();
		button_panel.setStyleName("btn blue");
		button_panel.add(button);
		mainPanel.add(button_panel);
		
		HTML icon = new HTML("<i class='fa fa-search' aria-hidden='true'></i>");
		icon.setStyleName("icon button");
		
		Label label = new Label("Scan");
		label.setStyleName("inline");
		
		last_scan.setStyleName("inline");
		
		button.add(icon);
		button.add(label);
		mainPanel.add(last_scan);
		
		button_panel.addClickHandler(this);
	}
	
	public void setDate(String value) {
		last_scan.setText("Last scan: " + value);
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		
		if(enabled) {
			mainPanel.removeStyleName("disabled");
		} else {
			mainPanel.addStyleName("disabled");
		}
	}
	
	@Override
	public void onClick(ClickEvent event) {
		if(enabled) {
			Session.getInstance().getHandlerManager().fireEvent(new ScanEvent());
		}
		
	}
	

}
