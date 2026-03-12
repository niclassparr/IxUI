package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.CILinkClickEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class CIMenuLink extends Composite implements ClickHandler {

	private int id;
	
	public CIMenuLink(int id, String name) {
		
		this.id = id;
		
		FlowPanel mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		FocusPanel link_panel = new FocusPanel();
		
		FlowPanel link = new FlowPanel();
		link_panel.add(link);
		mainPanel.add(link_panel);
		
		HTML html = new HTML("<i class='fa fa-caret-square-o-right aria-hidden='true'></i>");
		html.setStyleName("icon ci_menu");
		
		Label label = new Label(name);
		label.setStyleName("inline");
		
		link.add(html);
		link.add(label);
		
		link_panel.addClickHandler(this);
	}
		
	@Override
	public void onClick(ClickEvent event) {
		Session.getInstance().getHandlerManager().fireEvent(new CILinkClickEvent(id));
	}
	

}
