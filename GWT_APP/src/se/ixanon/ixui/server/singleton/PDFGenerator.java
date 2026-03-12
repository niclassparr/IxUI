package se.ixanon.ixui.server.singleton;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import se.ixanon.ixui.shared.Interface;
import se.ixanon.ixui.shared.PDFRow;
import se.ixanon.ixui.shared.PDFValue;

public class PDFGenerator {

	private static PDFGenerator instance = null;
	private static final int ROWS_PER_PAGE = 35;
	//private static final int TABLE_COLUMNS = 6;
	private static final float DATE_START_X = 450f;
	private static final float TABLE_START_X = 44.6f;
	private static final float TABLE_START_Y = 773.8f;
	private static final float TABLE_ROW_HEIGHT = 20;
	private static final float[] TABLE_COLUMN_OFFSET = {0, 50, 200, 250, 300, 350, 400};
	private PDFont font_reg = PDType1Font.HELVETICA;
	private PDFont font_bold = PDType1Font.HELVETICA_BOLD;
	
	// Generates document from Table object
    private void PDFGenerator() {

    }
    
    public static PDFGenerator getInstance() {
        if (instance == null) {
            instance = new PDFGenerator();
        }
        return instance;
    }
    
    public PDDocument generatePDF(HashMap<String, String> values, ArrayList<PDFValue> interfaces, 
    		ArrayList<Interface> modulators_net1, ArrayList<Interface> modulators_net2, 
    		ArrayList<PDFRow> channel_rows) throws IOException {
    	
        PDDocument doc = null;
        try {
            doc = new PDDocument();
            drawFirstPage(doc, values, interfaces);
            
            boolean dvbc_enable = Boolean.valueOf(values.get("dvbc_enable"));
            boolean dvbc_net2_enable = Boolean.valueOf(values.get("dvbc_net2_enable"));
            
            if(dvbc_enable) {
            	
            	String title = "";
            	            	
            	if(dvbc_net2_enable) {
            		title = "DVB-C NET1";
            	}
            	
            	drawOutputPage(doc, values, modulators_net1, title);
            }
            
            if(dvbc_net2_enable) {
            	drawOutputPage(doc, values, modulators_net2, "DVB-C NET2");
            }
            
            
            drawTable(doc, values, channel_rows);
            
            //doc.save("C:\\temp.pdf");
            
            
            //System.out.println(ContextManager.APP_NAME);

            //installation_"+filename+".pdf
            //doc.save("/srv/webapps/" + ContextManager.APP_NAME + "/doc/"+filename);
            
            
            
        }
        catch(IOException e) {
        	
        }
        
        return doc;
        
    }
    
    private void drawFirstPage(PDDocument doc, HashMap<String, String> values, ArrayList<PDFValue> interfaces) throws IOException {
    	
    	PDPage page = new PDPage(PDRectangle.A4);
		doc.addPage(page);
		PDPageContentStream contentStream = new PDPageContentStream(doc, page);
    	
    	Date now = new Date();
    	Format formatter = new SimpleDateFormat("yyyy-MM-dd");
    	String date = formatter.format(now);
    	
    	float nextY = TABLE_START_Y;
    	
    	showText(date, 12, false, DATE_START_X, TABLE_START_Y, contentStream);
    	showText("Installation Documentation", 16, true, TABLE_START_X, TABLE_START_Y, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("System Configuration", 13, true, TABLE_START_X, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Hostname:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("nw_hostname"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Serial Number:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("ui_serial"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	
    	for (int i = 0; i <= 9; i++) {
			
			if(!values.containsKey("nw_eth"+i+"_onboot")) {
				break;
			}
			
			showText("Eth"+i+" MAC:", 12, false, TABLE_START_X, nextY, contentStream);
			showText(values.get("nw_eth"+i+"_mac"), 12, false, TABLE_START_X+100, nextY, contentStream);
			    	
			showText("Eth"+i+" IP:", 12, false, TABLE_START_X+300, nextY, contentStream);
			showText(values.get("nw_eth"+i+"_ipaddr"), 12, false, TABLE_START_X+400, nextY, contentStream);
			nextY -= TABLE_ROW_HEIGHT;
			
    	}
    	
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Input Configuration", 13, true, TABLE_START_X, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	interfaces.add(0, new PDFValue("Interface", "Name"));
    	interfaces.add(0, new PDFValue("Interface", "Name"));
    	
    	for (int i = 0; i < interfaces.size(); ++i) {
    		
    		boolean bold = false;
    		
    		if(i < 2) {
    			bold = true;
    		} else {
    			bold = false;
    		}
    		
    		if (i % 2 == 0) {
    			// even
    			showText(interfaces.get(i).getName(), 12, bold, TABLE_START_X, nextY, contentStream);
            	showText(interfaces.get(i).getValue(), 12, bold, TABLE_START_X+100, nextY, contentStream);
            	
            	if(i == interfaces.size()-1) {
            		nextY -= TABLE_ROW_HEIGHT;
            	}
            	
    		} else {
    			// odd
    			showText(interfaces.get(i).getName(), 12, bold, TABLE_START_X+300, nextY, contentStream);
            	showText(interfaces.get(i).getValue(), 12, bold, TABLE_START_X+400, nextY, contentStream);
            	nextY -= TABLE_ROW_HEIGHT;
    		}
    	}
    	
    	nextY -= TABLE_ROW_HEIGHT;
    	/*   	
    	showText("Channel Configuration", 13, true, TABLE_START_X, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("See next page for channel list.", 12, false, TABLE_START_X, nextY, contentStream);
    	*/
    	contentStream.close();
    }
    
    private void drawOutputPage(PDDocument doc, HashMap<String, String> values, ArrayList<Interface> modulators, String title) throws IOException {
    	
    	String net2 = "";
    	
    	if(title.equals("DVB-C NET2")) {
    		net2 = "net2_";	
    	}
    	
    	PDPage page = new PDPage(PDRectangle.A4);
		doc.addPage(page);
		PDPageContentStream contentStream = new PDPageContentStream(doc, page);
    	
    	float nextY = TABLE_START_Y;
    	
    	showText("Output Configuration " + title, 13, true, TABLE_START_X, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Frequency:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("dvbc_"+net2+"freq"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Modulation:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("dvbc_"+net2+"qam"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Symbolrate:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("dvbc_"+net2+"symb"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Network ID:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("dvbc_"+net2+"netid"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Attenuation:", 12, false, TABLE_START_X, nextY, contentStream);
    	showText(values.get("dvbc_"+net2+"attenuation"), 12, false, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("Modulator Frequency", 13, true, TABLE_START_X, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	showText("MOD", 12, true, TABLE_START_X, nextY, contentStream);
    	showText("Frequency", 12, true, TABLE_START_X+100, nextY, contentStream);
    	nextY -= TABLE_ROW_HEIGHT;
    	
    	int start_freq = Integer.parseInt(values.get("dvbc_"+net2+"freq"));
    	
    	for (int i = 0; i < modulators.size(); ++i) {
    		
    		if(modulators.get(i).getActive()) {
    			showText(modulators.get(i).getPosition(), 12, false, TABLE_START_X, nextY, contentStream);
            	showText(""+(start_freq + (i*8000000)), 12, false, TABLE_START_X+100, nextY, contentStream);
            	nextY -= TABLE_ROW_HEIGHT;
    		}
    		
    	}
    	
    	contentStream.close();
    }
    
    private void showText(String text, float size, boolean bold, float x, float y, PDPageContentStream contentStream) throws IOException {
    	
    	contentStream.beginText();
		
		if(bold) {
			contentStream.setFont(font_bold, size);
		} else {
			contentStream.setFont(font_reg, size);
		}
		
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    	
    }
    
    private void drawTable(PDDocument doc, HashMap<String, String> values, ArrayList<PDFRow> rows) throws IOException {
    	
    	double rows_size = rows.size(); 
    	
    	double d = (double) rows_size / ROWS_PER_PAGE;
    	int numberOfPages = (int) Math.ceil(d);
    	
    	if(numberOfPages == 0) {
    		numberOfPages = 1;
    	}
    	
    	if(rows.size() == 0) {
    		numberOfPages = 0;
    	}
    	
    	boolean dvbc_enable_net1 = Boolean.valueOf(values.get("dvbc_enable"));
    	boolean dvbc_enable_net2 = Boolean.valueOf(values.get("dvbc_net2_enable"));
        boolean ip_enable = Boolean.valueOf(values.get("ip_enable"));
        
        /*
        if(dvbc_enable && ip_enable) {
        	return 2;
        } else {
        	if(dvbc_enable) {
            	return 0;
            }
        	
        	if(ip_enable) {
            	return 1;
            }
        }
        
        
        PDFRow top_row = new PDFRow("LCN", "Channel", "DSC", "MOD", null, null);
        
        if(dvbc_enable && ip_enable) {

        	
        	
        } else {
        	if(dvbc_enable) {
        		top_row = new PDFRow("LCN", "Channel", "DSC", "MOD", "SID Out", null);
            }
        	
        	if(ip_enable) {
        		top_row = new PDFRow("LCN", "Channel", "DSC", "MOD", null, "IP Out");
            }
        }
        */
        
        PDFRow top_row = new PDFRow("LCN", "Channel", "DSC", "MOD1", "MOD2", "SID", "IP");
        
        for (int pageCount = 0; pageCount < numberOfPages; pageCount++) {
    		PDPage page = new PDPage(PDRectangle.A4);
    		doc.addPage(page);
    		PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            //String[][] currentPageContent = getContentForCurrentPage(table, rowsPerPage, pageCount);
    		
    		float page_start_num = TABLE_START_Y; 
    		
    		if(pageCount == 0) {
    			
    			page_start_num = TABLE_START_Y-TABLE_ROW_HEIGHT;
    			
    			showText("Channel Configuration", 13, true, TABLE_START_X, TABLE_START_Y, contentStream);
    		}
    		
    		
    		int startRange = pageCount * ROWS_PER_PAGE;
    		int endRange = (pageCount * ROWS_PER_PAGE) + ROWS_PER_PAGE;
    		
    		if (endRange > rows.size()) {
                endRange = rows.size();
    		}
    		
    		writeContentLine(top_row, contentStream, page_start_num, true, dvbc_enable_net1, dvbc_enable_net2, ip_enable);
    		
    		ArrayList<PDFRow> currentPageContent = new ArrayList<PDFRow>(rows.subList(startRange, endRange));
    		
    		for (int i = 0; i < currentPageContent.size(); i++) {
    			float nextTextY = (page_start_num) - (i * TABLE_ROW_HEIGHT) - TABLE_ROW_HEIGHT;
    			writeContentLine(currentPageContent.get(i), contentStream, nextTextY, false, dvbc_enable_net1, dvbc_enable_net2, ip_enable);
                
    		}
    		contentStream.close();
    	}
    	
    }
    
    private void writeContentLine(PDFRow row, PDPageContentStream contentStream, float nextTextY, boolean header, boolean dvbc_enable_net1, boolean dvbc_enable_net2, boolean ip_enable) throws IOException {
    	
    	for (int i = 0; i < TABLE_COLUMN_OFFSET.length; i++) {
    		String text = row.getColumnText(i, dvbc_enable_net1, dvbc_enable_net2, ip_enable);
    		contentStream.beginText();
    		
    		if(header) {
    			contentStream.setFont(font_bold, 12);
    		} else {
    			contentStream.setFont(font_reg, 12);
    		}
    		
            contentStream.newLineAtOffset(TABLE_START_X + TABLE_COLUMN_OFFSET[i], nextTextY);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
    	}
    }
}
