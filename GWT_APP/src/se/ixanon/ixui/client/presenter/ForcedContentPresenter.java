package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.event.PopupClosedEventHandler;
import se.ixanon.ixui.client.item.dialog.ConfirmDialog;
import se.ixanon.ixui.client.item.dialog.ForcedContentControlDialog;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.shared.ForcedContent;
import se.ixanon.ixui.shared.Media;
import se.ixanon.ixui.shared.Response;

public class ForcedContentPresenter implements Presenter { 

	public interface Display {
		void setHeader(Header header);
		void setDisabled();
		void setValues(HashMap<Integer, ForcedContent> map, ArrayList<Media> media);
		ArrayList<ForcedContent> getValues();
		HasClickHandlers getControlButton(); 
		HasClickHandlers getSaveButton();
		Widget asWidget();
	}
	
	private final Display display;
	private final HandlerManager presenterBus = new HandlerManager(null);
	private ArrayList<Media> media;
	  
	public ForcedContentPresenter(Display view) {
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
		
		Session.getInstance().getHandlerManager().addHandler(PopupClosedEvent.TYPE, new PopupClosedEventHandler() {

			@Override
			public void onPopupClosed(PopupClosedEvent event) {
				Session.getInstance().setHandlerManager(presenterBus);
			}
			
		});
		
		display.getControlButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").add(new ForcedContentControlDialog());
				
			}
			
		});
		
		display.getSaveButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				save();
			}
			
		});
	}
	
	private void fetchData() {

		display.setHeader(new Header("Force Content", "arrow-circle-right"));
		
		
		Session.getInstance().getRpcService().getEnabledType("forced_content", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				
				if(result) {
					fetchData2();
				} else {
					display.setDisabled();
				}
				
			}
			
		});
		
		
	}
	
	private void fetchData2() {
		
		Session.getInstance().getRpcService().getMedia(new AsyncCallback<ArrayList<Media>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Media> result) {
				media = new ArrayList<Media>(result);
				fetchData3();
			}
			
		});
		
	}
	
	private void fetchData3() {
		
		Session.getInstance().getRpcService().getForcedContents(Session.getInstance().getSessionKey(), new AsyncCallback<HashMap<Integer, ForcedContent>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(HashMap<Integer, ForcedContent> result) {
				display.setValues(result, media);
			}
			
		});
		
	}
	
	private void save() {
		
		Session.getInstance().getRpcService().saveForcedContents(display.getValues(), new AsyncCallback<Response>() {

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