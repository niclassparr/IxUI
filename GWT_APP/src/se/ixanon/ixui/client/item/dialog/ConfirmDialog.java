package se.ixanon.ixui.client.item.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConfirmDialog extends Composite {
	
	public ConfirmDialog(String title, String message, String icon) {
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		FlowPanel mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog");
		wrapperPanel.add(mainPanel);
		
		FlowPanel mainPanelInner = new FlowPanel();
		mainPanelInner.setStyleName("dialog-inner");
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label(title);
		HTML textLabel = new HTML(message);
		
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		initWidget(wrapperPanel);
		
		if(!message.equals("The system has been shut down.")) {
			Button okButton = new Button("OK");
			okButton.setStyleName("btn blue login");
			mainPanel.add(okButton);
			
			okButton.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					RootPanel.get("overlay").clear();
					RootPanel.get("overlay").setVisible(false);
				}
				
			});
		}
		
		
	}
}
