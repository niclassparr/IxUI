package se.ixanon.ixui.client;

import java.util.ArrayList;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;

import se.ixanon.ixui.shared.SessionKeys;

public class Session {

	private static Session instance;
	private String sessionKey;
	private String username;
	private IxuiServiceAsync rpcService;
	private HandlerManager presenterBus;
	private Timer timer = null;
	private Timer menu_timer = null;
	private HandlerManager appBus = new HandlerManager(null);
	private ArrayList<SessionKeys> session_keys = new ArrayList<>();
	private boolean cloud;
	
	public static synchronized Session getInstance() {
	    if (instance == null)
	        instance = new Session();
	    return instance;
	}
	
	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}
	
	public String getSessionKey(){
		return this.sessionKey;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public void setRpcService(IxuiServiceAsync rpcService) {
		this.rpcService = rpcService;
	}
	
	public IxuiServiceAsync getRpcService() {
		return rpcService;
	}
	
	public void setHandlerManager(HandlerManager presenterBus) {
		this.presenterBus = presenterBus;
	}
	
	public HandlerManager getHandlerManager() {
		return presenterBus;
	}
	
	public void setTimer(Timer timer) {
		this.timer = timer;
	}
	
	public Timer getTimer() {
		return this.timer;
	}

	public Timer getMenuTimer() {
		return menu_timer;
	}

	public void setMenuTimer(Timer menu_timer) {
		this.menu_timer = menu_timer;
	}
	
	public HandlerManager getAppBus() {
		return appBus;
	}
	
	public void popSessionKey() {

		if(!session_keys.isEmpty()) {
			int index = session_keys.size() -1;
			session_keys.remove(index);
		}

	}

	public void addSessionKey(SessionKeys session_key) {

		session_keys.add(session_key);

	}
	
	public SessionKeys getCurrentSessionKey() {

		int index = session_keys.size() -1;

		return session_keys.get(index);
	}
	
	public boolean isCloud() {
		return cloud;
	}

	public void setCloud(boolean cloud) {
		this.cloud = cloud;
	}
}
