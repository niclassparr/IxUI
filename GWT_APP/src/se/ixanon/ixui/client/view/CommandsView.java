package se.ixanon.ixui.client.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.item.table.EventButton;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.IconText;
import se.ixanon.ixui.client.presenter.CommandsPresenter;

public class CommandsView extends Composite implements CommandsPresenter.Display {

	private FlowPanel mainPanel;
	
	public CommandsView() {
		mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		mainPanel.setStyleName("main commands");
	}
	
	public void setHeader(Header header, String serial, boolean softwareUpdate) {
		mainPanel.add(header);
		
		FlowPanel buWrapper = new FlowPanel();
		EventButton buButton = new EventButton("Backup", "cloud-download", "blue");
		IconText buText = new IconText("This will generate a system backup file.", "info-circle", "info-box blue");
		buWrapper.add(buText);
		buWrapper.add(buButton);
		
		FlowPanel rsWrapper = new FlowPanel();
		EventButton rsButton = new EventButton("Restore", "cloud-upload", "blue");
		IconText rsText = new IconText("This will restore the system with an uploaded backup file.", "info-circle", "info-box blue");
		rsWrapper.add(rsText);
		rsWrapper.add(rsButton);
		
		
		
		
		
		FlowPanel pdfWrapper = new FlowPanel();
		EventButton pdfButton = new EventButton("Document", "file-pdf-o", "blue");
		IconText pdfText = new IconText("This will generate a downloadable installation document (pdf).", "info-circle", "info-box blue");
		pdfWrapper.add(pdfText);
		pdfWrapper.add(pdfButton);
		
		FlowPanel powerWrapper = new FlowPanel();
		EventButton powerButton = new EventButton("Power off", "power-off", "blue");
		IconText powerText = new IconText("This will shut down the streamer.", "info-circle", "info-box blue");
		powerWrapper.add(powerText);
		powerWrapper.add(powerButton);
		
		FlowPanel rebootWrapper = new FlowPanel();
		EventButton rebootButton = new EventButton("Reboot", "repeat", "blue");
		IconText rebootText = new IconText("This will restart the streamer.", "info-circle", "info-box blue");
		rebootWrapper.add(rebootText);
		rebootWrapper.add(rebootButton);
		
		FlowPanel networkWrapper = new FlowPanel();
		EventButton networkButton = new EventButton("Restart network", "retweet", "blue");
		IconText networkText = new IconText("This will restart the network with new settings.", "info-circle", "info-box blue");
		networkWrapper.add(networkText);
		networkWrapper.add(networkButton);
		
		FlowPanel updateWrapper = new FlowPanel();
		EventButton updateButton = new EventButton("Update interfaces", "refresh", "blue");
		IconText updateText = new IconText("This will update the interface list. Any new interfaces will be added and removed/broken interfaces will be removed.", "exclamation-circle", "info-box red");
		updateWrapper.add(updateText);
		updateWrapper.add(updateButton);
		
		FlowPanel resetWrapper = new FlowPanel();
		EventButton resetButton = new EventButton("Reset Software", "exclamation-circle", "blue");
		IconText resetText = new IconText("This will reset the ixui software to factory default.", "info-circle", "info-box red");
		resetWrapper.add(resetText);
		resetWrapper.add(resetButton);
		
		FlowPanel startWrapper = new FlowPanel();
		EventButton startButton = new EventButton("Start all interfaces", "play-circle", "green");
		IconText startText = new IconText("This will start all interfaces.", "info-circle", "info-box orange");
		startWrapper.add(startText);
		startWrapper.add(startButton);
				
		FlowPanel stopWrapper = new FlowPanel();
		EventButton stopButton = new EventButton("Stop all interfaces", "stop-circle", "red");
		IconText stopText = new IconText("This will stop all interfaces.", "info-circle", "info-box orange");
		stopWrapper.add(stopText);
		stopWrapper.add(stopButton);
		
		mainPanel.add(buWrapper);
		mainPanel.add(rsWrapper);
		mainPanel.add(pdfWrapper);
		mainPanel.add(powerWrapper);
		mainPanel.add(rebootWrapper);
		mainPanel.add(networkWrapper);
		mainPanel.add(updateWrapper);
		mainPanel.add(resetWrapper);
		mainPanel.add(startWrapper);
		mainPanel.add(stopWrapper);
		
		if(softwareUpdate) {
			FlowPanel softwareupdateWrapper = new FlowPanel();
			EventButton softwareupdateButton = new EventButton("Software Update", "server", "blue");
			IconText softwareupdateText = new IconText("Check for available software updates. This might take serveral minutes.", "info-circle", "info-box blue");
			softwareupdateWrapper.add(softwareupdateText);
			softwareupdateWrapper.add(softwareupdateButton);
			
			mainPanel.add(softwareupdateWrapper);
		}
				
		
		
		
	}
	
	public Widget asWidget() {
		return this;
	}
}
