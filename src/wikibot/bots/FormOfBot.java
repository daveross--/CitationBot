package wikibot.bots;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Dump;
import wikibot.Wiki;
import wikibot.objects.Result;
import wikibot.tools.MD5;
import wikibot.tools.Spreadsheet;

public class FormOfBot implements Bot {
    Wiki wiki = new Wiki();
    Dump dump = new Dump();
    int interval = 3000, cursor = 0, stopOn10 = 0;    
    String content, title, initial, ending = "s";    
    Result result;
    
    LinkedList<String> pages, plurals;
    TreeMap<String,String> pageMap = new TreeMap<>();
    LinkedHashMap<String,String> extant = new LinkedHashMap<>();
    TreeSet<String> titles;
    
    Pattern trans = Pattern.compile("(?s)(.*?)(==\\s*Translingual\\s*==)(.*?)");
    Pattern eng   = Pattern.compile("(?s)(.*?)(==\\s*English\\s*==)(.*?)");
    
    String[] patterns = { "(?s)(.+)(\\{\\{en-noun}}|\\{\\{en-noun\\|s}})(.+)" };
    // This bot looks through the dump for instances of header templates which give form-of links which have pages missing the target section
    // {{en-noun}} => PAGENAMEs
    // could get very complicated with language sections etc.
    
    @Override
    public int getWidth(){ return 1000;}
    @Override
    public int getHeight(){ return 400; }
    @Override
    public void test(){   
        pageMap = dump.getPagesMatchingInLanguage("English", patterns, -1);
        System.out.println(pageMap.size());
//        update();
//        next();
    }
    @Override
    public void login(Wiki wiki){ 
        this.wiki = wiki;
        this.wiki.login(); 
        titles = titles("English");
        System.out.println("Retrieved " + titles.size() + " English titles.");
    }
    @Override
    public void update(){
        String[] patterns = { "(?s)(.+)(\\{\\{en-noun}}|\\{\\{en-noun\\|s}})(.+)" };
        if(!(titles != null)){ titles = titles("English"); }
        pages = new LinkedList<>(); plurals = new LinkedList<>();        
        while(pages.isEmpty()){
            ending = "s";       
            pageMap = dump.getPagesMatchingInLanguage("English", patterns, 200);
            LinkedList<String> pageList = new LinkedList<>(pageMap.keySet());
            for(String page : pageList){ 
                if(!titles.contains(page + "s")){ 
                    pages.add(page); 
                    plurals.add(page + ending);
                }else{ }
            }
        }  
        System.out.println("Found " + pages.size() + " pages to update.");
        extant = wiki.readAll2(plurals);
        System.out.println("Of which " + extant.size() + " already exist.");
        cursor = 0;
        next();
    }
    @Override
    public void next(){
//        System.out.println(title + "\t" + pages);
        if(pages.size() <= cursor){ 
            update(); 
        }else{
            title = pages.get(cursor++);
            ending = "s";            
            if(extant.containsKey(title + ending)){
                initial = extant.get(title + ending);
                Matcher tl = trans.matcher(initial);
                Matcher en = eng.matcher(initial);
                if(tl.matches()){
                    next(); // rebuild translingual plural
                }else if(en.matches()){ 
                    next(); // skip if we already have an English section
                }else{
                    content = plural() + "\n\n----\n\n" + initial;
                }
            }else{
                initial = pageMap.get(title);
                content = plural(); 
            }       
        }
//        pages.remove(--cursor);// fake post
    }
    @Override
    public void previous(){ 
        if(cursor >= 2){ cursor -= 2; }else{ cursor = 0; }
        next();
    }
    @Override
    public void post(){        
        pages.remove(--cursor);
        wiki.write(title + ending, content, "Creating plural form of [[" + title + "]] ([[WT:ACCEL|Accelerated]])");
    }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){  this.content = text; }
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
    public boolean useHighlighter(){ return false; }
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
    
    private String plural(){
        return "==English==\n" +
        "\n" +
        "===Noun===\n" +
        "{{head|en|noun plural form}}\n" +
        "\n" +
        "# {{plural of|" + title + "|lang=en}}";
    }
    private TreeSet<String> titles(String language){     
        String fname = dump.getTitle() + " - TITLES IN " + language
            , filename = System.getProperty("user.home") + "\\Google Drive\\Java\\" + fname + ".xlsx";   
        LinkedList<LinkedHashMap<String,String>> list = new LinkedList<>();
        TreeSet<String> titles = new TreeSet<>();
        try{
            File file = new File(filename);
            if(file.exists()){
                list = Spreadsheet.read(file);
                for(LinkedHashMap<String,String> lhm : list){ 
                    titles.add(lhm.get("title")); 
                }
            }else{
                titles = dump.getTitlesInLang(language, 10);
                for(String t : titles){
                    LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
                    lhm.put("title", t);
                    list.add(lhm);
                }
                if(titles.size() > 0){ Spreadsheet.printer(file, list); }
            }   
        }catch(Exception e){ e.printStackTrace(); }
        
        return titles;
    }
}
