package se.ixanon.ixui.client.presenter;

import java.util.ArrayList;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.item.dialog.UpdateLoadDialog;
import se.ixanon.ixui.shared.Package;
import se.ixanon.ixui.shared.Response;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class UpdatePresenter implements Presenter { 

	public interface Display {
		void setInitView(boolean isSoftwareUpdate);
		void setTableView();
		void setResultView();
		void displayError(String error, String color);
		void buildTable(ArrayList<Package> packages);
		boolean isPackageSelected(ArrayList<Package> packages);
		ArrayList<Package> updatePackages(ArrayList<Package> packages);
		void displayResult(String result);
		HasClickHandlers getOkButton();
		HasClickHandlers getUpdateButton();
		Widget asWidget();
	}
	
	private final Display display;
	private ArrayList<Package> packages;
	  
	public UpdatePresenter(Display view) {
		this.display = view;
	}
	
	public void go(final HasWidgets container) {
		bind();
		container.clear();
		container.add(display.asWidget());
		init();
	}
	
	public void bind() {
		
		
		display.getOkButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				RootPanel.get("overlay").add(new UpdateLoadDialog("Waiting", "repeat", null));
				
				Session.getInstance().getRpcService().runUpdateCommand("check-sw", new AsyncCallback<Response>() {

					@Override
					public void onFailure(Throwable caught) {
						
					}

					@Override
					public void onSuccess(Response result) {
						
						if(result.isSuccess()) {
							getUpdatePackages();
						} else {
							closeDialog();
							display.displayError(result.getError(), "red");
						}
						
					}
					
				});
			}
			
		});
		
		
		
		
		
		
		
		display.getUpdateButton().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				boolean isPackageSelected = display.isPackageSelected(display.updatePackages(packages));
				
				if(isPackageSelected) {
					RootPanel.get("overlay").add(new UpdateLoadDialog("Update", "server", null));
					
					Session.getInstance().getRpcService().updatePackages(display.updatePackages(packages), new AsyncCallback<Response>() {

						@Override
						public void onFailure(Throwable caught) {
							
						}

						@Override
						public void onSuccess(Response result) {
							
							if(result.isSuccess()) {
								getUpdateResult();
							} else {
								closeDialog();
								display.displayError(result.getError(), "red");
							}
							
						}
						
					});
				} else {
					display.displayError("No packages selected.", "orange");
				}
				
			}
			
		});
		
	}
	
	private void init() {
		
		Session.getInstance().getRpcService().isSoftwareUpdate(new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				display.setInitView(result);
			}
			
		});
		
		
	}
	
	private void getUpdatePackages() {
		
		Session.getInstance().getRpcService().getUpdatePackages(new AsyncCallback<ArrayList<Package>>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(ArrayList<Package> result) {
				closeDialog();
				
				packages = new ArrayList<Package>(result);
				
				if(packages != null && packages.size() > 0) {
					display.setTableView();
					display.buildTable(packages);
				} else {
					display.displayError("No packages needs to be updated.", "orange");
				}
				
				
			}
			
		});
	}
	
	private void getUpdateResult() {
		
		Session.getInstance().getRpcService().getUpdateResult(new AsyncCallback<String>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(String result) {
				closeDialog();
				display.setResultView();
				display.displayResult(result);
				
			}
			
		});
		
	}
	
	private void closeDialog() {
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(false);
	}
}