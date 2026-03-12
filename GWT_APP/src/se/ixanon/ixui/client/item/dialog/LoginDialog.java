package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.shared.SessionKeys;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginDialog extends Composite {

	private Label textLabel;
	private TextBox username;
	private PasswordTextBox password;
	
	public LoginDialog() {
		
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
		
		HTML icon = new HTML("<i class='fa fa-sign-in' aria-hidden='true'></i>");
		icon.setStyleName("icon header");
				
		Label headerLabel = new Label("Sign In");
		textLabel = new Label("Enter your username and password.");
		
		HTML iconUser = new HTML("<i class='fa fa-user' aria-hidden='true'></i>");
		HTML iconKey = new HTML("<i class='fa fa-key' aria-hidden='true'></i>");

		iconUser.setStyleName("form-icon");
		iconKey.setStyleName("form-icon");
		
		username = new TextBox();
		password = new PasswordTextBox();
		Button okButton = new Button("Login");
		
		okButton.setStyleName("btn blue login");
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		username.setStyleName("form");
		password.setStyleName("form");
		
		mainPanel.add(icon);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		mainPanelInner.add(iconUser);
		mainPanelInner.add(username);
		mainPanelInner.add(iconKey);
		mainPanelInner.add(password);
		
		mainPanel.add(okButton);
		
		initWidget(wrapperPanel);
		
		username.addKeyDownHandler(new KeyDownHandler() {

			@Override
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					checkLogin();
				}
								
			}
			
		});
		
		password.addKeyDownHandler(new KeyDownHandler() {

			@Override
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					checkLogin();
				}
								
			}
			
		});
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				checkLogin();
			}
			
		});
	}

	private void checkLogin() {
		
		if(password.getText().equals("") || username.getText().equals("")){
			
		} else {
			Session.getInstance().getRpcService().login(username.getText(), password.getText(), new AsyncCallback<String>() {

				@Override
				public void onFailure(Throwable caught) {
					
				}

				@Override
				public void onSuccess(String result) {
					if(result != null) {
						
						Cookies.setCookie("session", result);
						Cookies.setCookie("username", username.getText());
						
						Session.getInstance().setSessionKey(result);
						Session.getInstance().setUsername(username.getText());
						
						RootPanel.get("overlay").clear();
						RootPanel.get("overlay").setVisible(false);
						
						Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("dashboard")));
						
					} else {
						textLabel.setText("Wrong username or password.");
					}
				}
				
			});
		}
		
		
	}
}
