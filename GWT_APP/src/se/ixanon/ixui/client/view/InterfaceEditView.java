package se.ixanon.ixui.client.view;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import se.ixanon.ixui.client.Helper;
import se.ixanon.ixui.client.Session;
import se.ixanon.ixui.client.event.EmmUpdateEvent;
import se.ixanon.ixui.client.item.table.CheckField;
import se.ixanon.ixui.client.item.table.Header;
import se.ixanon.ixui.client.item.table.IconText;
import se.ixanon.ixui.client.item.table.ListField;
import se.ixanon.ixui.client.item.table.ScanButton;
import se.ixanon.ixui.client.item.table.TextField;
import se.ixanon.ixui.client.item.table.ToggleField;
import se.ixanon.ixui.client.item.table.ToggleTextField;
import se.ixanon.ixui.client.presenter.InterfaceEditPresenter;
import se.ixanon.ixui.shared.Config;
import se.ixanon.ixui.shared.Emm;
import se.ixanon.ixui.shared.Service;

public class InterfaceEditView extends Composite implements InterfaceEditPresenter.Display {

	private FlowPanel mainPanel;
	
	private ListField multiband = new ListField("Interface Type");
	//private Button saveMultiband = new Button("Update Interface");
	
	private TextField pres_url = new TextField("Presentation URL", true);
	private ListField format = new ListField("Format");
	private ToggleTextField max_bitrate = new ToggleTextField("Max Bitrate (bps)", false);
	
	private ListField constellation = new ListField("Constellation");
	
	private TextField name = new TextField("Name", true);
	//private ListField active = new ListField("Interface Active");
	private ToggleField toggle_active = new ToggleField("Interface Active");
	private ListField emm = new ListField("EMM");
	private ListField bw = new ListField("Bandwidth (MHz)");
	private TextField frequency = new TextField("Frequency (MHz)", false);
	private ListField polarization = new ListField("Polarization");
	private TextField symbol_rate = new TextField("Symbolrate", false);
	private ListField symbol_rate_dvbc = new ListField("Symbolrate (Ksym)");
	private ListField delivery_method = new ListField("Delivery Method");
	private ListField delivery_method_dvbt = new ListField("Delivery Method");
	private ListField delivery_method_dvbc = new ListField("Delivery Method");
	private ListField satellite_number = new ListField("Satellite Number");
	private ListField lnb_type = new ListField("LNB Type");
	private ScanButton scan_button;
	private FlexTable flexTable = new FlexTable();
	private FlowPanel buttonPanel = new FlowPanel();
	private Button saveInterfaceButton = new Button("Save");
	private Button saveServicesButton = new Button("Save Services");
	private Config config;
	private ArrayList<Service> services;
	
	private TextField in_ip = new TextField("Address", true);
	private TextField in_port = new TextField("Port", false);
	
	private ListField gain = new ListField("Gain (dB)");
	private ToggleTextField webradio_url = new ToggleTextField("Webradio Url", true);
	
	public InterfaceEditView() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("main");
		initWidget(mainPanel);
		
		/*
		ArrayList<String> active_list = new ArrayList<String>();
		active_list.add("true");
		active_list.add("false");
		active.setList(active_list);
		*/
		
		ArrayList<String> polarization_list = new ArrayList<String>();
		polarization_list.add("H");
		polarization_list.add("V");
		polarization.setList(polarization_list);
		
		ArrayList<String> delivery_method_list = new ArrayList<String>();
		delivery_method_list.add("DVBS");
		delivery_method_list.add("DVBS2");
		delivery_method.setList(delivery_method_list);
		
		ArrayList<String> delivery_method_dvbt_list = new ArrayList<String>();
		delivery_method_dvbt_list.add("DVBT");
		delivery_method_dvbt_list.add("DVBT2");
		delivery_method_dvbt.setList(delivery_method_dvbt_list);
		
		
		
		ArrayList<String> delivery_method_dvbc_list = new ArrayList<String>();
		delivery_method_dvbc_list.add("DVBC");
		delivery_method_dvbc_list.add("DVBC2");
		delivery_method_dvbc.setList(delivery_method_dvbc_list);
		
		ArrayList<String> symbol_rate_dvbc_list = new ArrayList<String>();
		symbol_rate_dvbc_list.add("6875");
		symbol_rate_dvbc_list.add("6900");
		symbol_rate_dvbc.setList(symbol_rate_dvbc_list);
		
		
		
		ArrayList<String> satellite_number_list = new ArrayList<String>();
		satellite_number_list.add("0");
		satellite_number_list.add("1");
		satellite_number_list.add("2");
		satellite_number_list.add("3");
		satellite_number.setList(satellite_number_list);
		
		ArrayList<String> lnb_type_list = new ArrayList<String>();
		lnb_type_list.add("None");
		lnb_type_list.add("Universal");
		lnb_type.setList(lnb_type_list);
		
		ArrayList<String> bw_list = new ArrayList<String>();
		bw_list.add("5");
		bw_list.add("6");
		bw_list.add("7");
		bw_list.add("8");
		bw.setList(bw_list);
		
		ArrayList<String> format_list = new ArrayList<String>();
		format_list.add("auto");
		format_list.add("720p50");
		format_list.add("720p60");
		format_list.add("1080i50");
		format_list.add("1080p50");
		format.setList(format_list);
		
		ArrayList<String> qam_list = new ArrayList<String>();
		qam_list.add("qam8");
		qam_list.add("qam16");
		qam_list.add("qam32");
		qam_list.add("qam64");
		qam_list.add("qam128");
		qam_list.add("qam256");
		constellation.setList(qam_list);
		
		ArrayList<String> db_list = new ArrayList<String>();
		db_list.add("-12");
		db_list.add("-11");
		db_list.add("-10");
		db_list.add("-9");
		db_list.add("-8");
		db_list.add("-7");
		db_list.add("-6");
		db_list.add("-5");
		db_list.add("-4");
		db_list.add("-3");
		db_list.add("-2");
		db_list.add("-1");
		db_list.add("0");
		db_list.add("+1");
		db_list.add("+2");
		db_list.add("+3");
		db_list.add("+4");
		db_list.add("+5");
		db_list.add("+6");
		db_list.add("+7");
		db_list.add("+8");
		db_list.add("+9");
		db_list.add("+10");
		db_list.add("+11");
		db_list.add("+12");
		gain.setList(db_list);
		
	}
	
	public void addInfo(String text) {
		IconText info = new IconText(text, "info-circle", "wrapper-webradio-info");
		mainPanel.add(info);
	}
	
	public void setHeader(Header header, boolean hls) {
		mainPanel.add(header);
		
		/*
		if(hls) {
			FlowPanel wrapper = new FlowPanel();
			wrapper.setStyleName("wiz-wrapper");
			EventButton button = new EventButton("Wizard", "magic", "blue");
			InlineLabel text = new InlineLabel("Use the wizard to add multiple channels with ease.");
			
			wrapper.add(button);
			wrapper.add(text);
			
			mainPanel.add(wrapper);
		}
		*/
	}
	
	public void buildMultiBand(String interface_type) {
		
		ArrayList<String> multiband_list = new ArrayList<String>();
		multiband_list.add("dvbs");
		multiband_list.add("dvbt");
		multiband_list.add("dvbc");
		
		multiband.setList(multiband_list);
		multiband.setListValue(interface_type);
		
		mainPanel.add(multiband);
		
		
		//saveMultiband.setStyleName("btn blue mb");
		//mainPanel.add(saveMultiband);
		
	}
	
	public void buildScan(String interface_type, Config config) {

		
		if(interface_type.equals("infoch")) {
			
			name.setValue(config.getInterfaceName());
			mainPanel.add(name);
						
			toggle_active.setToggle(config.getInterfaceActive());
			mainPanel.add(toggle_active);
			
		}
		
		saveInterfaceButton.setStyleName("btn blue");
		mainPanel.add(saveInterfaceButton);
		
		
		boolean webradio = false;
		
		if(interface_type.equals("webradio") && Session.getInstance().isCloud()) {
			webradio = true;
		}
		
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp") || interface_type.equals("hls2ip") || interface_type.equals("infoch") || webradio) {
			scan_button = new ScanButton();
			mainPanel.add(scan_button);
		}
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp") || interface_type.equals("infostreamer") || interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip") || interface_type.equals("hls2ip") || interface_type.equals("infoch") || webradio) {
			mainPanel.add(flexTable);
			mainPanel.add(buttonPanel);
		}
		
		/*
		if(interface_type.equals("webradio")) {
			Session.getInstance().getHandlerManager().fireEvent(new ToggleChangeEvent(null, webradio_url.isToggle()));
		}
		*/
		
	}
	
	/*
	public void setButtonsEnabled(String interface_type, boolean isToggle) {
		
		if(interface_type.equals("webradio")) {
			scan_button.setEnabled(!isToggle);
			saveServicesButton.setEnabled(!isToggle);
		}
		
		
	}
	*/
	
	public void build(Config config, String interface_type) {
		
		this.config = config;
		
		boolean check = true;
		if(interface_type.equals("infostreamer")) {
			check = false;
		} else if(interface_type.equals("dvbhdmi")) {
			check = false;
		} else if(interface_type.equals("hdmi2ip")) {
			check = false;
		} else if(interface_type.equals("hls2ip")) {
			check = false;	
		} else if(interface_type.equals("webradio")) {
			check = false;	
		}
		
		if(config != null) {
			
			name.setValue(config.getInterfaceName());
			
			if(!interface_type.equals("dsc")) {
				//active.setListValue(""+config.getInterfaceActive());
				toggle_active.setToggle(config.getInterfaceActive());
			}
			
			if(check) {
				//emm.setListValue(""+config.getEmm());
								
				if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc")) {
					frequency.setValue(""+config.getFreq());
				}
				
				if(interface_type.equals("dvbs")) {
					polarization.setListValue(""+config.getPol());
					symbol_rate.setValue(""+config.getSymb());
					delivery_method.setListValue(""+config.getDel());
					satellite_number.setListValue(Integer.toString(config.getSatno()));
					lnb_type.setListValue(config.getLnbType());
				} else if(interface_type.equals("dvbt")) {
					bw.setListValue("8");
					bw.setListValue(""+config.getBw());
					
					
					delivery_method_dvbt.setListValue(config.getDel());
					
				} else if(interface_type.equals("dvbc")) {
					//symbol_rate.setValue(""+config.getSymb());
					symbol_rate_dvbc.setListValue("6900");
					symbol_rate_dvbc.setListValue(""+config.getSymb());
					
					constellation.setListValue("qam256");
					constellation.setListValue(config.getConstellation());
					
					delivery_method_dvbc.setListValue(config.getDel());
					
				} else if(interface_type.equals("dvbudp")) {
					in_ip.setValue(config.getInIp());
					in_port.setValue(""+config.getInPort());
				}
			} else {
				if(interface_type.equals("infostreamer")) {
					pres_url.setValue(config.getPresUrl());
				}
				
				if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
					format.setListValue("auto");
					format.setListValue(config.getHdmiFormat());
				}
				
				if(interface_type.equals("hls2ip")) {
					max_bitrate.setValue(""+config.getMaxBitrate());
				}
				
				if(interface_type.equals("webradio")) {
					
					
					String value = String.valueOf(config.getGain()); 
					if(config.getGain() > 0) {
						value = "+" + value;
					}
					
					gain.setListValue(value);
					
					webradio_url.setValue(config.getWebradioUrl());
				}
				
			}
			
							
		}
		
		mainPanel.add(name);
		
		if(!interface_type.equals("dsc")) {
			mainPanel.add(toggle_active);
		}
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc")) {
			mainPanel.add(frequency);
		}
		
		if(interface_type.equals("dvbs")) {
			mainPanel.add(polarization);
			mainPanel.add(symbol_rate);
			mainPanel.add(delivery_method);
			mainPanel.add(satellite_number);
			mainPanel.add(lnb_type);
		} else if(interface_type.equals("dvbt")) {
			
			mainPanel.add(bw);
			mainPanel.add(delivery_method_dvbt);
			
		} else if(interface_type.equals("dvbc")) {
			mainPanel.add(symbol_rate_dvbc);
			mainPanel.add(constellation);
			mainPanel.add(delivery_method_dvbc);
		} else if(interface_type.equals("dvbudp")) {
			mainPanel.add(in_ip);
			mainPanel.add(in_port);
		}
		
		
		if(check) {
			//setEmmList(interface_type, emm_list, config.getEmm());
			Session.getInstance().getHandlerManager().fireEvent(new EmmUpdateEvent());
			mainPanel.add(emm);
		} else {
			
			if(interface_type.equals("infostreamer")) {
				mainPanel.add(pres_url);
			}
			
			if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
				mainPanel.add(format);
			}
			
			if(interface_type.equals("hls2ip")) {
				mainPanel.add(max_bitrate);
			}
			
			if(interface_type.equals("webradio")) {
				mainPanel.add(gain);
				
				if(!Session.getInstance().isCloud()) {
					mainPanel.add(webradio_url);
				}
				
				
			}
			
		}
		
		
		
		buildScan(interface_type, null);
		
	}
	
	public void setEmmList(String interface_type, Emm emm_result) {
		
		ArrayList<String> emm_list = new ArrayList<String>();
		boolean isNone = false;
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp")) {
			isNone = true;
		}
		
		
		for (int i = 0; i <= 5; i++) {
			
			if(i == 0) {
				if(isNone) {
					emm_list.add("None");
				}
				
			} else {
				
				if(emm_result.getFree().contains(i) || i == emm_result.getCurrent()) {
					emm_list.add(""+i);
				}
				
				
			}
			
			
		}
		
		emm.setList(emm_list);
		emm.setListValue(""+emm_result.getCurrent());
		
		
	}
	
	public void setScanDate(String scan_time) {
		scan_button.setDate(scan_time);
	}
	
	public void setCheckBoxIndex(int index) {
		for (int i = 1; i < flexTable.getRowCount(); i++) {
			CheckField cf = (CheckField) flexTable.getWidget(i, 1);
			
			if(i == index) {
				cf.setEnabled(true);
			} else {
				cf.setEnabled(false);
			}
		}
	}
	
	public void buildTable(ArrayList<Service> services, String interface_type) {
		
		this.services = new ArrayList<Service>(services);
		
		ArrayList<String> type_list = new ArrayList<String>();
		type_list.add("RADIO");
		type_list.add("TV_HD");
		type_list.add("TV_SD");
		
		flexTable.removeAllRows();
		
		flexTable.addStyleName("flexTable epg");
		flexTable.setWidth("100%");
		flexTable.setCellSpacing(0);
		flexTable.setCellPadding(5);
		
		//flexTable.setWidget(0, 0, new ReorderColumn("Name", "services"));
		
		flexTable.setText(0, 0, "Name");
		flexTable.setText(0, 1, "Enabled");
		
		if(!interface_type.equals("infostreamer")) {
			flexTable.setText(0, 2, "Sid");
			flexTable.setText(0, 3, "Type");
			flexTable.setText(0, 4, "Lang");
			flexTable.setText(0, 5, "Epg");
		} else {
			flexTable.setText(0, 2, "Radio URL");
			flexTable.setText(0, 3, "Show Presentation");
		}
		
		if(interface_type.equals("hls2ip") || interface_type.equals("webradio") || interface_type.equals("infoch")) {
			flexTable.setText(0, 6, "Url");
		}
		
		flexTable.getRowFormatter().addStyleName(0, "FlexTable-Header");
		
		for (int i = 0; i < services.size(); i++) {
			
			/*
			TextField name = new TextField(null, true);
			name.setValue(services.get(i).getName());
			
			flexTable.setWidget(i+1, 0, name);
			*/
			
			flexTable.setText(i+1, 0, services.get(i).getName());
			
			if(interface_type.equals("hls2ip") || interface_type.equals("webradio") || interface_type.equals("infoch")) {
				flexTable.setWidget(i+1, 1, new CheckField(services.get(i).isEnabled(), null, i+1));
			} else {
				flexTable.setWidget(i+1, 1, new CheckField(services.get(i).isEnabled(), null));
			}
			
			
			if(!interface_type.equals("infostreamer")) {
				flexTable.setText(i+1, 2, ""+services.get(i).getSid());
				
				ListField types = new ListField(null);
				types.setList(type_list);
				types.setListValue(services.get(i).getType());
				
				flexTable.setWidget(i+1, 3, types);
				
				ListField languages = new ListField(null);
				languages.setList(services.get(i).getAllLangs());
				languages.setListValue(services.get(i).getLang());
				
				flexTable.setWidget(i+1, 4, languages);
				
				String epg_value = services.get(i).getEpgUrl();
				
				if(epg_value == null || epg_value.equals("null")) {
					epg_value = "";
				}
				
				flexTable.setText(i+1, 5, epg_value);
				
			} else {
				
				TextField radio_url = new TextField(null, true);
				radio_url.setValue(services.get(i).getRadioUrl());
				flexTable.setWidget(i+1, 2, radio_url);
				
				flexTable.setWidget(i+1, 3, new CheckField(services.get(i).isShowPres(), null));
			}
			
			if(interface_type.equals("hls2ip")) {
				flexTable.setText(i+1, 6, services.get(i).getHlsUrl());
			} else if(interface_type.equals("webradio") || interface_type.equals("infoch")) {
				flexTable.setText(i+1, 6, services.get(i).getWebradioUrl());
			}
			
		}
		
		buttonPanel.clear();
		
		if(services.size() > 0) {
			saveServicesButton.setStyleName("btn blue");
			mainPanel.add(saveServicesButton);
		}
		
	}
	
	public Config getConfig(String interface_pos, String interface_type) {
		
		
		if(interface_type.equals("infoch")) {
			return new Config(interface_pos, name.getValue(), toggle_active.isToggle());
		}
		
		
		Config new_config = null;
		
		int id = 0;
		if(config != null) {
			id = config.getId();
		}
		
		int freq = 0;
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc")) {
			freq = Integer.parseInt(frequency.getValue());
		}
		
		if(interface_type.equals("dvbs")) {
			
			String pol = polarization.getValue();
			int symb = Integer.parseInt(symbol_rate.getValue());
			String del = delivery_method.getValue();
			int satno = Integer.parseInt(satellite_number.getValue());
			String lbn = lnb_type.getValue();
			
			new_config = new Config(id, interface_pos, freq, pol, symb, del, satno, lbn);
			
		} else if(interface_type.equals("dvbt")) {
			
			int bandwidth = Integer.parseInt(bw.getValue());
			String del = delivery_method_dvbt.getValue();
			
			new_config = new Config(id, interface_pos, freq, del, bandwidth); 
		
		} else if(interface_type.equals("dvbc")) {
			
			int symb = Integer.parseInt(symbol_rate_dvbc.getValue());
			String constellation_str = constellation.getValue();
			
			String del = delivery_method_dvbc.getValue();
			
			new_config = new Config(id, interface_pos, freq, symb, del, constellation_str); 
		} else if(interface_type.equals("dvbudp")) {
			
			String ip = in_ip.getValue();
			int port = Integer.parseInt(in_port.getValue());
			
			new_config = new Config(id, interface_pos, ip, port);
			
		} else if(interface_type.equals("dsc")) {
			new_config = new Config(0, interface_pos); 
		} else if(interface_type.equals("infostreamer")) {
			new_config = new Config(id, interface_pos);
			new_config.setPresUrl(pres_url.getValue());
		} else if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
			new_config = new Config(id, interface_pos);
			new_config.setHdmiFormat(format.getValue());
		} else if(interface_type.equals("hls2ip")) {
			new_config = new Config(id, interface_pos);
			new_config.setMaxBitrate(Helper.getNull(max_bitrate.getValue()));
		} else if(interface_type.equals("webradio")) {
			new_config = new Config(id, interface_pos);
			new_config.setGain(Helper.getNull(gain.getValue()));
			new_config.setWebradioUrl(webradio_url.getValue());
		}
		 
		new_config.setInterfaceName(name.getValue());
		
		if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp") || interface_type.equals("infostreamer") || interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip") || interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
			//new_config.setInterfaceActive(Boolean.valueOf(active.getValue()));
			new_config.setInterfaceActive(toggle_active.isToggle());
			
		}
		
		
		boolean check = true;
		if(interface_type.equals("infostreamer")) {
			check = false;
		} else if(interface_type.equals("dvbhdmi")) {
			check = false;	
		} else if(interface_type.equals("hdmi2ip")) {
			check = false;	
		} else if(interface_type.equals("hls2ip")) {
			check = false;	
		} else if(interface_type.equals("webradio")) {
			check = false;	
		}
		
		if(check) {
			int emm_temp = 0;
			if(!emm.getValue().equals("None")) {
				emm_temp = Integer.parseInt(emm.getValue());
			}
			new_config.setEmm(emm_temp);
		}
		
		return new_config;
	}
	
	public ArrayList<Service> getServices(String interface_type) {
		
		for (int i = 0; i < services.size(); i++) {
			
			String epg_value = services.get(i).getEpgUrl();
			
			if(epg_value == null || epg_value.equals("null")) {
				services.get(i).setEpgUrl("");
			}
			
			
			//TextField name = (TextField) flexTable.getWidget(i+1, 0);
			//services.get(i).setName(name.getValue());
			
			services.get(i).setName(flexTable.getText(i+1, 0));
			
			CheckField enabled = (CheckField) flexTable.getWidget(i+1, 1);
			services.get(i).setEnabled(enabled.getValue());
			
			if(!interface_type.equals("infostreamer")) {
				
				ListField types = (ListField) flexTable.getWidget(i+1, 3);
				services.get(i).setType(types.getValue());
				
				ListField languages = (ListField) flexTable.getWidget(i+1, 4);
				services.get(i).setLang(languages.getValue());
				
			} else {
				
				TextField radio_url = (TextField) flexTable.getWidget(i+1, 2);
				services.get(i).setRadioUrl(radio_url.getValue());
				
				CheckField show_pres = (CheckField) flexTable.getWidget(i+1, 3);
				services.get(i).setShowPres(show_pres.getValue());
				
			}
			
			if(interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
				services.get(i).setHlsUrl(flexTable.getText(i+1, 6));
			}
			
			
		}
		
		return this.services;
	}
	
	public void setName(String interface_type) {
		
		if(interface_type.equals("dvbs")) {
			name.setValue(frequency.getValue() + polarization.getValue().toLowerCase());
		}
		
		if(interface_type.equals("dvbt")) {
			name.setValue(frequency.getValue());
		}
	}
	
	public HasClickHandlers getSaveInterfaceButton() {
		return this.saveInterfaceButton;
	}
	
	public HasClickHandlers getSaveServicesButton() {
		return this.saveServicesButton;
	}
	/*
	public HasClickHandlers getSaveMultiband() {
		return this.saveMultiband;
	}
	*/
	public String getMultiBandInterfaceType() {
		return multiband.getValue();
	}
		
	public Widget asWidget() {
		return this;
	}
}
