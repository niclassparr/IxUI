package se.ixanon.ixui.client.item.dialog;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ReloadDialog extends Composite {
	
	private int sec = 50;
	private Label textLabel;
	
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			update();
		}
	};
	
	public ReloadDialog() {
		
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
		
		HTML html = new HTML("<i class='fa fa-repeat aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Reboot");
		textLabel = new Label("Server is restarting. Please wait. This page will auto reload in "+sec+" seconds.");
		
		HTML html2 = new HTML("<i class='fa fa-circle-o-notch fa-spin fa-3x fa-fw'></i>");
		html2.setStyleName("icon header dark");
		
		//Button okButton = new Button("OK");
		
		//okButton.setStyleName("btn blue login");
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(html2);	
		
		//mainPanel.add(okButton);
		
		initWidget(wrapperPanel);
		
		updateTimer.scheduleRepeating(1000);
		
		/*
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
		*/
	}
	
	private void update() {
		
		sec = sec - 1;
		
		textLabel.setText("Server is restarting. Please wait. This page will auto reload in "+sec+" seconds.");
		
		if(sec == 0) {
			updateTimer.cancel();
			Window.Location.reload();
		}
		
	}
}
