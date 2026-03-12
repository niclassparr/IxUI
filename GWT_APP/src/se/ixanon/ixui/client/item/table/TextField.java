package se.ixanon.ixui.client.item.table;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.TextFieldBlurEvent;

public class TextField extends Composite {
	
	private FlowPanel mainPanel;
	private boolean text;
	private TextBox box = new TextBox();
	private NumTextBox numbox = new NumTextBox();
	private Label label = new Label();
	private String blur_type;
	
	public TextField(String title, boolean text) {
		
		this.text = text;
		
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("edit-wrapper");
		initWidget(mainPanel);
		
		String extra_style = "";
		
		if(title != null) {
			label.setText(title);
			label.setStyleName("edit-label");
			mainPanel.add(label);
			
			extra_style = title.replaceAll("\\s+",""); 
		}
		
		if(!text) {
			numbox.setStyleName("edit-field " + extra_style.toLowerCase());
			mainPanel.add(numbox);
		} else {
			box.setStyleName("edit-field " + extra_style.toLowerCase());
			mainPanel.add(box);
		}
		
	}
	
	public String getValue() {
		if(!text) {
			return numbox.getText();
		} else {
			return box.getText();
		}
	}
	
	public void setValue(String value) {
		if(!text) {
			numbox.setText(value);
		} else {
			box.setText(value);
		}
	}
	
	public void setEnabled(boolean enabled) {
		if(!text) {
			numbox.setEnabled(enabled);
		} else {
			box.setEnabled(enabled);
		}
	}
	
	public void changeTitle(String new_title) {
		label.setText(new_title);
	}
	
	public void addIcon(String icon) {
		HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
		html.setStyleName("icon icon-edit-field");
		mainPanel.insert(html, 0);
		
		box.setStyleName("edit-field icon-edit-field");
	}

	public void hide() {
		mainPanel.addStyleName("hidden");
	}
	
	public void addTitle(String title) {
		box.setTitle(title);
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
	
	public void addBlurEvent(String type) {
		
		this.blur_type = type;
		
		if(!text) {
			
			numbox.addBlurHandler(new BlurHandler() {

				@Override
				public void onBlur(BlurEvent event) {
					Session.getInstance().getHandlerManager().fireEvent(new TextFieldBlurEvent(blur_type));
				}
				
			});
			
		} else {
			
			box.addBlurHandler(new BlurHandler() {

				@Override
				public void onBlur(BlurEvent event) {
					Session.getInstance().getHandlerManager().fireEvent(new TextFieldBlurEvent(blur_type));
				}
				
			});
			
		}
		
		
	}
}
