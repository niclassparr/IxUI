package se.ixanon.ixui.client.item.menu;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.shared.SessionKeys;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class LogoutLabel extends Composite implements ClickHandler {

	public LogoutLabel() {
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("menu-item");
		initWidget(mainPanel);
		
		HTML icon = new HTML("<i class='fa fa-sign-out' aria-hidden='true'></i>");
		icon.setStyleName("icon menu");
		
		Label label = new Label("Logout");
		label.setStyleName("inline link");
		
		mainPanel.add(icon);
		mainPanel.add(label);
		
		label.addClickHandler(this);
	}
	
	@Override
	public void onClick(ClickEvent event) {
		
			
		Session.getInstance().getRpcService().logout(Session.getInstance().getSessionKey(), new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Void result) {
				Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("dashboard")));
			}
			
		});
	}
	

}
