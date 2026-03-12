package se.ixanon.ixui.shared;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SessionKeys implements IsSerializable {

	public enum Type implements IsSerializable {
		INTERFACE_POS, INTERFACE_TYPE, MULTIBAND;
    }

	private String token;
    private HashMap<Type, String> keys;
    private boolean cloud;
    
    public SessionKeys() {

    }

	public SessionKeys(String token) {
		this.token = token;
		this.keys = new HashMap<>();
	}
	
	public boolean isCloud() {
		return cloud;
	}

	public void setCloud(boolean cloud) {
		this.cloud = cloud;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public HashMap<Type, String> getKeys() {
		return keys;
	}

	public void setKeys(HashMap<Type, String> keys) {
		this.keys = keys;
	}

	public String getKey(Type type) {
		if(keys.containsKey(type)) {
			return keys.get(type);
		}

		return null;
	}

	@Override
	public String toString() {
		return "SessionKeys [token=" + token + ", keys=" + keys + "]";
	}



}
