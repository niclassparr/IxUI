package se.ixanon.ixui.server.singleton;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

public class DatabaseUtils {

	private static DatabaseUtils instance = null;
	private static final String DB_URL = "jdbc:postgresql://localhost:5432/ixui";
	private static final String DB_IP = "localhost";
	//private static final String DB_URL = "jdbc:postgresql://192.168.0.79:5432/ixui";
	//private static final String DB_IP = "192.168.0.79";
	private static final String DB_NAME = "ixui";
	private static final int DB_PORT = 5432;
	
	private static final String DB_USER = "postgres";
	private static final String DB_PASSWORD = "";
	//private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private Connection connection;
	private static DataSource ds;
	
	private DatabaseUtils() {
		//System.out.println("Creating datassource");

		PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName(DB_IP);
        ds.setDatabaseName(DB_NAME);
        ds.setUser(DB_USER);
        ds.setPassword(DB_PASSWORD);
        ds.setPortNumber(DB_PORT);
        
        this.ds = ds;
	}
	
	public static DatabaseUtils getInstance() {
		if (instance == null)
	        instance = new DatabaseUtils();
	    return instance;
	}
	
	public void init() {
		try {
			Class.forName("org.postgresql.Driver").newInstance();
			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }	
        catch (Exception e) {
			System.out.println("ERROR: Failed to connect to database: "+ e.toString());
        }
	}
	
	
	
	
	public DataSource getDataSource() {
		return ds;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public boolean isDbConnected() {
	    try {
	        return connection != null && !connection.isClosed();
	    } catch (SQLException ignored) {
	    	
	    }

	    return false;
	}
	
	
	
	public boolean validateSession(String sessionKey, String username) {
		
		int uid = getUserIdFromSession(sessionKey);
		if (uid == -1) {
			//System.out.println("ERROR: Invalid Session");
			return false;
		}
		
		String name = getUserName(uid);
		
		if(username.equals(name)) {
			return true;
		}
		
		return false;
		
	}
	
	// Returns user id or -1 on failure.
	private int getUserIdFromSession(String sessionKey)	{
		
		int id = -1;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String dbQuery = "SELECT user_id FROM user_sessions WHERE session_key = ?;";

			conn = getDataSource().getConnection();
			stmt = conn.prepareStatement(dbQuery);
			stmt.setString(1, sessionKey);
			rs = stmt.executeQuery();
			
			rs.next();
			id = rs.getInt("user_id");
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
			
		}
		catch(Exception e){
			System.out.println("DatabaseUtils getUserIdFromSession ERROR: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		return id;
		
		
	}
	
	private String getUserName(int uid) {
		
		String name = null;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String dbQuery = "SELECT name FROM users WHERE id = ?;";

			conn = getDataSource().getConnection();
			stmt = conn.prepareStatement(dbQuery);
			stmt.setInt(1, uid);
			rs = stmt.executeQuery();
			
			rs.next();
			name = rs.getString("name");
			
			rs.close(); rs = null;
			stmt.close(); stmt = null;
			conn.close(); conn = null;
		}
		catch(Exception e){
			System.out.println("DatabaseUtils getUserName ERROR: " + e.getMessage());
		}
		finally {
			if(rs != null) {	try {rs.close();}   catch(SQLException e) {}}
			if(stmt != null) {	try {stmt.close();} catch(SQLException e) {}}
			if(conn != null) {	try {conn.close();} catch(SQLException e) {}}
		}
		
		return name;
	}
}
