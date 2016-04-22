package wikibot.bots;

import java.awt.Color;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.*;
import wikibot.Wiki;
import wikibot.objects.Result;
import wikibot.tools.Spreadsheet;

public class QuoteFixBot_redo1 implements Bot {
    Wiki wiki = new Wiki();
    int interval = 3000, cursor = 0, stopOn10 = 0;
    
    Result result;
    LinkedList<String> pages, parts;
    LinkedHashMap<String,String> page_contents;
    
    String title, page_text, content, contents;
    String continue_string = "";
    
    private final String category = "Category:Quotation_templates_using_both_date_and_year";
    
    public QuoteFixBot_redo1(){}
    
    @Override
    public int getWidth(){ return 1200;}
    @Override
    public int getHeight(){ return 400; }
    @Override
    public void test(){         
        wiki.login();
        title = "YouTube";
        page_text = wiki.read(title);
        contents = "";
        parts = split(page_text);
        for(String part : parts){
            if(part.startsWith("{{quote-")){
                part = clean(part);
            } 
            contents = contents + part;
        }
//        System.out.println(contents);
//        wiki.write(title, contents, "Fixing quote template which has both [[Category:Quotation templates using both date and year|date= and year= parameters]].");
    }
    @Override
    public void login(Wiki wiki){ 
        this.wiki = wiki;
        this.wiki.login(); 
    }
    @Override
    public void update(){
        result = wiki.getCategory(category, 100, continue_string);
        continue_string = result.getContinueString();
        cursor = 0;
        pages = result.getPages();
        result.retrieve(wiki, 100);
        page_contents = result.getContents();
    }
    @Override
    public void next(){
        if(cursor >= pages.size()){ update(); }
        title = pages.get(cursor++);
        page_text = wiki.read(title);
        if(page_text.length() > (1024*2)){ next(); }
        contents = "";
        parts = split(page_text);
        for(String part : parts){
            if(part.startsWith("{{quote-")){
                part = clean(part);
            } 
            contents = contents + part;
        }                
    }
    @Override
    public void previous(){ 
        if(cursor >= 2){ cursor -= 2; }else{ cursor = 0; }
        next();
    }
    @Override
    public void post(){        
        wiki.write(title, contents, "Fixing quote template which has both [[Category:Quotation templates using both date and year|date= and year= parameters]].");
        next();
    }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){  this.contents = text; }
    @Override
    public boolean hasNext(){ return true; }
    @Override
    public boolean usePrev(){ return true; }
    @Override
    public String getContents(){
        return this.contents;
    }
    @Override
    public String getPrevious(){ return page_text; }
    @Override
    public String getWord(){ return this.title; }
    @Override
    public String getStatus(){ return this.title; }
    @Override
    public boolean useHighlighter(){ return true; }
    @Override
    public HashMap<String,Color> highlights(){ 
        HashMap<String,Color> map = new HashMap<>();
        
        map.put("|date=",Color.ORANGE);
        map.put("|month=",Color.YELLOW);
        map.put("|year=",Color.CYAN);
        map.put("|rfc=",Color.RED);
        
        return map; 
    }
    @Override 
    public boolean useEditor(){ return true; }
    @Override
    public String highlightAction(String text, int start, int end){ return null; }
    @Override
    public int getInterval(){ return interval; }
    @Override
    public int setInterval(int i){ 
        if(i < 1000){ this.interval = 1000;
        }else{ this.interval = i; }
        
        return this.interval; 
    }  
    
    /* NON-BOT METHODS */
    private LinkedList<String> split(String in){
        LinkedList<String> P = new LinkedList<>();
        int c = 0;
        while(c >= 0){
            String sub = in.substring(c);
            if(!sub.contains("{{quote-")){ 
                c = -1; 
                P.add(sub);
            }else{
                int start = sub.indexOf("{{quote-");
                P.add(sub.substring(0,start));
                sub = sub.substring(start);
                c = c + start;
                
                if(sub.substring(2).contains("{{") && sub.substring(2).contains("}}")){
                    int open = sub.substring(2).indexOf("{{")+2, close = sub.substring(2).indexOf("}}")+2;
                    while(open < close && open > 0){
                        open = sub.indexOf("{{",open+2);
                        close = sub.indexOf("}}",close+2);
                    }
                    P.add(sub.substring(0, close+2));
                    c = c + close+2;
                }else if(sub.substring(2).contains("}}")){
                    P.add(sub.substring(0, sub.indexOf("}}")+2));
                    c = c + sub.indexOf("}}")+2;
                }else{
                    System.err.println("WTF?");
                    c = -1;
                }
            }
        } 
        return P;
    } 
    
    private String clean(String in){
        String out = "";
        in = C(in.replaceAll("\n"," "));
        
        if(in.endsWith("}}")){ in = in.substring(0, in.length()-2); } // remove closing braces and add them back at the end.
        
        LinkedList<String> P = splitQuote(in);
        for(String p : P){ 
            out = out + p;
        }
        out = out.replaceAll("\\|passage=", "\n|passage=");
        return out + "}}";
    }
    
    private LinkedList<String> splitQuote(String in){
        LinkedList<String> out = new LinkedList<>();
        int c = 1;
        if(count(in,"{{")-1 != count(in,"}}") || count(in,"[[") != count(in,"]]")){ System.out.println("Bad syntax: " + title); next(); }
        
        try{
        
            while(c > -1){
                String t = "";
                if(in.substring(c).contains("|")){
                    int n = in.indexOf("|", c);
                    while(count(in.substring(c,n),"{{") != count(in.substring(c,n),"}}") || count(in.substring(c,n),"[[") != count(in.substring(c,n),"]]")){
                        if(in.length() > n+1 && in.substring(n+1).contains("|")){ 
                            n = in.indexOf("|", n+1); 
                        }else{ 
                            n = -2; 
                            out.add(in.substring(c-1));
                        }
                    }
                    out.add(in.substring(c-1,n));
                    c = n+1;
                }else{ 
                    out.add(in.substring(c-1));
                    c = -1; 
                }
            }
        }catch(Exception e){ 
            System.out.println(title + "\n\t" + in);
        }
        
        out = nameParams(out); // name unnamed params
        out = fixDates(out); // fix date issues
        
        return out;
    }
    private LinkedList<String> fixDates(LinkedList<String> parts){
        LinkedList<String> temp = new LinkedList<>();
        LinkedList<String> out = new LinkedList<>();
        boolean RFC = false;
        boolean YR = false, MO = false, DT = false;
        String yr = "", mo = "", dt = "";
        int i=0;
        
        for(String part : parts){
            if(part.toLowerCase().startsWith("|date=")){
                DT = true; dt = part.substring(part.indexOf("=")+1);
            }else if(part.toLowerCase().startsWith("|year=")){                
                String x = part.replaceAll(Character.toString((char)0x200e), ""); // Remove weird characters which show up in some dates and years.
                YR = true; yr = x.substring(x.indexOf("=")+1);            
            }else if(part.toLowerCase().startsWith("|month=")){
                MO = true; mo = part.substring(part.indexOf("=")+1);
            }else if(part.startsWith("|rfc=true")){
                RFC = true;
            }else if(part.startsWith("{{quote-")){
                out.add(part); // 
            }else{
                temp.add(part);
            }
        }
        
        
        if(YR && !MO && !DT){ // year only
            out.add("|year=" + yr); // check if year is well formatted?
            
        }else if(YR && DT && !MO){ // date and year
            if(dt.matches("[0-9]{4}[-/][0-9]{2}[-/][0-9]{2}")){ // YYYY-MM-DD
                out.add("|date=" + dt);
            }else if(dt.matches("([a-zA-Z\\-/\\s]+?)")){ // Month OR Month/Month etc.
                out.add("|year=" + yr + "|month=" + dt);
            }else if(dt.matches("[A-Za-z.]{3,}\\s[0-9]{1,2}")){ // Month 15
//                System.out.println("A");
                out.add("|date=" + dt + ", " + yr);
            }else if(dt.matches("[0-9]{1,2}\\s[A-Za-z.]{3,}")){ // 15 Month
                String[] D = dt.split(" ");
                out.add("|date=" + D[1] + " " + D[0] + ", " + yr); 
            }else if(dt.matches("[A-Za-z.]{3,}\\s[0-9]{1,2}(st|nd|rd|th)")){ // Month 15th /st /nd /rd
                String[] D = dt.split(" ");
                out.add("|date=" + D[0] + " " + D[1].replaceAll("[^0-9]", "") + ", " + yr); 
            }else{
                out.add("|year=" + yr);
                out.add("|date=" + dt);
                RFC = true;
            }
//            System.out.println("YR & DT & !MO");
        
        }else if(YR && MO && DT){
            // RFC //
            RFC = true;
            if(YR){ out.add("|year=" + yr); }
            if(DT){ out.add("|date=" + dt); }
            if(MO){ out.add("|month=" + mo); }            
            
        }else if(!YR && DT && !MO){ // Date only
            out.add("|date=" + dt); // validate date formatting?
           
        }else if(!YR && DT && MO){
            out.add("|date=" + dt);
            out.add("|month=" + mo);
            
        }else if(!YR && !DT && MO){
            out.add("|month=" + mo);            
            
        }else{
            // RFC //
            RFC = true;
            if(YR){ out.add("|year=" + yr); }
            if(DT){ out.add("|date=" + dt); }
            if(MO){ out.add("|month=" + mo); }
        }
        
        for(String t : temp){ 
            if(t.startsWith("|passage=")){ if(RFC){ out.add("|rfc=true"); } }
            out.add(t); 
        }
        
        return out;
    }
    
    private LinkedList<String> nameParams(LinkedList<String> parts){        
        LinkedList<String> out = new LinkedList<>();
        
        LinkedList<String> journal = new LinkedList<>(Arrays.asList(new String[]{"year","author","title","journal","url","page","passage"}));
        LinkedList<String> book = new LinkedList<>(Arrays.asList(new String[]{"year","author","title","url","page","passage"}));
        LinkedList<String> newsgroup = new LinkedList<>(Arrays.asList(new String[]{"date","author","title","group","url","passage"}));
                
        LinkedList<String> unList = new LinkedList<>();
        String open = parts.get(0);
        if(open.matches("\\{\\{quote-journal") || open.matches("\\{\\{quote-magazine")){
            unList = journal;
        }else if(open.matches("\\{\\{quote-book")){
            unList = book;
        }else if(open.matches("\\{\\{quote-newsgroup")){
            unList = newsgroup;
        }
        
        LinkedList<String> A = new LinkedList<>();
        TreeSet<String> params = new TreeSet<>();
        Pattern named = Pattern.compile("\\|([a-zA-Z0-9]+?)=(.*?)");        
        int unCursor = 0;        
        boolean first = true, rfc = false;
        for(String part : parts){
            Matcher m = named.matcher(part);
            if(first){ first = false;
                out.add(part);
            }else if(m.matches()){
                if(params.contains(m.group(1))){ rfc = true; } params.add(m.group(1));
                out.add(part);
            }else{
                if(unCursor >= unList.size() || params.contains(unList.get(unCursor))){ 
                    rfc = true; 
                }else{
                    params.add(unList.get(unCursor));
                    part = "|" + unList.get(unCursor++) + "=" + part.substring(1);
                }
                out.add(part);
            }
        }
        
        // to do:
        //  clear spacing around "=" e.g. |param = data
        //  fix the dates
        //  fix URLs with |
        
        if(rfc){ // if there is a problem, add an RFC notice to categorize for scrutiny
            out = parts;
            String last = out.removeLast();
            out.add("|rfc=true");
            out.add(last);
        }
        
//        System.out.println(out);
        
        return out;
    }
    
    private String C(String in){
        return in.replaceAll("\n{2,}","\n").replaceAll("\\s{2,}", " ").replaceAll("\\s*\\|\\s*","|").trim();
    }
    private static int count(String full, String part){
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1){
            lastIndex = full.indexOf(part,lastIndex);
            if(lastIndex != -1){
                count++;
                lastIndex += part.length();
            }
        }
        return count;
    }
}
