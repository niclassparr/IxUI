package se.ixanon.ixui.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class ReorderEvent extends GwtEvent<ReorderEventHandler> {
	public static Type<ReorderEventHandler> TYPE = new Type<ReorderEventHandler>();
  
	private final String name;
	private final boolean reverse;
	private final String table;
  
	public ReorderEvent(String name, boolean reverse, String table) {
		this.name = name;
		this.reverse = reverse;
		this.table = table;
	}
  
	public String getName() { 
		return name;
	}
	
	public boolean getReverse() { 
		return reverse;
	}
	
	public String getTable() { 
		return table;
	}
  
	@Override
	public Type<ReorderEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(ReorderEventHandler handler) {
		handler.onReorder(this);
	}
}
