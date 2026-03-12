package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.shared.Response;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CommandDialog extends Composite {
	
	private String command;
	private String filename;
	
	public CommandDialog(String command, String desc, String filename) {
		
		this.command = command;
		this.filename = filename;
		
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
		
		HTML html = new HTML("<i class='fa fa-exclamation-triangle' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Confirm");
		HTML textLabel = new HTML("Are you sure you want to " + desc);
		
		Button okButton = new Button("OK");
		Button noButton = new Button("Cancel");
		
		okButton.setStyleName("btn blue login");
		noButton.setStyleName("btn orange login");
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		mainPanel.add(okButton);
		mainPanel.add(noButton);
		
		initWidget(wrapperPanel);
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				run();
			}
			
		});
		
		noButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
	}
	
	private void run() {
		
		if(command.equals("update interfaces")) {
			updateInterfaces();
		} else if(command.equals("generate pdf")) {
			generatePDF();
		} else {
			runCommand();
		}
		
	}
	
	private void generatePDF() {
		
		Session.getInstance().getRpcService().savePDF(filename, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ConfirmDialog("Done", "Command finished.", "check-square-o"));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
				}
			}
			
		});
	}
	
	private void updateInterfaces() {
		
		RootPanel.get("overlay").add(new UpdateLoadDialog("Update Interfaces", "refresh", null));
		
		Session.getInstance().getRpcService().interfaceUpdate(new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ConfirmDialog("Done", "Command finished.", "check-square-o"));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
				}
			}
			
		});
		
	}
	
	private void runCommand() {
		
		Session.getInstance().getRpcService().runCommand(command, filename, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				Debug.getInstance().printLine("failed: " + caught.getMessage());
			}

			@Override
			public void onSuccess(Response result) {
				
				Debug.getInstance().printLine("success: " + result.isSuccess());
				Debug.getInstance().printLine("error: " + result.getError());
				
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ConfirmDialog("Done", "Command finished.", "check-square-o"));
					
				} else {
					if(result.getError().contains("reboot")) {
						RootPanel.get("overlay").add(new ReloadDialog());
					} else if(result.getError().contains("poweroff")) {
						
						RootPanel.get("menu").clear();
						RootPanel.get("main").clear();
						RootPanel.get("overlay").add(new ConfirmDialog("Power off", "The system has been shut down.", "power-off"));
						
					} else {
						RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
					}
					
				}
			}
			
		});
	}
}
