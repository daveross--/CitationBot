package wikibot.bots;

import java.awt.Color;
import java.text.Collator;
import java.text.Normalizer;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.*;
import wikibot.objects.XML_Page;

public class BalanceBot implements Bot{
    /*
    Parse the dump, look for derived terms, related terms and translation sections? which have multiple entries but have unbalanced templates or no templates
    Then add them.
    */
    Wiki wiki;
    Dump1 dump = new Dump1();
    long cursor = 0L, previous = cursor;
    int interval = 3000;
    String title, contents, original, A, B;
    String summary = "/* Derived terms */ +temps.";
    XML_Page page;
    boolean hasNext = true;
    int minimumList = 5;
    
    @Override
    public void test(){
        next();
    }

    @Override
    public void login(Wiki w){ 
        wiki = w; 
        wiki.login(); 
    }
    @Override
    public void update(){ 
        dump.setCursor(cursor);
//        next();
    }
    @Override
    public void next(){ 
        contents = ""; A = ""; B = ""; // reset
        page = nextPage();
        title = page.getTitle();
        original = wiki.read(title);
        if(!parse()){ next(); }
    }
    @Override
    public void previous(){ 
        contents = ""; A = ""; B = ""; // reset
        page = prevPage();
        title = page.getTitle();
        original = wiki.read(title);
        if(!parse()){ previous(); }        
    }
    @Override
    public void post(){ 
        wiki.edit(title, contents, summary);
    }
    @Override
    public void close(){
        System.out.println("Dump cursor:\t" + dump.getCursor());
    }
    
    
    @Override
    public void setContents(String text){  }
    @Override
    public boolean hasNext(){ return true; }
    @Override
    public boolean usePrev(){ return true; }
    @Override
    public String getContents(){ return B; }
    @Override
    public String getPrevious(){ return A; }
    @Override
    public String getWord(){ return title; }
    @Override
    public String getStatus(){ return title; }
    @Override
    public boolean useHighlighter(){ return false; }
    @Override
    public HashMap<String,Color> highlights(){ 
        HashMap<String,Color> map = new HashMap<>();
        
        return map; 
    }
    @Override 
    public boolean useEditor(){ return false; }
    @Override
    public String highlightAction(String text, int start, int end){ return text; }
    @Override
    public int getInterval(){ return interval; }
    @Override
    public int setInterval(int i){ 
        if(i < 1000){ interval = 1000; }else{ interval = i; }
        return this.interval; 
    }
    @Override
    public int getWidth(){ return 1200; }
    @Override
    public int getHeight(){ return 800; }
    
    private boolean parse(){
        // assign a, b and c based on contents
        String C = original;
        Pattern der = Pattern.compile("(?s)(.+?)(={3,6}\\s*Derived [Tt]erms\\s*={3,6})(.+?)(={2,6}|\\{\\{-}}|----|\\n\\n)(.*?)");
        Pattern rel = Pattern.compile("(?s)(.+?)(={3,6}\\s*Related [Tt]erms\\s*={3,6})(.+?)(={2,6}|\\{\\{-}}|----|\\n\\n)(.*?)");
        Pattern list = Pattern.compile("\\n?={3,6}(.+?)={3,6}(\\n+?\\s*\\*\\s*([^\\n]*?)){3,}");
        int cur = 0;
        boolean relfix = false, derfix = false;
        boolean shortrel = false, shortder = false;
        String before = "", current, after;
        //<editor-fold defaultstate="collapsed" desc="Derived">
        while(cur < C.length()){
            Matcher d = der.matcher(C.substring(cur));
            if(d.matches() && !d.group(3).contains("{")){
                before = before + d.group(1);
                current = d.group(2) + d.group(3);
                after = d.group(4) + d.group(5);
                
                if(!current.contains("{")){
                    Matcher l = list.matcher(current);
                    
                    A = A + current + "\n\n";
                    String[] parts = split(current);
//                    if(parts.length == 0){ next(); }
                    int L = parts.length - 1;
                    if(!l.matches()){
//                        System.out.println(dump.getCursor());
                    }else if(parts.length <= 4){ // derfix = true;
                        shortder = true;
//                        current = parts[0] + "\n{{der-top}}\n";
//                        for(int i=1; i<parts.length; i++){
//                            current = current + parts[i] + "\n";
//                        }
//                        current = current + "{{der-mid}}\n{{der-bottom}}\n\n";
//                        
                    }else if(parts.length <= 11){ derfix = true;
                        int mid = (int)Math.ceil(L/2.);
                        current = parts[0] + "\n{{der-top}}\n";
                        for(int i=1; i<=mid; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid}}\n";
                        for(int i=mid+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-bottom}}\n\n";
                        
                    }else if(parts.length <= 19){ derfix = true;
                        int a = (int)Math.ceil(L/3.), b = (int)Math.ceil(L*2/3.);
                        current = parts[0] + "\n{{der-top3}}\n";
                        for(int i=1; i<=a; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid3}}\n";
                        for(int i=a+1; i<=b; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid3}}\n";
                        for(int i=b+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-bottom}}\n\n";       
                        
                    }else{ derfix = true;
                        int a = (int)Math.ceil(L/4.), b = (int)Math.ceil(L*2/4.), c = (int)Math.ceil(L*3/4.);
                        current = parts[0] + "\n{{der-top4}}\n";
                        for(int i=1; i<=a; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid4}}\n";
                        for(int i=a+1; i<=b; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid4}}\n";
                        for(int i=b+1; i<=c; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-mid4}}\n";
                        for(int i=c+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{der-bottom}}\n\n";                          
                    }
                    current = current.replaceAll("\n{3,}", "\n\n");
                    cur += current.length() + d.group(1).length();
                    C = before + current + after;
                    before = before + current;
                    B = B + current + "\n\n";
                }else{}                
            }else{
                cur = C.length();
            }
        } 
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Related">
        cur = 0;
        before = ""; current = ""; after = "";
        while(cur < C.length()){
            Matcher r = rel.matcher(C.substring(cur));
            if(r.matches() && !r.group(3).contains("{")){
                before = before + r.group(1);
                current = r.group(2) + r.group(3);
                after = r.group(4) + r.group(5);
                
                if(!current.contains("{")){
                    Matcher l = list.matcher(current);
                    A = A + current + "\n\n";
                    String[] parts = split(current);
//                    if(parts.length == 0){ next(); }
                    int L = parts.length - 1;
                    if(!l.matches()){ 
//                        System.out.println(dump.getCursor());
                    }else if(parts.length <= 4){ // relfix = true;
                        shortrel = true;
//                        current = parts[0] + "\n{{rel-top}}\n";
//                        for(int i=1; i<parts.length; i++){
//                            current = current + parts[i] + "\n";
//                        }
//                        current = current + "{{rel-mid}}\n{{rel-bottom}}\n\n";                        
                    }else if(parts.length <= 11){ relfix = true;
                        int mid = (int)Math.ceil(L/2.);
                        current = parts[0] + "\n{{rel-top}}\n";
                        for(int i=1; i<=mid; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid}}\n";
                        for(int i=mid+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-bottom}}\n\n";
                        
                    }else if(parts.length <= 19){ relfix = true;
                        int a = (int)Math.ceil(L/3.), b = (int)Math.ceil(L*2/3.);
                        current = parts[0] + "\n{{rel-top3}}\n";
                        for(int i=1; i<=a; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid3}}\n";
                        for(int i=a+1; i<=b; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid3}}\n";
                        for(int i=b+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-bottom}}\n\n";       
                        
                    }else{ relfix = true;
                        int a = (int)Math.ceil(L/4.), b = (int)Math.ceil(L*2/4.), c = (int)Math.ceil(L*3/4.);
                        current = parts[0] + "\n{{rel-top4}}\n";
                        for(int i=1; i<=a; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid4}}\n";
                        for(int i=a+1; i<=b; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid4}}\n";
                        for(int i=b+1; i<=c; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-mid4}}\n";
                        for(int i=c+1; i<parts.length; i++){ current = current + parts[i] + "\n"; }
                        current = current + "{{rel-bottom}}\n\n";                       
                    }
                    current = current.replaceAll("\n{3,}", "\n\n");
                    cur += current.length() + r.group(1).length();
                    C = before + current + after;
                    before = before + current;
                    B = B + current + "\n\n";
                }else{}                
            }else{
                cur = C.length();
            }
        } 
        //</editor-fold>
        contents = C.replaceAll("\n{3,}", "\n\n");
        if(relfix && derfix){
            summary = "+templates for Related and Derived terms";
        }else if(relfix){
            summary = "/* Related terms */ +templates";            
        }else if(derfix){
            summary = "/* Derived terms */ +templates";
        }else if(shortrel || shortder){
            
        }else{
            summary = "-error";
            System.out.println("Fail through?" + title);
        }
        return original.compareTo(contents) != 0;
    }
    
    private String[] split(String in){
        Comparator<String> IGNORE_CASE = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2){ 
                return norm(s1).compareToIgnoreCase(norm(s2)); 
            }
            private String norm(String s){
                s = s.replaceAll("[^\\p{L}]", "");
                s = Normalizer.normalize(s, Normalizer.Form.NFD);
                Pattern p = Pattern.compile("\\p{IncombiningDiacriticalMarks}+");
                return p.matcher(s).replaceAll("");
            }
        };
        
        Pattern fmt = Pattern.compile("^\\*\\s(.+?)$");
        
        SortedSet<String> ts = new TreeSet<>(IGNORE_CASE);
        String[] A = in.split("\n");
        String A0 = A[0];
        for(int i=1; i<A.length; i++){ ts.add(A[i]); }
        String[] B = new String[ts.size() + 1];
        B[0] = A0;
        for(int i=1; i<B.length; i++){
            String s = ts.first();
            Matcher m = fmt.matcher(s);
            if(!m.matches()){ return new String[0]; }
            ts.remove(s);
            B[i] = s;
        }
        return B;
    }
    
    private XML_Page nextPage(){
        XML_Page p;
        Pattern der = Pattern.compile("(?s)(.+?)(={3,6}\\s*Derived [Tt]erms\\s*={3,6})(.+?)(={2,6}|\\{\\{-}}|----)(.*?)");
        while((p = dump.next()) != null){
            String content = p.getRevisions().getFirst().getText();
            Matcher m = der.matcher(content);
            if(m.matches()){ // check if it has any templates at all for now...
//                System.out.println(m.group(3).replaceAll("\n","~") + "\n\n\tCount: " + count(m.group(3), "\n"));
                if(m.group(3).contains("{") != true && count(m.group(3),"\n") > minimumList){
                    return p;
                }
            }
        }
        
        return null;
    }
    private XML_Page prevPage(){
        XML_Page p;
        Pattern der = Pattern.compile("(?s)(.+?)(={3,6}\\s*Derived [Tt]erms\\s*={3,6})(.+?)(={2,6}|\\{\\{-}}|----)(.*?)");
        while((p = dump.previous()) != null){
            String content = p.getRevisions().getFirst().getText();
            Matcher m = der.matcher(content);
            if(m.matches()){ // check if it has any templates at all for now...
                if(m.group(3).contains("{") != true && count(m.group(3),"\n") > minimumList){
                    return p;
                }
            }
        }
        
        return null;
    }
    private static int count(String s, String i){
        // how many times does i occur in s?
        int out = 0;
        int c = 0;
        while(s.substring(c).contains(i)){
            c += s.substring(c).indexOf(i) + 1;
            out++;
        }
        return out;
    }
}
