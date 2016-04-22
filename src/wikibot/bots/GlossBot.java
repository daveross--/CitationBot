package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Wiki;
import wikibot.objects.Result;

public class GlossBot implements Bot {

    final Wiki wiki = new Wiki();
    int interval = 3000;
    final String category = "Category:Translation_table_header_lacks_gloss";
//    final String pattern = "(?s)(.*?==\\s*English\\s*==(?!----).*?)\n(#[^#*:][^\\[\\]{}]+?)\n(.*?)";
    final String pattern = "(?s)(.*?)(.*?==\\s*English\\s*==(?!----).*?)(.*?)";
    String contents;
    String content, before, after, initial;
    String title, cont = "";
    
    Result result;
    int cursor = 0;
    LinkedList<String> pages = new LinkedList<>();
    
    @Override
    public int getWidth(){ return 1200;}
    @Override
    public int getHeight(){ return 800; }
    @Override
    public void test(){ 
        
    }
    @Override
    public void login(Wiki wiki){ wiki.login(); }
    @Override
    public void update(){
        cursor = 0;
        result = wiki.getCategory(category, 5, cont);
        cont = result.getContinueString();
        result = result.retrieve(wiki);
        pages = result.getPages();
        title = pages.get(cursor++);
        contents = result.getContents().get(title);
        split();
    }
    @Override
    public void next(){
        if(pages.size() <= cursor){ update(); }
        title = pages.get(cursor++);
        System.out.println(title);
        contents = result.getContents().get(title);
        split();
    }
    @Override
    public void previous(){ 
    
    }
    @Override
    public void post(){
        System.out.println(before + "\n" + content + "\n" + after);
        pages.remove(--cursor);
        next();
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
    public String getStatus(){ return null; }
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
    private boolean split(){
        if(contents != null){
            Pattern L2 = Pattern.compile("^\\s*==\\s*([^=]+?)\\s*==\\s*$");
            Pattern LX = Pattern.compile("^\\s*={2,}\\s*([^=]+?)\\s*={2,}\\s*$");
            String[] lines = contents.split("\n");
            
            String lang = "";      
            boolean inA = true, inB = false, inC = false;            
            boolean update = false; // skip page if no Trans section is found
            
            String a = "", b = "", c = "";
            
            for(String line : lines){
                Matcher l2 = L2.matcher(line);
                Matcher lx = LX.matcher(line);
                if(l2.matches()){
                    if(l2.group(1).compareTo("English") == 0){ 
                        inB = true; inA = false;
                    }else if(inB){
                        inB = false; inC = true;
                    }
                }else if(lx.matches()){
                    if(lx.group(1).compareTo("Translations") == 0){ update = true; }
                }
                if(inA){ a = a + line + "\n"; }
                if(inB){ b = b + line + "\n"; }
                if(inC){ c = c + line + "\n"; }
            }
            before = a;
            content = b;
            after = c;
            initial = b;
            return true && update;
        }else{
            return false;
        }
    }
    
}
