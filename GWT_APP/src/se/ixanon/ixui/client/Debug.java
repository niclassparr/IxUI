package se.ixanon.ixui.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class Debug extends Composite {
	
	private static Debug instance;
	private FlowPanel mainPanel;

	private Debug() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		Label header = new Label("Debug");
		header.setStyleName("header");
		mainPanel.add(header);
	}

	public static synchronized Debug getInstance() {
	    if (instance == null)
	        instance = new Debug();
	    return instance;
	}
	
	public void printLine(String text){
		HTML html = new HTML(text);
		html.setStyleName("debug-row");
		mainPanel.add(html);
	}
	
	public static native void log( String s ) 
	/*-{ console.log( s ); }-*/;
}
