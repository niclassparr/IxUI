package se.ixanon.ixui.server;

import java.io.*;
import java.net.*;

public class StreamerManager {
	
	public static String command(String arg) {
		return execute(arg, false);
	}
	
	public static String command(String arg, boolean isLog) {
		return execute(arg, isLog);
	}
	
	private static String execute(String arg, boolean isLog) {
		
		//System.out.println("command: " + arg);
		
		String xdoc = "error";
		
		try {
			//Socket sock = new Socket("192.168.0.79", 8100);
			Socket sock = new Socket("127.0.0.1", 8100);
			sock.setSoTimeout(10*1000);
			DataOutputStream out = new DataOutputStream(sock.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));
			out.writeBytes(arg + '\r');

			String msg = "";
			String s;
			while ((s = in.readLine()) != null) {
				
				if(!isLog) {
					msg += s;
				} else {
					msg += s + "\n";
				}				
				
			}
			sock.close();
			
			//System.out.println("msg: " + msg);
			
			if (msg.contains("100 OK")) {
				xdoc = msg.substring(6);
			}
		} catch (SocketTimeoutException e) {
			System.out.println("command SocketTimeoutException: " + e.getMessage());
		} catch (Exception e) {	
			System.out.println("command Exception: " + e.getMessage());
		}
		
		return xdoc;
	}
}
