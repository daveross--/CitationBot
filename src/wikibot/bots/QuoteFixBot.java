package wikibot.bots;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.*;
import wikibot.Wiki;
import wikibot.objects.Result;
import wikibot.tools.Spreadsheet;

public class QuoteFixBot implements Bot {
    Wiki wiki = new Wiki();
    int interval = 3000, cursor = 0, stopOn10 = 0;
    
    private final String category = "Category:Quotation_templates_using_both_date_and_year";
    String contents, cont, title, initial;
    String content, before, after;
    
    LinkedList<String> parts = new LinkedList<>();   
    
    Result result;
    LinkedList<String> pages = new LinkedList<>();
    
    public QuoteFixBot(){}
    
    @Override
    public int getWidth(){ return 1200;}
    @Override
    public int getHeight(){ return 400; }
    @Override
    public void test(){ 
//        String t = "{{quote-book|year=1900|author={{w|Test}}|title={{w|{{Nested|}} T}}|passage=Blah blah blah.}}";
//        splitQuote(t);
        wiki.login();
        update();
        cleaner();
//        next();
        try{
            while(System.in.read() > 0){
                next();
            }
        }catch(Exception e){}
    }
    @Override
    public void login(Wiki wiki){ 
        this.wiki = wiki;
        this.wiki.login(); 
    }
    @Override
    public void update(){        
        cursor = 0;
//        cont = "0|500";
        result = wiki.getCategory(category, 100, cont);
        cont = result.getContinueString();
        result = result.retrieve(wiki);
        pages = result.getPages();
        title = pages.get(cursor++);
        contents = result.getContents().get(title);
    }
    @Override
    public void next(){
        if(pages.size() <= cursor){ update(); }
        initial = ""; content = "-";
        title = pages.get(cursor++);
        contents = result.getContents().get(title);
        if(contents.length() > 2056){ next(); } // skip big articles
        initial = contents;
        boolean cleaned = cleaner();
        if(!cleaned){ 
            System.out.println(title + "\t" + cleaned); 
            next(); 
        }else{
            System.out.println(title + "\t" + cleaned); 
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
        pages.remove(--cursor);
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
        return this.content;
    }
    @Override
    public String getPrevious(){ return initial; }
    @Override
    public String getWord(){ return this.title; }
    @Override
    public String getStatus(){ return this.title; }
    @Override
    public boolean useHighlighter(){ return true; }
    @Override
    public HashMap<String,Color> highlights(){ 
        HashMap<String,Color> map = new HashMap<>();
        
        return map; 
    }
    @Override 
    public boolean useEditor(){ return false; }
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
    
    private boolean cleaner(){
        split();
        String out = "";
        for(String part : parts){
            if(part.startsWith("{{quote-")){
                part = reformat(part);
            }else{ }
            out = out + part;
        }
        contents = out;
        content = out;
        return true;
    }
    private String reformat(String in){   
        if(in.matches("(?=)(.+?)\\|[0-9]{4}\\|(.+?)")){ next(); } // skip things with unnamed year param
        
        LinkedHashMap<String,String> map = getParams(in);
        map = fixDates(map);
        
//        System.out.println(rebuildQuote(map));
        return rebuildQuote(map);
    }   
    private LinkedHashMap<String,String> fixDates(LinkedHashMap<String,String> in){
        LinkedHashMap<String,String> map = new LinkedHashMap<>();
        for(String K : in.keySet()){
            String k = C(K).toLowerCase(), v = C(in.get(K));
            map.put(k, v);
        }
        
        Pattern d1 = Pattern.compile("\\p{L}{2,}[\\s\\.]*[0-9]{1,2}[\\s,]+[0-9]{4}"); // March 5, 1980
        Pattern d2 = Pattern.compile("[0-9]{1,2}[\\s]\\p{L}{2,}[\\s,\\.]+[0-9]{4}"); // 5 March, 1980
        Pattern d3 = Pattern.compile("[0-9]{4}[\\s]\\p{L}{2,}[\\s,\\.]+[0-9]{1,2}"); // 1980 March 05
        Pattern d4 = Pattern.compile("[0-9]{4}[/\\-][0-9]{1,2}[/\\-][0-9]{1,2}");    // 1980-03-05
        Pattern d5 = Pattern.compile("[\\p{L}\\s/\\-]+?");    // March // March/April // Spring
        Pattern d6 = Pattern.compile("[\\p{L}\\s/\\-\\.]+\\s[0-9]{1,2}");    // March 10
        Pattern d7 = Pattern.compile("[\\p{L}\\s/\\-\\.]+\\s[0-9]{4}");    // March 1980
        Pattern d8 = Pattern.compile("[0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{4}");    // 03/05/1980
        Pattern d9 = Pattern.compile("[0-9]{1,2}[/\\-][0-9]{1,2}");    // MM/DD
        // March 1894, Volume 21, Part 1
        
        String Y = "?", M = "", D = "";
        boolean useD = false, useYM = false, useY = false;
        
        for(String k : map.keySet()){
            String v = map.get(k); String o = "";           
            if(k.compareTo("date") == 0){
                Matcher m1 = d1.matcher(v), m2 = d2.matcher(v), m3 = d3.matcher(v), m4 = d4.matcher(v), m5 = d5.matcher(v)
                        , m6 = d6.matcher(v), m7 = d7.matcher(v), m8 = d8.matcher(v), m9 = d9.matcher(v);
                
                if(m1.matches()){ System.out.println("D1\t" + k + "\t" + v);            // March 5, 1980 
                    v = v.replaceAll(",", "");
                    String V[] = v.split(" "); v = V[0] + " " + V[1] + ", " + V[2];
                    D = v; useD = true;
                    
                }else if(m2.matches()){ 
                    o = v;
                    v = v.replaceAll(",", "");
                    String V[] = v.split(" "); v = V[0] + " " + V[1] + ", " + V[2];
                    D = v; useD = true;     
                    System.out.println("D2\t" + k + "\t" + o + "\t" + v);               // 5 March, 1980 
                    
                }else if(m3.matches()){
                    o = v;
                    v = v.replaceAll(",", "");
                    String V[] = v.split(" "); v = V[1] + " " + V[2] + ", " + V[0];
                    D = v; useD = true;   
                    System.out.println("D3\t" + k + "\t" + o + "\t" + v);               // 1980 March 05 
                     
                }else if(m4.matches()){ System.out.println("D4\t" + k + "\t" + v);      // 1980-03-05
                    D = v; useD = true;
                    
                }else if(m5.matches()){ System.out.println("D5\t" + k + "\t" + v);      // March // March/April // Spring
                    M = v; useYM = true;
                    
                }else if(m6.matches()){ System.out.println("D6\t" + k + "\t" + v);      // March 10
                    o = v;
                    if(map.containsKey("year")){ 
                        D = v + ", " + map.get("year");
                        useD = true;
                        System.out.println("D6\t" + k + "\t" + o + "\t" + v);           // March 10
                        
                    }else{
                        System.err.println("Missing year in format 6."); return in;
                    }
                    
                }else if(m7.matches()){ System.out.println("D7\t" + k + "\t" + v);      // March 1980
                    M = v.split(" ")[0]; Y = v.split(" ")[1]; useYM = true;
                    
                }else if(m8.matches()){ System.out.println("D8\t" + k + "\t" + v);      // 03/05/1980
                    System.err.println("Ambiguous date."); useD = true;
                    return in;
                    
                }else if(m9.matches()){ System.out.println("D9\t" + k + "\t" + v);      // MM/DD 
                    if(map.containsKey("year")){ 
                        D = v + "/" + map.get("year");
                        useD = true;
                    }else{
                        System.err.println("Missing year in format 6."); return in;
                    }             
                    
                }else{ System.out.println("NO\t" + k + "\t" + v); return in; } 
                
            }else if(k.compareTo("year") == 0){     Y = v; useY = true;            
            }else if(k.compareTo("month") == 0){    M = v; useYM = true;  
            }else{ 
            
            
            }
        }
        
        if(useD){
            map.remove("month"); map.remove("year");
            map.put("date", D);
        }else if(useYM){
            map.remove("date");
            map.put("year", Y); map.put("month", M);
        }else if(useY){
            map.put("year", Y);
        }else{
            System.err.println("useD and useYM are both null"); return in;
        }
        map = reorder(map);
        return map;
    }
    private LinkedHashMap<String,String> reorder(LinkedHashMap<String,String> in){        
        // This is to reorder the parameters
        LinkedList<String> order =  new LinkedList<>(Arrays.asList(new String[]{"quote-","year","month","date","author","speaker","title","debate","work","journal","newsgroup"}));
        LinkedList<String> keys = new LinkedList<>(in.keySet());
        LinkedHashMap<String,String> out = new LinkedHashMap<>();
        
        for(String k : order){
            if(keys.contains(k)){
                out.put(k, in.get(k));
                keys.remove(k);               
            }
        }
        for(String k : keys){
            out.put(k, in.get(k));
        }
        if(in.containsKey("passage")){ // passage is always last
            out.put("passage", in.get("passage"));
        }
        
        return out;
    }
    private LinkedHashMap<String,String> getParams(String in){
        //<editor-fold desc="notes">
        // journal: unnamed {{quote-journal|1=[year]|2=[author]|3=[title]|4=[journal]|5=[url]|6=[page]|7=[passage]}}
        // web: unnamed {{quote-web|1=[date]|2=[author]|3=[title]|4=[work]|5=[url]|6=[passage]}}
        // newsgroup: {{quote-newsgroup|1=[date]|2=[author]|3=[title]|4=[newsgroup]|5=[url]|6=[passage]}}
        // song: {{quote-song|1=[year]|2=[author]|3=[title]|4=[album]|5=[url]|6=[passage]}}
        // video: {{quote-video|1=[year]|2=[actor]|3=[title]|4=[season]|5=[number]|6=[time]|7=[passage]}}
        // hansard: {{quote-hansard|1=[year]|2=[speaker]|3=[debate]|4=[report]|5=[url]|6=[page]|7=[column]|8=[passage]}}
        // us-patent: {{quote-us-patent|1=[year]|2=[author]|3=[title]|4=[number]|5=[page]|6=[passage]}}
        // book: {{quote-book|[year]|[author]|[title]|[journal]|[url]|[page]|[passage]}}
        // magazine: {{quote-journal|[year]|[author]|[title]|[journal]|[url]|[page]|[passage]}}
        //</editor-fold>
        LinkedHashMap<String,LinkedList<String>> unnamed = new LinkedHashMap<>();
        unnamed.put("book",         new LinkedList<>(Arrays.asList(new String[]{"year","author","title","journal","url","page","passage"})));
        unnamed.put("journal",      new LinkedList<>(Arrays.asList(new String[]{"year","author","title","journal","url","page","passage"})));
        unnamed.put("magazine",     new LinkedList<>(Arrays.asList(new String[]{"year","author","title","journal","url","page","passage"})));
        unnamed.put("news",         new LinkedList<>(Arrays.asList(new String[]{"year","author","title","journal","url","page","passage"})));
        unnamed.put("web",          new LinkedList<>(Arrays.asList(new String[]{"date","author","title","work","url","passage"})));
        unnamed.put("newsgroup",    new LinkedList<>(Arrays.asList(new String[]{"date","author","title","newsgroup","url","passage"})));
        unnamed.put("usenet",       new LinkedList<>(Arrays.asList(new String[]{"date","author","title","newsgroup","url","passage"})));
        unnamed.put("song",         new LinkedList<>(Arrays.asList(new String[]{"year","author","title","album","url","passage"})));
        unnamed.put("video",        new LinkedList<>(Arrays.asList(new String[]{"year","actor","title","season","number","time","passage"})));
        unnamed.put("hansard",      new LinkedList<>(Arrays.asList(new String[]{"year","speaker","debate","report","url","page","column","passage"})));
        unnamed.put("us-patent",    new LinkedList<>(Arrays.asList(new String[]{"year","author","title","number","page","passage"})));
        
        LinkedHashMap<String,String> nlines = new LinkedHashMap<>();   
        
        in = C(in);
        String[] lines = splitQuote(in); //in.split("(?=\\|)"); 
        int un = 0;
        String lastParam = "";
        Pattern ptrn = Pattern.compile("(?s)(\\|[\\p{L}\\-0-9_\\s]+?=)(.+?)");
        
        for(String line : lines){
            Matcher mchr = ptrn.matcher(line);
            if(mchr.matches()){
                lastParam = mchr.group(1).replaceAll("[^\\p{L}0-9\\-_\\s]", "");
                nlines.put(lastParam, mchr.group(2));
            }else{
                if(un != 0){
                    if(lastParam.compareTo("url") == 0){
                        nlines.put("url", nlines.get("url") + line); // fixing url problems
                    }
                }else{
                    String type = C(line.substring(line.indexOf("quote-") + "quote-".length()));
                    nlines.put("quote-", type);
                    if(!unnamed.containsKey(type)){ System.err.println("Missing " + type); return null; }
                    un++;
                }
            }
        }
        if(nlines.containsKey("url")){ nlines.put("url", nlines.get("url").replaceAll("\\|", "%7C")); } // fixing url problems
        
        Pattern blank = Pattern.compile("(?s)(\\|(.+?)=[\\s\\n]*)");
        
        LinkedHashMap<String,String> out = new LinkedHashMap<>();
        for(String K : nlines.keySet()){
            String V = nlines.get(K);
            out.put(C(K), C(V));
        }
//        for(String k : out.keySet()){ System.out.println(k + "\t" + out.get(k)); }
        return out;
    }
    private String[] splitQuote(String in){
        // "quote-book|year=1900|author={{w|Test}}|title={{w|{{Nested|}} T}}|passage=Blah blah blah.
        if(in.startsWith("{{")){ in = in.substring(2); }
        if(in.endsWith("}}")){ in = in.substring(0,in.length()-2); }
        
        int c = 0, max = 10;
        LinkedList<String> l = new LinkedList<>();
        String[] ps = in.split("(?=\\|)");
        String temp = "";
        for(String p : ps){
            temp = temp + p;
            if(count(temp,"{{") == count(temp,"}}")){
                l.add(temp); temp = "";
            }
        }     
        return l.toArray(new String[l.size()]);
    }
    private void split(){
        parts = new LinkedList<>();
        int c = 0;
        while(c >= 0){
            String sub = contents.substring(c);
            if(!sub.contains("{{quote-")){ 
                c = -1; 
                parts.add(sub);
            }else{
                int start = sub.indexOf("{{quote-");
                parts.add(sub.substring(0,start));
                sub = sub.substring(start);
                c = c + start;
                
                if(sub.substring(2).contains("{{") && sub.substring(2).contains("}}")){
                    int open = sub.substring(2).indexOf("{{")+2, close = sub.substring(2).indexOf("}}")+2;
                    while(open < close && open > 0){
                        open = sub.indexOf("{{",open+2);
                        close = sub.indexOf("}}",close+2);
                    }
                    parts.add(sub.substring(0, close+2));
                    c = c + close+2;
                }else if(sub.substring(2).contains("}}")){
                    parts.add(sub.substring(0, sub.indexOf("}}")+2));
                    c = c + sub.indexOf("}}")+2;
                }else{
                    System.err.println("WTF?");
                }
            }
        } 
//        for(String part : parts){ System.out.println(part); }
    }
    private String rebuildQuote(LinkedHashMap<String,String> in){
        String out = "";
        boolean first = true;
        for(String k : in.keySet()){
            if(first){ first = false; 
                out = k + in.get(k);
            }else{
                out = out + "|" + k + "=" + in.get(k);
            }
        }
        return "{{" + out + "}}";
    }
    private String rebuild(){
        for(String part : parts){
            
        }
        return "";
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
    @Deprecated
    private boolean clean(){
        Pattern yeardate = Pattern.compile("(?s)(.+?)" + "(\\{\\{quote-[^{}]*?)"  + "(\\|year=[0-9]{4})" + "([^{}]*?)" + "(\\|date=[^{}|]{3,})"   + "(\\|*[^{}]*?}})" + "(.*+)");
        Pattern dateyear = Pattern.compile("(?s)(.+?)" + "(\\{\\{quote-[^{}]*?)"  + "(\\|date=[^{}|]{3,})"   + "([^{}]*?)" + "(\\|year=[0-9]{4})" + "(\\|*[^{}]*?}})" + "(.*+)");
        Pattern unnamed  = Pattern.compile("(?s)(.+?)" + "(\\{\\{quote-[^{}|]*?)" + "(\\|[0-9]{4}(?=\\s?[|]))" + "([^{}]*?)" + "(\\|date=[^{}]+?)" + "(\\|*[^{}]*?}})" + "(.*+)");
    
        initial = ""; content = "";
        
        String a, b;
        Matcher un = unnamed.matcher(contents);
        Matcher yd = yeardate.matcher(contents);
        Matcher dy = dateyear.matcher(contents);  
        
        Pattern date = Pattern.compile("(?s)\\|date=(\\p{L}+?\\.?\\s?[0-9]{1,2}[\\n\\s]*|[0-9]{1,2}\\s?\\p{L}+?\\.?[\\n\\s]*)"); // end with (\\s|\\n)*
        Pattern mnth = Pattern.compile("(?s)\\|date=(\\p{L}+?[\\n\\s]*)");
        Pattern parm = Pattern.compile("(?s)([\\p{L}0-9\\-]+?)=(.*?)");

        boolean go = (un.matches() || yd.matches() || dy.matches());
        boolean save = false;
        
        while(go){
            un = unnamed.matcher(contents);
            if(un.matches()){
                LinkedList<String> unmd = new LinkedList<>(Arrays.asList(new String[]{"year","author","title","work","url","pages","passage"}));
                before = un.group(1); after = un.group(7);
                initial = un.group(2) + un.group(3) + un.group(4) + un.group(5) + un.group(6);
                String[] params = initial.split("\\|");
                int p = -1;
                for(String param : params){
                    Matcher pr = parm.matcher(param);
                    if(pr.matches()){
                        String prm = pr.group(1);
                        if(unmd.contains(prm)){ unmd.remove(prm); }
                        content = content + "|" + param;                    
                    }else{
                        if(p < 0){ p++; 
                            content = content + param;                    
                        }else if(p<unmd.size()){                        
                            content = content + "|" + unmd.get(p++) + "=" + param;
                        }else{
                            System.err.println("No more unnamed params remain given:\t" + param + ".");
                        }
                    }
                }
                contents = before + content + after;
            }

            yd = yeardate.matcher(contents);
            dy = dateyear.matcher(contents);  
            if(yd.matches()){ 
                before = yd.group(1); after = yd.group(7);
                if(initial.isEmpty()){ initial = yd.group(2) + yd.group(3) + yd.group(4) + yd.group(5) + yd.group(6); }
                Matcher d = date.matcher(yd.group(5));
                Matcher m = mnth.matcher(yd.group(5));
                if(d.matches()){
                    String g3 = yd.group(3).replaceAll("[^0-9]", "");
                    String g5 = yd.group(5).replaceAll("[0-9]{4}","").trim();
                    content = yd.group(2) + g5 + ", " + g3 + yd.group(4) + yd.group(6);
                    save = true;        
                }else if(m.matches()){
                    content = yd.group(2) + yd.group(3) + yd.group(4) + "|month=" + m.group(1) + yd.group(6);
                    save = true;
                }else{
                    System.out.println(title + "\t" + yd.group(5));
//                    return save;
                }
            }else if(dy.matches()){ 
                before = dy.group(1); after = dy.group(7);
                if(initial.isEmpty()){ initial = dy.group(2) + dy.group(3) + dy.group(4) + dy.group(5) + dy.group(6); }

                Matcher d = date.matcher(dy.group(3));
                Matcher m = mnth.matcher(dy.group(3));
                if(d.matches()){
                    String g3 = dy.group(5).replaceAll("[^0-9]", "");
                    String g5 = dy.group(3).replaceAll("[0-9]{4}","").trim();
                    content = dy.group(2) + g5 + ", " + g3 + dy.group(4) + dy.group(6);
                    save = true;
                }else if(m.matches()){
                    content = dy.group(2) + dy.group(4) + dy.group(5) + "|month=" + m.group(1) + dy.group(6);
                    save = true;
                }else{
                    System.out.println(title + "\t" + dy.group(3));
//                    return save;
                }
            }else{
                return save;
            }
            contents = before + content + after;
            
            un = unnamed.matcher(contents);
            yd = yeardate.matcher(contents);
            dy = dateyear.matcher(contents);              
            go = (un.matches() || yd.matches() || dy.matches());
        }
//        if(save){ System.out.println(contents); }
        return save;
    }
}
