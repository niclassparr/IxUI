package se.ixanon.ixui.client.item.table;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class UsageItem extends Composite {

	public UsageItem(int count, int max) {
		
		float f = (float) count/max * 100;
		int p = Math.round(f);
		int w = p - 100;
		int x = Math.abs(w);
		
		if(p >= 100) {
			x = 0;
		}
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("usage");
		initWidget(mainPanel);
		
		Label label = new Label(p+"%");
		label.setStyleName("inline");
		
		HTML html = new HTML("<span style='width:"+x+"%;'></span>");
		html.setStyleName("gradient inline");
		
		mainPanel.add(label);
		mainPanel.add(html);
		
	}
}
