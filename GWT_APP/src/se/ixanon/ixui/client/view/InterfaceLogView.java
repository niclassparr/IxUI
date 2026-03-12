package se.ixanon.ixui.client.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.EventButton;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.presenter.InterfaceLogPresenter;

public class InterfaceLogView extends Composite implements InterfaceLogPresenter.Display {

	private FlowPanel mainPanel;
	private HTML html = new HTML();
	private EventButton updateButton = new EventButton("Update", "refresh", "blue");
	
	public InterfaceLogView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
		
		mainPanel.add(html);
		mainPanel.add(updateButton);
	}
	
	public void setLog(String text) {
		html.setHTML("<pre>"+text+"</pre>");
	}
		
	public Widget asWidget() {
		return this;
	}
}
