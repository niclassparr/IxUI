package se.ixanon.ixui.client.item.table;

import java.util.ArrayList;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ListChangeEvent;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class ListField extends Composite implements ChangeHandler {
	
	private FlowPanel mainPanel;
	private ListBox box = new ListBox();
	private Label label = new Label();
	private String title;
	private int id = -1;
	
	public ListField(String title) {
		
		this.title = title;
		
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
		
		if(title != null) {
			label.setText(title);
			label.setStyleName("edit-label");
			mainPanel.add(label);
			
			String extra_style = title.replaceAll("\\s+","");
			mainPanel.addStyleName(extra_style);
		}
		
		FlowPanel box_wrapper = new FlowPanel();
		box_wrapper.setStyleName("select-wrapper");
		
		box.setStyleName("edit-field");
		box.addChangeHandler(this);
		
		box_wrapper.add(box);
		
		mainPanel.add(box_wrapper);
	}
	
	public String getValue() {
		return box.getSelectedItemText();
	}
	
	public String getValue(boolean temp) {
		return box.getSelectedValue();
	}
	
	public void setList(ArrayList<String> list) {
		
		box.clear();
		
		for(int i = 0; i < list.size(); i++) {
			box.addItem(list.get(i));
		}
	}
	
	public void setList(ArrayList<String> items, ArrayList<String> values) {
		
		box.clear();
		
		for(int i = 0; i < items.size(); i++) {
			box.addItem(items.get(i), values.get(i));
		}
	}
	
	public void setListValue(String value) {
		
		if(value != null) {
			for(int i = 0; i < box.getItemCount(); i++) {
				if(value.equalsIgnoreCase(box.getItemText(i))) {
					box.setSelectedIndex(i);
				}
			}
		}
		
	}
	
	public void setListValue(String value, boolean temp) {
		
		if(value != null) {
			for(int i = 0; i < box.getItemCount(); i++) {
				if(value.equals(box.getValue(i))) {
					box.setSelectedIndex(i);
				}
			}
		}
		
	}
	
	public String getIndexText(int index) {
		return box.getItemText(index);
	}
	
	public void setListIndex(int index) {
		box.setSelectedIndex(index);
	}
	
	public int getListIndex() {
		return box.getSelectedIndex();
	}
	
	public void disable() {
		box.setEnabled(false);
	}
	
	public void setEnabled(boolean value) {
		box.setEnabled(value);
	}
	
	public void hide() {
		mainPanel.addStyleName("hidden");
	}
	
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public void onChange(ChangeEvent event) {
		
		if(id != -1) {
			Session.getInstance().getHandlerManager().fireEvent(new ListChangeEvent(id, box.getSelectedIndex()));
		} else {
			Session.getInstance().getHandlerManager().fireEvent(new ListChangeEvent(title));
		}
		
	}
	
	public void changeTitle(String new_title) {
		label.setText(new_title);
	}
	
}
