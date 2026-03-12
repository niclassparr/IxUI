package se.ixanon.ixui.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import se.ixanon.ixui.shared.Bitrate;
import se.ixanon.ixui.shared.Config;
import se.ixanon.ixui.shared.Emm;
import se.ixanon.ixui.shared.ForcedContent;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.IpMac;
import se.ixanon.ixui.shared.IpStatus;
import se.ixanon.ixui.shared.Media;
import se.ixanon.ixui.shared.NameValue;
import se.ixanon.ixui.shared.Package;
import se.ixanon.ixui.shared.Response;
import se.ixanon.ixui.shared.Route;
import se.ixanon.ixui.shared.Service;
import se.ixanon.ixui.shared.SessionKeys;
import se.ixanon.ixui.shared.StreamerStatus;
import se.ixanon.ixui.shared.TunerStatus;
import se.ixanon.ixui.shared.UnitInfo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface IxuiServiceAsync {

	void validateSession(String sessionKey, String username, AsyncCallback<Boolean> callback);

	void checkPersmission(String sessionKey, SessionKeys full_token, AsyncCallback<SessionKeys> callback);
	
	void login(String username, String password, AsyncCallback<String> callback);

	void logout(String sessionKey, AsyncCallback<Void> callback);

	void getInterfaces(String sessionKey, boolean isInterfaces, AsyncCallback<ArrayList<Interface>> callback);

	//void getInterfacePosition(String sessionKey, int interface_id, AsyncCallback<String> callback);
	
	void getInterfaceScanTime(String sessionKey, String interface_pos, AsyncCallback<Date> callback);
	
	void getConfig(String sessionKey, String interface_pos, String interface_type, AsyncCallback<Config> callback);
	
	void setConfig(String sessionKey, Config config, String interface_type, AsyncCallback<Response> callback);
	
	void getServices(String sessionKey, String interface_pos, AsyncCallback<ArrayList<Service>> callback);
	
	void saveServices(ArrayList<Service> services, String interface_type, String interface_pos, AsyncCallback<Response> callback);

	void getRoutes(String sessionKey, AsyncCallback<ArrayList<Route>> callback);

	void getBitrates(String sessionKey, AsyncCallback<ArrayList<Bitrate>> callback);

	void updateRoutes(String sessionKey, ArrayList<Route> routes, AsyncCallback<Response> callback);

	void getSettings(String sessionKey, AsyncCallback<ArrayList<NameValue>> callback);

	void updateSettings(String sessionKey, ArrayList<NameValue> values, AsyncCallback<Response> callback);

	void interfaceStatus(String interface_pos, AsyncCallback<String> callback);

	void interfaceSet(String sessionKey, String interface_pos, String interface_type, AsyncCallback<Response> callback);

	void interfaceScan(String interface_pos, AsyncCallback<Response> callback);

	void interfaceScanResult(String interface_pos, AsyncCallback<ArrayList<Service>> callback);

	void interfaceUpdate(AsyncCallback<Response> callback);

	void getInterface(String interface_pos, boolean with_status, AsyncCallback<Interface> callback);

	void interfaceStreamerStatus(String interface_pos, String interface_type, AsyncCallback<StreamerStatus> callback);

	void interfaceTunerStatus(String interface_pos, String interface_type, AsyncCallback<TunerStatus> callback);

	void interfaceCommand(String interface_pos, String command, AsyncCallback<Response> callback);

	void pushConfig(AsyncCallback<Response> callback);

	void interfaceLog(String interface_pos, AsyncCallback<String> callback);

	void isConfigChanged(String sessionKey, AsyncCallback<Boolean> callback);

	void getMaxBitrates(String interface_type, AsyncCallback<Integer> callback);

	void getEnabledType(String type, AsyncCallback<Boolean> callback);

	void getUnitInfo(AsyncCallback<UnitInfo> callback);

	void getNetworkSettings(AsyncCallback<HashMap<String, NameValue>> callback);

	void runCommand(String command, String filename, AsyncCallback<Response> callback);

	void savePDF(String filename, AsyncCallback<Response> callback);

	void getUpdatePackages(AsyncCallback<ArrayList<Package>> callback);

	void updatePackages(ArrayList<Package> packages, AsyncCallback<Response> callback);

	void runUpdateCommand(String command, AsyncCallback<Response> callback);

	void getUpdateResult(AsyncCallback<String> callback);

	void updateInterfaceMultibandType(String interface_pos, String interface_type, AsyncCallback<Response> callback);

	void getSessionValue(String key, AsyncCallback<String> callback);

	void runCommand2(String command, AsyncCallback<ArrayList<String>> callback);

	void saveDateTime(String sessionKey, boolean isRestart, String timezone, boolean ntp_mode, String date, String time, AsyncCallback<Response> callback);

	void getCloudDetails(String sessionKey, AsyncCallback<HashMap<String, String>> callback);

	void getInterfacesHls(String sessionKey, AsyncCallback<ArrayList<Interface>> callback);

	void saveHlsWizardServices(String sessionKey, ArrayList<Service> services, AsyncCallback<Response> callback);

	void getSettings(String sessionKey, boolean temp, AsyncCallback<HashMap<String, String>> callback);

	void updateSettings(String sessionKey, HashMap<String, String> settings, AsyncCallback<Response> callback);
	
	void updateSettingsNew(String sessionKey, HashMap<String, NameValue> settings, AsyncCallback<Response> callback);

	void getModulators(String sessionKey, AsyncCallback<HashMap<String, Integer>> callback);

	void saveModulatorsConfig(HashMap<String, Integer> modulators, AsyncCallback<Void> callback);

	void getForcedContents(String sessionKey, AsyncCallback<HashMap<Integer, ForcedContent>> callback);

	void saveForcedContents(ArrayList<ForcedContent> forced_contents, AsyncCallback<Response> callback);

	void getEnabledForcedContents(String sessionKey, AsyncCallback<ArrayList<ForcedContent>> callback);

	void saveForcedContentOverrideStatus(int id, int index, AsyncCallback<Void> callback);

	void getMedia(AsyncCallback<ArrayList<Media>> callback);

	void isSoftwareUpdate(AsyncCallback<Boolean> callback);

	void getInterfaceTypes(AsyncCallback<ArrayList<String>> callback);

	void updatePw(String username, String old_password, String new_password, AsyncCallback<Response> callback);

	void getNetworkStatus(String sessionKey, AsyncCallback<ArrayList<IpMac>> callback);

	void getNetworkStatus2(String sessionKey, AsyncCallback<HashMap<String, IpStatus>> callback);

	void setInterfaceInfoch(Config config, boolean isScan, AsyncCallback<Response> callback);

	void getInterfaceInfoch(String interface_pos, AsyncCallback<Config> callback);
	
	void getCurrentEmmList(String interface_pos, boolean isDsc, AsyncCallback<Emm> callback);

	void getJsonInfo(AsyncCallback<String> callback);
}
