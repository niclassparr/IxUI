package se.ixanon.ixui.client.presenter;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

public class FrontPagePresenter implements Presenter { 

	public interface Display {
		void setText(String header);
		Widget asWidget();
	}
	
	private final Display display;
	private String header;
	  
	public FrontPagePresenter(Display view, String header) {
		this.display = view;
		this.header = header;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		fetchData();
	}
	
	public void bind() {
		
	}
	
	public void fetchData() {
		display.setText(header);
	}
}