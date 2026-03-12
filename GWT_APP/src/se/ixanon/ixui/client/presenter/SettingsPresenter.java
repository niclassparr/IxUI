package se.ixanon.ixui.client.presenter;

import java.util.HashMap;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.event.PopupClosedEventHandler;
import se.ixanon.ixui.client.event.ToggleChangeEvent;
import se.ixanon.ixui.client.event.ToggleChangeEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.DateTimeDialog;
import se.ixanon.ixui.client.item.dialog.ModulatorSettingsDialog;
import se.ixanon.ixui.client.item.dialog.PwDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.UnitInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class SettingsPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void enableHlsAuth(boolean enabled);
		void enableRemux(boolean enabled);
		void build(HashMap<String, String> settings, boolean cloud, boolean forced_content, boolean hls_output, boolean portal);
		HashMap<String, String> getSettings(boolean cloud, boolean forced_content, boolean hls_output, boolean portal);
		HasClickHandlers getModButton();
		HasClickHandlers getDateTimeButton();
		HasClickHandlers getPwButton();
		HasClickHandlers getSaveButton();
		Widget asWidget();
	}
	
	private final HandlerManager presenterBus = new HandlerManager(null);
	private final Display display;
	private boolean cloud = false;
	private boolean forced_content = false;
	private boolean hls_output = false;
	private boolean portal = false;
	
	public SettingsPresenter(Display view) {
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
		
		Session.getInstance().getHandlerManager().addHandler(ToggleChangeEvent.TYPE, new ToggleChangeEventHandler() {

			@Override
			public void onToggleChange(ToggleChangeEvent event) {
				
				if(event.getType().equals("hls_ba_enabled")) {
					display.enableHlsAuth(event.isToggle());
				}
				
				if(event.getType().equals("remux_enabled")) {
					display.enableRemux(event.isToggle());
				}
				
			}
			
		});
		
		display.getPwButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new PwDialog());
			}
			
		});
		
		display.getModButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new ModulatorSettingsDialog());
			}
			
		});
		
		display.getDateTimeButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new DateTimeDialog());
			}
			
		});		
		
		display.getSaveButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				Session.getInstance().getRpcService().updateSettings(Session.getInstance().getSessionKey(), display.getSettings(cloud, forced_content, hls_output, portal), new AsyncCallback<Response>() {

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
			
		});
	}
	
	private void init() {
		display.setHeader(new Header("Settings", "wrench"));
	}
	
	public void fetchData() {
		
		Session.getInstance().getRpcService().getUnitInfo(new AsyncCallback<UnitInfo>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(UnitInfo result) {
				cloud = result.isCloud();
				forced_content = result.isForcedContent();
				hls_output = result.isHlsoutput();
				portal = result.isPortal();
				fetchData2();
			}
			
		});
		
	}
	
	public void fetchData2() {
		Session.getInstance().getRpcService().getSettings(Session.getInstance().getSessionKey(), true, new AsyncCallback<HashMap<String, String>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(HashMap<String, String> result) {
				display.build(result, cloud, forced_content, hls_output, portal);
				
				
				display.enableHlsAuth(Boolean.valueOf(result.get("hls_ba_enable")));
				display.enableRemux(Boolean.valueOf(result.get("remux_enable")));
				
			}
			
		});
	}
}