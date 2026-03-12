package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.AlphanumComparator;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.item.table.PwField;
import se.ixanon.ixui.shared.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PwDialog extends Composite {
	
	private FlowPanel mainPanelInner = new FlowPanel();
	private Label textLabel;
	private PwField old_password = new PwField("Old Password");
	private PwField new_password = new PwField("New Password");
	private PwField new2_password = new PwField("Verify New Password");
	
	public PwDialog() {
		
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
		mainPanel.setStyleName("dialog mod-settings");
		wrapperPanel.add(mainPanel);
		
		mainPanelInner.setStyleName("dialog-inner");
		
		HTML html = new HTML("<i class='fa fa-key' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Change Password");
		textLabel = new Label("Enter your passwords.");
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
				
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		mainPanelInner.add(old_password);
		mainPanelInner.add(new_password);
		mainPanelInner.add(new2_password);
		
		initWidget(wrapperPanel);
		
		Button okButton = new Button("OK");
		okButton.setStyleName("btn blue login");
		mainPanel.add(okButton);
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				updatePassword();				
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
		
	}
	
	private void updatePassword() {
		
		boolean post = true;
		
		if(!new_password.getValue().equals(new2_password.getValue())) {
			textLabel.setText("New passwords do not match.");
			post = false;
		}
		
		if(old_password.getValue().equals("") || new_password.getValue().equals("") || new_password.getValue().equals("")) {
			textLabel.setText("Please fill in all fields.");
			post = false;
		}
		
		if(post) {
			
			Session.getInstance().getRpcService().updatePw(Session.getInstance().getUsername(), old_password.getValue(), new_password.getValue(), new AsyncCallback<Response>() {

				@Override
				public void onFailure(Throwable caught) {
					
				}

				@Override
				public void onSuccess(Response result) {
					
					if(result.isSuccess()) {
						RootPanel.get("overlay").clear();
						RootPanel.get("overlay").setVisible(false);
					} else {
						textLabel.setText(result.getError());
					}
					
					
					
				}
				
			});
			
		}
		
	}
	
}
