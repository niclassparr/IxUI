package se.ixanon.ixui.client.item.menu;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;

public class MenuLink extends Composite {
	
	public MenuLink(String link, String name, String icon) {
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("menu-item");
		initWidget(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon menu");
		
		Hyperlink hyperlink = new Hyperlink(link, name);
		hyperlink.setStyleName("inline link");
		
		mainPanel.add(html);
		mainPanel.add(hyperlink);
	}	

}
