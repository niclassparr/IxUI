package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ToggleChangeEvent;

public class ToggleField extends Composite implements ClickHandler {

	private FlowPanel wrapper;
	private boolean toggle = false;
	private Label on = new Label("ON");
	private Label off = new Label("OFF");
	private String event_type;
	
	public ToggleField(String title) {
		
		this.event_type = title;
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
	
		if(title != null) {
			Label label = new Label();
			
			label.setText(title);
			label.setStyleName("edit-label");
			mainPanel.add(label);
			
			String extra_style = title.replaceAll("\\s+","");
			mainPanel.addStyleName(extra_style);
		}
		
		wrapper = new FlowPanel();
		
		on.setStyleName("toggle on");
		off.setStyleName("toggle off");
		
		setToggle(toggle);
		
		on.addClickHandler(this);
		off.addClickHandler(this);
		
		wrapper.add(on);
		wrapper.add(off);
		
		mainPanel.add(wrapper);
		
	}
	
	public void setEventType(String type) {
		this.event_type = type;
	}
	
	public void setToggle(boolean value) {
		toggle = value;
		wrapper.setStyleName("edit-field toggle-wrapper on-" + value);
	}
	
	public boolean isToggle() {
		return toggle;
	}
	
	@Override
	public void onClick(ClickEvent event) {
		Label label = (Label) event.getSource();
		
		boolean flag = false;
		
		if(label.getText().equals("ON") && !toggle) {
			flag = true;
		}
		
		if(label.getText().equals("OFF") && toggle) {
			flag = true;
		}
		
		if(flag) {
			setToggle(!toggle);
			Session.getInstance().getHandlerManager().fireEvent(new ToggleChangeEvent(event_type, toggle));
		}
		
	}

}
