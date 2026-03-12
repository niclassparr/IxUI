package se.ixanon.ixui.client.item.dialog;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import se.ixanon.ixui.client.Session;

public class UpdateLoadDialog extends Composite {
	
	private String key;
	private HTML textLabel;
	private Timer updateTimer = new Timer() {
		@Override
		public void run() {
			getInfo();
		}
	};
	
	public UpdateLoadDialog(String title, String icon, String key) {
		
		this.key = key;
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		FlowPanel mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog");
		wrapperPanel.add(mainPanel);
		
		HTML html = new HTML("<i class='fa fa-"+icon+" aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label(title);
		textLabel = new HTML("This might take several minutes.<br>Please wait...");
		
		HTML html2 = new HTML("<i class='fa fa-circle-o-notch fa-spin fa-3x fa-fw'></i>");
		html2.setStyleName("icon header dark");
		
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(html2);	
		
		initWidget(wrapperPanel);
		
		if(key != null) {
			updateTimer.scheduleRepeating(100);
		}
		
	}
	
	private void getInfo() {
		
		
		Session.getInstance().getRpcService().getSessionValue(key, new AsyncCallback<String>() {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(String result) {
				textLabel.setHTML("This might take several minutes.<br>"+result+"...");
			}

		});
	}
}
