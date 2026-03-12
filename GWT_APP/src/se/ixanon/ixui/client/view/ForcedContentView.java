package se.ixanon.ixui.client.view;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.ForcedContentItem;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.presenter.ForcedContentPresenter;
import se.ixanon.ixui.shared.ForcedContent;
import se.ixanon.ixui.shared.Media;

public class ForcedContentView extends Composite implements ForcedContentPresenter.Display {

	private FlowPanel mainPanel;
	private ArrayList<ForcedContentItem> items = new ArrayList<ForcedContentItem>();
	private Button control_button = new Button("Force Content Control");
	private Button save_button = new Button("Save");
	
	public ForcedContentView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main");
	}
	
	public void setHeader(Header header) {
		mainPanel.add(header);
	}
	
	public void setDisabled() {
		mainPanel.add(new Label("The force content function is disabled."));
	}
	
	public void setValues(HashMap<Integer, ForcedContent> map, ArrayList<Media> media) {
		
		for (int i = 1; i <= 4; i++) {
			
			if(!map.containsKey(i)) {
				map.put(i, new ForcedContent(i, false, "", 0, "", 0, 0, -1));
			}
			
			items.add(new ForcedContentItem(map.get(i), media));
		}
		
		control_button.setStyleName("btn blue");
		mainPanel.add(control_button);
		
		FlowPanel wrapperPanel = new FlowPanel();
		wrapperPanel.setStyleName("panel-wrapper");
		FlowPanel firstPanel = new FlowPanel();
		firstPanel.setStyleName("panel first");
		FlowPanel secondPanel = new FlowPanel();
		secondPanel.setStyleName("panel second");
		
		firstPanel.add(items.get(0));
		firstPanel.add(items.get(2));
		
		secondPanel.add(items.get(1));
		secondPanel.add(items.get(3));
		
		wrapperPanel.add(firstPanel);
		wrapperPanel.add(secondPanel);
		
		mainPanel.add(wrapperPanel);
		
		save_button.setStyleName("btn blue");
		mainPanel.add(save_button);
		
	}
	
	public ArrayList<ForcedContent> getValues() {
		
		ArrayList<ForcedContent> values = new ArrayList<ForcedContent>();
		
		for (int i = 0; i < items.size(); i++) {
			values.add(items.get(i).getValues());
		}
		
		return values;
		
	}
	public HasClickHandlers getControlButton() {
		return this.control_button;
	}
	
	public HasClickHandlers getSaveButton() {
		return this.save_button;
	}
	
	public Widget asWidget() {
		return this;
	}
}
