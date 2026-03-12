package se.ixanon.ixui.client.presenter;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ButtonClickEvent;
import se.ixanon.ixui.client.event.ButtonClickEventHandler;
import se.ixanon.ixui.client.item.table.Header;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

public class InterfaceLogPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void setLog(String text);
		Widget asWidget();
	}
	
	private final HandlerManager presenterBus = new HandlerManager(null);
	private final Display display;
	private String interface_pos;
	  
	public InterfaceLogPresenter(Display view, String interface_pos) {
		this.display = view;
		this.interface_pos = interface_pos;
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
		
		presenterBus.addHandler(ButtonClickEvent.TYPE, new ButtonClickEventHandler() {

			@Override
			public void onButtonClick(ButtonClickEvent event) {
				fetchData();
			}
			
		});
	}
	
	private void init() {
		display.setHeader(new Header("Log: " + interface_pos, "file-text-o"));
	}
	
	public void fetchData() {
		Session.getInstance().getRpcService().interfaceLog(interface_pos, new AsyncCallback<String>() {

			@Override
			public void onFailure(Throwable caught) {
								
			}

			@Override
			public void onSuccess(String result) {
				display.setLog(result);
			}
			
		});
	}
}