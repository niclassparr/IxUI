package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.CheckFieldClickedEvent;
import se.ixanon.ixui.client.event.CheckFieldClickedEventHandler;
import se.ixanon.ixui.client.event.EmmUpdateEvent;
import se.ixanon.ixui.client.event.EmmUpdateEventHandler;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.event.ListChangeEvent;
import se.ixanon.ixui.client.event.ListChangeEventHandler;
import se.ixanon.ixui.client.event.ScanEvent;
import se.ixanon.ixui.client.event.ScanEventHandler;
import se.ixanon.ixui.client.event.ScanReadyEvent;
import se.ixanon.ixui.client.event.ScanReadyEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.ScanDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Config;
import se.ixanon.ixui.shared.Emm;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.Service;
import se.ixanon.ixui.shared.SessionKeys;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class InterfaceEditPresenter implements Presenter { 

	public interface Display {
		void addInfo(String text);
		void setHeader(Header header, boolean hls);
		void buildMultiBand(String interface_type);
		void buildScan(String interface_type, Config config);
		void build(Config config, String interface_type);
		void setScanDate(String scan_time);
		void buildTable(ArrayList<Service> services, String interface_type);
		void setCheckBoxIndex(int index);
		void setEmmList(String interface_type, Emm emm_result);
		Config getConfig(String interface_pos, String interface_type);
		ArrayList<Service> getServices(String interface_type);
		void setName(String interface_type);
		HasClickHandlers getSaveInterfaceButton();
		HasClickHandlers getSaveServicesButton();
		//HasClickHandlers getSaveMultiband();
		String getMultiBandInterfaceType();
		//void setButtonsEnabled(String interface_type, boolean isToggle);
		Widget asWidget();
	}
	
	private final Display display;
	private final String interface_pos;
	private final String interface_type;
	private final boolean isMultiBand;
	private final HandlerManager presenterBus = new HandlerManager(null);
	  
	public InterfaceEditPresenter(Display view, String interface_pos, String interface_type, boolean isMultiBand) {
		this.display = view;
		this.interface_pos = interface_pos;
		this.interface_type = interface_type;
		this.isMultiBand = isMultiBand;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		init();
	}
	
	public void bind() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		
		/*
		presenterBus.addHandler(ToggleChangeEvent.TYPE, new ToggleChangeEventHandler() {

			@Override
			public void onToggleChange(ToggleChangeEvent event) {
				
				display.setButtonsEnabled(interface_type, event.isToggle());
				
			}
			
		});
		*/
		
		
		presenterBus.addHandler(EmmUpdateEvent.TYPE, new EmmUpdateEventHandler() {

			@Override
			public void onEmmUpdate(EmmUpdateEvent event) {
				
				boolean isDsc = interface_type.equals("dsc");
				
				
				Session.getInstance().getRpcService().getCurrentEmmList(interface_pos, isDsc, new AsyncCallback<Emm>() {

					@Override
					public void onFailure(Throwable caught) {
						
					}

					@Override
					public void onSuccess(Emm result) {
						display.setEmmList(interface_type, result);
					}
					
				});
				
			}
			
		});
		
		presenterBus.addHandler(CheckFieldClickedEvent.TYPE, new CheckFieldClickedEventHandler() {

			@Override
			public void onCheckFieldClicked(CheckFieldClickedEvent event) {
				
				int index = event.getIndex();
				
				if(index != -1) {
					display.setCheckBoxIndex(index);
				}
				
				
			}
			
		});
		
		presenterBus.addHandler(ListChangeEvent.TYPE, new ListChangeEventHandler() {

			@Override
			public void onListChange(ListChangeEvent event) {

				if(event.getType().equals("Interface Type")) {
				
					String new_type = display.getMultiBandInterfaceType();
					
					if(interface_type.equals(new_type)) {
						RootPanel.get("overlay").add(new ConfirmDialog("Error", "Selected interface type is already in use.", "info-circle"));
					} else {

						Session.getInstance().getRpcService().updateInterfaceMultibandType(interface_pos, new_type, new AsyncCallback<Response>() {

							@Override
							public void onFailure(Throwable caught) {
								
							}

							@Override
							public void onSuccess(Response result) {
								
								if(result.isSuccess()) {
									//String link = "interface-edit/id=" + interface_pos + "/id2=" + result.getError() + "/id3=true";
									//History.newItem(link, true);
									
									SessionKeys session_keys = new SessionKeys("interface-edit");
									session_keys.getKeys().put(SessionKeys.Type.INTERFACE_POS, interface_pos);
									session_keys.getKeys().put(SessionKeys.Type.INTERFACE_TYPE, result.getError());
									session_keys.getKeys().put(SessionKeys.Type.MULTIBAND, ""+true);
									
									Session.getInstance().getAppBus().fireEvent(new HistoryEvent(session_keys));
									
									
								} else {
									RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error.", "exclamation-triangle"));
								}
								
							}
							
						});
						
					}
				}
				
				
				
				
			}
			
		});
		
		presenterBus.addHandler(ScanEvent.TYPE, new ScanEventHandler() {

			@Override
			public void onScan(ScanEvent event) {
				
				if(interface_type.equals("infoch")) {
					
					saveInfoch(true);
										
				} else {
					
					display.setName(interface_type);
					
					Session.getInstance().getRpcService().setConfig(Session.getInstance().getSessionKey(), display.getConfig(interface_pos, interface_type), interface_type, new AsyncCallback<Response>() {

						@Override
						public void onFailure(Throwable caught) {
							
						}

						@Override
						public void onSuccess(Response result) {
							if(result.isSuccess()) {
								scan();
							} else {
								RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error 1.", "exclamation-triangle"));
							}
						}
						
					});
					
					
				}
				
				
				
				
				
			}
			
		});
		
		presenterBus.addHandler(ScanReadyEvent.TYPE, new ScanReadyEventHandler() {

			@Override
			public void onScanReady(ScanReadyEvent event) {
				Session.getInstance().getRpcService().interfaceScanResult(interface_pos, new AsyncCallback<ArrayList<Service>>() {

					@Override
					public void onFailure(Throwable caught) {
						
					}

					@Override
					public void onSuccess(ArrayList<Service> result) {
						
						Collections.sort(result, Service.Comparators.NAME);
						
						display.buildTable(result, interface_type);
					}
					
				});
			}
			
		});
		
		display.getSaveInterfaceButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				
				if(interface_type.equals("infoch")) {
					saveInfoch(false);
				} else {
				
				
					Session.getInstance().getRpcService().setConfig(Session.getInstance().getSessionKey(), display.getConfig(interface_pos, interface_type), interface_type, new AsyncCallback<Response>() {
	
						@Override
						public void onFailure(Throwable caught) {
							
						}
	
						@Override
						public void onSuccess(Response result) {
							if(result.isSuccess()) {
								RootPanel.get("overlay").add(new ConfirmDialog("Saved", "Your changes has been saved.", "download"));
							} else {
								RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error.", "exclamation-triangle"));
							}
						}
						
					});
					
				}
				
			}
			
		});
		
		display.getSaveServicesButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {

				Session.getInstance().getRpcService().saveServices(display.getServices(interface_type), interface_type, interface_pos, new AsyncCallback<Response>() {

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
						
						presenterBus.fireEvent(new EmmUpdateEvent());						
						
					}
					
				});
				
				
			}
			
		});
		
	}
	
	private void scan() {
		
		Session.getInstance().getRpcService().interfaceSet(Session.getInstance().getSessionKey(), interface_pos, interface_type, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ScanDialog(interface_pos));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error 2.", "exclamation-triangle"));
				}
			}
			
		});
		
		
	}
		
	private void init() {
		
		if(!interface_type.equals("infoch")) {
			fetchData();
		} else {
			
			Session.getInstance().getRpcService().getInterfaceInfoch(interface_pos, new AsyncCallback<Config>() {

				@Override
				public void onFailure(Throwable caught) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onSuccess(Config result) {
					
					display.setHeader(new Header("Config: " + interface_pos + " - " + Helper.translate(interface_type), "cogs"), interface_type.equals("hls2ip"));
					display.buildScan(interface_type, result);
					fetchData2();
					
				}
				
			});
			
			
			
		}
		
	}
	
	public void fetchData() {
		Session.getInstance().getRpcService().getConfig(Session.getInstance().getSessionKey(), interface_pos, interface_type, new AsyncCallback<Config>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Config result) {
				
				boolean showBack = true;
				
				if(isMultiBand) {
					display.setHeader(new Header("Multiband", "cube"), false);
					display.buildMultiBand(interface_type);
					
					showBack = false;
					
				}
				
				display.setHeader(new Header("Config: " + interface_pos + " - " + Helper.translate(interface_type), "cogs", showBack), interface_type.equals("hls2ip"));
				
				display.build(result, interface_type);
				
				boolean temp = !interface_type.equals("dsc");
				
				if(interface_type.equals("webradio") && !Session.getInstance().isCloud()) {
					
					temp = false;
						
					display.addInfo("<b>Supported formats for radio services:</b>"
							+ "<p><b>1. HLS, M3U8</b><br>ex: http://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d48000.norewind.m3u8</p>"
							+ "<p><b>2. M3U</b><br>ex: https://sverigesradio.se/topsy/direkt/132-mp3.m3u</p>"
							+ "<p><b>3. Audiostream (mp3 or aac)</b><br>ex: https://wr05-ice.stream.khz.se/wr05_aac</p>");
				}
								
				if(temp) {
					fetchData2();
				}
				
			}
			
		});
	}
	
	public void fetchData2() {
		Session.getInstance().getRpcService().getServices(Session.getInstance().getSessionKey(), interface_pos, new AsyncCallback<ArrayList<Service>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Service> result) {
				display.buildTable(result, interface_type);
				
				if(!interface_type.equals("infostreamer")) {
					if(!interface_type.equals("hdmi2ip")) {
						if(!interface_type.equals("dvbhdmi")) {
							fetchData3();
						}
					}
					
				}
				
			}
			
		});
	}
	
	public void fetchData3() {
		Session.getInstance().getRpcService().getInterfaceScanTime(Session.getInstance().getSessionKey(), interface_pos, new AsyncCallback<Date>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Date result) {
				
				if(result != null) {
					String dateString = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm").format(result);
					display.setScanDate(dateString);
				}
				
			}
			
		});
	}
	
	private void saveInfoch(boolean isScan) {
		
		Session.getInstance().getRpcService().setInterfaceInfoch(display.getConfig(interface_pos, interface_type), isScan, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				
				if(result.isSuccess()) {
					RootPanel.get("overlay").add(new ScanDialog(interface_pos));
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Saved", "Your changes has been saved.", "download"));
				}
				
				
			}
			
		});
	}
	
}