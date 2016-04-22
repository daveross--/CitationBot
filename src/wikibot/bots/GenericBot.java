package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import wikibot.Wiki;

public class GenericBot implements Bot{

    int interval = 3000;
    
    @Override
    public void test(){ }

    @Override
    public void login(Wiki wiki){ wiki.login(); }
    @Override
    public void update(){ }
    @Override
    public void next(){ }
    @Override
    public void previous(){ }
    @Override
    public void post(){ }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){ }
    @Override
    public boolean hasNext(){ return false; }
    @Override
    public boolean usePrev(){ return true; }
    @Override
    public String getContents(){ return null; }
    @Override
    public String getPrevious(){ return null; }
    @Override
    public String getWord(){ return null; }
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
