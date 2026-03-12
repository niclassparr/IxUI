package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.event.FilterClickEvent;
import se.ixanon.ixui.client.event.FilterClickEventHandler;
import se.ixanon.ixui.client.item.dialog.CommandDialog;
import se.ixanon.ixui.client.item.table.FilterMenu;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Interface;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class CloudPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void buildTable(HashMap<String, String> details);
		void addButtons(boolean isCloud);
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private ArrayList<Interface> interfaces;
	private ArrayList<Interface> filtered_interfaces;
	private String filter = "All";
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			fetchData();
		}
	};
	
	public CloudPresenter(Display view) {
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
		
		Session.getInstance().setTimer(updateTimer);
		Session.getInstance().getTimer().scheduleRepeating(5000);
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				
				if(event.getType().equals("connect")) {
					RootPanel.get("overlay").add(new CommandDialog("ixcloud-connect", "connect cloud", null));
				}
				
				if(event.getType().equals("disconnect")) {
					RootPanel.get("overlay").add(new CommandDialog("ixcloud-disconnect", "disconnect cloud", null));
				}
				
			}
			
		});
		
	}
	
	private void init() {
		display.setHeader(new Header("Cloud", "cloud"));
	}
	
	public void fetchData() {
				
		Session.getInstance().getRpcService().getCloudDetails(Session.getInstance().getSessionKey(), new AsyncCallback<HashMap<String, String>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(HashMap<String, String> result) {
				
				
				
				display.buildTable(result);
				display.addButtons(result.get("ixcloud_enable").equals("true"));
				
				
			}
			
		});
	}
	

}