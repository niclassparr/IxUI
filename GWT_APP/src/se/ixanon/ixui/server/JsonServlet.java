package se.ixanon.ixui.server;

import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FilenameUtils;

import se.ixanon.ixui.server.singleton.CommandUtils;
import se.ixanon.ixui.server.singleton.DatabaseUtils;
import se.ixanon.ixui.server.singleton.NameValueUtils;

@SuppressWarnings("serial")
public class JsonServlet extends HttpServlet {

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    	String skey = req.getParameter("skey");
    	String username = req.getParameter("user");
    	
    	if(!DatabaseUtils.getInstance().validateSession(skey, username)) {
    		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
    	}
    	
    	
    	
    	boolean cmd = CommandUtils.getInstance().runCommand("backup");
    	
    	if (!cmd) {
    	    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    	    return;
    	}
    	
    	Path jsonPath = Paths.get("/tmp/ixui_backup.json");
    	
    	if (!Files.exists(jsonPath)) {
    	    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    	    return;
    	}
    	
    	String serial = NameValueUtils.getInstance().getAll().get("ui_serial");
    	
    	Date now = new Date();
    	Format formatter = new SimpleDateFormat("yyyy-MM-dd");
    	String date = formatter.format(now);    	

    	resp.setContentType("application/json");
    	resp.setHeader("Content-Disposition", "attachment; filename=\"ixui_backup_"+serial+"_"+date+".json\"");

    	try (InputStream in = Files.newInputStream(jsonPath);
    		     OutputStream out = resp.getOutputStream()) {

    		    byte[] buffer = new byte[8192];
    		    int bytesRead;

    		    while ((bytesRead = in.read(buffer)) != -1) {
    		        out.write(buffer, 0, bytesRead);
    		    }
    		}

    	
    	
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		response.setContentType("text/html");
		//PrintWriter out = response.getWriter();

		boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
		if (!isMultipartContent) {
			return;
		}

		FileItemFactory factory = new DiskFileItemFactory();

		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setHeaderEncoding("UTF-8");

		FileUploadListener listener = new FileUploadListener();
		upload.setProgressListener(listener);

		HttpSession session = request.getSession();
		session.setAttribute("UPLOAD_LISTENER", listener);

		try {

			List<FileItem> fields = upload.parseRequest(request);
			Iterator<FileItem> item = fields.iterator();

			if (!item.hasNext()) {
				return;
			}

			while (item.hasNext()) {

				FileItem fileItem = item.next();
				InputStream stream = fileItem.getInputStream();

				if (fileItem.isFormField()) {

					// Process a regular form field
		        	String name = fileItem.getFieldName();
		        	String value = Streams.asString(stream);

		        	System.out.println("field name " + name);
		        	System.out.println("field value " + value);

				} else {
					
					String filename = FilenameUtils.getName(fileItem.getName());
					//System.out.println("filename: " + filename);
					
					
					String type = FilenameUtils.getExtension(filename);
					//System.out.println("type: " + type);

					String filepath = "/tmp/ixui_backup.json";
					Path target_path = Paths.get(filepath);
					//System.out.println("filepath: " + filepath);
					
					if(type.equalsIgnoreCase("json")) {
						
						Files.copy(stream, target_path, StandardCopyOption.REPLACE_EXISTING);
						
						//FileUtils.copyInputStreamToFile(stream, filepath);
					} else {
						System.out.println("File format not supperted");
					}

				}

			}

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			request.setAttribute("message", "There was an error: " + e.getMessage());
		}


	}


}


