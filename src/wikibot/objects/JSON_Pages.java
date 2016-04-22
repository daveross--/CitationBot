package wikibot.objects;

import java.util.LinkedHashMap;

public class JSON_Pages {
   
    boolean batchcomplete;
    Query  query;
    
    public JSON_Pages(){}
    
    public LinkedHashMap<String,String> getContents(){
        LinkedHashMap<String,String> output = new LinkedHashMap<>();
        if(this.batchcomplete){
            for(Page p : query.pages){
                if(!p.missing && !p.invalid){
                    output.put(p.title, p.revisions[0].content);
                }
            }
        }
        return output;
    }
    
    public String getFirst(){
        // fix this so that it gets the first which is !missing and !invalid...
        if(this.query != null){
            Query q = this.query;
            if(q.pages != null && q.pages.length > 0){
                Page page = q.pages[0];
                if(page.revisions != null && page.revisions.length > 0){
                    JSON_Pages.Page.Revision rev = page.revisions[0];
                    if(rev.getContent() != null){
                        return rev.getContent();
                    }
                }
            }
        }
        
        return "";
    }
    public Page getPage(){
        return this.query.pages[0];
    }

    public class Query {
        Page[] pages;
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
/*
  {    
    "batchcomplete": true,
    "query": {
      "pages": [
        {
          "pageid": 2635481,
          "ns": 0,
          "title": "inmutem",
          "revisions": [
            {
              "contentformat": "text/x-wiki",
              "contentmodel": "wikitext",
              "content": "==Latin==\n\n===Verb===\n{{la-verb-form|inmūtem}}\n\n# {{inflection of|inmūtō||1|s|pres|actv|subj|lang=la}}\n\n[[mg:inmutem]]"
            }
          ]
        }
      ]
    }
  }
*/