package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.Collections;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.FilterClickEvent;
import se.ixanon.ixui.client.event.FilterClickEventHandler;
import se.ixanon.ixui.client.item.table.FilterMenu;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Interface;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

public class InterfacesPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void buildFilterMenu(ArrayList<String> types);
		void buildTable(ArrayList<Interface> interfaces);
		FilterMenu getFilterMenu();
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
	
	public InterfacesPresenter(Display view) {
		this.display = view;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		init();
		//fetchData();
	}
	
	public void bind() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		Session.getInstance().setTimer(updateTimer);
		Session.getInstance().getTimer().scheduleRepeating(5000);
		
		presenterBus.addHandler(FilterClickEvent.TYPE, new FilterClickEventHandler() {

			@Override
			public void onFilterClick(FilterClickEvent event) {
				
				filter = event.getTitle();
				
				display.getFilterMenu().setActive(filter);
				
				buildTable();
			}
			
		});
		
	}
	
	private void init() {
		
		display.setHeader(new Header("Interfaces", "hdd-o"));
		
		Session.getInstance().getRpcService().getInterfaceTypes(new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<String> result) {
				display.buildFilterMenu(result);
				fetchData();
			}
			
		});
		
	}
	
	public void fetchData() {
				
		Session.getInstance().getRpcService().getInterfaces(Session.getInstance().getSessionKey(), true, new AsyncCallback<ArrayList<Interface>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Interface> result) {
				
				interfaces = new ArrayList<Interface>(result); 
				Collections.sort(interfaces, Interface.Comparators.POS);
				
				buildTable();
			}
			
		});
	}
	
	private void buildTable() {
		
		if(filter.equals("All") ) {
			filtered_interfaces = new ArrayList<Interface>(interfaces);
		} else {
			filtered_interfaces = new ArrayList<Interface>();
			
			for (int i = 0; i < interfaces.size(); i++) {
				
				String translated_type = Helper.translate(interfaces.get(i).getType()); 
				
				if(translated_type.equals(filter)) {
					filtered_interfaces.add(interfaces.get(i));
				}
			}
		}
		
		display.buildTable(filtered_interfaces);
		
	}
}