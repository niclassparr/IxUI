package se.ixanon.ixui.server.singleton;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NetworkUtils {

	private static NetworkUtils instance = null;
	
	private NetworkUtils() {
		
	}
	
	public static NetworkUtils getInstance() {
		if (instance == null)
	        instance = new NetworkUtils();
	    return instance;
	}
	
	public int getModulatorNetworkNum(String interface_pos) {
		
		int network_num = -1;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			String dbQuery = "SELECT network_num FROM config_eqam WHERE interface_pos = '"+interface_pos+"';";
			
			conn = DatabaseUtils.getInstance().getDataSource().getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(dbQuery);

			rs.next();
			network_num = rs.getInt("network_num");
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
		}
		catch(Exception e){
			System.out.println("NetworkUtils getModulatorNetworkNum: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		return network_num;
	}
	
	
	
}
