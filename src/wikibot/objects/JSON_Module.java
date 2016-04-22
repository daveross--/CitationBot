package wikibot.objects;

import com.google.gson.annotations.SerializedName;

public class JSON_Module {
    String type;
    String print;
    @SerializedName("return") String return_;
    long session;
    int sessionSize;
    int sessionMaxSize;
    String sessionIsNew;
    
    public JSON_Module(){}
    public String[] results(){
        return this.print.split("\\t");
    }
}
/*
{
  "type": "normal",
  "print": "головокружительный\tru\tRussian\tru\n",
  "return": "",
  "session": 882449172,
  "sessionSize": 494,
  "sessionMaxSize": 500000,
  "sessionIsNew": ""
}
*/