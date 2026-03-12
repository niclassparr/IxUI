package se.ixanon.ixui.client.presenter;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.event.CILinkClickEvent;
import se.ixanon.ixui.client.event.CILinkClickEventHandler;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.WaitDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.StreamerStatus;
import se.ixanon.ixui.shared.TunerStatus;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class InterfaceStatusPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header, String interface_type);
		void clearIdle();
		void setCiMenu(boolean isCiMenu);
		void buildCiMenu(TunerStatus tunerStatus);
		
		void buildInterface(Interface my_interface);
		void buildTuner(TunerStatus tuner_status, String interface_type);
		
		void buildTableInfoch(StreamerStatus streamer_status);
		void buildTable(StreamerStatus streamer_status, String interface_type);
		void buildTable(StreamerStatus streamer_status, boolean isWebradio);
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private final String interface_pos;
	private final String interface_type;
	private String command;
	//private String menu_title = "";
	private boolean isCiMenu = false;
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			fetchData();	
		}
	};
	  
	public InterfaceStatusPresenter(Display view, String interface_pos, String interface_type) {
		this.display = view;
		this.interface_pos = interface_pos;
		this.interface_type = interface_type;
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
		
		
		presenterBus.addHandler(CILinkClickEvent.TYPE, new CILinkClickEventHandler() {

			@Override
			public void onCILinkClick(CILinkClickEvent event) {
				
				Session.getInstance().getRpcService().interfaceCommand(interface_pos, "mmi_answer " + event.getId(), new AsyncCallback<Response>() {

					@Override
					public void onFailure(Throwable caught) {
						
					}

					@Override
					public void onSuccess(Response result) {
						fetchData();
						RootPanel.get("overlay").add(new WaitDialog());
						Session.getInstance().getTimer().cancel();
						Session.getInstance().getTimer().scheduleRepeating(5000);
					}
					
				});
				
			}
			
		});
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				
				if(event.getType().equals("log")) {
					//Window.Location.assign(GWT.getHostPageBaseURL() + "Ixui.html#interface-log/id=" + interface_pos);
					
					SessionKeys log_link = new SessionKeys("interface-log");
					log_link.getKeys().put(SessionKeys.Type.INTERFACE_POS, interface_pos);
					
					Session.getInstance().getAppBus().fireEvent(new HistoryEvent(log_link));
					
					
				} else {
					
					command = event.getType();
					
					if(event.getType().equals("start")) {
						command = "stream";
					}
					
					if(event.getType().equals("ci menu")) {
						command = "mmi_open";
						
						if(isCiMenu) {
							command = "mmi_close";
						} else {
							RootPanel.get("overlay").add(new WaitDialog());
							Session.getInstance().getTimer().cancel();
							Session.getInstance().getTimer().scheduleRepeating(5000);
						}
					}
					
					Session.getInstance().getRpcService().interfaceCommand(interface_pos, command, new AsyncCallback<Response>() {

						@Override
						public void onFailure(Throwable caught) {
							
						}

						@Override
						public void onSuccess(Response result) {
							if(result.isSuccess()) {
								
								
								if(command.contains("mmi_")) {
									
									isCiMenu = !isCiMenu;
									
									display.setCiMenu(isCiMenu);
									
									
								}
								
								fetchData();
								
							} else {
								RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error.", "exclamation-triangle"));
							}
						}
						
					});
					
				}
				
				
				
			}
			
			
		});
			
	}
	
	private void init() {
		display.setHeader(new Header("Status: " + interface_pos, "info-circle"), interface_type);
	}
	
	public void fetchData() {
		Session.getInstance().getRpcService().getInterface(interface_pos, true, new AsyncCallback<Interface>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Interface result) {
				display.buildInterface(result);
				
				boolean idle = result.getStatus().equals("idle");
				
				if(idle) {
					display.clearIdle();
				} else {
					fetchData2();
				}
				
			}
			
		});
	}
	
	public void fetchData2() {
		
		Session.getInstance().getRpcService().interfaceTunerStatus(interface_pos, interface_type, new AsyncCallback<TunerStatus>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(TunerStatus result) {
				if(result != null) {
					
					display.buildTuner(result, interface_type);
					
					if(interface_type.equals("dsc")) {
						display.buildCiMenu(result);
					}
					
					
					
				}
				
				fetchData3();
			}
			
		});
		
	}
	
	public void fetchData3() {
		Session.getInstance().getRpcService().interfaceStreamerStatus(interface_pos, interface_type, new AsyncCallback<StreamerStatus>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(StreamerStatus result) {
				
				if(result != null) {
					
					if(interface_type.equals("infoch")) {
						display.buildTableInfoch(result);
					} else if(interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
						display.buildTable(result, interface_type.equals("webradio"));
					} else {
						display.buildTable(result, interface_type);
					}
					
					
				}
				
			}
			
		});
	}
	
}