package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Dump;
import wikibot.Wiki;

public class SpanishBot implements Bot{

    int interval = 3000, cursor = 0;
    Dump dump = new Dump();
    
    TreeSet<String> titles;  
    LinkedHashMap<String,LinkedHashMap<String,String>> trans = new LinkedHashMap<>();
    LinkedHashMap<String,String> current;
    LinkedList<String> keys = new LinkedList<>();
    
    String contents = "", title = "", previous;
    
    @Override
    public void test(){   
    }

    @Override
    public void login(Wiki wiki){ wiki.login(); }
    @Override
    public void update(){
        Pattern p = Pattern.compile("[\\p{L}&&[^\\p{Lu}]]+?");
        titles = dump.getTitlesInLang("Spanish", -1);  
        LinkedList<LinkedHashMap<String,String>> t = dump.getTransToLang("es", -1);
        for(LinkedHashMap<String,String> lhm : t){
            String tr = lhm.get("trans");
            Matcher m = p.matcher(tr);
            if(m.matches() && !titles.contains(tr)){
                trans.put(tr, lhm);
                keys.add(tr);
            }
        }
    }
    @Override
    public void next(){
        contents = "";
        title = keys.get(cursor++);
        current = trans.get(title);
        previous = "English:\t" + current.get("title") + "\nSpanish:\t" + current.get("trans")  + " (" + current.get("pos") + ")" + "\nGloss:\t" + current.get("gloss") + "\n\n" + current.get("line");
        
//        System.out.println(title + "\t" + current.get("line")); // test
        String head = "", pos = current.get("pos");
        if(pos.compareTo("Verb") == 0){
            String root = title.substring(0, title.length()-2), ending = title.substring(title.length()-2);
//            if(ending.compareTo("ar") != 0 && ending.compareTo("er") != 0 && ending.compareTo("ir") != 0){ next(); }
            head = "{{es-verb|" + root + "|" + ending + "}}";
        }else if(pos.compareTo("Noun") == 0){
            String g1 = "";
            if(current.containsKey("g1")){ g1 = current.get("g1"); }else{ next(); }
            head = "{{es-noun|" + g1 + "}}";            
        }else if(pos.compareTo("Adjective") == 0){
            head = "{{es-adj}}";            
        }else if(pos.compareTo("Adverb") == 0){ // can add superlative "sup="
            head = "{{es-adv}}";            
        }else{ next(); }
        
        contents = "==Spanish==\n\n===" + current.get("pos") + "===\n" + head + "\n# [[" + current.get("title") + "]] (''" + current.get("gloss") + "'')";        
    }
    @Override
    public void previous(){ }
    @Override
    public void post(){ }
    @Override
    public void close(){
        System.out.println("Cursor:\t" + cursor + " (" + title + ")");
    }

    @Override
    public void setContents(String text){ }
    @Override
    public boolean hasNext(){ return cursor < keys.size(); }
    @Override
    public boolean usePrev(){ return true; }
    @Override
    public String getContents(){ return contents; }
    @Override
    public String getPrevious(){ return previous; }
    @Override
    public String getWord(){ return keys.get(cursor); }
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
    public int getHeight(){ return 400; }
    
}
