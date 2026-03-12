package se.ixanon.ixui.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import se.ixanon.ixui.shared.Media;

public class IxcAdminHelper {

	private static IxcAdminHelper instance = null;
	//private static final String DB_URL = "jdbc:postgresql://192.168.0.79:5432/ixcloud";
	private static final String DB_URL = "jdbc:postgresql://localhost:5432/ixcloud";
	private static final String DB_USER = "postgres";
	private static final String DB_PASSWORD = "";
	//private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private Connection Connection;
	private Statement Statement;
	private ResultSet Result;
	
	private IxcAdminHelper() {
		try {
			Class.forName("org.postgresql.Driver").newInstance();
			Connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }	
        catch (Exception e) {
			System.out.println("ERROR: Failed to connect to database: "+ e.toString());
        }
	}
	
	public static IxcAdminHelper getInstance() {
		if (instance == null)
	        instance = new IxcAdminHelper();
	    return instance;
	}
	
	public ArrayList<Media> getMedia() {
		
		ArrayList<Media> media = new ArrayList<Media>();
		
		try {
			String dbQuery = "SELECT videos.title, videos.internal_filename "
					+ "FROM ds_videos "
					+ "INNER JOIN videos ON ds_videos.video_id = videos.id " 
					+ "WHERE ds_videos.visible = true ORDER BY videos.id DESC;";

			Statement = Connection.createStatement();
			Result = Statement.executeQuery(dbQuery);
			
			while(Result.next()) {
				media.add(new Media(Result.getString("title"), Result.getString("internal_filename")));
			}
			
		}
		catch(Exception e) {
			System.out.println("Failed to prepare statement " + e.getMessage());
		}
		
		return media;
	}
	
}
