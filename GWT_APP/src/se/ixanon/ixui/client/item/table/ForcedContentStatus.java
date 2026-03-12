package se.ixanon.ixui.client.item.table;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.shared.ForcedContent;

public class ForcedContentStatus extends Composite {

	private StatusField status;
	private Label c_status = new Label("Warning - Communication Error");
	//private StatusField c_status;
	
	public ForcedContentStatus(ForcedContent fc) {
		
		FlowPanel mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		Label label = new Label(fc.getName());
		label.setStyleName("bold semi-header");
		mainPanel.add(label);
		
		c_status.setStyleName("edit-label full");
		mainPanel.add(c_status);
		c_status.setVisible(!fc.isComStatus());
		
		
		status = new StatusField("Signal Status");
		mainPanel.add(status);
		
		//c_status = new StatusField("Com Status");
		//mainPanel.add(c_status);
		
		ArrayList<String> override_list = new ArrayList<String>();
		override_list.add("None");
		override_list.add("On");
		override_list.add("Off");
		
		ListField override = new ListField("Override");
		override.setId(fc.getId());
		override.setList(override_list);
		mainPanel.add(override);
		
		status.setStatus(fc.getSignalStatus());
		//c_status.setStatus(fc.isComStatus());
		override.setListIndex(fc.getSignalOverride());
	}
	
	public void setStatus(int signal_status, boolean com_status) {
		status.setStatus(signal_status);
		c_status.setVisible(!com_status);
		//c_status.setStatus(com_status);
	}
}
