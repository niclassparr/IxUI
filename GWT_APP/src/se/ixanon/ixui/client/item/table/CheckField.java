package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.CheckFieldClickedEvent;

public class CheckField extends Composite implements ClickHandler {
	
	private FlowPanel mainPanel;
	private HTML icon = new HTML();
	private boolean enabled = true;
	private boolean active = true;
	private int index = -1;
	
	public CheckField(Boolean enabled, String title) {
		this.enabled = enabled;
		build(title);
	}
	
	public CheckField(Boolean enabled, String title, int index) {
		this.enabled = enabled;
		this.index = index;
		build(title);
	}
	
	private void build(String title) {
		
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		if(title != null) {
			Label label = new Label();
			
			label.setText(title);
			label.setStyleName("edit-label");
			mainPanel.add(label);
		}
		
		setValue();
		
		mainPanel.add(icon);
		
		icon.addClickHandler(this);
		
	}
	
	private void setValue() {
		
		if(active) {
			if(this.enabled) {
				icon.setHTML("<i class='fa fa-check-circle-o' aria-hidden='true'></i>");
				icon.setStyleName("icon check table");
			} else {
				icon.setHTML("<i class='fa fa-circle-thin' aria-hidden='true'></i>");
				icon.setStyleName("icon check table");
			}
		} else {
			icon.setHTML("<i class='fa fa-circle' aria-hidden='true'></i>");
			icon.setStyleName("icon check table blocked");
		}
		
		
		
	}
	
	public void setEnabled(boolean value) {
		this.enabled = value;
		setValue();
	}
	
	public void setActive(boolean value) {
		this.active = value;
		setValue();
	}
	
	public boolean getValue() {
		return this.enabled;
	}

	@Override
	public void onClick(ClickEvent event) {
		if(active) {
			this.enabled = !this.enabled;
			Session.getInstance().getHandlerManager().fireEvent(new CheckFieldClickedEvent(index));
			setValue();
		}
	}

}
