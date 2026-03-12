package se.ixanon.ixui.client.presenter;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.item.dialog.CommandDialog;
import se.ixanon.ixui.client.item.dialog.RestoreDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.UnitInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class CommandsPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header, String serial, boolean softwareUpdate);
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private String serial = "no_serial";
	  
	public CommandsPresenter(Display view) {
		this.display = view;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		fetchData();
	}
	
	public void bind() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				
				if(event.getType().equals("backup")) {
					//RootPanel.get("overlay").add(new CommandDialog("backup", "generate a new backup file", "ixui_backup_"+serial+".json"));
					

					String base = GWT.getModuleBaseURL().replaceFirst("/[^/]+/$", "");
					Window.open(base + "/api/json?skey=" + Session.getInstance().getSessionKey() + "&user=" + Session.getInstance().getUsername(), "_self", "");
					
				}
				
				if(event.getType().equals("restore")) {
					RootPanel.get("overlay").add(new RestoreDialog());
				}
				
				if(event.getType().equals("software update")) {
					//History.newItem("", true);
					Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("update")));
				}
				
				if(event.getType().equals("power off")) {
					RootPanel.get("overlay").add(new CommandDialog("command poweroff", event.getType(), null));
				}
				
				if(event.getType().equals("reboot")) {
					RootPanel.get("overlay").add(new CommandDialog("command reboot", event.getType(), null));
				}
				
				if(event.getType().equals("restart network")) {
					RootPanel.get("overlay").add(new CommandDialog("command netrestart", event.getType(), null));
				}
				
				if(event.getType().equals("update interfaces")) {
					RootPanel.get("overlay").add(new CommandDialog("update interfaces", event.getType(), null));
				}
				
				if(event.getType().equals("start all interfaces")) {
					RootPanel.get("overlay").add(new CommandDialog("command allstart", event.getType(), null));
				}
				
				if(event.getType().equals("stop all interfaces")) {
					RootPanel.get("overlay").add(new CommandDialog("command allstop", event.getType(), null));
				}
				
				if(event.getType().equals("document")) {
					//RootPanel.get("overlay").add(new CommandDialog("generate pdf", "generate a new pdf", "installation_"+serial+".pdf"));
					
					String base = GWT.getModuleBaseURL().replaceFirst("/[^/]+/$", "");
					Window.open(base + "/api/pdf?skey=" + Session.getInstance().getSessionKey() + "&user=" + Session.getInstance().getUsername(), "_self", "");
					
				}
				
				if(event.getType().equals("reset software")) {
					RootPanel.get("overlay").add(new CommandDialog("reset", event.getType(), null));
				}
				
			}
			
		});
		
	}
	
	public void fetchData() {
		
		Session.getInstance().getRpcService().getUnitInfo(new AsyncCallback<UnitInfo>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(UnitInfo result) {
				serial = result.getSerial();
				display.setHeader(new Header("Commands", "power-off"), serial, result.isSoftwareUpdate());
			}
			
		});
		
	}
	
}