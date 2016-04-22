package wikibot.objects;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedList;

public class JSON_Embeds {
   
    boolean batchcomplete;
    @SerializedName("continue") Continue Continue;
    Query  query;
    
    public JSON_Embeds(){}
    public LinkedList<String> getPages(){
        LinkedList<String> output = new LinkedList<>();
        for(Embed ei : this.query.embeddedin){
            output.add(ei.title); 
        }        
        return output;
    }
    public String getContinue(){
        return this.Continue.eicontinue;
    }
    public boolean getComplete(){
        return this.batchcomplete;
    }

    class Continue {
        String eicontinue;
        @SerializedName("continue") String Continue;
    }
    
    class Query {
        Embed[] embeddedin;
    }

    class Embed {
        int pageid;
        int ns;
        String title;    
    }

}
/*
{
{
  "batchcomplete": "",
  "continue": {
    "eicontinue": "0|35",
    "continue": "-||"
  },
  "query": {
    "embeddedin": [
      {
        "pageid": 19,
        "ns": 0,
        "title": "free"
      },
      {
        "pageid": 20,
        "ns": 0,
        "title": "thesaurus"
      },
*/