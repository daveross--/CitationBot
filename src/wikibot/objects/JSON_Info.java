package wikibot.objects;

import java.util.HashMap;

public class JSON_Info {  
    boolean batchcomplete;
    Query  query;
    
    public JSON_Info(){}
    //{"batchcomplete":true,"query":{"pages":[{"pageid":101243,"ns":114,"title":"Citations:hirsute","contentmodel":"wikitext","pagelanguage":"en","pagelanguagehtmlcode":"en","pagelanguagedir":"ltr","touched":"2016-01-04T05:35:05Z","lastrevid":26839623,"length":1124}]}}
    //{"batchcomplete":true,"query":{"pages":[{"ns":114,"title":"Citations:hairsuite","missing":true,"contentmodel":"wikitext","pagelanguage":"en","pagelanguagehtmlcode":"en","pagelanguagedir":"ltr"}]}}

    public boolean exists(){
        return (this.query.pages[0].pageid > 0);
    }
    public HashMap<String,Boolean> exist(){
        HashMap<String,Boolean> out = new HashMap<>();
        
        for(Page page : this.query.pages){
            if(page.missing == true){
                out.put(page.title, false);
            }else{
                out.put(page.title, true);
            }
        }
        
        return out;
    }
    public int pageCount(){
        return this.query.pages.length;
    }
    public void print(){
        for(Page page : this.query.pages){
            System.out.println(page.title + "\t" + !page.missing + "\t" + page.pageid);
        }
    }
    
    class Query {
        Normalized[] normalized;
        Page[] pages;
    }

    class Normalized {
        String from;
        String to;
    }
    
    class Page {
        int pageid;
        boolean missing = false;
        int ns;
        String title;  
        int length;        
    }
}
/*
{
  "batchcomplete": true,
  "query": {
    "normalized": [
      {
        "from": "Empetrum_nigrum",
        "to": "Empetrum nigrum"
      }
    ],
    "pages": [
      { (sometimes "missing": true...)
        "pageid": 5126940,
        "ns": 0,
        "title": "Empetrum nigrum",
        "contentmodel": "wikitext",
        "pagelanguage": "en",
        "pagelanguagehtmlcode": "en",
        "pagelanguagedir": "ltr",
        "touched": "2015-12-30T14:59:28Z",
        "lastrevid": 35994123,
        "length": 931
      }
    ]
  }
}
*/