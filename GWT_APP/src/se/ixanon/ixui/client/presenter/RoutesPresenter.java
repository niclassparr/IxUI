package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.Collections;

import se.ixanon.ixui.client.Debug;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.event.CheckFieldClickedEvent;
import se.ixanon.ixui.client.event.CheckFieldClickedEventHandler;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.event.ListChangeEvent;
import se.ixanon.ixui.client.event.ListChangeEventHandler;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.event.PopupClosedEventHandler;
import se.ixanon.ixui.client.event.ReorderEvent;
import se.ixanon.ixui.client.event.ReorderEventHandler;
import se.ixanon.ixui.client.event.TextFieldBlurEvent;
import se.ixanon.ixui.client.event.TextFieldBlurEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmCancelDialog;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.Bitrate;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.Route;
import se.ixanon.ixui.shared.SessionKeys;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class RoutesPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header, boolean isHls);
		ArrayList<String> checkDuplicateTextFieldValue();
		void buildLists(ArrayList<Interface> interfaces);
		void buildTable(ArrayList<Route> routes, boolean dvbc, boolean ip, boolean hls, boolean portal, boolean dvbc_net2);
		void buildTable2(ArrayList<Bitrate> bitrates, int max_mod, int max_dsc, int max_hls, int count_hls, boolean dvbc, boolean dvbc_net2, boolean hls);
		int countHls(boolean hls, boolean portal);
		void setHlsActive(boolean active, boolean hls, boolean portal);
		ArrayList<Route> getRoutes(boolean dvbc, boolean dvbc_net2, boolean hls, boolean portal, boolean is_ip);
		HasClickHandlers getSaveButton();
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private ArrayList<Bitrate> bitrates;
	private int max_mod = 0;
	private int max_dsc = 0;
	private int max_hls = 0;
	private int count_hls = 0;
	private boolean dvbc = false;
	private boolean dvbc_net2 = false;
	private boolean ip = false;
	private boolean hls = false;
	private boolean portal = false;
	private ArrayList<String> duplicates = new ArrayList<String>();
	  
	public RoutesPresenter(Display view) {
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
		
		presenterBus.addHandler(PopupClosedEvent.TYPE, new PopupClosedEventHandler() {

			@Override
			public void onPopupClosed(PopupClosedEvent event) {
				save();
			}
			
			
		});
		
		presenterBus.addHandler(TextFieldBlurEvent.TYPE, new TextFieldBlurEventHandler() {

			@Override
			public void onTextFieldBlur(TextFieldBlurEvent event) {
				
				duplicates = display.checkDuplicateTextFieldValue();
				
			}
			
		});
		
		presenterBus.addHandler(ReorderEvent.TYPE, new ReorderEventHandler() {

			@Override
			public void onReorder(ReorderEvent event) {
				reorder(event.getName(), event.getReverse());
			}
			
		});
		
		presenterBus.addHandler(CheckFieldClickedEvent.TYPE, new CheckFieldClickedEventHandler() {

			@Override
			public void onCheckFieldClicked(CheckFieldClickedEvent event) {
				hls_func();
				display.buildTable2(bitrates, max_mod, max_dsc, max_hls, count_hls, dvbc, dvbc_net2, hls);
			}
			
		});
		
		presenterBus.addHandler(ListChangeEvent.TYPE, new ListChangeEventHandler() {
			
			@Override
			public void onListChange(ListChangeEvent event) {
				display.buildTable2(bitrates, max_mod, max_dsc, max_hls, count_hls, dvbc, dvbc_net2, hls);
			}
			
		});
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				
				if(event.getType().equals("hls wizard")) {
					//History.newItem("hls-wizard", true);
					Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("hls-wizard")));
				}
				
			}
			
		});
		
		display.getSaveButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				if(duplicates.size() == 0) {
					save();
				} else {
					
					Collections.sort(duplicates);
					
					String message = "<b>The following warnings has been detected:</b><br>";
					
					for (int i = 0; i < duplicates.size(); i++) {
						message = message + duplicates.get(i) + "<br>";
					}
					
					RootPanel.get("overlay").add(new ConfirmCancelDialog("Duplicates found", message, "exclamation-triangle"));
				}
				
			}
			
		});
		
	}
	
	private void init() {
				
		Session.getInstance().getRpcService().getInterfaceTypes(new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<String> result) {
				display.setHeader(new Header("Layout", "list-ol"), result.contains("hls2ip"));
				fetchData();
			}
			
		});
	}
	
	private void reorder(String name, boolean reverse) {
		
		ArrayList<Route> reorder_routes = new ArrayList<Route>(display.getRoutes(dvbc, dvbc_net2, hls, portal, ip));
		
		if(name.equals("Service")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.SERVICE_NAME_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.SERVICE_NAME);
			}
		}
		
		if(name.equals("Interface")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.INTERFACE_POS_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.INTERFACE_POS);
			}
		}
		
		if(name.equals("LCN")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.LCN_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.LCN);
			}
		}
		
		if(name.equals("Out SID")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.OUT_SID_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.OUT_SID);
			}
		}
		
		if(name.equals("Out IP")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.OUT_IP_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.OUT_IP);
			}
		}
		
		if(name.equals("Descrambler")) {
			if(reverse) {
				Collections.sort(reorder_routes, Route.Comparators.DESCRAMBLER_POS_REVERSE);
			} else {
				Collections.sort(reorder_routes, Route.Comparators.DESCRAMBLER_POS);
			}
		}
		
		display.buildTable(reorder_routes, dvbc, ip, hls, portal, dvbc_net2);
	}
		
	public void fetchData() {
		Session.getInstance().getRpcService().getEnabledType("dvbc", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				dvbc = result;
				fetchData0();
			}
			
		});
	}
	
	public void fetchData0() {
		Session.getInstance().getRpcService().getEnabledType("dvbc_net2", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				dvbc_net2 = result;
				fetchDataIP();
			}
			
		});
	}
	
	public void fetchDataIP() {
		Session.getInstance().getRpcService().getEnabledType("ip", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				ip = result;
				fetchData1();
			}
			
		});
	}
	
	public void fetchData1() {
		Session.getInstance().getRpcService().getEnabledType("hls", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				hls = result;
				fetchData2();
			}
			
		});
	}
	
	public void fetchData2() {
		Session.getInstance().getRpcService().getEnabledType("portal", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				portal = result;
				fetchData3();
			}
			
		});
	}
	
	public void fetchData3() {
		Session.getInstance().getRpcService().getMaxBitrates("mod", new AsyncCallback<Integer>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Integer result) {
				max_mod = result;
				fetchData4();
			}
			
		});
	}
	
	public void fetchData4() {
		Session.getInstance().getRpcService().getMaxBitrates("dsc", new AsyncCallback<Integer>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Integer result) {
				max_dsc = result;
				fetchData4_1();
			}
			
		});
	}
	
	public void fetchData4_1() {
		Session.getInstance().getRpcService().getMaxBitrates("hls", new AsyncCallback<Integer>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Integer result) {
				max_hls = result;
				fetchData5();
			}
			
		});
	}
	
	public void fetchData5() {
		Session.getInstance().getRpcService().getInterfaces(Session.getInstance().getSessionKey(), false, new AsyncCallback<ArrayList<Interface>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(ArrayList<Interface> result) {
				display.buildLists(result);
				fetchData6();
			}
			
		});
	}
	
	public void fetchData6() {
		
		Session.getInstance().getRpcService().getRoutes(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<Route>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Route> result) {
				display.buildTable(result, dvbc, ip, hls, portal, dvbc_net2);
				display.checkDuplicateTextFieldValue();
				hls_func();
				fetchData7();
			}
			
		});
	}
	
	public void fetchData7() {
		
		Session.getInstance().getRpcService().getBitrates(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<Bitrate>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Bitrate> result) {
				
				bitrates = new ArrayList<Bitrate>(result);
				
				display.buildTable2(bitrates, max_mod, max_dsc, max_hls, count_hls, dvbc, dvbc_net2, hls);
			}
				
		});
	}
	
	private void hls_func() {
		
		if(hls) {
			
			count_hls = display.countHls(hls, portal);
			
			boolean active;
			
			if(count_hls >= max_hls) {
				active = false;
			} else {
				active = true;
			}
			
			display.setHlsActive(active, hls, portal);
		}
		
	}
	
	private void save() {
		
		Session.getInstance().getRpcService().updateRoutes(Session.getInstance().getSessionKey(), display.getRoutes(dvbc, dvbc_net2, hls, portal, ip), new AsyncCallback<Response>() {

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