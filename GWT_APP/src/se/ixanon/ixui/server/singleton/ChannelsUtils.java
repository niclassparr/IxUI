package se.ixanon.ixui.server.singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import se.ixanon.ixui.shared.PDFRow;

public class ChannelsUtils {

	private static ChannelsUtils instance = null;
	
	private ChannelsUtils() {
		
	}
	
	public static ChannelsUtils getInstance() {
		if (instance == null)
	        instance = new ChannelsUtils();
	    return instance;
	}
	
	public ArrayList<PDFRow> getPDFChannels() {
		
		ArrayList<PDFRow> channel_rows = new ArrayList<PDFRow>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String dbQuery = "SELECT routes.output_name, routes.lcn, routes.dsc_pos, routes.mod_pos, routes.mod_pos_net2, routes.out_sid, routes.out_ip FROM routes "
					+ "INNER JOIN services ON routes.service_key = services.key "
					+ "INNER JOIN interfaces ON services.interface_pos = interfaces.pos "
					+ "WHERE services.enable is true AND interfaces.active is true AND interface_pos IN (select pos from interfaces) ORDER BY lcn;";
			
			conn = DatabaseUtils.getInstance().getDataSource().getConnection();
			stmt = conn.prepareStatement(dbQuery);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				
				String service_name = rs.getString("output_name");
				String lcn = Integer.toString(rs.getInt("lcn"));
				String descrambler_pos = rs.getString("dsc_pos");
				String modulator_pos = rs.getString("mod_pos");
				String modulator_pos_net2 = rs.getString("mod_pos_net2");
				String out_sid = rs.getString("out_sid");
				String out_ip = rs.getString("out_ip");
				
				channel_rows.add(new PDFRow(lcn, service_name, descrambler_pos, modulator_pos, modulator_pos_net2, out_sid, out_ip));
				
				
			}
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
			
		}
		catch(Exception e){
			System.out.println("ChannelsUtils getPDFChannels: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		
		return channel_rows;
	}
	
	
	
}
