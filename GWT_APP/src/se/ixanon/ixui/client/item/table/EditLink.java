package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;

public class EditLink extends Composite {

	public EditLink(String title, String link, String icon) {
		build(title, link, icon, null, false);
	}
	
	public EditLink(String title, String link, String icon, String icon2, boolean emm) {
		build(title, link, icon, icon2, emm);
	}
	
	private void build(String title, String link, String icon, String icon2, boolean emm) {
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("");
		initWidget(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon table");
		
		Hyperlink hyperlink = new Hyperlink(title, link);
		hyperlink.setStyleName("inline link");
		
		mainPanel.add(html);
		mainPanel.add(hyperlink);
		
		if(icon2 != null) {
			HTML html2 = new HTML("<i class='fa fa-"+icon2+"' aria-hidden='true'></i>");
			html2.setStyleName("icon table");
			mainPanel.add(html2);
		}
		
		if(emm) {
			Image image = new Image("style/images/emm.png");
			image.setStyleName("inline");
			mainPanel.add(image);
		}
		
	}
}
