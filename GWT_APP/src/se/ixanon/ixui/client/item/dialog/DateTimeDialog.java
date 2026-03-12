package se.ixanon.ixui.client.item.dialog;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.ListChangeEvent;
import se.ixanon.ixui.client.event.ListChangeEventHandler;
import se.ixanon.ixui.client.item.table.ListField;
import se.ixanon.ixui.shared.Response;

public class DateTimeDialog extends Composite {
	
	private final HandlerManager presenterBus = new HandlerManager(null);
	
	private ListField zone = new ListField("Timezone");
	private ListField mode = new ListField("Clock Mode");
	
	private ListField year = new ListField("Year");
	private ListField month = new ListField("Month");
	private ListField day = new ListField("Day");
	
	private ListField hour = new ListField("Hours");
	private ListField min = new ListField("Minutes");
	
	private HTML textLabel;
	private String info_text = "";
	private String current_zone = null;
	private boolean isRestart = false;
	
	public DateTimeDialog() {
		
		Session.getInstance().setHandlerManager(presenterBus);
		
		getMode();
		
		RootPanel.get("overlay").clear();
		RootPanel.get("overlay").setVisible(true);
		
		FlowPanel mainPanel = new FlowPanel();
		
		VerticalPanel wrapperPanel = new VerticalPanel();
		wrapperPanel.setWidth("100%");
		wrapperPanel.setHeight("100%");
		wrapperPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		wrapperPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		wrapperPanel.add(mainPanel);
		
		wrapperPanel.setStyleName("overlay-inner");
		mainPanel.setStyleName("dialog date-time");
		wrapperPanel.add(mainPanel);
		
		FlowPanel mainPanelInner = new FlowPanel();
		mainPanelInner.setStyleName("dialog-inner");
		
		HTML html = new HTML("<i class='fa fa-clock-o' aria-hidden='true'></i>");
		html.setStyleName("icon header");
				
		Label headerLabel = new Label("Date and Time");
		textLabel = new HTML();
		
		headerLabel.setStyleName("header");
		textLabel.setStyleName("dialog-text");
		
		mainPanel.add(html);
		mainPanel.add(headerLabel);
		mainPanel.add(textLabel);
		mainPanel.add(mainPanelInner);	
		
		initWidget(wrapperPanel);
		
		
		
		/*
		FlowPanel datePanel = new FlowPanel();
		datePanel.setStyleName("edit-wrapper");
		
		Label dateLabel = new Label("Date");
		
		dateLabel.setStyleName("edit-label");
		
		DateTimeFormat dateFormat = DateTimeFormat.getFormat("yyyy-MM-dd");
		date.setFormat(new DateBox.DefaultFormat(dateFormat));
		
		date.setStyleName("edit-field");
		
		datePanel.add(dateLabel);
		datePanel.add(date);
		*/
		
		FlowPanel date_wrapper = new FlowPanel();
		date_wrapper.add(year);
		date_wrapper.add(month);
		date_wrapper.add(day);
		
		FlowPanel time_wrapper = new FlowPanel();
		time_wrapper.add(hour);
		time_wrapper.add(min);
		
		mainPanelInner.add(zone);
		mainPanelInner.add(mode);
		mainPanelInner.add(date_wrapper);
		mainPanelInner.add(time_wrapper);
		
	
		
		
		
		
		Button okButton = new Button("Save");
		okButton.setStyleName("btn blue login");
		mainPanel.add(okButton);
		
		okButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				save();
				
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
				
			}
			
		});
		
		Button cancelButton = new Button("Cancel");
		cancelButton.setStyleName("btn orange login");
		mainPanel.add(cancelButton);
		
		cancelButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				RootPanel.get("overlay").clear();
				RootPanel.get("overlay").setVisible(false);
			}
			
		});
		
		
		presenterBus.addHandler(ListChangeEvent.TYPE, new ListChangeEventHandler() {

			@Override
			public void onListChange(ListChangeEvent event) {
				
				if(event.getType().equals("Clock Mode")) {
					setdatetime(false);
					
					if(mode.getValue().equals("Local")) {
						setdatetime(true);
					}
				}
				
			}
			
		});
		
	}
	
	private void setdatetime(boolean value) {
		year.setEnabled(value);
		month.setEnabled(value);
		day.setEnabled(value);
		hour.setEnabled(value);
		min.setEnabled(value);
	}
	
	private void getMode() {
		
		ArrayList<String> mode_list = new ArrayList<String>();
		mode_list.add("NTP"); //true
		mode_list.add("Local"); //false
		mode.setList(mode_list);
		
		Session.getInstance().getRpcService().getEnabledType("ntp", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Boolean result) {
				
				info_text += "Info: If timezone is changed, the streamer will restart.<br><br>Current Date and time:<br> ";
				
				if(!result) {
					mode.setListValue("Local");
					setdatetime(true);
				} else {
					setdatetime(false);
				}
				
				getZones();
			}
			
		});
		
	}
	
	private void getZones() {
		
		Session.getInstance().getRpcService().runCommand2("list-timezones", new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {

			}

			@Override
			public void onSuccess(ArrayList<String> result) {
				zone.setList(result);
				getCurrentZone();
			}
			
		});
		
	}
	
	private void getCurrentZone() {
		
		Session.getInstance().getRpcService().runCommand2("get-timezone", new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {

			}

			@Override
			public void onSuccess(ArrayList<String> result) {
				current_zone = result.get(0);
				zone.setListValue(current_zone);
				
				info_text += current_zone + " ";
				
				getCurrentDate();
			}
			
		});
		
	}
	
	private void getCurrentDate() {
		
		ArrayList<String> year_list = new ArrayList<String>();
		for(int i = 2020; i < 2100; i++) {
			year_list.add(""+i);
		}
		year.setList(year_list);
		
		ArrayList<String> month_list = new ArrayList<String>();
		for(int i = 0; i <= 12; i++) {
			if(i < 10) {
				month_list.add("0"+i);
			} else {
				month_list.add(""+i);
			}
		}
		month.setList(month_list);
		
		ArrayList<String> day_list = new ArrayList<String>();
		for(int i = 0; i <= 31; i++) {
			if(i < 10) {
				day_list.add("0"+i);
			} else {
				day_list.add(""+i);
			}
		}
		day.setList(day_list);
		
		
		
		Session.getInstance().getRpcService().runCommand2("get-date", new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {

			}

			@Override
			public void onSuccess(ArrayList<String> result) {

				if(!result.isEmpty()) {
					info_text += result.get(0) + " ";
					String[] parts = result.get(0).split("-");
					year.setListValue(parts[0]);
					month.setListValue(parts[1]);
					day.setListValue(parts[2]);
				}
				
				getCurrentTime();
			}
			
		});
		
	}
	
	private void getCurrentTime() {
		
		ArrayList<String> hour_list = new ArrayList<String>();
		for(int i = 0; i <= 23; i++) {
			if(i < 10) {
				hour_list.add("0"+i);
			} else {
				hour_list.add(""+i);
			}
		}
		hour.setList(hour_list);
		
		ArrayList<String> min_list = new ArrayList<String>();
		for(int i = 0; i <= 59; i++) {
			if(i < 10) {
				min_list.add("0"+i);
			} else {
				min_list.add(""+i);
			}
		}
		min.setList(min_list);
		
		Session.getInstance().getRpcService().runCommand2("get-time", new AsyncCallback<ArrayList<String>>() {

			@Override
			public void onFailure(Throwable caught) {

			}

			@Override
			public void onSuccess(ArrayList<String> result) {
				
				if(!result.isEmpty()) {
					info_text += result.get(0) + ".";
					String[] parts = result.get(0).split(":");
					hour.setListValue(parts[0]);
					min.setListValue(parts[1]);
				}
				
				textLabel.setHTML(info_text);
				
			}
			
		});
		
	}
	
	private void save() {
		
		String timezone = zone.getValue();
		
		if(!current_zone.equals(timezone)) {
			isRestart = true;
		}
		
		boolean ntp_mode = true;
		String date = null;
		String time = null;
		
		if(mode.getValue().equals("Local")) {
			ntp_mode = false;
			date = year.getValue() +"-"+ month.getValue() +"-"+ day.getValue();
			time = hour.getValue() +":"+ min.getValue();
		}
		
		Session.getInstance().getRpcService().saveDateTime(Session.getInstance().getSessionKey(), isRestart, timezone, ntp_mode, date, time, new AsyncCallback<Response>() {

			@Override
			public void onFailure(Throwable caught) {
				
			}

			@Override
			public void onSuccess(Response result) {
				
				if(isRestart) {
					RootPanel.get("overlay").add(new ReloadDialog());
				} else {
					RootPanel.get("overlay").add(new ConfirmDialog("Done", "Command finished.", "check-square-o"));
				}
				
			}
			
		});
	}
}
