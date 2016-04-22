package wikibot.tools;

import java.awt.Desktop;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// @todo - add support for formulas, perhaps pass a hashmap which has index=>formula so they can be inserted inside of the data
// @todo - iterate over all column values before assigning a type to the cell

public class Spreadsheet {

    Workbook workbook;
    Sheet sheet;
    String sheetName;
    HashMap<String,CellStyle> styles;
                
    public Spreadsheet(File f){        
        try{
            this.workbook = WorkbookFactory.create(f);
            this.sheet = workbook.getSheetAt(0);            
        }catch(IOException | InvalidFormatException e){}        
    }
    public Spreadsheet(FileInputStream f) throws IOException{        
        try{
            this.workbook = WorkbookFactory.create(f);
            this.sheet = workbook.getSheetAt(0);            
        }catch(IOException | InvalidFormatException e){
        }finally{
            f.close(); // does this unlock the file?
        }        
    }
    // create a sheet from a bunch of rows              
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printer(name, rows, cols, true);
    }                  
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows, String sheetName){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printer(name, rows, cols, sheetName, true);
    }             
    public static void printer(File f, LinkedList<LinkedHashMap<String,String>> rows){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printer(f, rows, cols, true);
    }    
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows, boolean open){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printer(name, rows, cols, open);
    }
    public static void printer(File f, LinkedList<LinkedHashMap<String,String>> rows, boolean open){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printer(f, rows, cols, open);
    }    
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols){
        printer(name, rows, cols, true);
    }
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, boolean open){
        File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx");
        printer(f, rows, cols, open);
    }
    public static void printer(String name, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, String sheetName, boolean open){
        File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx");
        printer(f, rows, cols, sheetName, open);
    }
    public static void printer(File f, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, boolean open){
        printer(f, rows, cols, f.getName(), open);
    }
    public static void printer(File f, Set<String> set, String col){
        LinkedList<LinkedHashMap<String,String>> rows = new LinkedList<>();
        for(String s : set){ 
            LinkedHashMap<String,String> map = new LinkedHashMap<>(); 
            map.put(col, s);
            rows.add(map);
        }
        printer(f, rows);
    }
    public static void printer(String name, Set<String> set, String col){
        File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx");
        printer(f, set, col);        
    }
    public static void printer(File f, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, String sheetName, boolean open){
        Workbook wb = new XSSFWorkbook();
        if(sheetName.contains("\\") && sheetName.contains(".")){ sheetName = sheetName.substring(sheetName.lastIndexOf("\\")+1, sheetName.lastIndexOf(".")); }
        Sheet out = wb.createSheet(sheetName);

        Font font = wb.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short)9);
        
        CellStyle number = wb.createCellStyle();
            number.setDataFormat((short)2);
            number.setFont(font);
        CellStyle dollar = wb.createCellStyle();
            dollar.setDataFormat((short)8);
            dollar.setFont(font);
        CellStyle string = wb.createCellStyle();
            string.setDataFormat((short)0);   
            string.setFont(font);
        CellStyle integer = wb.createCellStyle();
            integer.setDataFormat((short)1);   
            integer.setFont(font);
            
        int r_count = 0;
        int max_col = 1;
        for(LinkedHashMap<String,String> row : rows){
            Row r = out.createRow(r_count++);
            int c_count = 0;
            
            if(r_count==1){
                for(String key : cols){
                    Cell c = r.createCell(c_count++);
                        c.setCellValue(key.replaceAll("_"," ").trim());
                        c.setCellStyle(string);
                }
                r = out.createRow(r_count++);
            }
            
            c_count = 0;            
            for(String key : cols){
                Cell c = r.createCell(c_count++);                 
                String val = row.get(key);
                switch (getType(val)){
                    case BLANK :
                        c.setCellType(Cell.CELL_TYPE_BLANK);
                        c.setCellStyle(string); 
                        break;
                    case NUMERIC :
                        c.setCellType(Cell.CELL_TYPE_NUMERIC);
                        c.setCellStyle(integer);
                        val = val.replaceAll("\\$", ""); // remove dollar signs and whitespace                        
                        c.setCellValue(Double.valueOf(val));
                        break;
                    default :
                        c.setCellType(Cell.CELL_TYPE_STRING);
                        c.setCellStyle(string); 
                        c.setCellValue(CRLF(val));                                               
                        break;
                }
            }
            int r_max = (int)r.getLastCellNum();
            if(r_max > max_col){ max_col = r_max; }
        }  
        for(int i=0; i<max_col; i++){ out.autoSizeColumn(i); } // auto-resize the columns based on data
        
        try{
            if(f.exists() && f.canWrite()){ f.delete(); }  
            f.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(f);
            wb.write(fileOut); 
            if(open){ Desktop.getDesktop().open(f); }
        }catch(IOException e){ e.printStackTrace(); }
    }
    public static void printerX(String name, LinkedList<LinkedHashMap<String,String>> rows){
        File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx"); 
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerX(f, rows, cols, true);
    }    
    public static void printerX(String name, LinkedList<LinkedHashMap<String,String>> rows, boolean open){
        File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx"); 
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerX(f, rows, cols, open);
    }    
    public static void printerX(File f, LinkedList<LinkedHashMap<String,String>> rows){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerX(f, rows, cols, true);
    } 
    public static void printerX(String sheet_name, File f, LinkedList<LinkedHashMap<String,String>> rows){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerX(sheet_name, f, rows, cols, true);
    } 
    public static void printerX(File f, LinkedList<LinkedHashMap<String,String>> rows, boolean open){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerX(f, rows, cols, open);
    }       
    public static void printerX(File f, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, boolean open){
        String name = f.getName();
        String sheet_name = name; 
        if(name.contains("\\") && name.contains(".")){ sheet_name = name.substring(name.lastIndexOf("\\")+1, name.lastIndexOf(".")); }
        printerX(sheet_name, f, rows, cols, open);
    }
    public static void printerX(String sheet_name, File f, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols, boolean open){
        // Streaming workbook writer for really big resultsets
        long starttime = System.nanoTime();
//        System.out.println("Writing " + rows.size() + " records to streaming workbook.");
        try{
            SXSSFWorkbook wb = new SXSSFWorkbook(1000);
//            String name = f.getName();
//            String sheet_name = name; 
//            if(name.contains("\\") && name.contains(".")){ sheet_name = name.substring(name.lastIndexOf("\\")+1, name.lastIndexOf(".")); }
            Sheet out = wb.createSheet(sheet_name);

            Font font = wb.createFont();
                font.setFontName("Calibri");
                font.setFontHeightInPoints((short)9);
                   
            
            CellStyle number = wb.createCellStyle();
                number.setDataFormat((short)2);
                number.setFont(font);
            CellStyle dollar = wb.createCellStyle();
                dollar.setDataFormat((short)8);
                dollar.setFont(font);
            CellStyle string = wb.createCellStyle();
                string.setDataFormat((short)0);   
                string.setFont(font);
            CellStyle integer = wb.createCellStyle();
                integer.setDataFormat((short)1);  
                integer.setFont(font); 

            int r_count = 0;
            int max_col = 1;
            for(LinkedHashMap<String,String> row : rows){
                Row r = out.createRow(r_count++);
                int c_count = 0;

                if(r_count==1){
                    for(String key : cols){
                        Cell c = r.createCell(c_count++);
//                            c.setCellValue(key.replaceAll("_"," ").trim()); // if we remove the underscores the cached queries fail... probably make a second function
                            c.setCellValue(key);
                            c.setCellStyle(string);
                    }
                    r = out.createRow(r_count++);
                }

                c_count = 0;            
                for(String key : cols){
                    Cell c = r.createCell(c_count++);                 
                    String val = row.get(key);
                    if(val == null){ val = ""; }
                    switch (getType(val)){
                        case BLANK :
                            c.setCellType(Cell.CELL_TYPE_BLANK);
                            c.setCellStyle(string); 
                            break;
                        case NUMERIC :
                            c.setCellType(Cell.CELL_TYPE_NUMERIC);
                            c.setCellStyle(integer);
                            c.setCellValue(Double.valueOf(val));
                            break;
                        default :
                            c.setCellType(Cell.CELL_TYPE_STRING);
                            c.setCellStyle(string); 
                            c.setCellValue(val);                                               
                            break;
                    }
                }
                int r_max = (int)r.getLastCellNum();
                if(r_max > max_col){ max_col = r_max; }
            }  
//            for(int i=0; i<max_col; i++){ out.autoSizeColumn(i); } // auto-resize the columns based on data
        
//            File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx"); 
                       
            try(FileOutputStream fos = new FileOutputStream(f)){
                wb.write(fos);
            }      
            
            System.out.println("Completed writing workbook, " + ((System.nanoTime() - starttime)/1000000) + "ms elapsed.  Resulting file is " + f.length() + ".");
            
            wb.dispose();               
            if(open){ Desktop.getDesktop().open(f); }
        }catch(Exception e){ e.printStackTrace(); }
    }
   
    public static File printerTDF(LinkedList<LinkedHashMap<String,String>> rows){
        File f = new File("C:\\Users\\davidr\\AppData\\Local\\Temp\\printerTDF\\" + System.currentTimeMillis() + ".txt");
        System.out.println(f.getAbsolutePath());
        printerTDF(f, rows);
        return f;
    }
    public static void printerTDF(File f, LinkedList<LinkedHashMap<String,String>> rows){
        LinkedList<String> cols = new LinkedList<>();
        cols.addAll(rows.get(0).keySet());
        printerTDF(f, rows, cols);
    }
    public static void printerTDF(File f, LinkedList<LinkedHashMap<String,String>> rows, LinkedList<String> cols){        
        try{
            if(f.exists()){ f.delete(); }
            f.createNewFile();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                for(LinkedHashMap<String,String> row : rows){
                    String r = "";
                    for(String k : row.keySet()){
                        r = r + row.get(k).replace("\\t"," ") + "\t";
                    }
                    r = r + "\r\n";
                    bw.write(r);
                }
            }
        }catch(Exception e){ e.printStackTrace(); }
    }
    
    public static void crossTab(String name, LinkedList<LinkedHashMap<String,String>> data, String header, String value){
        // creates an excel spreadsheet in the cross-tab format with a specified row representing the column headers

        // restructure date
        LinkedHashSet<String> prime_headers = new LinkedHashSet<>();    // this is where we keep track of which column headers we have natively
//        HashMap<String,Integer> col_nums = new HashMap<>();  // this is where we keep track of which value represents which column
        LinkedHashMap<String,String> first_row = data.get(0);
        for(String s : first_row.keySet()){ if(s.compareTo(header) != 0 && s.compareTo(value) != 0){ prime_headers.add(s); } }  // generate a list of prime headers
        TreeSet<String> t_sub_headers = new TreeSet<>();
        for(LinkedHashMap<String,String> row : data){ t_sub_headers.add(row.get(header)); } // generate an ordered list of all values in the sub_header column
        
            
        // build workbook
        Workbook wb = new XSSFWorkbook();
        Sheet out = wb.createSheet(name);

        CellStyle string = wb.createCellStyle();
            string.setDataFormat((short)0);   
        CellStyle integer = wb.createCellStyle();
            integer.setDataFormat((short)1);   
            
        int r_count = 0;
        int max_col = 1;
        
        LinkedHashMap<String, HashMap<String,Object>> crosstab_data = new LinkedHashMap<>(); 
        for(LinkedHashMap<String,String> row : data){
            String hash = "";
            for(String h : prime_headers){ hash = hash + row.get(h) + "||"; } // generates the hashed value of row headers
            
            HashMap<String,Object> values = new HashMap<>();
            if(crosstab_data.containsKey(hash)){ values = crosstab_data.get(hash); }
            
            if(values.containsKey(row.get(header))){ /* this is where we will handle functions (SUM, AVG) */ }
            values.put(row.get(header), row.get(value));
            crosstab_data.put(hash, values);            
        }
        
        for(String hash : crosstab_data.keySet()){
            HashMap<String,Object> row = crosstab_data.get(hash);
            Row r = out.createRow(r_count++);
            int c_count = 0;
            
            LinkedHashSet<String> t_headers = prime_headers;
            t_headers.addAll(t_sub_headers);
            if(r_count==1){
                for(String key : t_headers){
                    Cell c = r.createCell(c_count++);
                        c.setCellValue(key);
                        c.setCellStyle(string);
                }
                r = out.createRow(r_count++);
            }
            
            c_count = 0;    
            String[] row_heads = hash.split("\\|\\|");
//            System.out.println(hash + "\t" + row_heads.length + t_sub_headers);
            for(String val : row_heads){
                Cell c = r.createCell(c_count++);                 
//                String val = row.get(key).toString();
                switch (getType(val)){
                    case BLANK :
                        c.setCellType(Cell.CELL_TYPE_BLANK);
                        c.setCellStyle(string); 
                        break;
                    case NUMERIC :
                        c.setCellType(Cell.CELL_TYPE_NUMERIC);
                        c.setCellStyle(integer);
                        val = val.replaceAll("\\$", ""); // remove dollar signs and whitespace
                        c.setCellValue(Double.valueOf(val));
                        break;
                    default :
                        c.setCellType(Cell.CELL_TYPE_STRING);
                        c.setCellStyle(string); 
                        c.setCellValue(val);                                               
                        break;
                }
            }
            c_count = row_heads.length;
            for(String col_head : t_sub_headers){
                Cell c = r.createCell(c_count++);  
                if(row.containsKey(col_head)){
                    String val = row.get(col_head).toString();
                    switch (getType(val)){
                        case BLANK :
                            c.setCellType(Cell.CELL_TYPE_BLANK);
                            c.setCellStyle(string); 
                            break;
                        case NUMERIC :
                            c.setCellType(Cell.CELL_TYPE_NUMERIC);
                            c.setCellStyle(integer);
                            c.setCellValue(Double.valueOf(val));
                            break;
                        default :
                            c.setCellType(Cell.CELL_TYPE_STRING);
                            c.setCellStyle(string); 
                            c.setCellValue(val);                                               
                            break;
                    }     
                }
            }
                        
            int r_max = (int)r.getLastCellNum();
            if(r_max > max_col){ max_col = r_max; }
        }  
        for(int i=0; i<max_col; i++){ out.autoSizeColumn(i); } // auto-resize the columns based on data
        
        try{
            File f = new File(System.getProperty("user.home") + "\\Documents\\" + name + ".xlsx");
            if(f.exists() && f.canWrite()){ f.delete(); }  
            f.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(f);
            wb.write(fileOut); 
            Desktop.getDesktop().open(f);
        }catch(IOException e){ e.printStackTrace(); }
    }
    
    public Row getRow(int r){
        return sheet.getRow(r);
    }  
    public String[] cellsAsStrings(int r){ // returns the values of the cells in row r as Strings
        Row R = sheet.getRow(r);
        String[] out = new String[R.getLastCellNum()];
        for(short s = 0; s < R.getLastCellNum(); s++){
            out[s] = cellToString(R.getCell(s));
        }
        return out; 
    }
    public int rowCount(){
        return sheet.getLastRowNum();
    }
    public static Comment makeComment(String str, Sheet s, Cell c, int w, int h, Font f){
        CreationHelper factory = s.getWorkbook().getCreationHelper();
        Drawing d = s.createDrawingPatriarch();
        ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(c.getColumnIndex());
            anchor.setCol2(c.getColumnIndex()+w);
            anchor.setRow1(c.getRowIndex());
            anchor.setRow2(c.getRowIndex()+h);
        
        Comment comment = d.createCellComment(anchor);
        RichTextString rts = factory.createRichTextString(str);
            rts.applyFont(f);
            comment.setString(rts);
            comment.setAuthor("auto");
        
        c.setCellComment(comment);
            
        return comment;
    }
    public static void makeComment(Row r, int c, String cmt, int w, int h, Font f){
        r.getCell(c).setCellComment(Spreadsheet.makeComment(cmt, r.getSheet(), r.getCell(c), w, h, f));        
    }
    
    public static String cellToString(Cell cell){
        String out;
        if(cell == null){ return ""; }
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                out = cell.getRichStringCellValue().getString();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    out = cell.getDateCellValue().toString();
                } else {
                    double d = cell.getNumericCellValue();
//                    out = (new BigDecimal(d).toPlainString());
                    out = (String.format("%.12f",d));
                    if(out.contains(".")){ out = out.replaceAll("[0]*$", "").replaceAll("\\.$", ""); } // removing trailing zeroes if decimal number
                }
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                out = Boolean.toString(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_FORMULA:
                out = cell.getCellFormula();
                break;
            default:
                out = "";
        }        
        return (out);
    }
    public static String CV(Cell cell){ // alias
        return cellToString(cell);
    }
    
    private static String fmt(String in){
        if(in.contains(".")){
            in = in.substring(0,in.indexOf("."));
        }
        return in;
    }    
    private static CellType getType(String in){
        // returns the best type for the string input
        if(in == null || in.isEmpty()){ return CellType.STRING; }
        in = in.trim();
        
        if(in.length() == 0){ return CellType.BLANK; }
        
        Pattern I = Pattern.compile("^([\\-\\$]*)(\\s)[0-9]+?");
        Matcher m = I.matcher(in);
        if(m.matches()){ return CellType.NUMERIC; }
        
        Pattern D = Pattern.compile("^([\\-\\$]*)(\\s)[0-9\\.]+?");
        m = D.matcher(in);
        if(m.matches() && (in.lastIndexOf(".") == in.indexOf("."))){ return CellType.NUMERIC; }
        
        // Deeper double test for 9.99000E7 etc.
        try{
            in = in.replaceAll("[\\$](\\s*)", ""); // remove dollar signs and whitespace
            Double d = Double.valueOf(in);
            if(!d.isNaN()){ return CellType.NUMERIC; }
        }catch(Exception e){}
        
        // if we can't figure it out just use String
        return CellType.STRING;
    }
    
    public static void makeCell(Row r, int c, String val){
        makeCell(r, c, val, null, getType(val));
    }
    public static void makeCell(Row r, int c, String val, CellStyle cs, CellType type){
        // helper function which creates a cell from a value
        Cell cell = r.createCell(c);
        cell.setCellType(type.ordinal());
        if(cs != null){ cell.setCellStyle(cs); }

        if(type.ordinal() == CellType.NUMERIC.ordinal()){
            cell.setCellValue(D(val));  // numeric value
        }else{
            cell.setCellValue(val);
        }
    }
    public static void makeCell(Row r, int c, double val, CellStyle cs){
        // helper function which creates a numeric cell from a value
        Cell cell = r.createCell(c);
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellValue(val);  
    }        
    public static void makeCell(Row r, int c, String val, CellStyle cs){ // creates a cell, agnostic about cell type.  IF the val is a valid number create a numeric cell
        try{
            double d = Double.parseDouble(val);
            Spreadsheet.makeCell(r, c, d, cs);
        }catch(Exception e){
            Spreadsheet.makeCell(r, c, val, cs,CellType.STRING);
        }
    }
    
    
    public static void makeColCountDownCell(Row r, int c, int cellsDown, CellStyle cs){
        // creates a cell which sums the cells above itself from one above to cellsUp above
        Cell cell = r.createCell(c);
        int row = r.getRowNum();
        String col = Spreadsheet.colNameFromNum(c);        
        
        String formula = "IFERROR(COUNT(" + col + (row+2) + ":" + col + (row + cellsDown + 1) + "),\"-\")";
        
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);          
    }
    public static void makeColCountIfDownCell(Row r, int c, String criteria, int cellsDown, CellStyle cs){
        // creates a cell which sums the cells above itself from one above to cellsUp above
        Cell cell = r.createCell(c);
        int row = r.getRowNum();
        String col = Spreadsheet.colNameFromNum(c);        
        
        String formula = "IFERROR(COUNTIF(" + col + (row+2) + ":" + col + (row + cellsDown + 1) + "," + criteria + "),\"-\")";
        
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);          
    }
    public static void makeColSumCell(Row r, int c, int cellsUp, CellStyle cs){
        // creates a cell which sums the cells above itself from one above to cellsUp above
        Cell cell = r.createCell(c);
        int row = r.getRowNum();
        String col = Spreadsheet.colNameFromNum(c);        
        
        String formula = "IFERROR(SUM(" + col + (row) + ":" + col + (row - cellsUp + 1) + "),\"-\")";
        
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);          
    }
    public static void makeColSumCellTB(Row r, int c, int a, int b, CellStyle cs){
        // creates a cell which sums the cells in the column between a and b
        Cell cell = r.createCell(c);
        String col = Spreadsheet.colNameFromNum(c);  
        if(a<b){ int x = a; a = b; b = x; } 
        
        String formula = "IFERROR(SUM(" + col + (a) + ":" + col + (b) + "),\"-\")";
        
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);          
    }    
    public static void makeRowIndexCell(Row r, int c, int offsetNumerator, int offsetDenominator, CellStyle cs){
        // creates a cell which displays the index of two cells
        Cell cell = r.createCell(c);
        int row = r.getRowNum() + 1;
        String num = Spreadsheet.colNameFromNum(c + offsetNumerator); 
        String den = Spreadsheet.colNameFromNum(c + offsetDenominator);        
        
        String formula = "IFERROR(" + num + row + "/" + den + row + ",\"-\")";
        
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);          
    }
    
    public static void makeFormulaCell(Row r, int c, String formula, CellStyle cs){
        // helper function which creates a formula cell
        Cell cell = r.createCell(c);
        cell.setCellType(CellType.NUMERIC.ordinal());
        cell.setCellStyle(cs);
        cell.setCellFormula(formula);  
    }
    
    public static CellStyle makeStyle(Workbook wb, String format, Font font, short color){
        CellStyle style = wb.createCellStyle();
        DataFormat dataformat = wb.createDataFormat();
            style.setDataFormat(dataformat.getFormat(format));                            
            style.setFont(font);
            if(color >= 0){ 
                style.setFillForegroundColor(color);
                style.setFillPattern(CellStyle.SOLID_FOREGROUND); 
            }
        return style;
    }
    public static CellStyle makeStyle(Workbook wb, String format, Font font){
        return makeStyle(wb, format, font, (short)-1);
    }
    public static XSSFCellStyle makeStyleX(Workbook wb, String format, Font font, short color){
        XSSFCellStyle style = (XSSFCellStyle)wb.createCellStyle();
        
        DataFormat dataformat = wb.createDataFormat();
            style.setDataFormat(dataformat.getFormat(format));                            
            style.setFont(font);
            if(color >= 0){ 
                style.setFillForegroundColor(color);
                style.setFillPattern(CellStyle.SOLID_FOREGROUND); 
            }
        return style;
    }    
    public static XSSFCellStyle makeStyleX(Workbook wb, String format, Font font){
        return makeStyleX(wb, format, font, (short)-1);
    }
            
    public static String readCell(Cell cell){
        if(cell.getCellType() == Cell.CELL_TYPE_STRING){
            return cell.getStringCellValue();
        }else if(cell.getCellType() == Cell.CELL_TYPE_NUMERIC){
            double d = cell.getNumericCellValue();            
            return Integer.toString((int)(d - d%1));
        }else{
            return "";
        }
    }
    
    public static XSSFColor color(int r, int g, int b){
        return new XSSFColor(new java.awt.Color(r,g,b));
    }
    
    public static Font makeFont(Workbook wb, String name){
        return makeFont(wb, name, (short)10, (short)Font.BOLDWEIGHT_NORMAL, (short)HSSFColor.BLACK.index, false);        
    }
    public static Font makeFont(Workbook wb, String name, short size){
        return makeFont(wb, name, size, (short)Font.BOLDWEIGHT_NORMAL, (short)HSSFColor.BLACK.index, false);
    }
    public static Font makeFont(Workbook wb, String name, short size, short weight, short color, boolean italic){
        Font font = wb.createFont();
        font.setFontName(name);
        font.setFontHeightInPoints(size);
        font.setBoldweight(weight);
        font.setColor(color);
        font.setItalic(italic);
        
        return font;
    }
    
    public static void fill(CellStyle style, short color){
        style.setFillForegroundColor(color);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
    }  
    public static void fill(XSSFCellStyle style, XSSFColor color){
        style.setFillForegroundColor(color);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
    }
    public static CellStyle allBorders(CellStyle style, short color){
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(color);
        style.setTopBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
        
        return style;
    }
    public static CellStyle allBordersDoubleTop(CellStyle style, short color){
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_DOUBLE);
        style.setBottomBorderColor(color);
        style.setTopBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
        
        return style;
    }    
    public static CellStyle TBBorders(CellStyle style, short color){
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(color);
        style.setTopBorderColor(color);        
        return style;
    }
    public static CellStyle TBorders(CellStyle style, short color){
//        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
//        style.setBottomBorderColor(color);
        style.setTopBorderColor(color);        
        return style;
    }
    public static CellStyle BBorders(CellStyle style, short color){
        style.setBorderBottom(CellStyle.BORDER_THIN);
//        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(color);
//        style.setTopBorderColor(color);        
        return style;
    }   
    
    public static CellStyle align(CellStyle style, short alignment){
        style.setAlignment(alignment);
        return style;
    }    
    public static String fileSafe(String in){
        return in.replaceAll("[^A-Za-z0-9_]+", " ").trim();
    }
    public static void fileWrite(Workbook wb, String file){
        try{
            File f = new File(file);
            if(f.exists()){ 
                String a = file.substring(0, file.lastIndexOf("."));
                String b = file.substring(file.lastIndexOf("."));
                f.renameTo(new File(a + " [" + (System.currentTimeMillis()) + "]" + b));
            }
            f.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(f);
            wb.write(fileOut); 
            Desktop.getDesktop().open(f);
        }catch(IOException e){ 
            e.printStackTrace();
        }
    }
    
    public static void makeSheet(Sheet sheet, LinkedList<LinkedHashMap<String,String>> data){
        
        int r = 0;
        Row row;
        int c = 0;
        Cell cell;
        
        // Iterate over all data and find the appropriate cell type which will fit the whole column
        // If we find any cell which does is non-numeric we call the whole column a String        
        // Numeric == 0, String == 1
        TreeMap<Integer,Integer> types = new TreeMap<>();
        for(LinkedHashMap<String,String> d : data){
            if(!types.isEmpty()){
                for(String k : d.keySet()){        
                    if(types.containsKey(c)){ 
                        if(types.get(c) == 0){                        
                            if(getType(d.get(k)) == CellType.STRING){
                                types.put(c, 1);
                            }                 
                        }
                    }else{
                        if(getType(d.get(k)) == CellType.STRING){
                            types.put(c, 1); // set column type as string
                        }else{
                            types.put(c, 0); // set column type as numeric
                        }
                    }
                    c++;
                }
                c=0;
            }else{
                types.put(0, 0); // initialize, allow us to skip the header row
            }
        }
        // Column Headers on the first row
        row = sheet.createRow(r++);
        c = 0;
        for(String s : data.get(0).keySet()){             
            cell = row.createCell(c++);
            cell.setCellValue(s);
        }
        
        // Write data into sheet
        for(LinkedHashMap<String,String> d : data){
            c = 0;
            row = sheet.createRow(r++);
            for(String s : d.keySet()){
                cell = row.createCell(c++);
                if(types.get(c-1) == 0){
                    cell.setCellType(CellType.NUMERIC.ordinal());
                    cell.setCellValue(D(G(d,s)));
                }else{
                    cell.setCellType(CellType.STRING.ordinal());
                    cell.setCellValue(G(d,s));
                }
            }
        }        
    }
    
    public static LinkedList<LinkedHashMap<String,String>> read(File f){
        return read(f.getAbsolutePath());
    }
    public static LinkedList<LinkedHashMap<String,String>> read(String fileName){
        // read the first worksheet of an excel doc into a table-type collection using the first row as keys        
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
        
        try{
            File f = new File(fileName);
            if(f.exists() && f.canRead()){
                Workbook wb = new XSSFWorkbook(new FileInputStream(f));
                Sheet sheet = wb.getSheetAt(0);                
                LinkedList<String> headers = new LinkedList<>();
                boolean first = true;
                
                // iterate over rows
                Iterator<Row> rows = sheet.rowIterator();
                while(rows.hasNext()){                    
                    LinkedHashMap<String,String> map = new LinkedHashMap<>();
                    
                    Row row = rows.next();
                    // iterate over cells
                    Iterator<Cell> cells = row.cellIterator();
                    Iterator<String> heads = headers.listIterator();
                    boolean blank = true; 
                    
                    while(cells.hasNext()){
                        Cell cell = cells.next();
                        if(first){ // if this is the first row we capture the values as headers to use for keys
                            headers.add(Spreadsheet.cellToString(cell));
                        }else{ // if not first we add the values to the map using the keys from the first row
                            String c = Spreadsheet.cellToString(cell);
                            if(c.length() > 0){ blank &= false; }
                            map.put(heads.next(), c);
                        }
                    }
                    if(!first && !blank){ out.add(map); }
                    first &= false;
                }
            }else{ System.err.println("Failed to read file."); return null; }            
        }catch(Exception e){ e.printStackTrace(); }
        return out;
    }
    public static LinkedList<LinkedHashMap<String,String>> readX(String name){
        SpreadsheetX sx = new SpreadsheetX();
        File f = new File(name);
        return sx.readX(f);        
    }
    public static LinkedList<LinkedHashMap<String,String>> readX(File f){
        SpreadsheetX sx = new SpreadsheetX();
        return sx.readX(f);        
    }
    
    public static LinkedList<LinkedHashMap<String,String>> merge(LinkedList<LinkedHashMap<String,String>> a, LinkedList<LinkedHashMap<String,String>> b){
         a.addAll(b);
        return a;
    }
    
    public static double D(String in){
        // parses a string into a Double
        if(in != null){
            if(in.length() > 0){
                double d;
                try{ d = Double.parseDouble(in); }catch(NumberFormatException e){ return 0.0; }
                return d;
            }else{ return 0.0; }
        }else{ return 0.0; }
    }    
    public static String G(Map map, String key){
        // safely gets a value from a map
        if(map != null && !map.isEmpty() && map.containsKey(key)){
            if(map.get(key) != null){
                return map.get(key).toString();
            }else{ return ""; }
        }else{
            return "";
        }
    }
    public static double GD(Map map, String key){
        return D(G(map,key));
    }
    public static void U(Map<String,Double> map, String key, Double val){
        // safely update the value of KEY in MAP by adding VAL
        if(map.containsKey(key)){
            Double old = map.get(key);
            map.put(key, old + val);
        }else{
            map.put(key, val);
        }
//        return map;
    }
    
    public static String colNameFromNum(int c){ // 0 => A, 25 => Z, 26 => AA ...
        int modulo;
        int dividend = c + 1;
        String column = "";
        
        while(dividend > 0){ // A=65 Z=90
            modulo = (dividend-1) % 26;
            column = Character.toString((char)(65 + modulo)) + column;
            dividend = (int)((dividend - modulo) / 26);
        }
        return column;
    }
    
    public static LinkedList<String> safeCols(LinkedList<String> input){
        LinkedList<String> output = new LinkedList<>();
        for(String col : input){
            System.out.println(col + "\t" + col.replaceAll("\\[\\]",""));
            output.add(col.replaceAll("\\[\\]", ""));
        }
        return output;
    }
    private static String CRLF(String in){ // return a String which has the CRLF chars
        String crlf = Character.toString((char)13) + Character.toString((char)10);
        if(in != null && in.contains("\\r\\n")){
            in = in.replaceAll("\\r\\n", crlf);
        }
        return in;
    }
}

/*
0, "General"
1, "0"
2, "0.00"
3, "#,##0"
4, "#,##0.00"
5, "$#,##0_);($#,##0)"
6, "$#,##0_);[Red]($#,##0)"
7, "$#,##0.00);($#,##0.00)"
8, "$#,##0.00_);[Red]($#,##0.00)"
9, "0%"
0xa, "0.00%"
0xb, "0.00E+00"
0xc, "# ?/?"
0xd, "# ??/??"
0xe, "m/d/yy"
0xf, "d-mmm-yy"
0x10, "d-mmm"
0x11, "mmm-yy"
0x12, "h:mm AM/PM"
0x13, "h:mm:ss AM/PM"
0x14, "h:mm"
0x15, "h:mm:ss"
0x16, "m/d/yy h:mm"
*/