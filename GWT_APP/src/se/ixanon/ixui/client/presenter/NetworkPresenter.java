package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.HashMap;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.event.PopupClosedEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmCancelDialog;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.NetworkStatusDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.IpMac;
import se.ixanon.ixui.shared.NameValue;
import se.ixanon.ixui.shared.Response;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class NetworkPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void buildStatus(ArrayList<IpMac> status);
		void build(HashMap<String, NameValue> settings);
		HashMap<String, NameValue> getSettingsValues(HashMap<String, NameValue> settings);
		HasClickHandlers getStatusButton();
		HasClickHandlers getSaveButton();
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private HashMap<String, NameValue> settings;
	
	public NetworkPresenter(Display view) {
		this.display = view;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		init();
		fetchData();
	}
	
	public void bind() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		display.getStatusButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new NetworkStatusDialog());
			}
			
		});
		
		display.getSaveButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new ConfirmCancelDialog("Confirm", "System must be rebooted for configuration changes to take effect.", "exclamation-triangle"));
			}
			
		});
		
		presenterBus.addHandler(PopupClosedEvent.TYPE, new PopupClosedEventHandler() {

			@Override
			public void onPopupClosed(PopupClosedEvent event) {
				
				Session.getInstance().getRpcService().updateSettingsNew(Session.getInstance().getSessionKey(), display.getSettingsValues(settings), new AsyncCallback<Response>() {

					@Override
					public void onFailure(Throwable caught) {
						
					}

					@Override
					public void onSuccess(Response result) {
						
						if(result.isSuccess()) {
							runCommand();
						} else {
							RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
						}
						
					}
					
				});
				
			}
			
		});
		
	}
	
	private void init() {
		display.setHeader(new Header("Network", "globe"));
	}
	
	private void fetchData() {
		
		Session.getInstance().getRpcService().getNetworkStatus(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<IpMac>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<IpMac> result) {
				display.buildStatus(result);
				fetchData2();
			}
			
		});
		
		
	}
	
	private void fetchData2() {
		
		Session.getInstance().getRpcService().getNetworkSettings(new AsyncCallback<HashMap<String, NameValue>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(HashMap<String, NameValue> result) {
				
				settings = new HashMap<String, NameValue>(result);
				
				display.build(settings);
			}
			
		});
		
	}
	
	private void runCommand() {
		
		Session.getInstance().getRpcService().runCommand("wnet", null, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ConfirmDialog("Saved", "Your changes has been saved.", "download"));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Error", result.getError(), "exclamation-triangle"));
				}
				
			}
			
		});
	}
}