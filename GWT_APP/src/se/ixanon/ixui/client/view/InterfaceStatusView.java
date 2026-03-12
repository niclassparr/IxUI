package se.ixanon.ixui.client.view;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.item.table.CIMenuLink;
import se.ixanon.ixui.client.item.table.DisplayField;
import se.ixanon.ixui.client.item.table.EventButton;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.IconText;
import se.ixanon.ixui.client.item.table.ReorderColumn;
import se.ixanon.ixui.client.presenter.InterfaceStatusPresenter;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.StreamerStatus;
import se.ixanon.ixui.shared.TunerStatus;

public class InterfaceStatusView extends Composite implements InterfaceStatusPresenter.Display {

	private FlowPanel mainPanel;
	private FlowPanel ci_menuPanel = new FlowPanel();
	private FlowPanel interfacePanel = new FlowPanel();
	private FlowPanel tunerPanel = new FlowPanel();
	private FlowPanel streamerPanel = new FlowPanel();
	private FlexTable flexTable;
	private int summary;
	
	public InterfaceStatusView() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("main");
		initWidget(mainPanel);
		
		flexTable = new FlexTable();
		flexTable.addStyleName("flexTable summary");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
	}
	
	public void setHeader(Header header, String interface_type) {
		mainPanel.add(header);
		
		EventButton start = new EventButton("Start", "play-circle", "green");
		EventButton stop = new EventButton("Stop", "stop-circle", "red");
		EventButton log = new EventButton("Log", "file-text-o", "blue");
		
		mainPanel.add(start);
		mainPanel.add(stop);
		mainPanel.add(log);
		
		if(interface_type.equals("dsc")) {
			EventButton ci_menu = new EventButton("CI Menu", "caret-square-o-down", "blue");
			mainPanel.add(ci_menu);
		}
		
		ci_menuPanel.setStyleName("percent-50 right");
		mainPanel.add(ci_menuPanel);
		ci_menuPanel.setVisible(false);
		
		
		FlowPanel statusPanel = new FlowPanel();
		statusPanel.setStyleName("percent-50");
		mainPanel.add(statusPanel);
		
		statusPanel.add(interfacePanel);
		statusPanel.add(tunerPanel);
		statusPanel.add(streamerPanel);
	}
	
	public void clearIdle() {
		ci_menuPanel.clear();
		tunerPanel.clear();
		streamerPanel.clear();
		flexTable.removeAllRows();
	}
	
	public void setCiMenu(boolean isCiMenu) {
		ci_menuPanel.setVisible(isCiMenu);
	}
	
	public void buildCiMenu(TunerStatus tunerStatus) {
		
		ci_menuPanel.clear();
		
		Label label = new Label(tunerStatus.getMenuTitle());
		label.setStyleName("bold semi-header");
		ci_menuPanel.add(label);
		
		
		FlowPanel ci_inner_menuPanel = new FlowPanel();
		ci_inner_menuPanel.setStyleName("ci-menu");
		
		for (int i = 1; i < tunerStatus.getMenuItems().size(); i++) {
			ci_inner_menuPanel.add(new CIMenuLink(i, tunerStatus.getMenuItems().get(i)));
		}
		
		ci_inner_menuPanel.add(new CIMenuLink(0, tunerStatus.getMenuItems().get(0)));
		
		ci_menuPanel.add(ci_inner_menuPanel);
	}
	
	public void buildInterface(Interface my_interface) {
		
		interfacePanel.clear();
				
		Label interface_label = new Label("Interface status:");
		interface_label.setStyleName("bold semi-header");
		interfacePanel.add(interface_label);
		
		interfacePanel.add(new DisplayField("Name", my_interface.getName()));
		interfacePanel.add(new DisplayField("Status", my_interface.getStatus()));
	}
	
	public void buildTuner(TunerStatus tuner_status, String interface_type) {
		
		tunerPanel.clear();
		
		Label tuner_label = new Label();
		tuner_label.setStyleName("bold semi-header");
		tunerPanel.add(tuner_label);
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp") || interface_type.equals("dvbhdmi")) {
			
			tuner_label.setText("Tuner status:");
			
			tunerPanel.add(new DisplayField("Locked", ""+tuner_status.isLocked()));
			
			if(!interface_type.equals("dvbudp")) {
				if(!interface_type.equals("dvbhdmi")) {
					
					//int dbuv = (109 - (tuner_status.getSignalstrength()/1000));
					double n1 = tuner_status.getSignalstrength();
					double n2 = tuner_status.getSnr();
					
					
					double dbuv = n1 / 100;
					
					if(tuner_status.getSignalstrength() <= 0) {
						dbuv = 0;
					}
								
					String signal_temp = Helper.getFormatted(dbuv, 2) + " dBuV";
					
					double v;
					String cnr_temp;
					
					
					if(n2 >= 10000) {
						v =  n2 * 100 / 65535;
						
						cnr_temp = Helper.getFormatted(v, 2) + " %";
						
					} else {
						v = n2 / 100;
						
						cnr_temp = Helper.getFormatted(v, 2) + " dB";
					}
			
					tunerPanel.add(new DisplayField("Frequency", ""+tuner_status.getFrequency()));
					tunerPanel.add(new DisplayField("Signal", signal_temp));
					tunerPanel.add(new DisplayField("CNR", cnr_temp));
				}
			}
			
		}
		
		if(interface_type.equals("dsc")) {
			
			tuner_label.setText("CI status:");
			
			Image image = new Image();
			
			if((tuner_status.getCiStatus() & 4) != 0) {
				image.setUrl("style/images/capmt.png");
			} else if((tuner_status.getCiStatus() & 2) != 0) {
				image.setUrl("style/images/cam.png");
			} else {
				image.setUrl("style/images/nocam.png");
			}
			
			tunerPanel.add(image);
			
			//tunerPanel.add(new DisplayField("Locked", ""+tuner_status.getCiStatus()));
			tunerPanel.add(new DisplayField("Receiving EMM", ""+tuner_status.isCaEmm()));
			
			tunerPanel.add(new DisplayField("CI Text", tuner_status.getCaText()));
			tunerPanel.add(new DisplayField("CI Message", tuner_status.getCaOsd()));
			
			
		}
		
	}
	
	public void buildTableInfoch(StreamerStatus streamer_status) {
		streamerPanel.clear();
		
		Label streamer_label = new Label("Streamer status:");
		streamer_label.setStyleName("bold semi-header");
		streamerPanel.add(streamer_label);
		
		flexTable.removeAllRows();
		
		flexTable.setText(0, 0, "Name");
		flexTable.setText(0, 1, "Bitrate");
		
		flexTable.setText(1, 0, streamer_status.getServices().get(0).getName());
		flexTable.setText(1, 1, formatBitrate(streamer_status.getServices().get(0).getBitrate()));
		
		mainPanel.add(flexTable);
	}
	
	public void buildTable(StreamerStatus streamer_status, boolean isWebradio) {
		streamerPanel.clear();
		
		Label streamer_label = new Label("Streamer status:");
		streamer_label.setStyleName("bold semi-header");
		streamerPanel.add(streamer_label);
		
		flexTable.removeAllRows();
		summary = 0;
		
		if(isWebradio) {
			flexTable.setText(0, 0, "Bitrate");
			flexTable.setText(0, 1, "Buffer Level");
		} else {
			flexTable.setText(0, 0, "Download Bitrate");
			flexTable.setText(0, 1, "Selected Bitrate");
			flexTable.setText(0, 2, "Segment Counter");
			flexTable.setText(0, 3, "Stream Switches");
			flexTable.setText(0, 4, "Segments Missed");
			flexTable.setText(0, 5, "Bitrate");
			flexTable.setText(0, 6, "Buffer Level");
		}
		
		if(isWebradio) {
			flexTable.setText(1, 0, formatBitrate(streamer_status.getServices().get(0).getBitrate()));
			flexTable.setText(1, 1, ""+streamer_status.getServices().get(0).getBufferlevel() + " ms");
		} else {
			flexTable.setText(1, 0, formatBitrate(streamer_status.getServices().get(0).getDownload_bitrate()));
			flexTable.setText(1, 1, formatBitrate(streamer_status.getServices().get(0).getSelected_bitrate()));
			flexTable.setText(1, 2, ""+streamer_status.getServices().get(0).getSegmentCounter());
			flexTable.setText(1, 3, ""+streamer_status.getServices().get(0).getNum_stream_switches());
			flexTable.setText(1, 4, ""+streamer_status.getServices().get(0).getNum_segments_missed());
			flexTable.setText(1, 5, formatBitrate(streamer_status.getServices().get(0).getBitrate()));
			flexTable.setText(1, 6, ""+streamer_status.getServices().get(0).getBufferlevel() + " ms");
		}
		
		mainPanel.add(flexTable);
	}
		
	public void buildTable(StreamerStatus streamer_status, String interface_type) {
		
		streamerPanel.clear();
		
		Label streamer_label = new Label("Streamer status:");
		streamer_label.setStyleName("bold semi-header");
		streamerPanel.add(streamer_label);
		
		if(interface_type.equals("dsc") || interface_type.equals("mod")) {
			streamerPanel.add(new DisplayField("Multiplex Usage", streamer_status.getMuxLoad() + " (max " + streamer_status.getMaxMuxLoad() + ") %"));
		}
		
		if(interface_type.equals("dsc")) {
			streamerPanel.add(new DisplayField("Descrambler Usage", streamer_status.getCaServices() + " service(s) and " + streamer_status.getCaPids() + " pids"));
		}
		
		
		flexTable.removeAllRows();
		summary = 0;
		
		flexTable.setText(0, 0, "Name");
		
		if(interface_type.equals("dsc") || interface_type.equals("mod")) {
			//flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Source", "services"));
		}
		
		if(!interface_type.equals("mod")) {
			//flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Destination", "services"));
		}
		
		flexTable.setText(0, flexTable.getCellCount(0), "Bitrate");
		
		if(!interface_type.equals("infostreamer")) {
			if(!interface_type.equals("hdmi2ip")) {
				flexTable.setText(0, flexTable.getCellCount(0), "Discontinuity");
			}
			
		}
		
		if(interface_type.equals("dsc") || interface_type.equals("mod")) {
			flexTable.setWidget(0, flexTable.getCellCount(0), new ReorderColumn("Mux Load", "services"));
		}
		
		
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i < streamer_status.getServices().size(); i++) {
			
			if(interface_type.equals("mod") || interface_type.equals("infostreamer") || interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
				flexTable.setText(i+1, 0, streamer_status.getServices().get(i).getName());
			} else {
				if(streamer_status.getServices().get(i).isScrambled()) {
					flexTable.setWidget(i+1, 0, new IconText(streamer_status.getServices().get(i).getName(), "lock"));
				} else {
					flexTable.setWidget(i+1, 0, new IconText(streamer_status.getServices().get(i).getName(), "unlock"));
				}
			}
			
			
			if(interface_type.equals("dsc") || interface_type.equals("mod")) {
				//flexTable.setText(i+1, flexTable.getCellCount(i+1), streamer_status.getServices().get(i).getSource());
			}
			
			if(!interface_type.equals("mod")) {
				//flexTable.setText(i+1, flexTable.getCellCount(i+1), streamer_status.getServices().get(i).getDestination());
			}
			
			flexTable.setText(i+1, flexTable.getCellCount(i+1), formatBitrate(streamer_status.getServices().get(i).getBitrate()));
			
			if(!interface_type.equals("infostreamer")) {
				if(!interface_type.equals("hdmi2ip")) {
					flexTable.setText(i+1, flexTable.getCellCount(i+1), ""+streamer_status.getServices().get(i).getDiscontinuity());
				}
			}
			
			if(interface_type.equals("dsc") || interface_type.equals("mod")) {
				flexTable.setText(i+1, flexTable.getCellCount(i+1), streamer_status.getServices().get(i).getMuxLoad()+" (max "+streamer_status.getServices().get(i).getMaxMuxLoad()+") %");
			}
			
			summary += streamer_status.getServices().get(i).getBitrate();
		}
		
		
		
		flexTable.setText(flexTable.getRowCount(), 0, streamer_status.getServices().size() + " channel(s)");
		
		if(interface_type.equals("dsc")) {
			//flexTable.setText(flexTable.getRowCount()-1, 1, "");
			//flexTable.setText(flexTable.getRowCount()-1, 2, "");
			flexTable.setText(flexTable.getRowCount()-1, 1, formatBitrate(summary));
			flexTable.setText(flexTable.getRowCount()-1, 2, "");
			flexTable.setText(flexTable.getRowCount()-1, 3, "");
		} else {
			//flexTable.setText(flexTable.getRowCount()-1, 1, "");
			flexTable.setText(flexTable.getRowCount()-1, 1, formatBitrate(summary));
			
			if(!interface_type.equals("infostreamer")) {
				if(!interface_type.equals("hdmi2ip")) {
					flexTable.setText(flexTable.getRowCount()-1, 2, "");
				}
			}
		}
		
		mainPanel.add(flexTable);
	}
	
	private String formatBitrate(int value) {
		
		double d = value * 0.000001;
		String formatted = NumberFormat.getFormat("#.00").format(d);
		
		return formatted + " Mbps";
	}
	
	public Widget asWidget() {
		return this;
	}
}
