package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import wikibot.Wiki;
import wikibot.objects.Result;

public class MoveBot implements Bot{

    Wiki wiki = new Wiki();
    int interval = 3000;
    int cursor = 0;
    String from, to, summary = "Moving proto- languages to Reconstruction namespace";
    String cont = "";
    LinkedList<String> keys = new LinkedList<>();
    LinkedHashMap<String,String> moves = new LinkedHashMap<>();
    
    @Override
    public void test(){ }

    @Override
    public void login(Wiki w){ wiki = w; wiki.setBot(); wiki.login(); }
    @Override
    public void update(){ 
        cursor = 0;
        boolean go;
        do{
            Result r = wiki.getNamespace("100", 50, cont, "Proto");
            LinkedHashMap<String,String> contents = wiki.readAll(r.getPages());
            
            for(String key : contents.keySet()){
                String content = contents.get(key);
                if(content.contains("{{reconstruct")){
                    from = key; to = key.replace("Appendix:", "Reconstruction:");                
                    moves.put(from, to);
                }
            }
            
            cont = r.getContinueString();
//            go = cont.compareTo("") != 0;
            go = false;
        }while(go);    
        keys = new LinkedList<>();
        keys.addAll(moves.keySet());
//        System.out.println("Keys:\t" + keys.size());
    }
    @Override
    public void next(){
        if(cursor >= keys.size()){ update(); }
        from = keys.get(cursor++);
        to = moves.get(from);
    }
    @Override
    public void previous(){ 
        if(cursor > 1){ cursor -= 2; }else{ cursor = 0; }
        next();
    }
    @Override
    public void post(){ 
        wiki.move(from, to, summary);
        moves.remove(from);
        keys.remove(from);
        cursor--;
    }
    @Override
    public void close(){}

    @Override
    public void setContents(String text){ }
    @Override
    public boolean hasNext(){ return moves.size() > 0; }
    @Override
    public boolean usePrev(){ return false; }
    @Override
    public String getContents(){ return "Moving:\t" + from + "\nTo:\t" + to; }
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
