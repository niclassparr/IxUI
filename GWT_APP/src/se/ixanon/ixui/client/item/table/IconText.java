package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

public class IconText extends Composite {

	public IconText(String text, String icon) {
		build(text, icon, null);
	}
	
	public IconText(String text, String icon, String style) {
		build(text, icon, style);
	}
	
	private void build(String text, String icon, String style) {
		
		String mainStyle = "";
		String htmlStyle = "table";
		
		if(style != null) {
			mainStyle = style;
			htmlStyle = "";
		}
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName(mainStyle);
		initWidget(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon " + htmlStyle);
		
		HTML label = new HTML(text);
		label.setStyleName("inline");
		
		mainPanel.add(html);
		mainPanel.add(label);
	}
}
