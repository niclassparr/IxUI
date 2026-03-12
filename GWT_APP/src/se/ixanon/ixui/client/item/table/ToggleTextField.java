package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ToggleChangeEvent;

public class ToggleTextField extends Composite implements ClickHandler {

	private FlowPanel wrapper;
	private String title;
	private boolean toggle = false;
	private HTML on = new HTML("<i class='fa fa-check' aria-hidden='true'></i>");
	private HTML off = new HTML("<i class='fa fa-times' aria-hidden='true'></i>");
	private boolean text;
	private TextBox box = new TextBox();
	private NumTextBox numbox = new NumTextBox();
	
	public ToggleTextField(String title, boolean text) {
		
		this.title = title;
		this.text = text;
		
		FlowPanel mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
	
		String extra_style = "";
		
		if(title != null) {
			Label label = new Label();
			
			label.setText(title);
			label.setStyleName("edit-label");
			mainPanel.add(label);
			
			extra_style = title.replaceAll("\\s+","");
			mainPanel.addStyleName(extra_style.toLowerCase());
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
		
		
		if(!text) {
			numbox.setStyleName("edit-field half " + extra_style.toLowerCase());
			wrapper.add(numbox);
		} else {
			box.setStyleName("edit-field half " + extra_style.toLowerCase());
			wrapper.add(box);
		}
		
	}
	
	public void setToggle(boolean value) {
		toggle = value;
		wrapper.setStyleName("edit-field half toggle-wrapper on-" + value);
		
		if(!text) {
			numbox.setEnabled(value);
		} else {
			box.setEnabled(value);
		}
		
		if(!toggle) {
			if(!text) {
				numbox.setText("");
			} else {
				box.setText("");
			}
		}
		
		setHighlighted(toggle);
		
	}
	
	public boolean isToggle() {
		return toggle;
	}
	
	public String getValue() {
		
		if(!toggle) {
			if(!text) {
				return "0";
			} else {
				return null;
			}
		}
		
		if(!text) {
			return numbox.getText();
		} else {
			return box.getText();
		}
	}
	
	public void setValue(String value) {
		
		if(value == null || value.equals("0")) {
			setToggle(false);
		} else {
			
			setToggle(true);
			
			if(!text) {
				numbox.setText(value);
			} else {
				box.setText(value);
			}
		}
		
	}
	
	public void setHighlighted(boolean highlighted) {
		
		
		
		if(highlighted) {
			
			if(!text) {
				numbox.addStyleName("highlighted");
			} else {
				box.addStyleName("highlighted");
			}
			
		} else {
			
			if(!text) {
				numbox.removeStyleName("highlighted");
			} else {
				box.removeStyleName("highlighted");
			}
			
		}
		
	}
	
	@Override
	public void onClick(ClickEvent event) {
		HTML label = (HTML) event.getSource();
		
		boolean flag = false;
		
		if(label.getHTML().contains("check") && !toggle) {
			flag = true;
		}
		
		if(label.getHTML().contains("times") && toggle) {
			flag = true;
		}
		
		if(flag) {
			setToggle(!toggle);
			Session.getInstance().getHandlerManager().fireEvent(new ToggleChangeEvent(title, toggle));
		}
		
	}

}
