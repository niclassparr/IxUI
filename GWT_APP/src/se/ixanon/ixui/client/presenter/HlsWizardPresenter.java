package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.event.CheckFieldClickedEvent;
import se.ixanon.ixui.client.event.CheckFieldClickedEventHandler;
import se.ixanon.ixui.client.event.FilterClickEvent;
import se.ixanon.ixui.client.event.FilterClickEventHandler;
import se.ixanon.ixui.client.event.FilterToggleEvent;
import se.ixanon.ixui.client.event.FilterToggleEventHandler;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.event.ScanEvent;
import se.ixanon.ixui.client.event.ScanEventHandler;
import se.ixanon.ixui.client.event.ScanReadyEvent;
import se.ixanon.ixui.client.event.ScanReadyEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.ScanDialog;
import se.ixanon.ixui.client.item.table.CheckField;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.Route;
import se.ixanon.ixui.shared.Service;
import se.ixanon.ixui.shared.SessionKeys;

public class HlsWizardPresenter implements Presenter { 

	public interface Display {
		boolean isSelected(int table, int index);
		void setSelectedAll(boolean value);
		void setHeader(Header header);
		void buildFilterMenu(List<String> tags);
		void buildTables(ArrayList<Service> services, ArrayList<Service> selected_services);
		void buildTable1(ArrayList<Service> selected_services);
		void buildTable2(int max, int count);
		//int countHls();
		//void setHlsActive(boolean active);
		HasClickHandlers getSaveServicesButton();
		HasClickHandlers getAllLink();
		Widget asWidget();
	}
	
	private final HandlerManager presenterBus = new HandlerManager(null);
	private final Display display;
	private ArrayList<Service> services;
	private ArrayList<Service> filterd_services = new ArrayList<Service>();
	private ArrayList<Service> selected_services = new ArrayList<Service>();
	private ArrayList<Route> routes;
	private String interface_pos = null;
	private int count_hls, max_hls = 0;
	private boolean all_flag = true;
	private List<String> all_tags = new ArrayList<String>();
	private List<String> user_tags = new ArrayList<String>();
	
	public HlsWizardPresenter(Display view) {
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
		
		presenterBus.addHandler(FilterToggleEvent.TYPE, new FilterToggleEventHandler() {

			@Override
			public void onFilterToggle(FilterToggleEvent event) {
				
				if(event.isToggle()) {
					user_tags.add(event.getTitle());
				} else {
					if(user_tags.contains(event.getTitle())) {
						user_tags.remove(event.getTitle());
					}
				}
				
				if(user_tags.isEmpty()) {
					//filterNew(all_tags);
					display.buildTables(services, null);
				} else {
					filterNew(user_tags);
				}
				
			}
			
		});
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				
				//Debug.log("click: " + event.getType());
				
				boolean flag = true;
				
				if(event.getType().equals("arrow-left")) {
					
					for (int i = selected_services.size()-1; i >= 0; --i) {
					
						if(display.isSelected(1, i)) {
							services.add(selected_services.get(i));
							selected_services.remove(i);
						}
						
					}
					
				}
				
				if(event.getType().equals("arrow-right")) {
					
					ArrayList<Service> temp = new ArrayList<>();
					
					if(!user_tags.isEmpty()) {
						temp.addAll(filterd_services);
					} else {
						temp.addAll(services);
					}
					
					
					int count = 0;
					
					//check if to many services
					for (int i = 0; i < temp.size(); ++i) {
						if(display.isSelected(0, i)) {
							count++;
						}
					}
					
					if((selected_services.size() + count) > max_hls) {
						
						flag = false;
						
						RootPanel.get("overlay").add(new ConfirmDialog("Warning", "To many channels selected.", "exclamation-triangle"));
					}
					
					ArrayList<String> removed_services = new ArrayList<>();
					
					if(flag) {
						
						for (int i = temp.size()-1; i >= 0; --i) {
							
							if(display.isSelected(0, i)) {
								selected_services.add(temp.get(i));
																
								removed_services.add(temp.get(i).getName());
								
							}
							
						}
						
					}
					
					for (int i = services.size()-1; i >= 0; --i) {
						
						if(removed_services.contains(services.get(i).getName())) {
							services.remove(i);
						}
						
					}
					
					
				}
				
			
				
				if(flag) {
					
					Collections.sort(services, Service.Comparators.NAME);
					Collections.sort(selected_services, Service.Comparators.NAME);
					
					
					
					display.buildTables(services, selected_services);
					//hls_func();
					display.buildTable2(max_hls, selected_services.size());
					
					if(!user_tags.isEmpty()) {
						filterNew(user_tags);
					}
					
					//display.setSelectedChannels(selected_services.size());
					
				}
				
				
				
				
				
			}
			
		});
		
		presenterBus.addHandler(ScanEvent.TYPE, new ScanEventHandler() {

			@Override
			public void onScan(ScanEvent event) {
				RootPanel.get("overlay").add(new ScanDialog(interface_pos));
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
						services = new ArrayList<Service>(result);
						selected_services.clear();
						user_tags.clear();
						
						Collections.sort(services, Service.Comparators.NAME);
						
						//selected services
						for (int i = 0; i < routes.size(); ++i) {
							
							inner:
							for (int j = 0; j < services.size(); ++j) {
								
								if(routes.get(i).getServiceName().equals(services.get(j).getName())) {
									selected_services.add(services.get(j));
									break inner;
								}
								
							}
							
						}
						
						//all tags						
						for (int i = 0; i < services.size(); ++i) {
							
							for(String filter : services.get(i).getFilters()) {
														
								if(!all_tags.contains(filter)) {
									all_tags.add(filter);
								}
								
							}
							
						}
						
						//remove services
						for (int i = services.size()-1; i >= 0; --i) {
							
							inner:
							for (int j = 0; j < selected_services.size(); ++j) {
								
								if(services.get(i).getHlsUrl().equals(selected_services.get(j).getHlsUrl())) {
									services.remove(i);
									break inner;
								}
								
							}
						}
						
						Collections.sort(all_tags);
						display.buildFilterMenu(all_tags);
						
						
						display.buildTables(services, selected_services);
						//hls_func();
						display.buildTable2(max_hls, selected_services.size());
						
						//display.setSelectedChannels(selected_services.size());
					}
					
				});
			}
			
		});
		
		/*
		presenterBus.addHandler(CheckFieldClickedEvent.TYPE, new CheckFieldClickedEventHandler() {

			@Override
			public void onCheckFieldClicked(CheckFieldClickedEvent event) {
				//hls_func();
				//display.buildTable2(max_hls, count_hls);
			}
			
		});
		*/
		display.getAllLink().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				if(selected_services.size() > 0) {
					
					display.setSelectedAll(all_flag);
					
					//display.buildTable1(selected_services);
					
					all_flag = !all_flag;
					
				}
				
			}
			
		});
		
		display.getSaveServicesButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				save();
			}
			
		});
		
	}
	
	public void fetchData() {
		
		Session.getInstance().getRpcService().getRoutes(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<Route>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Route> result) {
				routes = new ArrayList<Route>(result);
				fetchData2();
			}
			
		});
		
	}
	
	public void fetchData2() {
		display.setHeader(new Header("HLS Wizard", "magic"));
		
		Session.getInstance().getRpcService().getInterfacesHls(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<Interface>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Interface> result) {
				interface_pos = result.get(0).getPosition();
				max_hls = result.size();
			}
			
		});
		
	}
	
	private void save() {
		
		if(selected_services.size() > max_hls) {
			RootPanel.get("overlay").add(new ConfirmDialog("Warning", "To many channels selected.", "exclamation-triangle"));
		} else {
			
			for (int i = 0; i < selected_services.size(); ++i) {
				selected_services.get(i).setEnabled(true);
			}
			
			for (int i = 0; i < services.size(); ++i) {
				services.get(i).setEnabled(false);
			}
			
			ArrayList<Service> all_services = new ArrayList<Service>(selected_services);
			all_services.addAll(services);
			
			Session.getInstance().getRpcService().saveHlsWizardServices(Session.getInstance().getSessionKey(), all_services, new AsyncCallback<Response>() {

				@Override
				public void onFailure(Throwable caught) {
					
				}

				@Override
				public void onSuccess(Response result) {

					if(result.isSuccess()) {
						//RootPanel.get("overlay").add(new ConfirmDialog("Saved", "Your changes has been saved.", "download"));
						//History.newItem("layout", true);
						Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("layout")));
					} else {
						RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server error.", "exclamation-triangle"));
					}
					
				}
				
			});
			
		}
		
		
	}
	
	/*
	private void hls_func() {
		
		count_hls = display.countHls();
			
		boolean active;
		
		if(count_hls >= max_hls) {
			active = false;
		} else {
			active = true;
		}
			
		display.setHlsActive(active);
		
	}
	*/
	
	
	private void filterNew(List<String> tags) {

		Debug.log("filterNew " + tags.toString());
		
		filterd_services.clear();
		
		Debug.log("xxx1");
		
		for (int i = 0; i < services.size(); ++i) {

			Debug.log("--- start (found false) ---");

			services.get(i).setFound(false);
			List<String> item_filters = services.get(i).getFilters();

			Debug.log("item filters: " + item_filters.toString());

			int count_found = 0;

			user_loop:
			for(String user_filter : tags) {

				Debug.log("user filter: " + user_filter);

				item_loop:
				for(String item_filter : item_filters) {

					if(item_filter != null) {

						if(!item_filter.equals("")) {

							Debug.log("item filter: " + item_filter);

							if(item_filter.toLowerCase().contains(user_filter.toLowerCase())) {
								
								count_found++;
								
								Debug.log(count_found +  " = count_found " + item_filter + " == " + user_filter);

								//break user_loop;
							}

						}


					}
		        }
				
	        }


			if(count_found == tags.size()) {
				services.get(i).setFound(true);
			}
			
			if(services.get(i).isFound()) {
				filterd_services.add(services.get(i));
			}

		}
		
		display.buildTables(filterd_services, null);


	}
	
	
}