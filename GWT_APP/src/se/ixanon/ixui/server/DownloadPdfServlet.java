package se.ixanon.ixui.server;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pdfbox.pdmodel.PDDocument;

import se.ixanon.ixui.server.singleton.ChannelsUtils;
import se.ixanon.ixui.server.singleton.DatabaseUtils;
import se.ixanon.ixui.server.singleton.InterfacesUtils;
import se.ixanon.ixui.server.singleton.NameValueUtils;
import se.ixanon.ixui.server.singleton.PDFGenerator;
import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.PDFRow;
import se.ixanon.ixui.shared.PDFValue;

//@WebServlet("/downloadPdf")
@SuppressWarnings("serial")
public class DownloadPdfServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    	String skey = req.getParameter("skey");
    	String username = req.getParameter("user");
    	
    	if(!DatabaseUtils.getInstance().validateSession(skey, username)) {
    		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
    	}
    	
    	HashMap<String, String> values = NameValueUtils.getInstance().getAll();
        
        ArrayList<Interface> interfaces = InterfacesUtils.getInstance().getInterfaces(null);
		Collections.sort(interfaces, Interface.Comparators.POS);
        
		ArrayList<Interface> modulators_net1 = InterfacesUtils.getInstance().getModNet(1);
		ArrayList<Interface> modulators_net2 = InterfacesUtils.getInstance().getModNet(2);
		
		ArrayList<PDFRow> channel_rows = ChannelsUtils.getInstance().getPDFChannels();
		
		
		
		ArrayList<PDFValue> pdf_interfaces = new ArrayList<PDFValue>();
		
		
		for (int i = 0; i < interfaces.size(); ++i) {
			if(interfaces.get(i).getActive()) {
				if(interfaces.get(i).getType().contains("dvb") || interfaces.get(i).getType().equals("infostreamer") || interfaces.get(i).getType().equals("dvbhdmi") || interfaces.get(i).getType().equals("hdmi2ip") || interfaces.get(i).getType().equals("hls2ip") || interfaces.get(i).getType().equals("webradio")) {
					
					pdf_interfaces.add(new PDFValue(interfaces.get(i).getPosition(), interfaces.get(i).getName()));
					
					//getInterfaceFreqPol(interfaces.get(i).getPosition(), interfaces.get(i).getType())
					
				}
			}
		}
		
		Date now = new Date();
    	Format formatter = new SimpleDateFormat("yyyy-MM-dd");
    	String date = formatter.format(now);		
		
    	
    	String serial = values.get("ui_serial");
    	
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"installation_"+serial+"_"+date+".pdf\"");

        try (PDDocument document = PDFGenerator.getInstance().generatePDF(values, pdf_interfaces, modulators_net1, modulators_net2, channel_rows);
             ServletOutputStream out = resp.getOutputStream()) {

            document.save(out);
        }
    }
}

