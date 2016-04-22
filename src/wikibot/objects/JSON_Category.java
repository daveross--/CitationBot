package wikibot.objects;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedList;

public class JSON_Category {
   
    boolean batchcomplete;
    @SerializedName("continue") Continue Continue;
    Query  query;
    
    public JSON_Category(){}
    public LinkedList<String> getPages(){
        LinkedList<String> output = new LinkedList<>();
//        if(this.batchcomplete){
            for(CategoryMember cm : this.query.categorymembers){
                output.add(cm.title); 
            }        
            return output;
    }
    public String getContinue(){
        if(this.Continue != null && this.Continue.cmcontinue != null){
            return this.Continue.cmcontinue;
        }else{
            return "";
        }
    }
    public boolean getComplete(){
        return this.batchcomplete;
    }

    class Continue {
        String cmcontinue;
        @SerializedName("continue") String Continue;
    }
    
    class Query {
        CategoryMember[] categorymembers;
    }

    class CategoryMember {
        int pageid;
        int ns;
        String title;    
//        Revision[] revisions;
    }

//    class Revision {
//        String contentformat;
//        String contentmodel;
//        String content;
//    }
}
/*
{
  "batchcomplete": "",
  "continue": {
    "cmcontinue": "page|4c55465459|5113753",
    "continue": "-||"
  },
  "query": {
    "categorymembers": [
      {
        "pageid": 61957,
        "ns": 0,
        "title": "hag"
      },
      {
        "pageid": 4790590,
        "ns": 0,
        "title": "lionine"
      },
      {
        "pageid": 4801403,
        "ns": 0,
        "title": "long-eared guinea pig"
      },
      {
        "pageid": 1172322,
        "ns": 0,
        "title": "lose touch"
      },
      {
        "pageid": 343869,
        "ns": 0,
        "title": "loss function"
      }
    ]
  }
}
*/