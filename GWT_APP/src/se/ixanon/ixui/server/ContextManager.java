package se.ixanon.ixui.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextManager implements ServletContextListener {

	//public static final String ATTR_DATASOURCE = "datasource";
	public static String APP_NAME = "ixui";
	public static String CLOUD = "true";
	public static String FORCED_CONTENT = "true";
	public static String ENABLE_SOFTWARE_UPDATE = "false";
	public static String HLS_OUTPUT = "true";
	public static String PORTAL = "true";
	
	public ContextManager() {

	}
	
	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		
		ServletContext context = contextEvent.getServletContext();
		
		/*
		try {
			context.setAttribute(ATTR_DATASOURCE, (DataSource) (new InitialContext()).lookup("java:/comp/env/portalDataSourceLink"));
		} catch (NamingException e) {
			context.setAttribute(ATTR_DATASOURCE, null);
		}
		*/
		
		APP_NAME = context.getInitParameter("app_name");
		CLOUD = context.getInitParameter("enableCloud");
		FORCED_CONTENT = context.getInitParameter("enableForcedContent");
		ENABLE_SOFTWARE_UPDATE = context.getInitParameter("enableSoftwareUpdate");
		HLS_OUTPUT = context.getInitParameter("enableHlsoutput");
		PORTAL = context.getInitParameter("enablePortal");
	}
	
	public static boolean isCloud() {
		
		if(CLOUD != null) {
			if(CLOUD.equals("false")) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isHlsoutput() {
		
		if(HLS_OUTPUT != null) {
			if(HLS_OUTPUT.equals("false")) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isPortal() {
		
		if(PORTAL != null) {
			if(PORTAL.equals("false")) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isForcedContent() {
		
		if(FORCED_CONTENT != null) {
			if(FORCED_CONTENT.equals("false")) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isSoftwareUpdate() {
		
		if(ENABLE_SOFTWARE_UPDATE != null) {
			if(ENABLE_SOFTWARE_UPDATE.equals("true")) {
				return true;
			}
		}
		
		return false;
	}
}
