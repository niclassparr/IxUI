package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.item.menu.Link;
import se.ixanon.ixui.shared.SessionKeys;

public class Header extends Composite {

	public Header(String name, String icon) {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("header");
		initWidget(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon header");
		
		Label label = new Label(name);
		label.setStyleName("inline");
		
		
		
		Link back = new Link("Back", "chevron-left", "back", false, null);
		
		/*
		FlowPanel backPanel = new FlowPanel();
		backPanel.setStyleName("back");
		
		HTML html_back = new HTML("<i class='fa fa-' aria-hidden='true'></i>");
		html_back.setStyleName("icon back");
		
		Label label_back = new Label("Back");
		label_back.setStyleName("inline");
		
		backPanel.add(html_back);
		backPanel.add(label_back);
		*/		
		
		mainPanel.add(html);
		mainPanel.add(label);
		mainPanel.add(back);
				
	}
	
	public Header(String name, String icon, boolean showBack) {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("header");
		initWidget(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon header");
		
		Label label = new Label(name);
		label.setStyleName("inline");
		
		mainPanel.add(html);
		mainPanel.add(label);
		
		
		if(showBack) {
			Link back = new Link("Back", "chevron-left", "back", false, null);
			mainPanel.add(back);
		}
		
		
	}
	
}
