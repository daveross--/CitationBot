package wikibot.objects;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;
import wikibot.tools.Spreadsheet;

public class JSON_Blocks {
    boolean batchcomplete;
    @SerializedName("continue") Continue c;
    Query query;
    
    class Continue{
        String bkcontinue;
        String bgstart;
        @SerializedName("continue") String c;        
    }
    
    class Query{
        Block[] blocks;
        Block[] globalblocks;
        
        class Block{
            long id = 0;
            String user = "";
            String address = "";
            String expiry = "";
            String timestamp = "";
            String by = "";
            String reason = "";
            String rangestart = "";
            String rangeend = "";
        }
    }
    
    public String cont(){
        if(this.c != null){
            if(this.c.bkcontinue != null){
                return this.c.bkcontinue;                
            }else if(this.c.bgstart != null){
                return this.c.bgstart;      
            }
        }
        return "";
    }
    public LinkedList<LinkedHashMap<String,String>> getIndefIPBlocks(){
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
        LinkedList<LinkedHashMap<String,String>> blocks = getBlocks();
        
        Pattern IPV4 = Pattern.compile("(([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/[0-9]{1,2})*");
        Pattern IPV6 = Pattern.compile("([0-9A-F]{0,4}:){7}[0-9A-F]{0,4}(/[0-9]{1,2})*");        
        
        for(LinkedHashMap<String,String> block : blocks){
            boolean inf = (Spreadsheet.G(block, "expiry").compareTo("infinity") == 0);
            boolean ipv4 = IPV4.matcher(Spreadsheet.G(block, "user")).matches();
            boolean ipv6 = IPV6.matcher(Spreadsheet.G(block, "user")).matches();
            
            if(inf && (ipv4 || ipv6)){
//                System.out.println(Spreadsheet.G(block,"user") + "\t" + Spreadsheet.G(block, "expiry"));
                out.add(block);
            }else{
//                System.err.println(block);
            }
        }
        
        return out;
    }
    public LinkedList<LinkedHashMap<String,String>> getBlocks(){
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
        
        if(this.query != null){
            if(this.query.blocks != null){
                for(Query.Block block : this.query.blocks){
                    if(block.user != null){
                        LinkedHashMap<String,String> o = new LinkedHashMap<>();
    //                    o.put("id", Long.toString(block.id));
                        if(block.user.length() > 1){ o.put("user", block.user); }else{ o.put("user", ""); }
    //                    if(block.address.length() > 1){ o.put("address", block.address); }else{ o.put("address", ""); }
                        o.put("expiry", block.expiry);
                        o.put("by", block.by);
                        o.put("reason", block.reason);
                        o.put("rangestart", block.rangestart);
                        o.put("rangeend", block.rangeend);
                        out.add(o);
                    }
                }
            }else if(this.query.globalblocks != null){
                for(Query.Block block : this.query.globalblocks){
                    LinkedHashMap<String,String> o = new LinkedHashMap<>();
//                    o.put("id", Long.toString(block.id));
//                    if(block.user.length() > 1){ o.put("user", block.user); }else{ o.put("user", ""); }
                    if(block.address.length() > 1){ o.put("address", block.address); }else{ o.put("address", ""); }
                    o.put("expiry", block.expiry);
                    o.put("by", block.by);
                    o.put("reason", block.reason);
                    o.put("rangestart", block.rangestart);
                    o.put("rangeend", block.rangeend);
                    out.add(o);
                }
            }
        }
        
        return out;
    }
}
/*
    {"batchcomplete":true,
        "continue":{"bkcontinue":"20160224201002|149631","continue":"-||"},
        "query":{"blocks":[
                        {"id":149643,"expiry":"2016-02-26T18:40:28Z","reason":"Vandalism"},
                        {"id":149642,"expiry":"2016-02-26T18:09:54Z","reason":"Vandalism"},
                        {"id":149632,"expiry":"infinity","reason":"Vandalism"}
                ]}}
*/