package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ListChangeEvent extends GwtEvent<ListChangeEventHandler> {
	public static Type<ListChangeEventHandler> TYPE = new Type<ListChangeEventHandler>();
  
	private final String type;
	private final int id;
	private final int index;
  
	public ListChangeEvent(String type) {
		this.type = type;
		this.id = 0;
		this.index = 0;
	}
	
	public ListChangeEvent(int id, int index) {
		this.type = null;
		this.id = id;
		this.index = index;
	}

	public String getType() { 
		return type;
	}
	
	public int getId() { 
		return id;
	}
	
	public int getIndex() { 
		return index;
	}
  
	@Override
	public Type<ListChangeEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(ListChangeEventHandler handler) {
		handler.onListChange(this);
	}
}
