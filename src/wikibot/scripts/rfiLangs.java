package wikibot.scripts;

import java.awt.Color;
import wikibot.bots.Bot;
import wikibot.objects.Result;
import wikibot.Wiki;
import wikibot.tools.Lang;
import java.util.*;
import java.util.regex.*;

public class rfiLangs implements Bot {
    private Wiki wiki = new Wiki();
    private LinkedList<String> pages = new LinkedList<>();
    private Result result;
    private int cursor = -1;
    private int interval= 3000;
    private final int chunk = 500;
    private String current = null;
    private String contents = null;
    private final Lang LANGS = new Lang();
    
    @Override
    public int getWidth(){ return 900; }
    @Override
    public int getHeight(){ return 600; }

    @Override
    public void test(){
        
    }
    @Override
    public void login(Wiki wiki){
        this.wiki = wiki;
        this.wiki.setBot();
        this.wiki.login();
    }
    @Override
    public void update(){
        if(hasNext()){ next(); }
    }
    @Override
    public void next(){     
        if(hasNext()){
            current = pages.get(++cursor);
            contents = fix(wiki.read(current));
        }      
    }
    @Override
    public void previous(){   
        if(cursor >= 1){
            current = pages.get(--cursor);
            contents = fix(wiki.read(current));   
        }
    }
    @Override
    public void post(){  
        wiki.edit(current, contents, "Updating rfi template with language parameter.");
        pages.remove(cursor--);
    }
    @Override
    public void close(){}
    
    @Override
    public void setContents(String text){   
        contents = text;
    }
    @Override
    public boolean hasNext(){
        if(pages.size() == 0){
            System.out.println("\r\nGetting " + chunk + " more records...\r\n");
            Result r = wiki.getCategory("Category:Entries needing images by language", chunk, (result != null ? result.getContinueString() : ""));
            pages = r.getPages();    
            cursor = -1;            
        }
        if(pages.size() == 0){ return false; }else if(cursor >= pages.size()){ return false; }else{ return true; }
    }
    @Override
    public boolean usePrev(){
        return false;    
    }
    @Override
    public String getContents(){
        return contents;    
    }
    @Override
    public String getPrevious(){
        return contents;        
    }
    @Override
    public String getWord(){
        return "";        
    }

    @Override
    public String getStatus(){
        return "";        
    }

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
    public String highlightAction(String text, int start, int end){
        return text;        
    }

    @Override
    public int getInterval(){
        return interval;        
    }
    @Override
    public int setInterval(int i){
        interval = i;
        return interval;
    }
    
    private String fix(String in){
        String out = "";
        String lang = null;
        Pattern L2 = Pattern.compile("^==\\s*([\\p{L}\\-\\s]+?)\\s*==$");
        Pattern RFI = Pattern.compile("^(.*?)(\\{\\{(rfi|rfimage|[Rr]eqphoto|rfdrawing|rfphoto)(\\|lang=\\p{L}+?)?}})(.*?)$");
        for(String line : in.split("\n")){
            Matcher M2 = L2.matcher(line.trim());
            if(M2.matches()){ 
                lang = LANGS.getCode(M2.group(1).trim()); 
            }
            if(lang != null){
                Matcher rfi = RFI.matcher(line);
                if(rfi.matches()){
                    line = line.replaceAll("|lang=(\\p{L}+?)","");
                    line = line.replaceAll("\\{\\{rfi}}","{{rfi|" + lang + "}}");
                    line = line.replaceAll("\\{\\{(rfi|rfimage|[Rr]eqphoto|rfdrawing|rfphoto)(\\|lang=\\p{L}+?)?}}","{{rfi|" + lang + "}}");
                }
            }            
            out = out + line + "\n";
        }
        return out;
    }
}
