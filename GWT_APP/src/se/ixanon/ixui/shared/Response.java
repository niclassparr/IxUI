package se.ixanon.ixui.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Response implements Serializable {
	
	private boolean success;
	private String error;
	
	public Response(){
		
	}
	
	public Response(boolean success, String error){
		this.success =  success;
		this.error =  error;
	}
	
	public String getError() {
		return this.error;
	}
	
	public boolean isSuccess() {
		return this.success;
	}
	
	public void setError(String error) {
		this.error = error;
	}
}
