package se.ixanon.ixui.client.item.dialog;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;

import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.shared.Response;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class RestoreDialog extends Composite {
	
	private final FormPanel form = new FormPanel();
	private final FileUpload fileUpload = new FileUpload();
	private Button okButton = new Button("Restore");
	private HTML infoLabel = new HTML();
	
	private void bind() {
		
		fileUpload.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {

				if(fileUpload.getFilename() != null && !fileUpload.getFilename().isEmpty()) {
					
					Debug.log("onChange -> form.submit");
					
					form.submit();
				} else {
					infoLabel.setText("Error: Invalid file.");
				}
				
			}
			
		});
		
		
		form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				
				Session.getInstance().getRpcService().getJsonInfo(new AsyncCallback<String>() {

					@Override
					public void onFailure(Throwable caught) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onSuccess(String result) {
						infoLabel.setHTML(result);
						
						if(result.equals("Error: Invalid backup file.")) {
							okButton.setEnabled(false);
						} else {
							okButton.setEnabled(true);
						}
						
						
						
					}
					
				});
				
				
				
				
				
			}
			
		});
		
		
	}
	
	public RestoreDialog() {
		
		form.setAction("api/json");
		form.setEncoding(FormPanel.ENCODING_MULTIPART);
		form.setMethod(FormPanel.METHOD_POST);
		fileUpload.getElement().setAttribute("accept", "application/json");
		fileUpload.setName("file_upload");
		
		bind();		
		
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
		
		HTML html = new HTML("<i class='fa fa-cloud-upload' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Restore");
		HTML textLabel = new HTML("Upload a ixui backup file to restore the system.");
		
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		mainPanelInner.add(form);
		form.add(fileUpload);
		infoLabel.setStyleName("dialog-text");
		mainPanel.add(infoLabel);
		
		okButton.setEnabled(false);
		okButton.setStyleName("btn blue login");
		mainPanel.add(okButton);
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				runCommand();
			}
			
		});
		
		
		Button cancelButton = new Button("Cancel");
		cancelButton.setStyleName("btn orange login");
		mainPanel.add(cancelButton);
		
		cancelButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
		
		initWidget(wrapperPanel);
		
	}
	
	private void runCommand() {
		
		Session.getInstance().getRpcService().runCommand("restore", null, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(Response result) {

				if(result.isSuccess()) {
					RootPanel.get("overlay").clear();
					RootPanel.get("overlay").add(new ConfirmDialog("Success", "Restore was completed without any problems.", "thumbs-up"));
				} else {
					infoLabel.setText(result.getError());
				}
				
			}
			
		});
		
		
	}
}
