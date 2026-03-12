package se.ixanon.ixui.server.singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class NameValueUtils {

	private static NameValueUtils instance = null;
	
	private NameValueUtils() {
		
	}
	
	public static NameValueUtils getInstance() {
		if (instance == null)
	        instance = new NameValueUtils();
	    return instance;
	}
	
	public HashMap<String, String> getAll() {

		HashMap<String, String> values = new HashMap<String, String>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			
			String dbQuery = "SELECT name, value FROM nv;";
			
			conn = DatabaseUtils.getInstance().getDataSource().getConnection();
			stmt = conn.prepareStatement(dbQuery);
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				values.put(rs.getString("name"), rs.getString("value"));
			}
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
			
		}
		catch(Exception e){
			System.out.println("NameValueUtils getAll: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		return values;
		
	}
	
	
	
	
	
}
