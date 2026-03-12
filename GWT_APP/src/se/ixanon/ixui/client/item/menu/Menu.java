package se.ixanon.ixui.client.item.menu;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.UnitInfo;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;

public class Menu extends Composite {

	private FlowPanel mainPanel;
	
	private FlowPanel push = new FlowPanel();
	private PushButton pushButton = new PushButton();
	private HTML infoLabel = new HTML();
	
	private LogoutLabel logoutButton = new LogoutLabel();
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			update();
		}
	};
	
	public Menu() {
		
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("menu-outer");
		initWidget(mainPanel);
		
		Session.getInstance().getRpcService().getUnitInfo(new AsyncCallback<UnitInfo>() {

			@Override
			public void onFailure(Throwable caught) {
				infoLabel.setHTML("<div class='logo inline'></div>");
				//build(true, true);
			}

			@Override
			public void onSuccess(UnitInfo result) {
				infoLabel.setHTML("<div class='logo inline'></div><div class='svh inline'><b>Hostname:</b> " + result.getHostname() + "<br><b>Serial:</b> " + result.getSerial() + "<br><b>Version:</b> " + result.getVersion());
				build(result.isCloud(), result.isForcedContent());
			}
			
		});
		
	}
	
	private void build(boolean cloud, boolean forcedContent) {
		
		FlowPanel info = new FlowPanel();
		info.setStyleName("info grey");
				
		infoLabel.setStyleName("");
		info.add(infoLabel);
		push.setStyleName("info");
		
		Label pushLabel = new Label("Please push config to use new settings.");
		pushLabel.setStyleName("inline");
		
		push.add(pushLabel);
		push.add(pushButton);
		
				
		FlowPanel menubar = new FlowPanel();
		menubar.setStyleName("menu-bar");
		
		menubar.add(new Link("Network", "globe", "menu-item", true, new SessionKeys("network")));
		menubar.add(new Link("Settings", "wrench", "menu-item", true, new SessionKeys("settings")));
		
		if(cloud) {
			menubar.add(new Link("Cloud", "cloud", "menu-item", true, new SessionKeys("cloud")));
		}
		
		menubar.add(new Link("Interfaces", "hdd-o", "menu-item", true, new SessionKeys("interfaces")));
		menubar.add(new Link("Layout", "list-ol", "menu-item", true, new SessionKeys("layout")));
		
		if(forcedContent) {
			menubar.add(new Link("Force Content", "arrow-circle-right", "menu-item", true, new SessionKeys("force-content")));
		}
		
		menubar.add(new Link("Commands", "power-off", "menu-item", true, new SessionKeys("commands")));
		//menubar.add(new MenuLink("Update", "update", "server"));
		
		
		logoutButton.setStyleName("menu-item");
		menubar.add(logoutButton);
		
		//Image image = new Image("style/images/shadow.png");
		//image.setStyleName("shadow");
		//menubar.add(image);
		
		FlowPanel shadow = new FlowPanel();
		shadow.setStyleName("menu-shadow");
		menubar.add(shadow);
		
		shadow.add(new InlineLabel());
		shadow.add(new InlineLabel());
		
		mainPanel.add(info);
		mainPanel.add(push);
		mainPanel.add(menubar);
		
		update();
		
	}
	
	private void update() {
		Session.getInstance().getRpcService().isConfigChanged(Session.getInstance().getSessionKey(), new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				//updateTimer.cancel();
				Session.getInstance().setMenuTimer(updateTimer);
				Session.getInstance().getMenuTimer().schedule(5000);
				
				push.removeStyleName("active-" + !result);
				
				push.addStyleName("active-" + result);
				pushButton.toggleFlash(result);
				
			}
			
		});
	}
	
	
}
