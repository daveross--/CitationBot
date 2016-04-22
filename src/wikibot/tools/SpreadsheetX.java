package wikibot.tools;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class SpreadsheetX {
    // for use reading very large spreadsheet quickly.  Does not preserve CellType, CellStyle, Formulas...or anything really
    public SpreadsheetX(){}
    public LinkedList<LinkedHashMap<String,String>> readX(File f){
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
        try{
            out = processOneSheet(f.getAbsolutePath());
        }catch(Exception e){ e.printStackTrace(); }
        return out;
    }
    public LinkedList<LinkedHashMap<String,String>> processOneSheet(String filename) throws Exception {
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();

        XMLReader parser = fetchSheetParser(sst);

        // rId2 found by processing the Workbook
        // Seems to either be rId# or rSheet#
        InputStream sheet = r.getSheet("rId1");
        InputSource sheetSource = new InputSource(sheet);
        parser.parse(sheetSource);
        SheetHandler handler = (SheetHandler)parser.getContentHandler();
        sheet.close();        
        return handler.results;
    }
    
    public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        SheetHandler handler = new SheetHandler(sst);
        parser.setContentHandler(handler);
        return parser;
    }
    
    private class SheetHandler extends DefaultHandler {
        private SharedStringsTable sst;
        private String lastContents;
        private boolean nextIsString = false;
        private String cur_cell;
        private LinkedHashMap<String,String> headers;
        private LinkedHashMap<String,String> row;
        public LinkedList<LinkedHashMap<String,String>> results; // ignores blank lines, switch to TreeMap<Int,TreeMap<Str,Str>> later?

        private SheetHandler(SharedStringsTable sst) {
            this.sst = sst;
            this.results = new LinkedList<>();
            this.headers = new LinkedHashMap<>();
            this.row = new LinkedHashMap<>();
        }
        
        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            // c => cell
            if(name.equals("c")){                               
                // Figure out if the value is an index in the SST
                String cellType = attributes.getValue("t");
                if(cellType != null && cellType.equals("s")){ nextIsString = true;
                }else{ nextIsString = false; }                
                cur_cell = attributes.getValue("r"); // r => cell reference
            }            
            lastContents = ""; // Clear contents cache
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            // Process the last contents as required.
            // Do now, as characters() may be called more than once
            if(nextIsString) {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                nextIsString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if(name.equals("v")) {                
                Pattern p = Pattern.compile("([A-Z]+?)([0-9]+?)"); Matcher m = p.matcher(cur_cell);
                if(m.matches()){
                    if(m.group(2).compareTo("1") == 0){
                        this.headers.put(m.group(1), lastContents); // save the first row as headers
                    }else{
                        this.row.put(headers.get(m.group(1)), lastContents); // build a row from values
                    }
//                    System.out.println(m.group(1) + " " + m.group(2) + "\t" + lastContents);
                }else{ System.out.println("No match: " + cur_cell); }
            }
            if(name.equals("row") && row.size() > 1){
                // when we get to the end of the row we add the row to the results
                this.results.add(row);
                this.row = new LinkedHashMap<>();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            lastContents += new String(ch, start, length);
        }
    }  
}
