package se.ixanon.ixui.client.item.dialog;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ListChangeEvent;
import se.ixanon.ixui.client.event.ListChangeEventHandler;
import se.ixanon.ixui.client.event.PopupClosedEvent;
import se.ixanon.ixui.client.item.table.ForcedContentStatus;
import se.ixanon.ixui.shared.ForcedContent;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ForcedContentControlDialog extends Composite {
	
	private final HandlerManager presenterBus = new HandlerManager(null);
	private FlowPanel mainPanelInner = new FlowPanel();
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			getStatus();
		}
	};
	
	private void bind() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		Session.getInstance().setTimer(updateTimer);
		Session.getInstance().getTimer().schedule(0);
		
		presenterBus.addHandler(ListChangeEvent.TYPE, new ListChangeEventHandler() {

			@Override
			public void onListChange(ListChangeEvent event) {
				setStatus(event.getId(), event.getIndex());
			}
			
		});
		
		
	}
	
	public ForcedContentControlDialog() {
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		FlowPanel mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog mod-settings");
		wrapperPanel.add(mainPanel);
		
		mainPanelInner.setStyleName("dialog-inner");
		
		//HTML html = new HTML("<i class='fa fa-lightbulb-o' aria-hidden='true'></i>");
		//html.setStyleName("icon header");
				
		Label headerLabel = new Label("Force Content Control");
				
		headerLabel.setStyleName("header");
				
		//mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(mainPanelInner);	
		
		initWidget(wrapperPanel);
				
		Button cancelButton = new Button("Close");
		cancelButton.setStyleName("btn blue login");
		mainPanel.add(cancelButton);
		
		cancelButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
				Session.getInstance().getTimer().cancel();
				Session.getInstance().getHandlerManager().fireEvent(new PopupClosedEvent());
			}
			
		});
		
		bind();
		
	}
	
	private void setStatus(int id, int index) {
		
		Session.getInstance().getTimer().cancel();
		
		Session.getInstance().getRpcService().saveForcedContentOverrideStatus(id, index, new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Void result) {
				getStatus();
			}
			
		});
		
	}
	
	private void getStatus() {
		
		Session.getInstance().getRpcService().getEnabledForcedContents(Session.getInstance().getSessionKey(), new AsyncCallback<ArrayList<ForcedContent>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<ForcedContent> result) {
				
				//mainPanelInner.clear();
				
				if(mainPanelInner.getWidgetCount() != result.size()) {
					for (int i = 0; i < result.size(); i++) {
						ForcedContentStatus fcs = new ForcedContentStatus(result.get(i));
						mainPanelInner.add(fcs);
					}
				} else {
					
					for (int i = 0; i < result.size(); i++) {
						ForcedContentStatus fcs = (ForcedContentStatus) mainPanelInner.getWidget(i);
						fcs.setStatus(result.get(i).getSignalStatus(), result.get(i).isComStatus());
					}
					
				}
				
				Session.getInstance().getTimer().schedule(1000);
				
			}
		});
		
	}
}
