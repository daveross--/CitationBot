package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import wikibot.Wiki;
import wikibot.tools.Spreadsheet;

public class UnblockBot implements Bot{

    Wiki wiki = new Wiki();
    int interval = 500, cursor = 0;
    LinkedList<LinkedHashMap<String, String>> blocks;
    LinkedHashMap<String,String> block;
    String contents = "- start -";
    String ip, expiry, by, summary, timestamp;
    long id = 0;
    String reason = "Removing permanent blocks of IPs per [[Wiktionary:Beer_parlour/2016/February#Rescinding_all_indefinite_IP_blocks_which_are_in_place|this discussion]].";
    
    @Override
    public void test(){ 
        ip = "139.19.142.3x"; id = 0;
        wiki.login();
        wiki.unblock(id, ip, reason);
    }

    @Override
    public void login(Wiki w){ wiki = w; wiki.login(); }
    @Override
    public void update(){ 
        blocks = wiki.getIndefIPBlocks();
    }
    @Override
    public void next(){ 
        block = blocks.get(cursor++);
        ip = Spreadsheet.G(block, "user");
        id = (long)Spreadsheet.GD(block, "id");
        by = Spreadsheet.G(block, "by");
        expiry = Spreadsheet.G(block, "expiry");
        timestamp = Spreadsheet.G(block, "timestamp");
        summary = Spreadsheet.G(block, "reason");
        
        contents = ip + "\t(" + id + ") [" + expiry + "]\nBY:\t" + by + "\t(" + timestamp + ")\nReason:\t" + summary;
    }
    @Override
    public void previous(){ 
        cursor -= 2;
        if(cursor < 0){ cursor = 0; }
        next();
    }
    @Override
    public void post(){ 
        wiki.unblock(id, ip, reason);
    }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){ }
    @Override
    public boolean hasNext(){ return blocks.size() > cursor; }
    @Override
    public boolean usePrev(){ return false; }
    @Override
    public String getContents(){ return contents; }
    @Override
    public String getPrevious(){ return null; }
    @Override
    public String getWord(){ return " "; }
    @Override
    public String getStatus(){ return " "; }
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
