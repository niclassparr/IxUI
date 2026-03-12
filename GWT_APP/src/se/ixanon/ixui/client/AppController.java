package se.ixanon.ixui.client;

import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.client.event.HistoryEventHandler;
import se.ixanon.ixui.client.item.dialog.LoginDialog;
import se.ixanon.ixui.client.item.menu.Menu;
import se.ixanon.ixui.client.presenter.CloudPresenter;
import se.ixanon.ixui.client.presenter.CommandsPresenter;
import se.ixanon.ixui.client.presenter.ForcedContentPresenter;
import se.ixanon.ixui.client.presenter.FrontPagePresenter;
import se.ixanon.ixui.client.presenter.HlsWizardPresenter;
import se.ixanon.ixui.client.presenter.InterfaceEditPresenter;
import se.ixanon.ixui.client.presenter.InterfaceLogPresenter;
import se.ixanon.ixui.client.presenter.InterfaceStatusPresenter;
import se.ixanon.ixui.client.presenter.InterfacesPresenter;
import se.ixanon.ixui.client.presenter.NetworkPresenter;
import se.ixanon.ixui.client.presenter.Presenter;
import se.ixanon.ixui.client.presenter.RoutesPresenter;
import se.ixanon.ixui.client.presenter.SettingsPresenter;
import se.ixanon.ixui.client.presenter.UpdatePresenter;
import se.ixanon.ixui.client.view.CloudView;
import se.ixanon.ixui.client.view.CommandsView;
import se.ixanon.ixui.client.view.ForcedContentView;
import se.ixanon.ixui.client.view.FrontPageView;
import se.ixanon.ixui.client.view.HlsWizardView;
import se.ixanon.ixui.client.view.InterfaceEditView;
import se.ixanon.ixui.client.view.InterfaceLogView;
import se.ixanon.ixui.client.view.InterfaceStatusView;
import se.ixanon.ixui.client.view.InterfacesView;
import se.ixanon.ixui.client.view.NetworkView;
import se.ixanon.ixui.client.view.RoutesView;
import se.ixanon.ixui.client.view.SettingsView;
import se.ixanon.ixui.client.view.UpdateView;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.SessionKeys.Type;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;

public class AppController implements Presenter {
	private HasWidgets container;
	private String currentToken = null;
	private boolean first = true;
	/*
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			Window.Location.reload();
		}
	};
	*/
	
	public AppController() {
		bind();
	}
	
	private void bind() {

		Session.getInstance().getAppBus().addHandler(HistoryEvent.TYPE, new HistoryEventHandler() {

			@Override
			public void onHistory(HistoryEvent event) {
				
				
				if (Session.getInstance().getTimer() != null && Session.getInstance().getTimer().isRunning()) {
					Session.getInstance().getTimer().cancel();
				}
				
				if (Session.getInstance().getMenuTimer() != null && Session.getInstance().getMenuTimer().isRunning()) {
					Session.getInstance().getMenuTimer().cancel();
				}
				
				
				
				
				
				if(event.isBack()) {
					Session.getInstance().popSessionKey();
				} else {
					Session.getInstance().addSessionKey(event.getKeys());
				}

				valSession();

			}

		});
		
	}

	public void valSession() {
		
		Debug.log("asd 1");
		
		//currentToken = event.getValue();

		Session.getInstance().getRpcService().validateSession(Session.getInstance().getSessionKey(), Session.getInstance().getUsername(), new AsyncCallback<Boolean>(){

			@Override
			public void onFailure(Throwable caught) {
				//FIXME
				//RootPanel.get("overlay").add(new ConfirmDialog("Error", "Server not responding maybe it is restarting or offline.", "exclamation-triangle"));
				//updateTimer.schedule(10000);
			}

			@Override
			public void onSuccess(Boolean result) {
				
				Debug.log("asd validate session " + result);
				
				if(result) {
					
					Session.getInstance().getRpcService().checkPersmission(Session.getInstance().getSessionKey(), Session.getInstance().getCurrentSessionKey(), new AsyncCallback<SessionKeys>() {

						@Override
						public void onFailure(Throwable caught) {
							
						}

						@Override
						public void onSuccess(SessionKeys result) {
							
							Debug.log("asd check permission");
							
							Session.getInstance().setCloud(result.isCloud());
							
							RootPanel.get("menu").clear();
							RootPanel.get("menu").add(new Menu());
							
							if(first) {
								//RootPanel.get("menubar").add(new MenuBar(rpcService));
								first = false;
							}
							setPresenter(result);
						}
						
					});
					
					
					
				} else {
					
					Debug.log("asd login dialog");
					
					RootPanel.get("overlay").add(new LoginDialog());
					
					RootPanel.get("menubar").clear();
					RootPanel.get("menu").clear();
					RootPanel.get("main").clear();
					
					first = true;
				}
			}
			
		});
		
	}

	private void setPresenter(SessionKeys session_keys) {
		
		/*
		String id = "";
		String id2 = null;
		String id3 = null;
		String temp_id = null;
		String temp_id2 = null;
		
		if(token.contains("/id=")){
			String[] parts = token.split("/id=");
			token = parts[0];
			temp_id = parts[1];
			
			if(temp_id.contains("/id2=")) {
				String[] parts2 = temp_id.split("/id2=");
				id = parts2[0];
				temp_id2 = parts2[1];
				
				if(temp_id2.contains("/id3=")) {
					String[] parts3 = temp_id2.split("/id3=");
					id2 = parts3[0];
					id3 = parts3[1];
				} else {
					id2 = temp_id2;
				}
								
			} else {
				id = temp_id;
			}
		}
		*/
		
		String token = session_keys.getToken();
		
		Debug.log("asd set presenter " + token);
		
		Presenter presenter = null;
		
		if(token == null || token.equals("dashboard")) {
			presenter = new FrontPagePresenter(new FrontPageView(), "Welcome");
		} else if(token.equals("interfaces")) {
			presenter = new InterfacesPresenter(new InterfacesView());
		} else if(token.equals("interface-edit")) {
			presenter = new InterfaceEditPresenter(new InterfaceEditView(), session_keys.getKey(Type.INTERFACE_POS), session_keys.getKey(Type.INTERFACE_TYPE), Boolean.valueOf(session_keys.getKey(Type.MULTIBAND)));
		} else if(token.equals("interface-status")) {
			presenter = new InterfaceStatusPresenter(new InterfaceStatusView(), session_keys.getKey(Type.INTERFACE_POS), session_keys.getKey(Type.INTERFACE_TYPE));
		} else if(token.equals("interface-log")) {
			presenter = new InterfaceLogPresenter(new InterfaceLogView(), session_keys.getKey(Type.INTERFACE_POS));
		} else if(token.equals("layout")) {
			presenter = new RoutesPresenter(new RoutesView());
		} else if(token.equals("settings")) {
			presenter = new SettingsPresenter(new SettingsView());
		} else if(token.equals("network")) {
			presenter = new NetworkPresenter(new NetworkView());
		} else if(token.equals("commands")) {
			presenter = new CommandsPresenter(new CommandsView());
		} else if(token.equals("update")) {
			presenter = new UpdatePresenter(new UpdateView());
		} else if(token.equals("cloud")) {
			presenter = new CloudPresenter(new CloudView());
		} else if(token.equals("hls-wizard")) {
			presenter = new HlsWizardPresenter(new HlsWizardView());
		} else if(token.equals("force-content")) {
			presenter = new ForcedContentPresenter(new ForcedContentView());
		}
		
		
		
		if (presenter != null) {
			presenter.go(container);
		}
		
		
	}
	
	public void go(HasWidgets container) {
		this.container = container;
		
		Session.getInstance().setSessionKey(Cookies.getCookie("session"));
		Session.getInstance().setUsername(Cookies.getCookie("username"));
		
		//History.fireCurrentHistoryState();
		Session.getInstance().getAppBus().fireEvent(new HistoryEvent(new SessionKeys("dashboard")));
	}
	
}
	
	
