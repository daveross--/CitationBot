package wikibot.objects;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class JSON_AllPages {
    
    boolean batchcomplete;
    @SerializedName("continue") Continue cont;
    Query  query;
    
    public JSON_AllPages(){}
    
    public LinkedHashMap<String,String> getContents(){
        LinkedHashMap<String,String> output = new LinkedHashMap<>();
        if(this.batchcomplete){
            for(Page p : query.allpages){
                if(!p.missing && !p.invalid){
                    output.put(p.title, p.revisions[0].content);
                }
            }
        }
        return output;
    }
    public LinkedList<String> getPages(){
        LinkedList<String> output = new LinkedList<>();
//        if(this.batchcomplete){
        for(JSON_AllPages.Page cm : this.query.allpages){
            output.add(cm.title); 
        }        
        return output;
    } 
    public boolean getComplete(){ return false; }
    public String getContinue(){ 
        if(cont != null){
            if(cont.apcontinue != null){
                return cont.apcontinue;
            }else{ return ""; }
        }else{ return ""; } 
    }
    public void setContinue(String in){ }
    public String getFirst(){
        // fix this so that it gets the first which is !missing and !invalid...
        if(this.query != null){
            Query q = this.query;
            if(q.allpages != null && q.allpages.length > 0){
                Page page = q.allpages[0];
                if(page.revisions != null && page.revisions.length > 0){
                    JSON_AllPages.Page.Revision rev = page.revisions[0];
                    if(rev.getContent() != null){
                        return rev.getContent();
                    }
                }
            }
        }
        
        return "";
    }
    public Page getPage(){
        return this.query.allpages[0];
    }

    public class Continue {
        String apcontinue = "";
        @SerializedName("continue") String cont = "";
    }
    
    public class Query {
        Page[] allpages;
    }

    public class Page {
        long pageid;
        int ns;
        String title;   
        Revision[] revisions;
        boolean invalid;
        boolean missing; 
        
        public String getTitle(){ return this.title; }
        public long getPageId(){ return this.pageid; }
        public Revision[] getRevisions(){
            return this.revisions;
        } 

        public class Revision {
            String revid;
            String parentid;
            String user;
            String flags;
            String timestamp;
            String comment;
            String contentformat;
            String contentmodel;
            String content = "";

            public String getContent(){
                return this.content;
            }
            public String getRevid(){
                return this.revid;
            }
            public String getParent(){
                return this.parentid;
            }
            public String getUser(){
                return this.user;
            }
            public String getFlags(){
                return this.flags;
            }
            public String getTimestamp(){
                return this.timestamp;
            }
            public String getComment(){
                return this.comment;
            }
        }       
    }
}
