package se.ixanon.ixui.client.item.table;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ReorderEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

public class ReorderColumn extends Composite implements ClickHandler {

	private String name;
	private String table;
	private boolean reverse = true;
	
	public ReorderColumn(String name, String table) {
		
		this.name = name;
		this.table = table;
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("reorder-wrapper");
		initWidget(mainPanel);
		
		if(name != null && table != null) {
			
			HTML icon = new HTML("<i class='fa fa-sort' aria-hidden='true'></i>");
			icon.setStyleName("icon menu");
			
			Label label = new Label(name);
			label.setStyleName("inline link");
			
			mainPanel.add(icon);
			mainPanel.add(label);
			
			label.addClickHandler(this);
			
		}
		
		
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void onClick(ClickEvent event) {
		Session.getInstance().getHandlerManager().fireEvent(new ReorderEvent(name, reverse, table));
		
		reverse = !reverse;
	}
	

}
