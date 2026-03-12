package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.shared.Response;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PushDialog extends Composite {
	
	public PushDialog() {
		
		Session.getInstance().getRpcService().pushConfig(new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ConfirmDialog("Pushed", "Your changes has been pushed.", "upload"));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
				}
			}
			
		});
		
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
		
		HTML html = new HTML("<i class='fa fa-upload aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Push Config");
		Label textLabel = new Label("Push in progress. Please wait.");
		
		HTML html2 = new HTML("<i class='fa fa-cog fa-spin fa-3x fa-fw'></i>");
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
}
