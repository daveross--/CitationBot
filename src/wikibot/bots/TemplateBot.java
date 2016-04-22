package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import wikibot.Wiki;
import wikibot.objects.Result;

public class TemplateBot implements Bot {
    Wiki wiki = new Wiki();
    int interval = 3000, chunk = 50, counter;
    String title, initial, content, previous, status, contString = "nap";
    LinkedList<String> pages;
    LinkedHashMap<String,String> contents;
    boolean next = true;
    
    int count = 0;
    
    Result result;
    
    @Override
    public void test(){ }

    @Override
    public void login(Wiki wiki){ 
        this.wiki = wiki; 
        wiki.login(); 
    }
    @Override
    public void update(){ 
        result = wiki.getNamespace("10", chunk, contString);        
        pages = result.getPages();
        contString = pages.getLast().replaceAll("Template:","") + "~";
        contents = wiki.readAll(pages); 
        next = (chunk <= pages.size());
        System.out.println(pages);
        counter = 0;
    }
    @Override
    public void next(){   
        content = ""; initial = "";
        if(pages.size() <= counter){ update(); }
        title = pages.get(counter++);
        if(!title.contains("documentation")){
            initial = contents.get(title);
            int IOo = count(initial.toLowerCase(), "<includeonly>"), IOc = count(initial.toLowerCase(), "</includeonly>");
            int NIo = count(initial.toLowerCase(), "<noinclude>"), NIc = count(initial.toLowerCase(), "</noinclude>");
            if((IOo != IOc || NIo != NIc)){ //  && initial.endsWith("<noinclude>{{documentation}}")
                status = (title + "\t" + (IOo != IOc ? "includeonly" : "") + " " + (NIo != NIc ? "noinclude" : "")).trim();
                content = initial;
                if(IOo > IOc && NIo == NIc){
                    if(content.endsWith("<includeonly>") && IOo > 1){
                        int index = content.lastIndexOf("<includeonly>");
                        content = content.substring(0,index) + content.substring(index).replace("<includeonly>","</includeonly>");
                    }else if(content.endsWith("<includeonly>") && IOo == 1 && IOc == 0){
                        int index = content.lastIndexOf("<includeonly>");
                        content = content.substring(0,index) + content.substring(index).replace("<includeonly>","");                        
                    }else{
                        content = content + "</includeonly>";  
                    }
                }else if(IOo == IOc && NIo > NIc){
                    if(content.endsWith("<noinclude>") && NIo > 1){
                        int index = content.lastIndexOf("<noinclude>");
                        content = content.substring(0,index) + content.substring(index).replace("<noinclude>","</noinclude>");
                    }else if(content.endsWith("<noinclude>") && NIo == 1 && NIc == 0){
                        int index = content.lastIndexOf("<noinclude>");
                        content = content.substring(0,index) + content.substring(index).replace("<noinclude>","");                        
                    }else{
                        content = content + "</noinclude>";  
                    }
                }else if(!content.contains("[[Category:Templates with unbalanced tags]]")){
                    content = content + "<!--\n\n--><noinclude>[[Category:Templates with unbalanced tags]]</noinclude>"; // too complicated
                }else{
                    next();
                }
            }else{
                next();
            }
        }else{
            next();
        }
    }
    @Override
    public void previous(){ 
    }
    @Override
    public void post(){         
        wiki.edit(title, content, "Adding closing [[Category:Templates with unbalanced tags|noinclude/includeonly]] tag.");
    }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){ content = text; }
    @Override
    public boolean hasNext(){ return next; }
    @Override
    public boolean usePrev(){ return true; }
    @Override
    public String getContents(){ return content; }
    @Override
    public String getPrevious(){ return initial; }
    @Override
    public String getWord(){ return "noinclude"; }
    @Override
    public String getStatus(){ return status; }
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
    
    private static int count(String full, String part){
        int count = 0;
        try{
            int lastIndex = 0;
            while(lastIndex != -1){

                lastIndex = full.indexOf(part,lastIndex);

                if(lastIndex != -1){
                    count ++;
                    lastIndex += part.length();
                }
            }
        }catch(Exception e){ return count; }
        return count;
    }
}
