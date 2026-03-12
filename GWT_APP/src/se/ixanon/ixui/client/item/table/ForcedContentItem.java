package se.ixanon.ixui.client.item.table;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import se.ixanon.ixui.shared.ForcedContent;
import se.ixanon.ixui.shared.Media;

public class ForcedContentItem extends Composite {

	private int id;
	private ToggleField enabled = new ToggleField("Enabled");
	private TextField name = new TextField("Name", true);
	private ListField networks = new ListField("DVB-C Networks");
	//FIXME
	//private TextField ts_filename = new TextField("Filename", true);
	private ListField ts_filename = new ListField("Media Name");
	private ListField operation_mode = new ListField("Operation Mode");
	private ListField signal_type = new ListField("Signal Type");
	private ListField volume = new ListField("Volume %");
	
	public ForcedContentItem(ForcedContent fc, ArrayList<Media> media) {
		
		id = fc.getId();
		
		FlowPanel mainPanel = new FlowPanel();
		initWidget(mainPanel);
		
		ArrayList<String> media_items = new ArrayList<String>();
		ArrayList<String> media_values = new ArrayList<String>();
		
		media_items.add("None");
		media_values.add("None");
		
		for(int i = 0; i < media.size(); i++) {
			media_items.add(media.get(i).getTitle());
			media_values.add(media.get(i).getInternalFilename());
		}
		
		ArrayList<String> networks_list = new ArrayList<String>();
		networks_list.add("None");
		networks_list.add("1");
		networks_list.add("2");
		networks_list.add("Both");
		
		ArrayList<String> operation_mode_list = new ArrayList<String>();
		operation_mode_list.add("Continuity");
		operation_mode_list.add("Single");
		
		ArrayList<String> signal_type_list = new ArrayList<String>();
		signal_type_list.add("Normally Open");
		signal_type_list.add("Normally Closed");
		
		
		ArrayList<String> volume_list = new ArrayList<String>();
		volume_list.add("None");
		
		for(int i = 0; i <= 100; i+=5) {
			volume_list.add(""+i);
		}
		
		
		
		
		
		Label label = new Label("Force Content #"+fc.getId()+":");
		label.setStyleName("bold semi-header");
		mainPanel.add(label);
		
		enabled.setToggle(fc.isEnable());
		mainPanel.add(enabled);
		
		name.setValue(fc.getName());
		mainPanel.add(name);
		
		networks.setList(networks_list);
		networks.setListIndex(fc.getNetworks());
		mainPanel.add(networks);
		
		//ts_filename.setValue(fc.getTsFilename());
		ts_filename.setList(media_items, media_values);
		ts_filename.setListValue(fc.getTsFilename(), false);
		mainPanel.add(ts_filename);
		
		operation_mode.setList(operation_mode_list);
		operation_mode.setListIndex(fc.getOperationMode());
		mainPanel.add(operation_mode);
		
		signal_type.setList(signal_type_list);
		signal_type.setListIndex(fc.getSignalType());
		mainPanel.add(signal_type);
		
		volume.setList(volume_list);
		volume.setListValue(""+fc.getVolume());
		mainPanel.add(volume);
	}
	
	public ForcedContent getValues() {
		
		String value = volume.getValue();
		int vol = -1;
		
		if(!value.equals("None")) {
			vol = Integer.parseInt(value);
		}
		
		return new ForcedContent(id, enabled.isToggle(), name.getValue(), networks.getListIndex(), ts_filename.getValue(false), operation_mode.getListIndex(), signal_type.getListIndex(), vol);
		
	}
}
