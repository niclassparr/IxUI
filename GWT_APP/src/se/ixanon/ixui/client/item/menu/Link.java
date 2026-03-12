package se.ixanon.ixui.client.item.menu;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.HistoryEvent;
import se.ixanon.ixui.shared.SessionKeys;

public class Link extends Composite implements ClickHandler {

	FlowPanel wrapper = new FlowPanel();
	private SessionKeys keys;
	private String inline = "";
	
	public Link(String text, String icon, String style, boolean isInline, SessionKeys keys) {
		this.keys = keys;
		this.inline = (isInline) ? "inline" : "not-inline";
		
		FlowPanel widget_wrapper = new FlowPanel();
		widget_wrapper.setStyleName(inline);
		
		wrapper.setStyleName(style);
		widget_wrapper.add(wrapper);
		
		if(icon != null) {
			HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
			html.setStyleName("icon");
			wrapper.add(html);
		}
				
		Label label = new Label(text);
		label.setStyleName(inline + " link");
		wrapper.add(label);
		label.addClickHandler(this);
		
		initWidget(widget_wrapper);
	}

	public void addIcon(String icon, boolean emm) {
		
		if(icon != null) {
			HTML html = new HTML("<i class='fa fa-"+icon+"' aria-hidden='true'></i>");
			html.setStyleName("icon");
			wrapper.add(html);
		}
		
		if(emm) {
			Image image = new Image("style/images/emm.png");
			image.setStyleName(inline);
			wrapper.add(image);
		}
		
	}
	
	@Override
	public void onClick(ClickEvent event) {

		Session.getInstance().getAppBus().fireEvent(new HistoryEvent(keys));

	}

}
