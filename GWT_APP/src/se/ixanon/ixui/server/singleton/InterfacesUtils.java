package se.ixanon.ixui.server.singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import se.ixanon.ixui.shared.Interface;

public class InterfacesUtils {

	private static InterfacesUtils instance = null;
	
	private InterfacesUtils() {
		
	}
	
	public static InterfacesUtils getInstance() {
		if (instance == null)
	        instance = new InterfacesUtils();
	    return instance;
	}
	
	public ArrayList<Interface> getInterfaces(String sessionKey) {		
		
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			String dbQuery = "SELECT * FROM interfaces ORDER BY pos;";
			
			conn = DatabaseUtils.getInstance().getDataSource().getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(dbQuery);
			
			while (rs.next()) {

				String type = rs.getString("type");
				
				String pos = rs.getString("pos");
				String name = rs.getString("name");
				
				boolean active = rs.getBoolean("active");
				boolean multiband = rs.getBoolean("multiband");
				
				interfaces.add(new Interface(pos, name, type, null, active, multiband));
				
			}
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
			
		}
		catch(Exception e){
			System.out.println("getInterfaces: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		for (int i = 0; i < interfaces.size(); ++i) {
			if(interfaces.get(i).getType().equals("mod")) {
				
				int network_num = NetworkUtils.getInstance().getModulatorNetworkNum(interfaces.get(i).getPosition());
				
				if(network_num == -1) {
					interfaces.get(i).setNetworkNum(1);
				} else {
					interfaces.get(i).setNetworkNum(network_num);
				}
				
			}
		}
		
		return interfaces;
	}
	
	public ArrayList<Interface> getModNet(int network_num) {
				
		ArrayList<Interface> modulators = new ArrayList<Interface>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			
			//String dbQuery = "SELECT pos FROM interfaces WHERE active is true AND type='mod' ORDER BY pos;";
			String dbQuery = "SELECT pos, active FROM interfaces "
					+ "INNER JOIN config_eqam on interfaces.pos = config_eqam.interface_pos "
					+ "WHERE type='mod' AND config_eqam.network_num = ? ORDER BY pos";
			
			conn = DatabaseUtils.getInstance().getDataSource().getConnection();
			stmt = conn.prepareStatement(dbQuery);
			stmt.setInt(1, network_num);
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				modulators.add(new Interface(rs.getString("pos"), rs.getBoolean("active")));
			}
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
			
		} catch(Exception e){
			System.out.println("getModNet: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		return modulators;
		
	}
	
}
