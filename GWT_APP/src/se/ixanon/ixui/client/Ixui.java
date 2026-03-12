package se.ixanon.ixui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

public class Ixui implements EntryPoint {

	public void onModuleLoad() {
		
		RootPanel.get("debug").add(Debug.getInstance());
		
		Session.getInstance().setRpcService(GWT.create(IxuiService.class));
		
		AppController appViewer = new AppController();
		appViewer.go(RootPanel.get("main"));
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(false);
		
	}
}
