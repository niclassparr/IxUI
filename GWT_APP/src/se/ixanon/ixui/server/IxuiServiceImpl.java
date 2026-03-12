package se.ixanon.ixui.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.client.IxuiService;
import se.ixanon.ixui.server.singleton.DatabaseUtils;
import se.ixanon.ixui.server.singleton.PDFGenerator;
import se.ixanon.ixui.shared.Bitrate;
import se.ixanon.ixui.shared.Config;
import se.ixanon.ixui.shared.Emm;
import se.ixanon.ixui.shared.ForcedContent;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.IpMac;
import se.ixanon.ixui.shared.IpStatus;
import se.ixanon.ixui.shared.Media;
import se.ixanon.ixui.shared.NameValue;
import se.ixanon.ixui.shared.PDFRow;
import se.ixanon.ixui.shared.PDFValue;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.Route;
import se.ixanon.ixui.shared.Service;
import se.ixanon.ixui.shared.ServiceStatus;
import se.ixanon.ixui.shared.StreamerStatus;
import se.ixanon.ixui.shared.TunerStatus;
import se.ixanon.ixui.shared.UnitInfo;
import se.ixanon.ixui.shared.Package;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class IxuiServiceImpl extends RemoteServiceServlet implements IxuiService {

	private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private Connection Connection;
	private Statement Statement;
	private ResultSet Result;
	
	public void init(){
		DatabaseUtils.getInstance().init();
		Connection = DatabaseUtils.getInstance().getConnection();
	}
	
	
	
	public boolean validateSession(String sessionKey, String username) {
		
		//System.out.println("sessionKey: " + sessionKey);
		
		int uid = getUserIdFromSession(sessionKey);
		if (uid == -1) {
			//System.out.println("ERROR: Invalid Session");
			return false;
		}
		
		try {
			String dbQuery = "SELECT name FROM users WHERE id = " + uid + ";";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			Result.next();
			String name = Result.getString("name");
			
			if(username.equals(name)) {
				return true;
			}
		}
		catch(Exception e){
			return false;
		}
		
		return false;
		
	}
	
	public SessionKeys checkPersmission(String sessionKey, SessionKeys session_key) {
		
		String ixcloud_enable = null;
		
		try {
			String dbQuery = "SELECT name, value FROM nv;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				if(name.equals("ixcloud_enable")) {
					ixcloud_enable = Result.getString("value");
				}				
			}
			
		}
		catch(Exception e){
			System.out.println("checkPersmission: " + e.getMessage());
		}
		
		boolean cloud = false;
		
		if(ixcloud_enable != null) {
			if(ixcloud_enable.equals("true")) {
				cloud = true;
			}
		}
		
		session_key.setCloud(cloud);
		
		return session_key;
		
	}
	
	public String login(String username, String password) {
		
		if(!DatabaseUtils.getInstance().isDbConnected()) {
			init();
		}
		
		System.out.println("Login");
		
		try {
			String dbQuery = "SELECT password FROM users WHERE name = '" + username + "';";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			Result.next();
			String pwd = Result.getString("password");
			
			
			
			if(password.equals(pwd)) {
				
				System.out.println("Login ok");
				
				HttpSession httpSession = getThreadLocalRequest().getSession();
				httpSession.setMaxInactiveInterval(1000 * 60 *2);
				String skey = httpSession.getId();
				createSession(username, skey);
				//System.out.println("INFO: Login: " + username + ", session_key = " + skey);
				return skey;
			}
		}
		catch(Exception e) {
			System.out.println("Login Exception: " + username + ", " + e.getMessage());
		}
		return null;
	}
	
	public Response updatePw(String username, String old_password, String new_password) {
		
		boolean check = false;
		
		try {
			String dbQuery = "SELECT password FROM users WHERE name = '"+username+"';";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			Result.next();
			String pwd = Result.getString("password");
			check = old_password.equals(pwd);
		
		}
		catch(Exception e) {
			System.out.println("updatePw ERROR: " + e.getMessage());
			return new Response(false, "Server error.");
		}
		
		
		
		try {
			if(check){
				
				String updateQuery = "UPDATE users SET password = '"+new_password+"' WHERE name = '"+username+"';";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(updateQuery);
				
			} else {
				return new Response(false, "Old password do not match.");
			}
		
		}
		catch(Exception e) {
			System.out.println("updatePw ERROR: " + e.getMessage());
			return new Response(false, "Server error.");
		}
		
		
		
		return new Response(true, "Done.");
		
	}
	
	private Boolean createSession(String username, String skey)	{
		int uid = 0;

		try {
			// Find the user_id
			String dbQuery = "SELECT id FROM users WHERE name='" + username + "';";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			Result.next();
			uid = Result.getInt("id");
		}
		catch(Exception e){
			return false;
		}

		try {
			// Delete old sessions for this user
			String dbDeleteQuery = "DELETE FROM user_sessions WHERE user_id = " + uid + " AND created < NOW() - INTERVAL '10 days';";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbDeleteQuery);
		}
		catch(Exception e){
			// Empty
		}

		try {
			// Store the new session
			String dbInsertQuery = "INSERT INTO user_sessions (user_id, session_key) VALUES (" + uid + ", '" + skey + "');";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbInsertQuery);
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	public void logout(String sessionKey) {
		
		int uid = getUserIdFromSession(sessionKey);
		if (uid == -1) {
			//System.out.println("ERROR: Invalid Session");
			return;
		}

		try {
			
			//System.out.println("Logout");
			
			String dbQuery = "DELETE FROM user_sessions WHERE session_key = '" + sessionKey + "';";
			//System.out.println("xxxDB: " + dbQuery);
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
			HttpSession httpSession = getThreadLocalRequest().getSession(false);
			httpSession.invalidate();
		}
		catch(Exception e){
			//System.out.println("Logout ERROR: " + e.getMessage());
		}
	}
	
	// Returns user id or -1 on failure.
	private int getUserIdFromSession(String sessionKey)	{
		try {
			String dbQuery = "SELECT user_id FROM user_sessions WHERE session_key = '" + sessionKey + "';";
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			Result.next();
			return Result.getInt("user_id");
		}
		catch(Exception e){
			return -1;
		}
	}
	
	public ArrayList<Interface> getInterfacesHls(String sessionKey) {
		
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE type = 'hls2ip' ORDER BY pos;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {

				String type = Result.getString("type");
				
				String pos = Result.getString("pos");
				String name = Result.getString("name");
				
				interfaces.add(new Interface(pos, name, type));
				
			}
			
		}
		catch(Exception e){
			System.out.println("getInterfaces: " + e.getMessage());
		}
		
		return interfaces;
		
	}
	
	public ArrayList<String> getInterfaceTypes() {
		
		
		
		ArrayList<String> interfaces = new ArrayList<String>();
		
		try {
			String dbQuery = "SELECT type FROM interfaces GROUP BY type ORDER BY type;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {

				interfaces.add(Result.getString("type"));
				
			}
			
		}
		catch(Exception e){
			//System.out.println("getInterfaces: " + e.getMessage());
		}
		
		return interfaces;
		
	}
	
	public HashMap<String, Integer> getModulators(String sessionKey) {
		
		HashMap<String, Integer> mods = new HashMap<String, Integer>();
		
		try {
			String dbQuery = "SELECT pos FROM interfaces WHERE type = 'mod' ORDER BY pos;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			
			
			while (Result.next()) {

				String pos = Result.getString("pos");
				mods.put(pos, 1);
				
			}
			
		}
		catch(Exception e){
			System.out.println("getModulators: " + e.getMessage());
		}
		
		
		for (Map.Entry<String, Integer> mod : mods.entrySet()) {
			
			int network_num = getModulatorNetworkNum(mod.getKey());
			
			if(network_num != -1) {
				mod.setValue(network_num);
			}
		}
				
		return mods;
	}
	
	private int getModulatorNetworkNum(String interface_pos) {
		
		int network_num = -1;
		
		try {
			String dbQuery = "SELECT network_num FROM config_eqam WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);

			Result.next();
			network_num = Result.getInt("network_num");
			
		}
		catch(Exception e){
			//System.out.println("getModulatorNetworkNum: " + e.getMessage());
		}
		
		return network_num;
	}
	
	public void saveModulatorsConfig(HashMap<String, Integer> modulators) {
	
		for (Map.Entry<String, Integer> modulator : modulators.entrySet()) {
			saveModulatorConfig(modulator.getKey(), modulator.getValue());
		}
		
	}
	
	private void saveModulatorConfig(String interface_pos, int network_num) {
		
		//Check if the config exists
		int check_network_num = getModulatorNetworkNum(interface_pos);
		
		
		String dbQuery = "UPDATE config_eqam SET network_num = "+network_num+" WHERE interface_pos = '"+interface_pos+"';";
		
		if(check_network_num == -1) {
			dbQuery = "INSERT INTO config_eqam (interface_pos, network_num) VALUES ('"+interface_pos+"', "+network_num+");";
		}
		
		try {
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("saveModulatorConfig: " + e.getMessage());
		}
		
	}
	
	
	public ArrayList<Interface> getInterfaces(String sessionKey, boolean isInterfaces) {
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		try {
			String dbQuery = "SELECT * FROM interfaces ORDER BY pos;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {

				String type = Result.getString("type");
				
				String pos = Result.getString("pos");
				String name = Result.getString("name");
				
				boolean active = Result.getBoolean("active");
				boolean multiband = Result.getBoolean("multiband");
				
				interfaces.add(new Interface(pos, name, type, null, active, multiband));
				
			}
			
		}
		catch(Exception e){
			System.out.println("getInterfaces: " + e.getMessage());
		}
		
		for (int i = 0; i < interfaces.size(); ++i) {
			if(interfaces.get(i).getType().equals("mod")) {
				
				int network_num = getModulatorNetworkNum(interfaces.get(i).getPosition());
				
				if(network_num == -1) {
					interfaces.get(i).setNetworkNum(1);
				} else {
					interfaces.get(i).setNetworkNum(network_num);
				}
				
			}
		}
		
		if(isInterfaces) {
			for (int i = 0; i < interfaces.size(); ++i) {
				String status = interfaceStatus(interfaces.get(i).getPosition());
				
				int emm = getConfigEmm(interfaces.get(i).getPosition(), interfaces.get(i).getType().equals("dsc"));
								
				interfaces.get(i).setStatus(status);
				interfaces.get(i).setEmm(emm > 0);
			}
		}
		
		//System.out.println("size: " + interfaces.size());
		
		return interfaces;
	}
	
	/*
	private int getInterfaceEmm(String interface_pos) {
		
		int emm = 0;
		
		try {
			String dbQuery = "SELECT emm FROM config_emm WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			emm = Result.getInt("emm");
		
		}
		catch(Exception e){
			//System.out.println("getInterfaceEmm: " + e.getMessage());
		} 
		
		return emm;
		
	}
	*/
	
	public Emm getCurrentEmmList(String interface_pos, boolean isDsc) {
		
		
		
		
		
		
		
		
		int current = getConfigEmm(interface_pos, isDsc);
		
		Emm emm = new Emm(current);
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		if(isDsc) {
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(4);
			list.add(5);
			
			emm.setFree(list);
			return emm;
		}
		
		
		try {
			String dbQuery = "SELECT emm FROM config_emm WHERE interface_pos IS NULL;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				list.add(Result.getInt("emm"));
			}
		
		}
		catch(Exception e){
			System.out.println("getInterfaceEmmList 2: " + e.getMessage());
		} 
		
		emm.setFree(list);
		
		return emm;
		
	}
	
	private ArrayList<NameValue> printInterfaceEmmList() {
		
		ArrayList<NameValue> list = new ArrayList<NameValue>();
		
		try {
			String dbQuery = "SELECT emm, interface_pos FROM config_emm;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				list.add(new NameValue(Result.getInt("emm"), Result.getString("interface_pos")));
			}
		
		}
		catch(Exception e){
			System.out.println("printInterfaceEmmList: " + e.getMessage());
		} 

		return list;
		
	}
	
	public StreamerStatus interfaceStreamerStatus(String interface_pos, String interface_type) {
		
		String infoch_name = "name?";
		
		if(interface_type.equals("infoch")) {
			infoch_name = getInterfaceInfoch(interface_pos).getInterfaceName();
		}
		
		StreamerStatus status = null;
		ArrayList<ServiceStatus> services = new ArrayList<ServiceStatus>();
		Document doc = null;
		
		try {
			String xmlstr = StreamerManager.command("interface/"+interface_pos+"/streamerStatus get");
			//System.out.println("xmlstr: " + xmlstr);
			
			if(!xmlstr.equals("")) {
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource inputSource = new InputSource(new StringReader(xmlstr));
				doc = db.parse(inputSource);	
			} else {
				return status;
			}
			
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
		}
		
		if(doc != null) {
			
			if(interface_type.equals("infoch")) {
				
				NodeList sink_list = doc.getElementsByTagName("sink");
				
				Node sink_node = sink_list.item(0);
				Element sink_element = (Element)sink_node;
					
				int bitrate = Integer.parseInt(getValue(sink_element, "bitrate"));
								
				services.add(new ServiceStatus(infoch_name, bitrate));
				
				status = new StreamerStatus(services);
				
			} else if(interface_type.equals("webradio")) {
			
				NodeList sink_list = doc.getElementsByTagName("sink");
				
				Node sink_node = sink_list.item(0);
				Element sink_element = (Element)sink_node;
					
				int bitrate = Integer.parseInt(getValue(sink_element, "bitrate"));
				int bufferlevel = Integer.parseInt(getValue(sink_element, "bufferlevel"));
				
				services.add(new ServiceStatus(bitrate, bufferlevel));
				
				status = new StreamerStatus(services);
				
			} else if(interface_type.equals("hls2ip")) {
				
				NodeList source_list = doc.getElementsByTagName("source");
				
				Node source_node = source_list.item(0);
				Element source_element = (Element)source_node;
								
				int download_bitrate = Integer.parseInt(getValue(source_element, "download_bitrate"));
				int selected_bitrate = Integer.parseInt(getValue(source_element, "selected_bitrate"));
				int segmentCounter = Integer.parseInt(getValue(source_element, "segmentCounter"));
				int num_stream_switches = Integer.parseInt(getValue(source_element, "num_stream_switches"));
				int num_segments_missed = Integer.parseInt(getValue(source_element, "num_segments_missed"));
				
				
				
				
				NodeList sink_list = doc.getElementsByTagName("sink");
				
				Node sink_node = sink_list.item(0);
				Element sink_element = (Element)sink_node;
					
				int bitrate = Integer.parseInt(getValue(sink_element, "bitrate"));
				int bufferlevel = Integer.parseInt(getValue(sink_element, "bufferlevel"));
				
				
				services.add(new ServiceStatus(download_bitrate, selected_bitrate, segmentCounter, num_stream_switches, num_segments_missed, bitrate, bufferlevel));
				
				status = new StreamerStatus(services);
				
				
			} else if(interface_type.equals("infostreamer") || interface_type.equals("hdmi2ip")) {
				//not dvbhdmi
				try {
					String dbQuery = "SELECT name, enable FROM services WHERE interface_pos = '"+interface_pos+"' ORDER BY id;";
					
					Statement = Connection.createStatement();
					Result = Statement.executeQuery(dbQuery);
					
					while (Result.next()) {
						String name = Result.getString("name");
						boolean enable = Result.getBoolean("enable");
						
						if(enable) {
							services.add(new ServiceStatus(name));
						}
						
					}
					
				}
				catch(Exception e){
					//System.out.println("getInterfaces: " + e.getMessage());
				}
				
				
				NodeList node_list = doc.getElementsByTagName("sink");
				
				for (int i = 0; i < node_list.getLength(); ++i) {
					
					Node node = node_list.item(i);
					Element element = (Element)node;
					
					int bitrate = Integer.parseInt(getValue(element, "bitrate"));
					
					if(services.size() > i) {
						services.get(i).setBitrate(bitrate);
					}
					
				}
				
				status = new StreamerStatus(services);
			
			} else {
				
				//dvbhdmi goes here
				
				NodeList node_list = doc.getElementsByTagName("channel");
				
				for (int i = 0; i < node_list.getLength(); ++i) {
					
					Node node = node_list.item(i);
					Element element = (Element)node;
					
					String name = getValue(element, "userText");
					boolean scrambled = Boolean.valueOf(getValue(element, "scrambledStream"));
					String destination = getValue(element, "destination");
					
					int bitrate = Integer.parseInt(getValue(element, "bitRate"));
					int discontinuity = Integer.parseInt(getValue(element, "discontinuityCounter"));
					
					String source = "";
					int muxLoad = 0;
					int maxMuxLoad = 0;
					
					if(interface_type.equals("dsc") || interface_type.equals("mod")) {
						source = getValue(element, "source");
						muxLoad = Integer.parseInt(getValue(element, "muxLoad"));
						maxMuxLoad = Integer.parseInt(getValue(element, "maxMuxLoad"));
					}
					
					services.add(new ServiceStatus(name, scrambled, destination, bitrate, discontinuity, source, muxLoad, maxMuxLoad));
				}
				
				if(interface_type.equals("dsc") || interface_type.equals("mod")) {
					
					String temp = "dscStreamerStatus";
					
					if(interface_type.equals("mod")) {
						temp = "eqamStreamerStatus";
					}
					
					NodeList dsc_list = doc.getElementsByTagName(temp);
					
					Node node = dsc_list.item(0);
					Element element = (Element)node;

					int muxLoad = Integer.parseInt(getValue(element, "muxLoad"));
					int maxMuxLoad = Integer.parseInt(getValue(element, "maxMuxLoad"));
					int caServices = 0;
					int caPids = 0;
					
					if(interface_type.equals("dsc")) {
						caServices = Integer.parseInt(getValue(element, "caServices"));
						caPids = Integer.parseInt(getValue(element, "caPids"));
					}
					
					
					status = new StreamerStatus(muxLoad, maxMuxLoad, caServices, caPids, services);
					
				} else {
					status = new StreamerStatus(services);
				}
				
			}
			
		}
		
		return status;
	}
	
	public TunerStatus interfaceTunerStatus(String interface_pos, String interface_type) {
		
		Document doc = null;
		
		try {
			String xmlstr = StreamerManager.command("interface/"+interface_pos+"/tunerStatus get");
			//System.out.println("xmlstr: " + xmlstr);
			
			if(!xmlstr.equals("")) {
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource inputSource = new InputSource(new StringReader(xmlstr));
				doc = db.parse(inputSource);
			}
			
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
		}
		
		if(doc != null) {
			if(interface_type.equals("dvbs") || interface_type.equals("dvbt") || interface_type.equals("dvbc") || interface_type.equals("dvbudp")) {
			
				NodeList node_list = doc.getElementsByTagName(interface_type + "TunerStatus");
				
				Node node = node_list.item(0);
				Element element = (Element)node;
				
				boolean locked = Boolean.valueOf(getValue(element, "locked"));
				
				if(!interface_type.equals("dvbudp")) {
					int frequency = Integer.parseInt(getValue(element, "frequency"));
					int signalstrength = Integer.parseInt(getValue(element, "signalStrength"));
					int snr = Integer.parseInt(getValue(element, "snr"));
					
					return new TunerStatus(locked, frequency, signalstrength, snr);
				} else {
					return new TunerStatus(locked);
				}
				
				
			}
			
			if(interface_type.equals("dsc")) {
				
				NodeList node_list = doc.getElementsByTagName("dscTunerStatus");
				
				Node node = node_list.item(0);
				Element element = (Element)node;

				int ci_status = Integer.parseInt(getValue(element, "ciStatus"));
				boolean ca_emm = Boolean.valueOf(getValue(element, "caEmm"));
				String ca_text = getValue(element, "caText");


				
				NodeList ca_list = doc.getElementsByTagName("caMmiMenu");
				
				Node ca_node = ca_list.item(0);
				Element ca_element = (Element)ca_node;
				
				String ca_title = getValue(ca_element, "title");
				String ca_sub_title = getValue(ca_element, "subTitle");
				
				String menu_title = ca_title + " - " + ca_sub_title;
				
				
				
				HashMap<Integer, String> menu_items = new HashMap<Integer, String>();
				menu_items.put(0, "Cancel");
				
				
				NodeList menu_item_list = ca_element.getElementsByTagName("item");
				
				for (int i = 0; i < menu_item_list.getLength(); ++i) {
					Node item_node = menu_item_list.item(i);
					Element item_element = (Element)item_node;
					
					if(menu_item_list.getLength() > 0 && item_element.getChildNodes().getLength() > 0) {
						menu_items.put(i+1, ((Node)item_element.getChildNodes().item(0)).getNodeValue().toString());
		            }
					
				}
				
				
				
				
				String ca_osd = "";
				
				NodeList osd_list = doc.getElementsByTagName("caMmiOsd");
				
				Node osd_node = osd_list.item(0);
				Element osd_element = (Element)osd_node;
				
				NodeList osd_list_2 = osd_element.getElementsByTagName("item");
				
				for (int i = 0; i < osd_list_2.getLength(); ++i) {
					
					Node item_node = osd_list_2.item(i);
					Element item_element = (Element)item_node;
					
					if(osd_list_2.getLength() > 0 && item_element.getChildNodes().getLength() > 0) {
						ca_osd += ((Node)item_element.getChildNodes().item(0)).getNodeValue().toString();
						
						if(!(i < osd_list.getLength()-1)) {
							ca_osd += "<br>";
						}
					}
					
				}
				
				
				return new TunerStatus(ci_status, ca_emm, ca_text, ca_osd, menu_title, menu_items);
			}
		}
		return null;
	}
	/*
	public String getInterfacePosition(String sessionKey, int interface_id) {
		
		String pos = "N/A";
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE id = "+interface_id+";";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				pos = Result.getString("pos");
			}
			
		}
		catch(Exception e){
			System.out.println("getInterfaces: " + e.getMessage());
		}
		
		return pos;
	}
	*/
	
	public Date getInterfaceScanTime(String sessionKey, String interface_pos) {
		
		Date date = null;
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				date = Result.getTimestamp("scantime");
			}
			
		}
		catch(Exception e){
			//System.out.println("getInterfaceScanTime: " + e.getMessage());
		}
		
		return date;
	}
	
	public Config getConfig(String sessionKey, String interface_pos, String interface_type) {
		
		if(interface_type.equals("dvbudp")) {
			return getConfigDvbudp(interface_pos);
		} else if(interface_type.equals("dvbs")) {
			return getConfigDvbs(interface_pos);
		} else if(interface_type.equals("dvbt")) {
			return getConfigDvbt(interface_pos);
		} else if(interface_type.equals("dvbc")) {
			return getConfigDvbc(interface_pos);
		} else if(interface_type.equals("dsc")) {
			return getConfigDsc(interface_pos);
		} else if(interface_type.equals("infostreamer") || interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip") || interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
			return getConfigOthers(interface_pos, interface_type);
		} else {
			return null;
		}
	}
	
	private Config getConfigOthers(String interface_pos, String interface_type) {
		
		Config config = null;
		
		try {
			
			String dbQuery = "SELECT * FROM config_istr WHERE interface_pos = '"+interface_pos+"';";
			
			if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
				dbQuery = "SELECT * FROM config_hdmi WHERE interface_pos = '"+interface_pos+"';";
			} 
			
			if(interface_type.equals("hls2ip")) {
				dbQuery = "SELECT * FROM config_hls WHERE interface_pos = '"+interface_pos+"';";
			}
			
			if(interface_type.equals("webradio")) {
				dbQuery = "SELECT * FROM config_webradio WHERE interface_pos = '"+interface_pos+"';";
			}
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			int id = Result.getInt("id");
			
			config = new Config(id, interface_pos);
			
			if(interface_type.equals("infostreamer")) {
				config.setPresUrl(Result.getString("presentation_url"));
			} else if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
				config.setHdmiFormat(Result.getString("format"));
			} else if(interface_type.equals("hls2ip")) {
				config.setMaxBitrate(Result.getInt("max_bitrate"));
			} else if(interface_type.equals("webradio")) {
				config.setGain(Result.getInt("gain"));
				config.setWebradioUrl(Result.getString("webradio_url"));
			}
			
		
		}
		catch(Exception e){
			//System.out.println("getConfigDsc 1: " + e.getMessage());
		}
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String name = Result.getString("name");
			boolean active = Result.getBoolean("active");
			
			if(config == null) {
				config = new Config(0, interface_pos);
			}
			
			config.setInterfaceName(name);
			config.setInterfaceActive(active);
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 2: " + e.getMessage());
		}
		
		return config;
	}
	
	private Config getConfigDsc(String interface_pos) {
		
		Config config = null;
		int emm = 1;
		
		try {
			String dbQuery = "SELECT emm FROM config_dsc WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			emm = Result.getInt("emm");
			
			config = new Config(0, interface_pos);
			config.setEmm(emm);
		
		}
		catch(Exception e){
			//System.out.println("getConfigDsc 1: " + e.getMessage());
		} 
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String name = Result.getString("name");
			
			if(config == null) {
				config = new Config(0, interface_pos);
				config.setEmm(1);
			}
			
			config.setInterfaceName(name);
			
		}
		catch(Exception e){
			//System.out.println("getConfigDsc 2: " + e.getMessage());
		}
		
		return config;
		
	}
	
	private Config getConfigDvbudp(String interface_pos) {
		
		Config config = null; 
		
		try {
			String dbQuery = "SELECT * FROM config_dvbudp WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			int id = Result.getInt("id");
			String in_ip = Result.getString("in_ip");
			int in_port = Result.getInt("in_port");
			
			
			config = new Config(id, interface_pos, in_ip, in_port);
		
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 1: " + e.getMessage());
		} 
		
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String name = Result.getString("name");
			boolean active = Result.getBoolean("active");
			
			if(config == null) {
				config = new Config(0, interface_pos, 0, 0);
			}
			
			config.setInterfaceName(name);
			config.setInterfaceActive(active);
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 2: " + e.getMessage());
		}
		
		config.setEmm(getConfigEmm(interface_pos, false));
		
		return config;
		
	}
	
	private Config getConfigDvbt(String interface_pos) {
		
		Config config = null; 
		
		try {
			String dbQuery = "SELECT * FROM config_ter WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			int id = Result.getInt("id");
			int freq = Result.getInt("freq");
			int bw = Result.getInt("bw");
			String del = Result.getString("del");
			
			config = new Config(id, interface_pos, freq, del, bw);
		
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 1: " + e.getMessage());
		} 
		
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String name = Result.getString("name");
			boolean active = Result.getBoolean("active");
			
			if(config == null) {
				config = new Config(0, interface_pos, 0, 0);
			}
			
			config.setInterfaceName(name);
			config.setInterfaceActive(active);
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 2: " + e.getMessage());
		}
		
		config.setEmm(getConfigEmm(interface_pos, false));
		
		return config;
		
	}
	
	private Config getConfigDvbc(String interface_pos) {
		
		Config config = null; 
		
		try {
			String dbQuery = "SELECT * FROM config_dvbc WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			int id = Result.getInt("id");
			int freq = Result.getInt("freq");
			int symb = Result.getInt("symb");
			String del = Result.getString("del");
			String constellation = Result.getString("constellation");
			
			config = new Config(id, interface_pos, freq, symb, del, constellation);
		
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 1: " + e.getMessage());
		} 
		
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String name = Result.getString("name");
			boolean active = Result.getBoolean("active");
			
			if(config == null) {
				config = new Config(0, interface_pos, 0, 0);
			}
			
			config.setInterfaceName(name);
			config.setInterfaceActive(active);
			
		}
		catch(Exception e){
			//System.out.println("getConfigDvbt 2: " + e.getMessage());
		}
		
		config.setEmm(getConfigEmm(interface_pos, false));
		
		return config;
		
	}
	
	private Config getConfigDvbs(String interface_pos) {
		
		Config config = null; 
		
		try {
			String dbQuery = "SELECT * FROM config_sat WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			int id = Result.getInt("id");
			int freq = Result.getInt("freq");
			String pol = Result.getString("pol");
			int symb = Result.getInt("symb");
			String del = Result.getString("del");
			int satno = Result.getInt("satno");
			String lnb_type = Result.getString("lnb_type");
			
			config = new Config(id, interface_pos, freq, pol, symb, del, satno, lnb_type);
		
			
		}
		catch(Exception e){
			//System.out.println("getConfig: " + e.getMessage());
		}
		
		try {
			String dbQuery = "SELECT * FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			String name = Result.getString("name");
			boolean active = Result.getBoolean("active");
			
			if(config == null) {
				config = new Config(0, interface_pos, 0, "H", 0, "DVBS2", 0, "Universal");
			}
			
			config.setInterfaceName(name);
			config.setInterfaceActive(active);
			
		}
		catch(Exception e){
			//System.out.println("getConfig: " + e.getMessage());
		}
		
		config.setEmm(getConfigEmm(interface_pos, false));
		
		return config;
	}
	
	private int getConfigEmm(String interface_pos, boolean isDsc) {
		
		//System.out.println("getConfigEmm, isDsc: " + isDsc);
		
		int emm = 1;
		
		if(isDsc) {
			
			try {
				String dbQuery = "SELECT emm FROM config_dsc WHERE interface_pos = '"+interface_pos+"';";
				
				Statement = Connection.createStatement();
				Result = Statement.executeQuery(dbQuery);
				
				Result.next();

				emm = Result.getInt("emm");
			
			}
			catch(Exception e){
				//System.out.println("getConfigEmm: " + e.getMessage());
			}
			
			return emm;
			
		}
		
		emm = 0;
		
		try {
			String dbQuery = "SELECT emm FROM config_emm WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();

			emm = Result.getInt("emm");
		
		}
		catch(Exception e){
			//System.out.println("getConfigEmm: " + e.getMessage());
		} 
		
		return emm;
	}
	
	public Response setConfig(String sessionKey, Config config, String interface_type) {
		
		if(interface_type.equals("dvbudp")) {
			
			if(Config.Comparators.UDP.compare(config, getConfigDvbudp(config.getInterfacePos())) == 0) {
				//System.out.println("UDP Config = db - no need to save.");
				return new Response(true, null);
			} else {
				return setConfigDvbudp(config);
			}
			
		} else if(interface_type.equals("dvbs")) {
			
			if(Config.Comparators.SAT.compare(config, getConfigDvbs(config.getInterfacePos())) == 0) {
				//System.out.println("SAT Config = db - no need to save.");
				return new Response(true, null);
			} else {
				return setConfigDvbs(config);
			}
						
		} else if(interface_type.equals("dvbt")) {
			
			if(Config.Comparators.TER.compare(config, getConfigDvbt(config.getInterfacePos())) == 0) {
				//System.out.println("TER Config = db - no need to save.");
				return new Response(true, null);
			} else {
				return setConfigDvbt(config);
			}
						
		} else if(interface_type.equals("dvbc")) {
			
			if(Config.Comparators.CABLE.compare(config, getConfigDvbc(config.getInterfacePos())) == 0) {
				//System.out.println("CABLE Config = db - no need to save.");
				return new Response(true, null);
			} else {
				return setConfigDvbc(config);
			}
			
		} else if(interface_type.equals("dsc")) {
			return setConfigDsc(config);
		} else if(interface_type.equals("infostreamer") || interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip") || interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
			return setConfigOthers(config, interface_type);
		} else {
			return new Response(false, "Server Error"); 
		}
		
	}
	
	private Response setConfigOthers(Config config, String interface_type) {
		
		Config config_db = getConfigOthers(config.getInterfacePos(), interface_type);
		
		String dbDeleteQuery = "DELETE FROM config_istr WHERE interface_pos = '"+config.getInterfacePos()+"';";
		String dbInsertQuery = "INSERT INTO config_istr (interface_pos, presentation_url) VALUES ('"+config.getInterfacePos()+"', '"+config.getPresUrl()+"');";
		
		if(interface_type.equals("dvbhdmi") || interface_type.equals("hdmi2ip")) {
			dbDeleteQuery = "DELETE FROM config_hdmi WHERE interface_pos = '"+config.getInterfacePos()+"';";
			dbInsertQuery = "INSERT INTO config_hdmi (interface_pos, format) VALUES ('"+config.getInterfacePos()+"', '"+config.getHdmiFormat()+"');";
		}
		
		if(interface_type.equals("hls2ip")) {
			
			if(Config.Comparators.HLS2IP.compare(config, config_db) == 0) {
				//System.out.println("HLS Config = db - no need to save.");
				return new Response(true, null);
			}
			
			dbDeleteQuery = "DELETE FROM config_hls WHERE interface_pos = '"+config.getInterfacePos()+"';";
			dbInsertQuery = "INSERT INTO config_hls (interface_pos, max_bitrate) VALUES ('"+config.getInterfacePos()+"', "+config.getMaxBitrate()+");";
		}
		
		if(interface_type.equals("webradio")) {
			dbDeleteQuery = "DELETE FROM config_webradio WHERE interface_pos = '"+config.getInterfacePos()+"';";
			
			String temp = "NULL";
			
			if(config.getWebradioUrl() != null) {
				temp = "'"+config.getWebradioUrl()+"'";
			}
						
			dbInsertQuery = "INSERT INTO config_webradio (interface_pos, gain, webradio_url) VALUES ('"+config.getInterfacePos()+"', "+config.getGain()+", "+temp+")";
			
			System.out.println(dbInsertQuery);
		}
		
		
		try {
			//delete old config for this pos
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbDeleteQuery);
		}
		catch(Exception e){
			System.out.println("setConfigInfoStreamer delete: " + e.getMessage());
		}
		
		//System.out.println(dbInsertQuery);
		
		try {
			//save new config
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbInsertQuery);
		}
		catch(Exception e){
			System.out.println("setConfigInfoStreamer save: " + e.getMessage());
		}
		
		
		//save interface name
		updateInterface(config, true);

		setConfigChanged(true);
		
		if(interface_type.equals("webradio") && config.getWebradioUrl() != null) {
			
			int sid = 2000 + simpleHash(config.getInterfacePos())%100;
			
			ArrayList<String> all_langs = new ArrayList<String>();
			all_langs.add("All");
			
			ArrayList<Service> services = new ArrayList<Service>();
			services.add(new Service(0, config.getInterfacePos(), config.getInterfaceName(), sid, "RADIO", "All", true, all_langs, "", false, false, "", "", config.getWebradioUrl(), 0));
			
			saveServices(services, interface_type, config.getInterfacePos());
			
			
		}
		
		return new Response(true, null); 
	}
	
	private int simpleHash(String data) {
		int hash = 7;
		for (int i = 0; i < data.length(); i++) {
		    hash = hash * 31 + data.charAt(i);
		}
		return hash;
	}
	
	
	private Response setConfigDsc(Config config) {
		
		//System.out.println("emm: " + config.getEmm());
		
		try {
			//delete old config for this pos
			String dbDeleteQuery = "DELETE FROM config_dsc WHERE interface_pos = '"+config.getInterfacePos()+"';";
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbDeleteQuery);
		}
		catch(Exception e){
			//System.out.println("setConfigDsc delete: " + e.getMessage());
		}
		
		if(config.getEmm() != 0) {
			try {
				//save new config
				String dbInsertQuery = "INSERT INTO config_dsc (interface_pos, emm) VALUES ('"+config.getInterfacePos()+"', "+config.getEmm()+");";
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbInsertQuery);
			}
			catch(Exception e){
				System.out.println("setConfigDsc save emm: " + e.getMessage());
			}
		}
		
		//save interface name
		updateInterface(config, false);

		setConfigChanged(true);
		
		return new Response(true, null); 
	}
	
	private Response setConfigDvbudp(Config config) {
		
		int id = -1;
		
		try {
			//String dbQuery = "INSERT INTO config_ter (interface_pos, freq, bw) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", " + config.getBw() + " WHERE NOT EXISTS (SELECT id, interface_pos, freq, bw FROM config_ter WHERE interface_pos = '" + config.getInterfacePos() + "' AND freq = " + config.getFreq() + " AND bw = " + config.getBw() + ") returning id;";
			String dbQuery = "INSERT INTO config_dvbudp (interface_pos, in_ip, in_port) "
					+ "SELECT '" + config.getInterfacePos() + "', '" + config.getInIp() + "', " + config.getInPort() + " "
					+ "WHERE NOT EXISTS (SELECT id, interface_pos FROM config_ter WHERE interface_pos = '" + config.getInterfacePos() + "') "
					+ "returning id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			id = Result.getInt("id");
			
			
		}
		catch(Exception e){
			//System.out.println("setConfigDvbt 1: " + e.getMessage());
		}
		
		if(id == -1) {
			
			try {
				
				String dbQuery = "UPDATE config_dvbudp SET "
						+ "in_ip = '" + config.getInIp() + "', "
						+ "in_port = " + config.getInPort() + " "
						+ "WHERE interface_pos = '" + config.getInterfacePos() + "';";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				//System.out.println("setConfigDvbt 2: " + e.getMessage());
			}
			
		}
		
		updateInterface(config, true);
		setConfigEmm(config);
		
		setConfigChanged(true);
		
		return new Response(true, null);
	}
	
	private Response setConfigDvbt(Config config) {
		
		int id = -1;
		
		try {
			//String dbQuery = "INSERT INTO config_ter (interface_pos, freq, bw) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", " + config.getBw() + " WHERE NOT EXISTS (SELECT id, interface_pos, freq, bw FROM config_ter WHERE interface_pos = '" + config.getInterfacePos() + "' AND freq = " + config.getFreq() + " AND bw = " + config.getBw() + ") returning id;";
			String dbQuery = "INSERT INTO config_ter (interface_pos, freq, bw, del) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", " + config.getBw() + ", '" + config.getDel() + "' WHERE NOT EXISTS (SELECT id, interface_pos FROM config_ter WHERE interface_pos = '" + config.getInterfacePos() + "') returning id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			id = Result.getInt("id");
			
			
		}
		catch(Exception e){
			//System.out.println("setConfigDvbt 1: " + e.getMessage());
		}
		
		if(id == -1) {
			
			try {
				
				String dbQuery = "UPDATE config_ter SET "
						+ "freq = " + config.getFreq() + ", "
						+ "bw = " + config.getBw() + ", "
						+ "del = '" + config.getDel() + "' "
						+ "WHERE interface_pos = '" + config.getInterfacePos() + "';";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				//System.out.println("setConfigDvbt 2: " + e.getMessage());
			}
			
		}
		
		updateInterface(config, true);
		setConfigEmm(config);
		
		setConfigChanged(true);
		
		return new Response(true, null);
	}
	
	private Response setConfigDvbc(Config config) {
		
		int id = -1;
		
		try {
			
			
			//String dbQuery = "INSERT INTO config_ter (interface_pos, freq, bw) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", " + config.getBw() + " WHERE NOT EXISTS (SELECT id, interface_pos, freq, bw FROM config_ter WHERE interface_pos = '" + config.getInterfacePos() + "' AND freq = " + config.getFreq() + " AND bw = " + config.getBw() + ") returning id;";
			String dbQuery = "INSERT INTO config_dvbc (interface_pos, freq, symb, del, constellation) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", " + config.getSymb() + ", '" + config.getDel() + "', '" + config.getConstellation() + "' WHERE NOT EXISTS (SELECT id, interface_pos FROM config_dvbc WHERE interface_pos = '" + config.getInterfacePos() + "') returning id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			id = Result.getInt("id");
			
			
		}
		catch(Exception e){
			//System.out.println("setConfigDvbt 1: " + e.getMessage());
		}
		
		if(id == -1) {
			
			try {
				
				String dbQuery = "UPDATE config_dvbc SET "
						+ "freq = " + config.getFreq() + ", "
						+ "symb = " + config.getSymb() + ", "
						+ "del = '" + config.getDel() + "', "
						+ "constellation = '" + config.getConstellation() + "' " 
						+ "WHERE interface_pos = '" + config.getInterfacePos() + "';";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				//System.out.println("setConfigDvbt 2: " + e.getMessage());
			}
			
		}
		
		updateInterface(config, true);
		setConfigEmm(config);
		
		setConfigChanged(true);
		
		return new Response(true, null);
	}
	
	private Response setConfigDvbs(Config config) {
		
		int id = -1;
		
		/*
		
		String dbQuery = "INSERT INTO config_sat (interface_pos, freq, pol, symb, del, satno, lnb_type) VALUES ("
				+ "'" + config.getInterfacePos() +"', "
				+ config.getFreq() +", "
				+ "'" + config.getPol() +"', "
				+ config.getSymb() +", "
				+ "'" + config.getDel() +"', "
				+ config.getSatno() +", "
				+ "'" + config.getLnbType() +"');";
		
		if(config.getId() != 0) {
			dbQuery = "UPDATE config_sat SET "
					+ "interface_pos = '" + config.getInterfacePos() +"', "
					+ "freq = " + config.getFreq() + ", "
					+ "pol = '" + config.getPol() + "', "
					+ "symb = " + config.getSymb() +", "
					+ "del = '" + config.getDel() + "', "
					+ "satno = " + config.getSatno() + ", "
					+ "lnb_type = '" + config.getLnbType() + "' "
					+ "WHERE id = " + config.getId() + ";";
		}
		
		*/
		
		try {
			//String dbQuery = "INSERT INTO config_sat (interface_pos, freq, pol, symb, del, satno, lnb_type) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", '" + config.getPol() + "', " + config.getSymb() + ", '" + config.getDel() + "', " + config.getSatno() + ", '" + config.getLnbType() + "' WHERE NOT EXISTS (SELECT id, interface_pos, freq, pol, symb, del, satno, lnb_type FROM config_sat WHERE interface_pos = '" + config.getInterfacePos() + "' AND freq = " + config.getFreq() + " AND pol = '" + config.getPol() + "' AND symb = " + config.getSymb() + " AND del = '" + config.getDel() + "' AND satno = " + config.getSatno() + " AND lnb_type = '" + config.getLnbType() + "') returning id;";
			String dbQuery = "INSERT INTO config_sat (interface_pos, freq, pol, symb, del, satno, lnb_type) SELECT '" + config.getInterfacePos() + "', " + config.getFreq() + ", '" + config.getPol() + "', " + config.getSymb() + ", '" + config.getDel() + "', " + config.getSatno() + ", '" + config.getLnbType() + "' WHERE NOT EXISTS (SELECT id, interface_pos FROM config_sat WHERE interface_pos = '" + config.getInterfacePos() + "') returning id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			id = Result.getInt("id");
			
			
		}
		catch(Exception e){
			//System.out.println("setConfigDvbs 1: " + e.getMessage());
		}
		
		if(id == -1) {
			
			try {
				
				String dbQuery = "UPDATE config_sat SET "
						+ "freq = " + config.getFreq() + ", "
						+ "pol = '" + config.getPol() + "', "
						+ "symb = " + config.getSymb() +", "
						+ "del = '" + config.getDel() + "', "
						+ "satno = " + config.getSatno() + ", "
						+ "lnb_type = '" + config.getLnbType() + "' "
						+ "WHERE interface_pos = '" + config.getInterfacePos() + "';";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				//System.out.println("setConfigDvbs 2: " + e.getMessage());
			}
			
		}

		updateInterface(config, true);
		setConfigEmm(config);
		
		setConfigChanged(true);
		
		return new Response(true, null);
	}
	
	private void setConfigEmm(Config config) {
		
		//System.out.println("setConfigEmm EMM:" + config.getEmm());
		
		int emm = getConfigEmm(config.getInterfacePos(), false);
		
		if(emm == config.getEmm()) {
			//System.out.println("current emm == config emm");
			return;
		}
		
		
		ArrayList<Integer> free_list = getCurrentEmmList(config.getInterfacePos(), false).getFree();
		ArrayList<Integer> new_list = new ArrayList<Integer>(); 
		
		for (int i = 1; i <= 5; ++i) {
			
			if(free_list.contains(i)) {
				
			} else {
				new_list.add(i);
			}
			
		}
		
		
		
		
		
		
		if(new_list.contains(config.getEmm())) {
			//System.out.println("emm 'excists alreday'");
			return;
		}
		
		
		if(emm > 0) {
			
			//System.out.println("current emm set to NULL");
			
			try {
				
				String dbQuery = "UPDATE config_emm SET "
						+ "interface_pos = NULL "
						+ "WHERE emm = "+emm+";";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				System.out.println("setConfigEmm 1: " + e.getMessage());
			}
			
		}
		
		if(config.getEmm() > 0) {

			//System.out.println("config emm set to interfacePos");
			
			try {
				
				String dbQuery = "UPDATE config_emm SET "
						+ "interface_pos = '"+config.getInterfacePos()+"' "
						+ "WHERE emm = "+config.getEmm()+";";
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			
			}
			catch(Exception e){
				System.out.println("setConfigEmm 2: " + e.getMessage());
			}
			
		}
		
		
		//System.out.println(printInterfaceEmmList());
		
	}
	
	private void updateInterface(Config config, boolean setActive) {
		
		try {
			
			String dbQuery = "UPDATE interfaces SET "
					+ "name = '"+config.getInterfaceName()+"', "
					+ "active = "+config.getInterfaceActive()+" "
					+ "WHERE pos = '"+config.getInterfacePos()+"';";
			
			if(!setActive) {
				dbQuery = "UPDATE interfaces SET "
						+ "name = '"+config.getInterfaceName()+"' "
						+ "WHERE pos = '"+config.getInterfacePos()+"';";
			}
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		
		}
		catch(Exception e){
			//System.out.println("updateInterface: " + e.getMessage());
		}
		
	}
	
	public ArrayList<Service> getServices(String sessionKey, String interface_pos) {
		
		ArrayList<Service> services = new ArrayList<Service>();
		
		try {
			String dbQuery = "SELECT * FROM services WHERE interface_pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				int id = Result.getInt("id");
				String name = Result.getString("name");
				int sid = Result.getInt("sid");
				String type = Result.getString("type");
				String lang = Result.getString("lang");
				boolean enabled = Result.getBoolean("enable");
				
				String radio_url = Result.getString("istr_url");
				boolean show_pres = Result.getBoolean("istr_video");
				
				Array arr = Result.getArray("all_langs");
				ArrayList<String> all_langs;
				if(arr != null) {
					String[] arr_all_langs = (String[])arr.getArray();
				    all_langs = new ArrayList<String>(Arrays.asList(arr_all_langs));
				} else {
					all_langs = new ArrayList<String>();
					all_langs.add("All");
				}
			    
				boolean scrambled = Result.getBoolean("scrambled");
				
				String epg_id = Result.getString("epg_id");
				
				String hls_url = Result.getString("hls_url");
				String webradio_url = Result.getString("webradio_url");
				
			    services.add(new Service(id, interface_pos, name, sid, type, lang, enabled, all_langs, radio_url, show_pres, scrambled, epg_id, hls_url, webradio_url, 0));
			}
			
		}
		catch(Exception e){
			//System.out.println("getServices: " + e.getMessage());
		}
		
		//System.out.println("size services: " + services.size());
		
		return services;
	}
	
	public Response saveHlsWizardServices(String sessionKey, ArrayList<Service> services) {
		
		//System.out.println("saveHlsWizardServices");
		
		HashMap<String, Integer> routes = getRoutes();
		
		
		
		
		
		ArrayList<Interface> hls_interfaces = getInterfacesHls(sessionKey);
		Collections.sort(hls_interfaces, Interface.Comparators.POS);
		
		//set all interfaces configs + services enable false + routes empty
		for (int i = 0; i < hls_interfaces.size(); ++i) {
			
			tempRoutes(hls_interfaces.get(i).getPosition());
			
			Config c_inactive = new Config(0, hls_interfaces.get(i).getPosition());
			c_inactive.setInterfaceName("name?");
			c_inactive.setInterfaceActive(false);
			updateInterface(c_inactive, true);
			
			
			setAllServiceEnabledFalse(hls_interfaces.get(i).getPosition());
			
			
			/*
			for (int j = 0; j < services.size(); ++j) {
				String key = hls_interfaces.get(i).getPosition() + ", " + services.get(j).getSid();
				
				System.out.println("setting false key: " + key);
				
				
				setServiceEnable(key, false);
			}
			*/
			
		}
		
		String ip = getStartIp();
		
		for (int i = 0; i < hls_interfaces.size(); ++i) {
		
			inner:
			for (int j = 0; j < services.size(); ++j) {
				
				if(services.get(j).isEnabled()) {
					
					String name = services.get(j).getName().replace("'", "");
					String key = hls_interfaces.get(i).getPosition() + ", " + services.get(j).getSid();
					
					insertService(services.get(j), hls_interfaces.get(i).getPosition(), name);
					updateServiceHlsUrl(services.get(j), hls_interfaces.get(i).getPosition());
					
					if(routes.containsKey(name)) {
						updateRoute(routes.get(name), key);
					} else {
						insertRoute(services.get(j), hls_interfaces.get(i).getPosition(), ip);
					}
					
					
					
					
					//if service did already exist then enable it
					setServiceEnable(key, true);
					
					Config c_active = new Config(0, hls_interfaces.get(i).getPosition());
					c_active.setInterfaceName(name);
					c_active.setInterfaceActive(true);
					updateInterface(c_active, true);
					
					//remove this service so the next enabled service will be used in the next iteration
					services.remove(j);
					
					break inner;
				}
				
			}
		
			deleteDisabledServices(hls_interfaces.get(i).getPosition());			
		}
		
		deleteRoutes("temp");
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	private void updateRoute(int id, String key) {
		
		//System.out.println("updateRoute id " + id + " key " + key);
		
		try {
			
			String dbQuery = "UPDATE routes SET service_key = '"+key+"' WHERE id = " +id;
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("updateRoute: " + e.getMessage());
		}
		
	}
	
	private void tempRoutes(String interface_pos) {
		
		//System.out.println("tempRoutes " + interface_pos);
		
		try {
			
			String dbQuery = "UPDATE routes SET service_key = 'temp' WHERE service_key LIKE '"+interface_pos+",%'";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("tempRoutes: " + e.getMessage());
		}
		
	}
	
	private void setAllServiceEnabledFalse(String interface_pos) {
		
		try {
			
			String dbQuery = "UPDATE services SET enable = false WHERE key LIKE '"+interface_pos+",%'";
			
			//System.out.println("dbQuery: " + interface_pos);
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("setAllServiceEnabledFalse: " + e.getMessage());
		}
		
	}
	
	private HashMap<String, Integer> getRoutes() {
		
		//System.out.println("getRoutes");
		
		
		HashMap<String, Integer> routes = new HashMap<String, Integer>();
		
		try {
			String dbQuery = "SELECT routes.id, services.name FROM routes "
					+ "INNER JOIN services ON routes.service_key = services.key "
					+ "ORDER BY routes.id";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				int id = Result.getInt("id");
				String service_name = Result.getString("name");
				
				routes.put(service_name, id);
			}
		}
		catch(Exception e){
			System.out.println("getRoutes: " + e.getMessage());
		}
		
		return routes;
		
	}
	
	private void setServiceEnable(String key, boolean enable) {
		
		//System.out.println("setServiceEnable " + key + " " + enable);
		
		try {
			
			String dbQuery = "UPDATE services SET enable = "+enable+" WHERE key = '"+key+"'";
		
			System.out.println("setServiceEnable dbQuery: " + dbQuery);
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
	
		}
		catch(Exception e){
			System.out.println("setServiceEnabled: " + e.getMessage());
		}
		
	}
	
	private void insertService(Service service, String interface_pos, String name) {
		
		String key = interface_pos + ", " + service.getSid();
		
		//System.out.println("insertService " + key);
		
		String dbInsertQuery = "INSERT INTO services (interface_pos, name, sid, type, enable, key, scrambled, hls_url) "
				+ "SELECT '" + interface_pos + "', "
				+ "'" + name + "', "
				+ service.getSid() + ", "
				+ "'TV_HD', "
				+ service.isEnabled() + ", "
				+ "'" + key + "', "
				+ service.isScrambled() + ", "
				+ "'" + service.getHlsUrl() + "' "
				+ "WHERE NOT EXISTS (SELECT 1 FROM services WHERE key = '"+key+"')";
		
		//System.out.println("dbInsertQuery " + dbInsertQuery);
		
		try {
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbInsertQuery);
		}
		catch(Exception e){
			System.out.println("saveHlsWizardServices insert: " + e.getMessage());
		}
		
	}
	
	private void updateServiceHlsUrl(Service service, String interface_pos) {
		
		String key = interface_pos + ", " + service.getSid();
		
		String dbQuery = "UPDATE services SET hls_url = '"+service.getHlsUrl()+"' WHERE key = '"+ key +"';";
		
        try {
        
		Statement = Connection.createStatement();
		Statement.executeUpdate(dbQuery);
     
        }
		catch(Exception e){
			System.out.println("updateService: " + e.getMessage());
		}
		
	}
	
	private void insertRoute(Service service, String interface_pos, String ip_startaddr) {
		
		String key = interface_pos + ", " + service.getSid();
		
		//System.out.println("insertRoute " + key);
		
		int sid = service.getSid();
		int num = (sid+255)%255;
		
		String address_port[] = ip_startaddr.split(":"); 
		String adress[] = address_port[0].split("\\.");
		
		String out_ip = adress[0] + "." + adress[1] + "." + adress[2] + "." + num + ":" + address_port[1];
		
		String dbRoutesQuery = "INSERT INTO routes(service_key, lcn, dsc_pos, mod_pos, out_sid, out_ip, output_name, epg_id) "
				+ "SELECT '"+key+"', "+service.getPreferedLcn()+", 'None', 'None', "+sid+", '"+out_ip+"', '"+service.getName()+"', '"+service.getEpgUrl()+"' "
				+ "WHERE NOT EXISTS (SELECT 1 FROM routes WHERE service_key = '"+key+"');";

		//System.out.println("dbRoutesQuery " + dbRoutesQuery);
		
		try {
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbRoutesQuery);
		}
		catch(Exception e){
			System.out.println("insertRoute: " + e.getMessage());
		}
		
		
	}
	
	private void deleteServices(String interface_pos) {
		
		try {
			// Delete old services for this interface
			String dbDeleteQuery = "DELETE FROM services WHERE interface_pos = '"+interface_pos+"';";
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbDeleteQuery);
		}
		catch(Exception e){
			System.out.println("deleteServices: " + e.getMessage());
		}
		
	}
	
	private void deleteDisabledServices(String interface_pos) {
		
		try {
			// Delete old services for this interface
			String dbDeleteQuery = "DELETE FROM services WHERE interface_pos = '"+interface_pos+"' AND enable = false";
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbDeleteQuery);
		}
		catch(Exception e){
			System.out.println("deleteDisabledServices: " + e.getMessage());
		}
		
	}
	
	private void deleteRoutes(String key) {
		
		//System.out.println("deleteRoutes " + key);
		
		try {
			// Delete temp routes 

			String dbDeleteQuery = "DELETE FROM routes WHERE service_key = '"+key+"';"; 
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbDeleteQuery);
		}
		catch(Exception e){
			System.out.println("deleteRoutes: " + e.getMessage());
		}
		
	}
	
	
	public Response saveServices(ArrayList<Service> services, String interface_type, String interface_pos) {
		
		deleteServices(services.get(0).getInterfacePos());		
		
		//check for duplicate name (no need anymore)
		
		//System.out.println("size: " + services.size());
		
		
		// Store the new services
		for (int i = 0; i < services.size(); ++i) {
			
			ArrayList<String> temp_array = new ArrayList<String>();
			
			for (int j = 0; j < services.get(i).getAllLangs().size(); ++j) {
				temp_array.add("'" + services.get(i).getAllLangs().get(j) + "'");
			}
			
			String arr_str = temp_array.toString();
			
			
			String temp_type = services.get(i).getType();
			
			if(interface_type.equals("infostreamer")) {
				if(services.get(i).isShowPres() == true) {
					temp_type = "TV_SD";
				} else {
					temp_type = "RADIO";
				}
			}
			
			if(interface_type.equals("infoch")) {
				services.get(i).setRadioUrl(services.get(i).getWebradioUrl());
				services.get(i).setWebradioUrl("");
			}
			
			
			String name = services.get(i).getName().replace("'", "");
			
			String dbInsertQuery = "INSERT INTO services (interface_pos, name, sid, type, lang, all_langs, enable, istr_url, istr_video, key, scrambled, hls_url, webradio_url, epg_id) "
					+ "VALUES ('" + services.get(i).getInterfacePos() + "', "
					+ "'" + name + "', "
					+ services.get(i).getSid() + ", "
					+ "'" + temp_type + "', "
					+ "'" + services.get(i).getLang() + "', "
					+ "ARRAY" + arr_str + ", "
					+ services.get(i).isEnabled() + ", "
					+ "'" + services.get(i).getRadioUrl() + "', "
					+ services.get(i).isShowPres() + ", "
					+ "'" + services.get(i).getKey() + "', "
					+ services.get(i).isScrambled() + ", "
					+ "'" + services.get(i).getHlsUrl() + "', "
					+ "'" + services.get(i).getWebradioUrl() + "', "
					+ "'" +services.get(i).getEpgUrl() + "');";
			
			//System.out.println("dbQuery: " + dbInsertQuery);
			try {
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbInsertQuery);
			}
			catch(Exception e){
				System.out.println("saveServices insert: " + e.getMessage());
			}
		}
		
		
		
		String ip_startaddr = getStartIp();
		
		String address_port[] = ip_startaddr.split(":"); 
		String adress[] = address_port[0].split("\\.");
		
		String interface_name = null;
		
		try {
			for (int i = 0; i < services.size(); ++i) {
				if(services.get(i).isEnabled()) {
					
					interface_name = services.get(i).getName();
					
					int sid = services.get(i).getSid();
					int num = (sid+255)%255;
					
					String out_ip = adress[0] + "." + adress[1] + "." + adress[2] + "." + num + ":" + address_port[1];
					
					/*
					String output_name_cleaned = "";
					
					String regex = " ("+services.get(i).getInterfacePos()+", "+sid+")";
					
					int index = services.get(i).getName().indexOf(regex);
					
					if(index != -1) {
						output_name_cleaned = services.get(i).getName().substring(0, index);
					}
					*/
					
					String dbInsertQuery = "INSERT INTO routes(service_key, lcn, dsc_pos, mod_pos, out_sid, out_ip, output_name, epg_id) SELECT '"+services.get(i).getKey()+"', 0, 'None', 'None', "+sid+", '"+out_ip+"', '"+services.get(i).getName()+"', '"+services.get(i).getEpgUrl()+"' WHERE NOT EXISTS (SELECT 1 FROM routes WHERE service_key = '"+services.get(i).getKey()+"');";
					
					Statement = Connection.createStatement();
					Statement.executeUpdate(dbInsertQuery);
				}
			}
		}
		catch(Exception e){
			System.out.println("saveServices routes: " + e.getMessage());
		}
		
		if(interface_type.equals("hls2ip") || interface_type.equals("webradio") || interface_type.equals("infoch")) {
			if(interface_name != null) {
				Config c = new Config(0, interface_pos);
				c.setInterfaceName(interface_name);
				updateInterface(c, false);
			}
		}
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	private String getStartIp() {
		
		String ip_startaddr = "239.1.1.1:10000";
		//get start address
		try {
			
				
			String dbQuery = "SELECT value FROM nv WHERE name = 'ip_startaddr';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			ip_startaddr = Result.getString("value");
			
		}
		catch(Exception e){
			System.out.println("saveServices address: " + e.getMessage());
		}
		
		return ip_startaddr;
	}
	
	public ArrayList<Route> getRoutes(String sessionKey) {
		
		ArrayList<Route> routes = new ArrayList<Route>();
		
		try {
			
			/*
			String dbQuery = "SELECT routes.*, services.name, services.type as service_type, interfaces.pos, interfaces.type as interface_type FROM routes "
					+ "INNER JOIN services ON routes.service_id = services.id "
					+ "INNER JOIN interfaces ON services.interface_id = interfaces.id "
					+ "WHERE services.enable is true;";
			*/
			
			String dbQuery = "SELECT routes.*, services.name as service_name, services.id as service_id, services.type as service_type, services.scrambled, services.interface_pos, interfaces.type, interfaces.multiband FROM routes "
					+ "INNER JOIN services ON routes.service_key = services.key "
					+ "INNER JOIN interfaces ON services.interface_pos = interfaces.pos "
					+ "WHERE services.enable is true AND interfaces.active is true AND interface_pos IN (select pos from interfaces) ORDER BY lcn;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				int id = Result.getInt("id");
				int service_id = Result.getInt("service_id");
				String service_name = Result.getString("service_name");
				String service_type = Result.getString("service_type");
				
				boolean scrambled = Result.getBoolean("scrambled");
				
				String interfaces_pos = Result.getString("interface_pos");
				String interface_type =  Result.getString("type");
				int lcn = Result.getInt("lcn");
				String descrambler_pos = Result.getString("dsc_pos");
				String modulator_pos = Result.getString("mod_pos");
				String modulator_pos_net2 = Result.getString("mod_pos_net2");
				int out_sid = Result.getInt("out_sid");
				String out_ip = Result.getString("out_ip");
				String output_name = Result.getString("output_name");
				String epg_id = Result.getString("epg_id");
				
				boolean hls_enable = Result.getBoolean("hls_enable");
				boolean interfaces_multiband = Result.getBoolean("multiband");
				//System.out.println("out_ip " + out_ip);
				
				routes.add(new Route(id, service_id, service_name, service_type, interfaces_pos, interface_type, interfaces_multiband, lcn, descrambler_pos, modulator_pos, modulator_pos_net2, out_sid, out_ip, scrambled, output_name, epg_id, hls_enable));
			}
			
		}
		catch(Exception e){
			System.out.println("getRoutes: " + e.getMessage());
		}
		
		//System.out.println("getRoutes: " + routes.size());
		
		return routes;
	}
	
	public ArrayList<Bitrate> getBitrates(String sessionKey) {
		
		ArrayList<Bitrate> bitrates = new ArrayList<Bitrate>();
		
		try {
			String dbQuery = "SELECT name, value FROM nv;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				if(name.startsWith("bitrate_")) {
					int value = Integer.parseInt(Result.getString("value"));
					bitrates.add(new Bitrate(name, value));
				}
			}
			
		}
		catch(Exception e){
			//System.out.println("getBitrates: " + e.getMessage());
		}
		
		return bitrates;
	}
	
	public Response updateRoutes(String sessionKey, ArrayList<Route> routes) {
		
		try {
			for (int i = 0; i < routes.size(); ++i) {
				
				//System.out.println(routes.get(i).getModulatorPosNet2());
				
				String dbQuery = "UPDATE routes SET "
						+ "lcn = "+routes.get(i).getLcn()+", "
						+ "dsc_pos = '"+routes.get(i).getDescramblerPos()+"', "
						+ "mod_pos = '"+routes.get(i).getModulatorPos()+"', "
						+ "mod_pos_net2 = '"+routes.get(i).getModulatorPosNet2()+"', "
						+ "out_sid = "+routes.get(i).getOutSid()+", "
						+ "out_ip = '"+routes.get(i).getOutIp()+"', "
						+ "output_name = '"+routes.get(i).getOutputName()+"', "
						+ "epg_id = '"+routes.get(i).getEpgUrl()+"', "
						+ "hls_enable = "+routes.get(i).isHls()+" "
						+ "WHERE id = "+routes.get(i).getId();
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			}
		}
		catch(Exception e){
			//System.out.println("updateRoutes: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	public ArrayList<NameValue> getSettings(String sessionKey) {
		
		ArrayList<NameValue> values = new ArrayList<NameValue>();
		
		try {
			String dbQuery = "SELECT * FROM nv ORDER BY id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				//System.out.println("name: " + name);
				
				if(!name.equals("config_changed")) {
					int id = Result.getInt("id");
					String value = Result.getString("value");
					values.add(new NameValue(id, name, value));
				}
			}
			
		}
		catch(Exception e){
			//System.out.println("getBitrates: " + e.getMessage());
		}
		
		return values;
	}
	
	public HashMap<String, String> getSettings(String sessionKey, boolean temp) {
		
		HashMap<String, String> settings = new HashMap<String, String>();
		
		try {
			String dbQuery = "SELECT * FROM nv ORDER BY id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				if(!name.equals("config_changed")) {
					String value = Result.getString("value");
					settings.put(name, value);
				}
			}
			
		}
		catch(Exception e){
			//System.out.println("getSettings: " + e.getMessage());
		}
				
		return settings;
	}
	
	public Response updateSettings(String sessionKey, HashMap<String, String> settings) {
		
		for (Map.Entry<String, String> setting : settings.entrySet()) {
            //System.out.println(setting.getKey() + " = " + setting.getValue());
            
            String dbQuery = "UPDATE nv SET value = '"+setting.getValue()+"' WHERE name = '"+ setting.getKey()+"';";
			
            try {
            
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
         
            }
    		catch(Exception e){
    			System.out.println("updateSettings: " + e.getMessage());
    			//return new Response(false, "Server Error");
    		}
        }
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	public Response updateSettings(String sessionKey, ArrayList<NameValue> values) {
		
		try {
			for (int i = 0; i < values.size(); ++i) {
				String dbQuery = "UPDATE nv SET value = '"+values.get(i).getValue()+"' WHERE id = "+values.get(i).getId();
				
				Statement = Connection.createStatement();
				Statement.executeUpdate(dbQuery);
			}
		}
		catch(Exception e){
			//System.out.println("updateSettings: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	public Response updateSettingsNew(String sessionKey, HashMap<String, NameValue> settings) {
		
		settings.forEach( (key, value) -> {
            
			if(value.getId() == 0) {
				
				String dbInsertQuery = "INSERT INTO nv (name, value) VALUES ('" + value.getName() + "', '" + value.getValue() + "');";
								
				try {
					Statement = Connection.createStatement();
					Result = Statement.executeQuery(dbInsertQuery);
				}
				catch(Exception e){
					System.out.println("updateSettingsNew insert: " + e.getMessage());
				}
								
			} else {
				
				String dbUpdateQuery = "UPDATE nv SET value = '"+value.getValue()+"' WHERE id = "+value.getId();
				
				try {
					Statement = Connection.createStatement();
					Statement.executeUpdate(dbUpdateQuery);
				}
				catch(Exception e){
					System.out.println("updateSettingsNew update: " + e.getMessage());
				}
				
				
				
			}
			
        });
		
		setConfigChanged(true);
		return new Response(true, null);
	}
	
	public String interfaceStatus(String interface_pos) {
		
		String status = "null";
		
		try {
			status = StreamerManager.command("interface/"+interface_pos+"/status get");
			//System.out.println(interface_pos + " status: " + status);
			
			if(!status.equals("")) {
				return status;
			} else {
				return "N/A";
			}
			
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
		}
		
		return status;
	}
	
	public Response interfaceSet(String sessionKey, String interface_pos, String interface_type) {
		
		if(interface_type.equals("dvbudp")) {
			return interfaceSetDvbudp(interface_pos);
		} else if(interface_type.equals("dvbs")) {
			return interfaceSetDvbs(interface_pos);
		} else if(interface_type.equals("dvbt")) {
			return interfaceSetDvbt(interface_pos);
		} else if(interface_type.equals("dvbc")) {
			return interfaceSetDvbc(interface_pos);
		} else if(interface_type.equals("hls2ip") || interface_type.equals("webradio")) {
			return interfaceSetHls(interface_pos, interface_type);
		} else {
			return new Response(false, "Server Error"); 
		}
		
	}
	

	private Response interfaceSetHls(String interface_pos, String interface_type) {
		//Config config = getConfigOthers(interface_pos, interface_type);
		
		Document xml = null;
		
		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			
			xml = docBuilder.newDocument();
			Element root = xml.createElement("TunerConfig");
			xml.appendChild(root);
			
		}
		catch(Exception e){
			//System.out.println("interfaceSet: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		String xmlstr = xmlToString(xml);
		xmlstr = "<?xml version='1.0' encoding='UTF-8' ?>" + xmlstr;
		//System.out.println("xml: " + xmlstr);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/tunerConfig set " + xmlstr);
			StreamerManager.command("interface/"+interface_pos+"/command save");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		//do not set anymore. Scan should not show up as changed
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	private Response interfaceSetDvbudp(String interface_pos) {
		Config config = getConfigDvbudp(interface_pos);
		
		Document xml = null;
		
		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			
			xml = docBuilder.newDocument();
			Element root = xml.createElement("dvbudpTunerConfig");
			xml.appendChild(root);
			
			Element address = xml.createElement("address");
			address.appendChild(xml.createTextNode(config.getInIp()));
			root.appendChild(address);
			
			Element port = xml.createElement("port");
			port.appendChild(xml.createTextNode(""+config.getInPort()));
			root.appendChild(port);
			
		}
		catch(Exception e){
			//System.out.println("interfaceSet: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		String xmlstr = xmlToString(xml);
		xmlstr = "<?xml version='1.0' encoding='UTF-8' ?>" + xmlstr;
		//System.out.println("xml: " + xmlstr);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/tunerConfig set " + xmlstr);
			StreamerManager.command("interface/"+interface_pos+"/command save");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	private Response interfaceSetDvbc(String interface_pos) {
		Config config = getConfigDvbc(interface_pos);
		
		Document xml = null;
		
		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			
			xml = docBuilder.newDocument();
			Element root = xml.createElement("dvbcTunerConfig");
			xml.appendChild(root);
			
			Element freq = xml.createElement("frequency");
			freq.appendChild(xml.createTextNode(""+config.getFreq()));
			root.appendChild(freq);
			
			Element symb = xml.createElement("symbolRate");
			symb.appendChild(xml.createTextNode(""+config.getSymb()));
			root.appendChild(symb);
			
			Element constellation = xml.createElement("constellation");
			constellation.appendChild(xml.createTextNode(""+config.getConstellation()));
			root.appendChild(constellation);
			
			Element deliverySystem = xml.createElement("deliverySystem");
			deliverySystem.appendChild(xml.createTextNode(config.getDel().toLowerCase()));
			root.appendChild(deliverySystem);
		}
		catch(Exception e){
			//System.out.println("interfaceSet: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		String xmlstr = xmlToString(xml);
		xmlstr = "<?xml version='1.0' encoding='UTF-8' ?>" + xmlstr;
		//System.out.println("xml: " + xmlstr);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/tunerConfig set " + xmlstr);
			StreamerManager.command("interface/"+interface_pos+"/command save");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	private Response interfaceSetDvbt(String interface_pos) {
		Config config = getConfigDvbt(interface_pos);
		
		Document xml = null;
		
		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			
			xml = docBuilder.newDocument();
			Element root = xml.createElement("dvbtTunerConfig");
			xml.appendChild(root);
			
			Element freq = xml.createElement("frequency");
			freq.appendChild(xml.createTextNode(""+config.getFreq()));
			root.appendChild(freq);
			
			Element bw = xml.createElement("bandwidth");
			bw.appendChild(xml.createTextNode(""+config.getBw()));
			root.appendChild(bw);
			
			Element deliverySystem = xml.createElement("deliverySystem");
			deliverySystem.appendChild(xml.createTextNode(config.getDel().toLowerCase()));
			root.appendChild(deliverySystem);
		}
		catch(Exception e){
			//System.out.println("interfaceSet: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		String xmlstr = xmlToString(xml);
		xmlstr = "<?xml version='1.0' encoding='UTF-8' ?>" + xmlstr;
		//System.out.println("xml: " + xmlstr);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/tunerConfig set " + xmlstr);
			StreamerManager.command("interface/"+interface_pos+"/command save");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	private Response interfaceSetDvbs(String interface_pos) {
		Config config = getConfigDvbs(interface_pos);
		
		Document xml = null;
		
		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			
			xml = docBuilder.newDocument();
			Element root = xml.createElement("dvbsTunerConfig");
			xml.appendChild(root);
			
			Element lnbType = xml.createElement("lnbType");
			lnbType.appendChild(xml.createTextNode(config.getLnbType().toLowerCase()));
			root.appendChild(lnbType);
			
			Element satNo = xml.createElement("satNo");
			satNo.appendChild(xml.createTextNode(""+config.getSatno()));
			root.appendChild(satNo);
			
			Element deliverySystem = xml.createElement("deliverySystem");
			deliverySystem.appendChild(xml.createTextNode(config.getDel().toLowerCase()));
			root.appendChild(deliverySystem);
			
			Element frequency = xml.createElement("frequency");
			frequency.appendChild(xml.createTextNode(""+config.getFreq()));
			root.appendChild(frequency);
			
			Element symbolRate = xml.createElement("symbolRate");
			symbolRate.appendChild(xml.createTextNode(""+config.getSymb()));
			root.appendChild(symbolRate);
			
			Element polarization = xml.createElement("polarization");
			polarization.appendChild(xml.createTextNode(config.getPol().toLowerCase()));
			root.appendChild(polarization);
			
		}
		catch(Exception e){
			//System.out.println("interfaceSet: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		String xmlstr = xmlToString(xml);
		xmlstr = "<?xml version='1.0' encoding='UTF-8' ?>" + xmlstr;
		//System.out.println("xml: " + xmlstr);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/tunerConfig set " + xmlstr);
			StreamerManager.command("interface/"+interface_pos+"/command save");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	public Response interfaceCommand(String interface_pos, String command) {
		
		//System.out.println("command: " + interface_pos + " " + command);
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/command " + command);
			//value = StreamerManager.command("interface/x8f/command scan");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		return new Response(true, null);
	}
	
	public Response interfaceScan(String interface_pos) {
		
		
		try {
			StreamerManager.command("interface/"+interface_pos+"/command stop");
			StreamerManager.command("interface/"+interface_pos+"/command scan");
			//System.out.println(value);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Server Error");
		}
		
		try {
			
			Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String date_str = formatter.format(new Date());
			
			//System.out.println("date_str " +date_str);
			
			String timestamp_str = "TIMESTAMPTZ '"+ date_str + "'";
			
			String dbQuery = "UPDATE interfaces SET "
					+ "scantime = "+timestamp_str+" "
					+ "WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		
		}
		catch(Exception e){
			//System.out.println("interfaceScan: " + e.getMessage());
			return new Response(false, "Server Error");
		}
		
		//no need to set config changed for scan 
		//setConfigChanged(true);
		return new Response(true, null);
	}
	
	public ArrayList<Service> interfaceScanResult(String interface_pos) {
		
		ArrayList<Service> services = new ArrayList<Service>();
		Document doc = null;
		
		try {
			String xmlstr = StreamerManager.command("interface/"+interface_pos+"/scannerResult get");
			//System.out.println(xmlstr);
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(xmlstr));
			doc = db.parse(inputSource);
		} catch (Exception e) {			
			System.out.println("interfaceScanResult ERROR: " + e.toString());
			return services;
		}
		
		NodeList service_list = doc.getElementsByTagName("service");
	
		for(int i = 0; i < service_list.getLength(); i++) {
		
			Node node = service_list.item(i);
			Element element = (Element)node;
		
			int sid = Integer.parseInt(getValue(element, "serviceId"));
			String name = getValue(element, "name");
			
			//+ " (" + interface_pos + ", " + sid + ")"
			
			int num_type = 0;
			
			try {
				num_type = Integer.parseInt(getValue(element, "serviceType"));
			}
			catch(NumberFormatException ex){
				//System.out.println("ERROR: " + ex.toString());
			}
			
			int pref_lcn = 0;
			
			try {
				pref_lcn = Integer.parseInt(getValue(element, "prefLcn", "0"));
			}
			catch(NumberFormatException ex){
				//System.out.println("ERROR: " + ex.toString());
			}
			
			String type = "TV_SD";
			
			if(num_type == 2) {
				type = "RADIO";
			} else if(num_type >= 25 && num_type <= 31) {
				type = "TV_HD";
			}
		
			boolean scrambled = Boolean.valueOf(getValue(element, "scrambled"));
			
			String selected_lang = "All";
			
			ArrayList<String> all_langs = new ArrayList<String>(); 
			NodeList stream_list = element.getElementsByTagName("stream");
			
			for(int j = 0; j < stream_list.getLength(); j++) {
				Node stream_node = stream_list.item(j);
				Element stream_element = (Element)stream_node;
				
				String lang = getValue(stream_element, "language");
				
				if(!lang.equals("N/A")) {
					all_langs.add(lang);
				}
			}
			
			Set<String> set = new LinkedHashSet<>(all_langs);
			all_langs.clear();
			all_langs.addAll(set);
			all_langs.add(0, "All");
			
			String originalNetworkId = getValue(element, "originalNetworkId");
			String transportStreamId = getValue(element, "transportStreamId");
			
			String epg_id = originalNetworkId +"."+ transportStreamId +"."+ sid;
			
			//TODO add streamUrl
			String streamUrl = getValue(element, "streamUrl");
			
			String tags = getValue(element, "tags");
			
			//System.out.println("tags: " + tags);
			
			List<String> tags_list = Collections.<String>emptyList();
			
			if(!tags.equals("N/A")) {
				
				if(tags.contains(",")) {
					
					tags_list = Arrays.asList(tags.split("\\s*,\\s*"));
					
				} else {
					
					tags_list = Arrays.asList(tags);
					
				}
			}
			
			
			//System.out.println("epg: " + epg_id);
			
			Service service = new Service(0, interface_pos, name, sid, type, selected_lang, false, all_langs, "", false, scrambled, epg_id, streamUrl, streamUrl, pref_lcn);
			service.setFilters(tags_list);
						
			services.add(service);
		}
		
		//NodeList service_list = doc.getElementsByTagName("service");
		
		return services;
	}
	
	public Response interfaceUpdate() {
		
		setSessionValue("update_interface", "Starting interface update");
		
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		Document doc = null;
		
		try {
			String xmlstr = StreamerManager.command("interface dir");
			
			setSessionValue("update_interface", "Reading XML");
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(xmlstr));
			doc = db.parse(inputSource);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
			return new Response(false, "Error reading interface dir.");
		}
		
		NodeList node_list = doc.getElementsByTagName("node");
		
		int error_count = 0;
		
		for(int i = 0; i < node_list.getLength(); i++) {
			
			Node node = node_list.item(i);
			Element element = (Element)node;
			
			Interface new_interface = getInterface(getValue(element, "name"), false);
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
			
			if(new_interface != null) {
				interfaces.add(new_interface);
				
				setSessionValue("update_interface", "Adding interface " + new_interface.getPosition());
			} else {
				error_count++;
			}
			
		}
				
		//delete interfaces
		try {
			String dbQuery = "DELETE FROM interfaces;";
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			//System.out.println("delete ERROR: " + e.toString());
		}
		
		int start_sid_infostreamer = 9000;
		int start_hdmi_count = 0;
		int start_hdmi_sid = 9100;
		
		//save new interfaces
		try {
			for (int i = 0; i < interfaces.size(); ++i) {
				
				
				
				
				if(interfaces.get(i).getType().equals("dvbs") || 
						interfaces.get(i).getType().equals("dvbt") || 
						interfaces.get(i).getType().equals("dvbc") ||
						interfaces.get(i).getType().equals("dvbhdmi") || 
						interfaces.get(i).getType().equals("hdmi2ip") || 
						interfaces.get(i).getType().equals("infostreamer") || 
						interfaces.get(i).getType().equals("dsc") || 
						interfaces.get(i).getType().equals("mod") || 
						interfaces.get(i).getType().equals("dvbudp") || 
						interfaces.get(i).getType().equals("hls2ip") ||
						interfaces.get(i).getType().equals("infoch") ||
						interfaces.get(i).getType().equals("webradio")) {
					String dbQuery = "INSERT INTO interfaces (pos, name, type, active, multiband) VALUES ('"+interfaces.get(i).getPosition()+"', '"+interfaces.get(i).getName()+"', '"+interfaces.get(i).getType()+"', "+interfaces.get(i).getActive()+", "+interfaces.get(i).isMultiBand()+");";
					//System.out.println("dbQuery " + dbQuery);
					Statement.executeUpdate(dbQuery);
					
					if(interfaces.get(i).getType().equals("hls2ip")) {
						String dbQueryHls = "INSERT INTO config_hls (interface_pos) SELECT '"+interfaces.get(i).getPosition()+"' WHERE NOT EXISTS (SELECT interface_pos FROM config_hls WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						//System.out.println("dbQuery " + dbQueryHls);
						Statement.executeUpdate(dbQueryHls);
					}
					
					//save infotv config
					if(interfaces.get(i).getType().equals("infostreamer")) {
						String dbQueryInfo = "INSERT INTO config_istr (interface_pos, presentation_url) SELECT '"+interfaces.get(i).getPosition()+"', '' WHERE NOT EXISTS (SELECT interface_pos FROM config_istr WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						//System.out.println("dbQuery " + dbQueryInfo);
						Statement.executeUpdate(dbQueryInfo);
						
						//save infotv services
						
						String dbQueryInfoService = "with data(name, sid, key)  as ( values "
								+ "('Info and Radio 1', "+start_sid_infostreamer+", '"+interfaces.get(i).getPosition()+", "+start_sid_infostreamer+"'), "
								+ "('Info and Radio 2', "+(start_sid_infostreamer+1)+", '"+interfaces.get(i).getPosition()+", "+(start_sid_infostreamer+1)+"'), "
								+ "('Info and Radio 3', "+(start_sid_infostreamer+2)+", '"+interfaces.get(i).getPosition()+", "+(start_sid_infostreamer+2)+"'), "
								+ "('Info and Radio 4', "+(start_sid_infostreamer+3)+", '"+interfaces.get(i).getPosition()+", "+(start_sid_infostreamer+3)+"'), "
								+ "('Info and Radio 5', "+(start_sid_infostreamer+4)+", '"+interfaces.get(i).getPosition()+", "+(start_sid_infostreamer+4)+"') ) "
								+ "insert into services (interface_pos, name, sid, type, lang, all_langs, enable, istr_url, istr_video, key) "
								+ "select '"+interfaces.get(i).getPosition()+"', d.name, d.sid, 'RADIO', '', NULL, false, '', false, d.key "
								+ "from data d "
								+ "where not exists (select interface_pos from services where interface_pos = '"+interfaces.get(i).getPosition()+"');";
						
						
						//System.out.println("dbQuery: " + dbQueryInfoService);
						Statement.executeUpdate(dbQueryInfoService);
						start_sid_infostreamer += 5;
					}
					
					
					if(interfaces.get(i).getType().equals("dsc")) {
						String dbQueryDsc = "INSERT INTO config_dsc (interface_pos, emm) SELECT '"+interfaces.get(i).getPosition()+"', 1 "
								+ "WHERE NOT EXISTS (SELECT interface_pos FROM config_dsc WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						Statement.executeUpdate(dbQueryDsc);
					}
					
					if(interfaces.get(i).getType().equals("mod")) {
						String dbQueryMod = "INSERT INTO config_eqam (interface_pos, network_num) SELECT '"+interfaces.get(i).getPosition()+"', 1 "
								+ "WHERE NOT EXISTS (SELECT interface_pos FROM config_eqam WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						Statement.executeUpdate(dbQueryMod);
					}
					
					if(interfaces.get(i).getType().equals("dvbhdmi") || interfaces.get(i).getType().equals("hdmi2ip")) {
						
						start_hdmi_count++;
						start_hdmi_sid++;
						
						String name = "Hdmi " +  start_hdmi_count;
						
						//changed format to auto
						String dbQueryHdmi = "INSERT INTO config_hdmi (interface_pos, format) SELECT '"+interfaces.get(i).getPosition()+"', 'auto' WHERE NOT EXISTS (SELECT interface_pos FROM config_hdmi WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						//System.out.println("dbQuery " + dbQueryHdmi);						
						Statement.executeUpdate(dbQueryHdmi);
						
						String dbInsertService = "INSERT INTO services (interface_pos, name, sid, type, lang, all_langs, enable, istr_url, istr_video, scrambled, key) "
								+ "SELECT '"+interfaces.get(i).getPosition()+"', '"+name+"', "+start_hdmi_sid+", 'TV_HD', '', NULL, false, NULL, false, false, '"+ interfaces.get(i).getPosition() + ", "+start_hdmi_sid+"' "
								+ "WHERE NOT EXISTS (SELECT interface_pos FROM services WHERE interface_pos = '"+interfaces.get(i).getPosition()+"')";
						Statement.executeUpdate(dbInsertService);
						//System.out.println("dbQuery " + dbInsertService);
					}
					
					setSessionValue("update_interface", "Inserting interface " + interfaces.get(i).getPosition() + " to database");
					
				}
			}
		}
		catch(Exception e){
			System.out.println("ERROR: " + e.toString());
		}
		
		//set services enabled false for interfaces with type mod or dsc
		try {

			String dbQuery = "UPDATE services "
					+ "SET enable = false "
					+ "FROM interfaces "
					+ "WHERE interfaces.pos = services.interface_pos " 
					+ "AND (interfaces.type = 'mod' OR interfaces.type = 'dsc')";
			
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("set services enabled false for interfaces with type mod or dsc\nERROR: " + e.toString());
		}		
		
		if(error_count > 0) {
			return new Response(false, error_count + " interface(s) failed.");
		}
		return new Response(true, null);
	}
	
	public Interface getInterface(String interface_pos, boolean with_status) {
		
		Document doc = null;
		
		try {
			String xmlstr = StreamerManager.command("interface/"+interface_pos+"/interfaceConfig get");
			//System.out.println(xmlstr);
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(xmlstr));
			doc = db.parse(inputSource);
		} catch (Exception e) {
			System.out.println("getInterface ERROR: " + e.getMessage());
			return null;
		}
		
		NodeList node_list = doc.getElementsByTagName("interfaceConfig");
		
		Node node = node_list.item(0);
		Element element = (Element)node;
		
		String type = getValue(element, "type");
		String name = getValue(element, "userText");
		
		if(type.equals("eqam")) {
			type = "mod";
		}
		
		if(type.equals("mod") || type.equals("dsc")) {
			name = type.toUpperCase() +  "-" + interface_pos;
		}
		
		boolean active = Boolean.valueOf(getValue(element, "autoStart"));
		boolean multiband = Boolean.valueOf(getValue(element, "multiBand"));
		
		String status = null;
		if(with_status) {
			status = interfaceStatus(interface_pos);
		}
		

		
		if(type.equals("infoch")) {
			String infoch_name = getInterfaceInfoch(interface_pos).getInterfaceName();
			return new Interface(interface_pos, infoch_name, type, status, active, multiband);
		}
		
		//System.out.println("getInterface");
		//FIXME add multiband from StreamerManager !done
		return new Interface(interface_pos, name, type, status, active, multiband);
	}
	
	public Response pushConfig() {
		
		Boolean cmdResult = false;
        try {
        	int r = Runtime.getRuntime().exec("/usr/bin/ixuiconf --dvbconf").waitFor();

        	//System.out.println("pushConfig result " + r);
        	
			if(r == 0){
				cmdResult = true;
			}
        }
        catch(Exception e){
        	System.out.println("Failed to run ixuiconf, " + e.getMessage());
        	 //return new Response(false, "Exception Error");
        }
		
        //System.out.println("pushConfig flag " + cmdResult);
        
        String log = "";
        
        if(cmdResult) {
        	return new Response(true, null);
        } else {
        	
        	BufferedReader br = null;
    		FileReader fr = null;

    		try {

    			fr = new FileReader("/tmp/ixuiconf.log");
    			br = new BufferedReader(fr);

    			String sCurrentLine;

    			br = new BufferedReader(fr);

    			while ((sCurrentLine = br.readLine()) != null) {
    				//System.out.println(sCurrentLine);
    				log += sCurrentLine + "<br>";
    			}

    		} catch (IOException e) {
    			e.printStackTrace();
    			return new Response(false, "Exception Error");
    		} finally {
    			try {
    				if (br != null)
    					br.close();
    				if (fr != null)
    					fr.close();
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}

    		}
        	
        }
        
        //System.out.println("pushConfig log " + log);
        
        setConfigChanged(false);
        return new Response(false, log);
        
	}
	
	public String interfaceLog(String interface_pos) {
		
		String log = "";
		
		try {
			log = StreamerManager.command("interface/"+interface_pos+"/log get", true);
			//System.out.println(log);
		} catch (Exception e) {			
			//System.out.println("ERROR: " + e.toString());
		}
		
		//return log.replaceAll("\\.", "<br>");
		return log;
		
	}
	
	public boolean isConfigChanged(String sessionKey) {
		
		try {
			String dbQuery = "SELECT name, value FROM nv;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				if(name.equals("config_changed")) {
					return Boolean.valueOf(Result.getString("value"));
				}
			}
			
		}
		catch(Exception e){
			//System.out.println("isConfigChanged: " + e.getMessage());
		}
		
		return false;
	}
	
	private void setConfigChanged(boolean changed) {
		
		try {
			String dbQuery = "UPDATE nv SET value = '"+changed+"' WHERE name = 'config_changed';";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		
		}
		catch(Exception e){
			//System.out.println("setConfigChanged: " + e.getMessage());
		}
		
	}
	
	private String getValue(Element element, String tag) {
		
		NodeList itemList = null;
		Element itemElement = null;
		
		itemList = element.getElementsByTagName(tag);
		itemElement = (Element)itemList.item(0);
		
		String item = "N/A";
        
        if(itemList != null) {
        	if(itemList.getLength() > 0 && itemElement.getChildNodes().getLength() > 0) {
            	item = ((Node)itemElement.getChildNodes().item(0)).getNodeValue().toString();
            }
        }
        
        return item;
	}
	
	private String getValue(Element element, String tag, String default_value) {
		
		NodeList itemList = null;
		Element itemElement = null;
		
		itemList = element.getElementsByTagName(tag);
		itemElement = (Element)itemList.item(0);
		
		String item = default_value;
        
        if(itemList != null) {
        	if(itemList.getLength() > 0 && itemElement.getChildNodes().getLength() > 0) {
            	item = ((Node)itemElement.getChildNodes().item(0)).getNodeValue().toString();
            }
        }
        
        return item;
	}
		
	private String xmlToString(Document doc) {
		String output = "";
		
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		}
		catch(Exception e){
			//System.out.println("docToString: " + e.getMessage());
		}
		
		return output;
	}
	
	public int getMaxBitrates(String type) {
		
		int max_bitrate = 0;
		
		String temp = "dsc_bitrate";
		
		if(type.equals("mod")) {
			temp = "dvbc_qam";
		} else if(type.equals("hls")) {
			temp = "hls_services";
		}
		
		try {
			String dbQuery = "SELECT value FROM nv WHERE name = '"+temp+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			String value = Result.getString("value");
			
			if(type.equals("mod")) {
				if(value.equals("QAM-256")) {
					max_bitrate = 51000000;
				}
				
				if(value.equals("QAM-128")) {
					max_bitrate = 44000000;
				}
				
				if(value.equals("QAM-64")) {
					max_bitrate = 38000000;
				}
			} else {
				max_bitrate = Integer.parseInt(value);
			}
			
		}
		catch(Exception e){
			System.out.println("getMaxBitrates: " + e.getMessage());
		}
		
		return max_bitrate;
		
	}
	
	public boolean getEnabledType(String type) {
		
		boolean value = false;
		
		try {
			String dbQuery = "SELECT value FROM nv WHERE name = '"+type+"_enable';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			value = Boolean.valueOf(Result.getString("value"));
		}
		catch(Exception e){
			//System.out.println("saveServices address: " + e.getMessage());
		}
				
		return value;
		
	}
	
	public UnitInfo getUnitInfo() {
		
		String serial = null;
		String version = null;
		String hostname = null;
		
		boolean isCloud = ContextManager.isCloud();
		boolean isForcedContent = ContextManager.isForcedContent();
		boolean isSoftwareUpdate = ContextManager.isSoftwareUpdate();
		boolean isHlsoutput = ContextManager.isHlsoutput();
		boolean isPortal = ContextManager.isPortal();
		
		try {
			String dbQuery = "SELECT name, value FROM nv;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String name = Result.getString("name");
				
				if(name.equals("ui_serial")) {
					serial = Result.getString("value");
				}
				
				if(name.equals("ui_swversion")) {
					version = Result.getString("value");
				}
				
				if(name.equals("nw_hostname")) {
					hostname = Result.getString("value");
				}
				
				
			}
			
		}
		catch(Exception e){
			//System.out.println("getUnitInfo: " + e.getMessage());
		}
		
		return new UnitInfo(serial, version, hostname, isCloud, isForcedContent, isSoftwareUpdate, isHlsoutput, isPortal);
		
	}
	
	public HashMap<String, NameValue> getNetworkSettings() {
		
		HashMap<String, NameValue> settings = new HashMap<String, NameValue>();
		
		try {
			String dbQuery = "SELECT * FROM nv ORDER BY id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				int id = Integer.parseInt(Result.getString("id"));
				String name = Result.getString("name");
				
				if(name.startsWith("nw_")) {
					String value = Result.getString("value");
					settings.put(name, new NameValue(id, name, value));
				}
			}
			
		}
		catch(Exception e){
			System.out.println("getNetworkSettings: " + e.getMessage());
		}
		
		return settings;
	}
	
	public Response runCommand(String command, String filename) {
		
		Boolean cmdResult = false;
		
        try {
        	int r = Runtime.getRuntime().exec("/usr/bin/ixuiconf --" + command).waitFor();

			if(r == 0){
				cmdResult = true;
			}
        }
        catch(Exception e){
        	//System.out.println("Failed to run ixuiconf, " + e.getMessage());
        }
        
        if(cmdResult) {
        	
        	/*
        	if(command.equals("backup")) {
        		
        		String serial = getUnitInfo().getSerial();
            	            	
            	Path source = Paths.get("/tmp/ixui_backup.json");
            	Path destination = Paths.get("/srv/webapps/" + ContextManager.APP_NAME + "/doc/ixui_backup_"+serial+".json");
            	
            	try {
    				Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        	}
        	*/
        	
        	
        	return new Response(true, null);
        } else {
        	return new Response(false, "Failed to run " + command);
        }
        
	}
	
	public Response savePDF(String filename) {
		
		HashMap<String, String> values = new HashMap<String, String>();
		
		try {
			
			String dbQuery = "SELECT name, value FROM nv;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while(Result.next()) {
				values.put(Result.getString("name"), Result.getString("value"));
			}
			
		}
		catch(Exception e){
			//System.out.println("savePDF: " + e.getMessage());
			return new Response(false, "Server error code: 1.");
		}
		
		
		ArrayList<Interface> interfaces = getInterfaces(null, false);
		Collections.sort(interfaces, Interface.Comparators.POS);
		
		ArrayList<PDFValue> pdf_interfaces = new ArrayList<PDFValue>();
		
		
		for (int i = 0; i < interfaces.size(); ++i) {
			if(interfaces.get(i).getActive()) {
				if(interfaces.get(i).getType().contains("dvb") || interfaces.get(i).getType().equals("infostreamer") || interfaces.get(i).getType().equals("dvbhdmi") || interfaces.get(i).getType().equals("hdmi2ip") || interfaces.get(i).getType().equals("hls2ip") || interfaces.get(i).getType().equals("webradio")) {
					
					pdf_interfaces.add(new PDFValue(interfaces.get(i).getPosition(), interfaces.get(i).getName()));
					
					//getInterfaceFreqPol(interfaces.get(i).getPosition(), interfaces.get(i).getType())
					
				}
			}
		}
		
		ArrayList<Interface> modulators_net1 = new ArrayList<Interface>();
		
		try {
			
			//String dbQuery = "SELECT pos FROM interfaces WHERE active is true AND type='mod' ORDER BY pos;";
			String dbQuery = "SELECT pos, active FROM interfaces "
					+ "INNER JOIN config_eqam on interfaces.pos = config_eqam.interface_pos "
					+ "WHERE type='mod' AND config_eqam.network_num = 1 ORDER BY pos";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				modulators_net1.add(new Interface(Result.getString("pos"), Result.getBoolean("active")));
			}
			
		} catch(Exception e){
			//System.out.println("savePDF: " + e.getMessage());
			return new Response(false, "Server error code: 2.");
		}
		
		ArrayList<Interface> modulators_net2 = new ArrayList<Interface>();
		
		try {
			
			//String dbQuery = "SELECT pos FROM interfaces WHERE active is true AND type='mod' ORDER BY pos;";
			String dbQuery = "SELECT pos, active FROM interfaces "
					+ "INNER JOIN config_eqam on interfaces.pos = config_eqam.interface_pos "
					+ "WHERE type='mod' AND config_eqam.network_num = 2 ORDER BY pos";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				modulators_net2.add(new Interface(Result.getString("pos"), Result.getBoolean("active")));
			}
			
		} catch(Exception e){
			//System.out.println("savePDF: " + e.getMessage());
			return new Response(false, "Server error code: 2.");
		}
		
		ArrayList<PDFRow> channel_rows = new ArrayList<PDFRow>();
		
		
		try {
			String dbQuery = "SELECT routes.output_name, routes.lcn, routes.dsc_pos, routes.mod_pos, routes.mod_pos_net2, routes.out_sid, routes.out_ip FROM routes "
					+ "INNER JOIN services ON routes.service_key = services.key "
					+ "INNER JOIN interfaces ON services.interface_pos = interfaces.pos "
					+ "WHERE services.enable is true AND interfaces.active is true AND interface_pos IN (select pos from interfaces) ORDER BY lcn;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				String service_name = Result.getString("output_name");
				String lcn = Integer.toString(Result.getInt("lcn"));
				String descrambler_pos = Result.getString("dsc_pos");
				String modulator_pos = Result.getString("mod_pos");
				String modulator_pos_net2 = Result.getString("mod_pos_net2");
				String out_sid = Result.getString("out_sid");
				String out_ip = Result.getString("out_ip");
				
				channel_rows.add(new PDFRow(lcn, service_name, descrambler_pos, modulator_pos, modulator_pos_net2, out_sid, out_ip));
				
				
			}
			
		}
		catch(Exception e){
			//System.out.println("savePDF: " + e.getMessage());
			return new Response(false, "Server error code: 3.");
		}
		
		
		PDFGenerator gen = new PDFGenerator();
		
		try {
			gen.generatePDF(values, pdf_interfaces, modulators_net1, modulators_net2, channel_rows);
		} catch (IOException e) {
			e.printStackTrace();
			return new Response(false, "PDF generation error.");
		}
		
		return new Response(true, null);
	}
	/*
	private String getInterfaceFreqPol(String interface_pos, String interface_type) {
		
		String result = "";
		
		if(interface_type.equals("dvbs")) {
			
			try {
				String dbQuery = "SELECT freq, pol FROM config_sat WHERE interface_pos = '"+interface_pos+"';";
				
				Statement = Connection.createStatement();
				Result = Statement.executeQuery(dbQuery);
				
				Result.next();
				
				result = Result.getString("freq") + Result.getString("pol").toLowerCase();
				
			}
			catch(Exception e){
				//System.out.println("saveServices address: " + e.getMessage());
			}
			
		} else if(interface_type.equals("dvbt")) {
			
			try {
				String dbQuery = "SELECT freq FROM config_ter WHERE interface_pos = '"+interface_pos+"';";
				
				Statement = Connection.createStatement();
				Result = Statement.executeQuery(dbQuery);
				
				Result.next();
				
				result = Result.getString("freq");
				
			}
			catch(Exception e){
				//System.out.println("saveServices address: " + e.getMessage());
			}
			
		}
		
		return result;
		
	}
	*/
	public ArrayList<Package> getUpdatePackages() {
		
		ArrayList<Package> packages = null;
			
		try {

			File fXmlFile = new File("/tmp/ixui_check_sw.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
		
			doc.getDocumentElement().normalize();
			
			NodeList package_list = doc.getElementsByTagName("package");
			
			packages = new ArrayList<Package>();
			
			for(int i = 0; i < package_list.getLength(); i++) {
				
				Node node = package_list.item(i);
				Element element = (Element)node;
			
				String name = getValue(element, "name");
				String version = getValue(element, "version");
				
				packages.add(new Package(name, version));
			}
			
			
		} catch (Exception e) {
			return null;
			//e.printStackTrace();
		}
			
		return packages;
	}
	
	public Response updatePackages(ArrayList<Package> packages) {
		
		//boolean all = true;
		String update_packages = "";
		
		for (int i = 0; i < packages.size(); i++) {
			
			if(packages.get(i).isUpdate()) {
				update_packages += " " + packages.get(i).getName();
			}
		}
		
		Response response = runUpdateCommand("update-sw" + update_packages);

		return response;
	}
	
	public String getUpdateResult() {
		
		try(FileInputStream inputStream = new FileInputStream("/tmp/ixui_update_sw.txt")) {     
			return IOUtils.toString(inputStream);
		    
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "An error occurred during reading the results of the update.";
		
	}
	
	public Response runUpdateCommand(String command) {
		
		Boolean cmdResult = false;
		String cmdError = null;
        try {
        	int r = Runtime.getRuntime().exec("/usr/bin/ixuiconf --" + command).waitFor();

			if(r == 0){
				cmdResult = true;
				
			} else if(r == 2){
				cmdError = "Network error, please check your Internet access.";
				
			} else {
				cmdError = "An error occured while checking for available updates.";
				
			}
        }
        catch(Exception e){
        	//System.out.println("Failed to run ixuiconf, " + e.getMessage());
        	 //return new Response(false, "Exception Error");
        }
        
        if(cmdResult) {
        	return new Response(true, null);
        } else {
        	return new Response(false, cmdError);
        }
        
	}
	
	public Response updateInterfaceMultibandType(String interface_pos, String interface_type) {
		
		try {
			
			String dbQuery = "UPDATE interfaces SET "
					+ "type = '"+interface_type+"' "
					+ "WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		
		}
		catch(Exception e){
			System.out.println("updateInterfaceMultibandType: " + e.getMessage());
			return new Response(false, "Server error.");
			
		}
		
		return new Response(true, interface_type);
		
	}
	
	private boolean setSessionValue(String key, String value) {
		
		HttpSession session = getThreadLocalRequest().getSession();
		
		if(session != null) {
			session.setAttribute(key, value);
			return true;
		}
		
		return false;
	}
	
	public String getSessionValue(String key) {
		
		HttpSession session = getThreadLocalRequest().getSession();
		
		if(session == null) {
			return null;
		}
		
		if(session.getAttribute(key) == null) {
			return null;
		} else {
			String value = (String) session.getAttribute(key);
			//session.removeAttribute(key);
			return value;
		}
		
	}
	
	public ArrayList<String> runCommand2(String command) {
		
		//System.out.println("runCommand2 " + command);
		
		
		ArrayList<String> rows = new ArrayList<String>();
		
		try {
			
			Process pr = Runtime.getRuntime().exec("/usr/bin/ixuiconf --" + command);

			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		    String line;
		    while ((line = in.readLine()) != null) {
		    	rows.add(line);
		    }
		    pr.waitFor();
		    in.close();
			
		}
		catch(Exception e){
			System.out.println("Failed to run command " + command);	
		}
		
		
		
		return rows;
		
	    
	}
	
	public Response saveDateTime(String sessionKey, boolean isRestart, String timezone, boolean ntp_mode, String date, String time) {
		
		setNtpMode(ntp_mode);
		runCommand("set-clock-mode", null);
		
		if(!ntp_mode) {
			runCommand("set-date " + date, null);
			runCommand("set-time " + time, null);
		}
		
		if(isRestart) {
			runCommand("set-timezone '"+timezone+"'", null);
		}

		return new Response(true, "Done.");
	}
	
	private void setNtpMode(boolean ntp_mode) {
		
		try {
			String dbQuery = "UPDATE nv SET value = '"+ntp_mode+"' WHERE name = 'ntp_enable';";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		
		}
		catch(Exception e){
			//System.out.println("setConfigChanged: " + e.getMessage());
		}
		
	}
	
	public HashMap<String, String> getCloudDetails(String sessionKey) {
		
		HashMap<String, String> details = new HashMap<String, String>();
		
		try {
			String dbQuery = "SELECT * FROM nv ORDER BY id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while (Result.next()) {
				
				//int id = Integer.parseInt(Result.getString("id"));
				String name = Result.getString("name");
				
				if(name.startsWith("ixcloud_")) {
					
					if(name.endsWith("enable") || name.endsWith("online") || name.endsWith("validate_date") || name.endsWith("validate_message") || name.endsWith("beaconid")) {
						String value = Result.getString("value");
						
						details.put(name, value);
						
						//details.add(new NameValue(id, name, value));
					}
					
				}
			}
			
		}
		catch(Exception e){
			//System.out.println("getNetworkSettings: " + e.getMessage());
		}
		
		return details;
	}
	
	public HashMap<Integer, ForcedContent> getForcedContents(String sessionKey) {
		
		HashMap<Integer, ForcedContent> map = new HashMap<Integer, ForcedContent>();
		
		try {
			String dbQuery = "SELECT * FROM forced_content;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);

			while(Result.next()) {
				int id = Result.getInt("id");
				
				boolean enable = Result.getBoolean("enable");
				String name = Result.getString("name");
				int networks = Result.getInt("networks");
				String ts_filename = Result.getString("ts_filename");
				int operation_mode = Result.getInt("operation_mode");
				int signal_type = Result.getInt("signal_type");
				int volume = Result.getInt("volume");
				
				map.put(id, new ForcedContent(id, enable, name, networks, ts_filename, operation_mode, signal_type, volume));
			}
			
			
		}
		catch(Exception e){
			System.out.println("getForcedContents: " + e.getMessage());
		}
		
		return map;
	}
	
	public ArrayList<ForcedContent> getEnabledForcedContents(String sessionKey) {
		
		ArrayList<ForcedContent> list = new ArrayList<ForcedContent>();
		
		try {
			String dbQuery = "SELECT * FROM forced_content WHERE enable = true ORDER BY id;";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);

			while(Result.next()) {
				int id = Result.getInt("id");
				String name = Result.getString("name");
				int signal_override = Result.getInt("signal_override");
				int signal_status = Result.getInt("signal_status");
				boolean com_status = Result.getBoolean("com_status");
				
				list.add(new ForcedContent(id, name, signal_override, signal_status, com_status));
			}
			
			
		}
		catch(Exception e){
			System.out.println("getEnabledForcedContents: " + e.getMessage());
		}
		
		return list;
	}
	
	private void checkForcedContentChanged(ForcedContent content_to_save, ForcedContent current_db_content) {
		
		boolean config_changed = false; 
		
		if(ForcedContent.Comparators.ENABLED.compare(content_to_save, current_db_content) != 0) {
			config_changed = true;
		}
		
		if(ForcedContent.Comparators.NETWORK.compare(content_to_save, current_db_content) != 0) {
			config_changed = true;
		}
		
		if(config_changed) {
			setConfigChanged(true);
		}
		
		
	}
	
	public Response saveForcedContents(ArrayList<ForcedContent> forced_contents) {
				
		HashMap<Integer, ForcedContent> map = getForcedContents(null);
		
		for (int i = 0; i < forced_contents.size(); i++) {
			
			checkForcedContentChanged(forced_contents.get(i), map.get(forced_contents.get(i).getId()));
			
			saveForcedContent(forced_contents.get(i));
		}
		
		return new Response(true, "Done.");
	}
	
	private void saveForcedContent(ForcedContent fc) {
		
		String dbQuery = "UPDATE forced_content SET "
				+ "enable = "+fc.isEnable()+", "
				+ "name = '"+fc.getName()+"', "
				+ "networks = "+fc.getNetworks()+", "
				+ "ts_filename = '"+fc.getTsFilename()+"', "
				+ "operation_mode = "+fc.getOperationMode()+", "
				+ "signal_type = "+fc.getSignalType()+", "
				+ "volume = "+fc.getVolume()+" "
				+ "WHERE id = "+fc.getId()+";";
		
		try {
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("saveForcedContent: " + e.getMessage());
		}
		
	}
	
	public void saveForcedContentOverrideStatus(int id, int index) {
		
		String dbQuery = "UPDATE forced_content SET signal_override = "+index+" WHERE id = "+id+";";
		
		try {
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
		}
		catch(Exception e){
			System.out.println("saveForcedContent: " + e.getMessage());
		}
		
	}
	
	public ArrayList<Media> getMedia() {
		return IxcAdminHelper.getInstance().getMedia();
	}
	
	public boolean isSoftwareUpdate() {
		return ContextManager.isSoftwareUpdate();
	}
	
	public ArrayList<IpMac> getNetworkStatus(String sessionKey) {
		
		ArrayList<IpMac> result = new ArrayList<IpMac>();
		
		HashMap<String, String> settings = getSettings(sessionKey, true);
		
		
		for (int i = 0; i <= 9; i++) {
			
			if(!settings.containsKey("nw_eth"+i+"_onboot")) {
				break;
			}
			
			result.add(getIpMac("eth"+i));
			
			
			
		}
		
		/*	
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			
			while (networkInterfaces.hasMoreElements()) {
			    NetworkInterface ni = networkInterfaces.nextElement();
			    
			    System.out.println("getDisplayName " + ni.getDisplayName());
			    System.out.println("getName " + ni.getName());
			    
			    
			    byte[] hardwareAddress = ni.getHardwareAddress();
			    if (hardwareAddress != null) {
			        String[] hexadecimalFormat = new String[hardwareAddress.length];
			        for (int i = 0; i < hardwareAddress.length; i++) {
			            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
			        }
			        System.out.println(String.join(":", hexadecimalFormat));
			    }
			    
			    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
		        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
		        	System.out.println("InetAddress: " + inetAddress);
		        }
			    
			}
			
		} catch (SocketException e) {
			
		}
		*/
		
		return result;
	}
	
	private IpMac getIpMac(String interfaceName) {
		
		//System.out.println("getIpMac interfaceName " + interfaceName);
		
		
		IpMac result = new IpMac();
		
		ArrayList<String> addresses = new ArrayList<String>();
	    //interfaceName = "eth0";
	    
		try {
			NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
			
			if(networkInterface == null) {
				return new IpMac("N/A", "N/A");
			}
			
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			
	        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	        	
	        	//System.out.println("getIpMac inetAddress " + inetAddress.toString());
	        	
	        	if(inetAddress instanceof Inet4Address) {
	        		result.setIp(inetAddress.getHostAddress());
	        	}
	        	
	        	
	        }
	        
	        byte[] hardwareAddress = networkInterface.getHardwareAddress();
		    if (hardwareAddress != null) {
		        String[] hexadecimalFormat = new String[hardwareAddress.length];
		        for (int i = 0; i < hardwareAddress.length; i++) {
		            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
		        }
		        result.setMac(String.join(":", hexadecimalFormat));
		    }
			
		    
		    
		    
		    
		} catch (SocketException e) {
			System.out.println("getIpMac Exception " + e.getMessage());
		}
	    
		
		
		
		return result;
		
	}
	
	public HashMap<String, IpStatus> getNetworkStatus2(String sessionKey) {
		
		HashMap<String, String> settings = getSettings(sessionKey, true);
		
		HashMap<String, IpStatus> status = new HashMap<String, IpStatus>();
		
		
	    
		
		//System.out.println(new java.util.Date().toString() + "--Start");
		
		String ip = getPublicIpAddress();
		
		//System.out.println(new java.util.Date().toString()+"--- after public fetch");
		
		status.put("gateway", new IpStatus(settings.get("nw_gateway"), isIpReachable(settings.get("nw_gateway"), 1000)));
		
		//System.out.println(new java.util.Date().toString()+"--- after gateway");
		
		status.put("dns1", new IpStatus(settings.get("nw_dns1"), isIpReachable(settings.get("nw_dns1"), 1000)));
		
		//System.out.println(new java.util.Date().toString()+"--- after dns1");
		
		status.put("dns2", new IpStatus(settings.get("nw_dns2"), isIpReachable(settings.get("nw_dns2"), 1000)));
		
		//System.out.println(new java.util.Date().toString()+"--- after dns2");
		
		status.put("public", new IpStatus(ip, isIpReachable(ip, 1000)));
		
		//System.out.println(new java.util.Date().toString()+"--- after public");
		
		return status;		
		
	}
	
	private boolean isIpReachable(String ip, int timeout){
	    boolean state = false;
	    
	    if(ip == null || ip.equals("")) {
	    	return false;
	    }	    
	    
	    try {
	        state = InetAddress.getByName(ip).isReachable(timeout);
	    } catch (IOException e) {
	    	System.out.println("isIpReachable Exception " + e.getMessage());
	    }

	    return state;
	}
	
	private String getPublicIpAddress() {
	
		String result = null;
		
		try {
			final URL url = new URL("http://checkip.amazonaws.com");
		    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
		    con.setRequestMethod("GET");
		    con.setConnectTimeout(2000);
		    con.setReadTimeout(2000);
		
		    if (con.getResponseCode() != 200) {
		        return null;
		    }
		
		    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8"));
		    String inputLine;
		    StringBuffer response = new StringBuffer();
		
		    while ((inputLine = in.readLine()) != null) {
		        response.append(inputLine);
		    }
		    
		    result = response.toString();
		    
		    in.close();
		
		} catch (Exception e) {
			System.out.println("getPublicIpAddress Exception " + e.getMessage());
		}
	    
	
	    return result;
	}
	
	public Config getInterfaceInfoch(String interface_pos) {
		
		Config config = new Config();
		
		try {
			String dbQuery = "SELECT name, active FROM interfaces WHERE pos = '"+interface_pos+"';";
			
			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			Result.next();
			
			config = new Config(interface_pos, Result.getString("name"), Result.getBoolean("active"));
			
		}
		catch(Exception e){
			System.out.println("getInterfaceInfoch: " + e.getMessage());
		}
		
		return config;
		
	}
	
	public Response setInterfaceInfoch(Config config, boolean isScan) {
		
		
		try {
			
			String dbQuery = "UPDATE interfaces SET name = '"+config.getInterfaceName()+"', active = "+config.getInterfaceActive()+" WHERE pos = '"+config.getInterfacePos()+"'";
			
			Statement = Connection.createStatement();
			Statement.executeUpdate(dbQuery);
			
			
			
		}
		catch(Exception e){
			System.out.println("setInterfaceInfoch: " + e.getMessage());
		}
		
		
		
		return new Response(isScan, null);
				
	}
	
	public String getJsonInfo() {
		
		String result = "";
		
		JSONParser parser = new JSONParser();

		String target_filepath = "/tmp/ixui_backup.json";
		
        try {
        	
        	File f = new File(target_filepath);
			
			if(f.exists() && !f.isDirectory()) {
				
				Object obj = parser.parse(new FileReader(target_filepath));
	            	            
	            JSONObject jsonObject =  (JSONObject) obj;

	            String date = (String) jsonObject.get("backup_date");
	            result += "<b>Backup date:</b> " + date;
	            
	            String serial = (String) jsonObject.get("serial");
	            result += "<br><b>Serial:</b> " + serial;
	            
	            if(serial == null) {
	            	result = "Error: Invalid backup file.";
	            }
				
			} else {
				result = "Error: Invalid backup file.";
			}
        	
        	
        	
            
		
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
		
        return result;
	}
	
}
