package wikibot.objects;

import wikibot.Wiki;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Result {
    // Container to pass around sets of pages which result from getter queries, keep so we can continue queries from within functions
    Wiki wiki = new Wiki();
    String reference; // page or category which is the subject of the query
    boolean complete = false;
    String continueString;
    LinkedList<String> pages;
    LinkedHashMap<String,String> contents = new LinkedHashMap<>(); // hashmap with page titles and contents
    
    public Result(){}
    
    public Result retrieve(Wiki wiki){ return retrieve(wiki, 100); } // default chunk 100?
    public Result retrieve(Wiki wiki, int chunksize){
        this.wiki = wiki;
        // Bundle the pages into chunks to be read together (for efficient API calls).
        int chunked = 0;
        pages = this.getPages();
        
        LinkedList<LinkedList<String>> chunks = new LinkedList<>();
        while(chunked < pages.size()){
            if(pages.size() > chunked+chunksize){
                chunks.add((new LinkedList<>(pages.subList(chunked, chunked+chunksize))));
                chunked += chunksize;
            }else{
                chunks.add(new LinkedList<>(pages.subList(chunked, pages.size())));
                chunked = pages.size();
            }
        }   
        for(LinkedList<String> chunk : chunks){
            LinkedHashMap<String,String> pageMap;
            pageMap = wiki.readAll(chunk);
            if(pageMap != null){ this.contents.putAll(pageMap); }
        }
        
        return this;
    }
    
    public Result setReference(String _r){
        this.reference = _r;
        return this;
    }
    public Result setPages(LinkedList<String> _p){
        this.pages = _p;
        return this;
    }
    public Result setComplete(boolean _c){
        this.complete = _c;
        return this;
    }
    public Result setContinueString(String _c){
        this.continueString = _c;
        return this;
    }
    public Result setContents(LinkedHashMap<String,String> _c){
        this.contents = _c;
        return this;
    }
    
    public String getReference(){
        return this.reference;
    }
    public LinkedList<String> getPages(){
        return this.pages;
    }
    public String getContinueString(){
        if(this.continueString != null){
            return this.continueString;
        }else{
            return "";
        }
    }
    public boolean getComplete(){
        return this.complete;
    }
    public LinkedHashMap<String,String> getContents(){
        return this.contents;
    }
}
