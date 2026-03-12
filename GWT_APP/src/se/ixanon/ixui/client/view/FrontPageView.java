package se.ixanon.ixui.client.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.presenter.FrontPagePresenter;

public class FrontPageView extends Composite implements FrontPagePresenter.Display {

	private FlowPanel mainPanel;
	
	public FrontPageView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
	}
	
	public void setText(String header) {
		Label title = new Label(header);
		title.setStyleName("header");

		mainPanel.add(title);
		
		HTML body = new HTML();
		body.setStyleName("all-html");
		mainPanel.add(body);
		

		if(header.equals("Welcome")) {
			body.setHTML("Succesfully signed in.");
		}
		
		if(header.equals("Permission Denied")) {
			body.setHTML("You do not have permission to view this page.");
		}
		
	}
	
	
	
	
	
	public Widget asWidget() {
		return this;
	}
}
