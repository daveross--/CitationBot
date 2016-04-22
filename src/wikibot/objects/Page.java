package wikibot.objects;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

public class Page {
    public String title, text, username, comment, timestamp;    
    public int namespace;
    public long pageid, revid, parentid, userid;    
    
    private static final Pattern XML_OC = Pattern.compile("\\s*<([A-Za-z0-9\\s]+?)>(.*?)</\\1>\\s*"); // open and close
    private static final Pattern XML_O = Pattern.compile("\\s*<([A-Za-z0-9\\s:=\"]+?)>(.*?)");  // open, no close
    private static final Pattern XML_C = Pattern.compile("(.*?)</([A-Za-z0-9\\s]+?)>\\s*"); // close
//    private static final Pattern XML_B = Pattern.compile("\\s*<([A-Za-z0-9\\s]+?)/>\\s*"); // booleans
    
    
    public Page(){}
    
    public static Page parse(String xml){ // xml from <page> to </page>
        String[] lines = xml.split("\n");
        Page out = new Page();
        String tag = "";
        String contents = "";
        boolean intext = false;
        
        LinkedList<String> tags = new LinkedList<>();
        
        for(String line : lines){
            Matcher openclose = XML_OC.matcher(line);
            Matcher open = XML_O.matcher(line);
            Matcher close = XML_C.matcher(line);
//            Matcher bool = XML_B.matcher(line);
            
            if(openclose.matches()){ 
                tag = cleantag(openclose.group(1));
                String hashtag = (tags.getLast() + "-" + tag);
                tags.add(tag);
                String data = "";
                StringEscapeUtils.unescapeXml(openclose.group(2));
                System.out.println(data);
                if(hashtag.compareTo("page-title") == 0){
                    out.title = data;
                }else if(hashtag.compareTo("page-ns") == 0){
                    try{ out.namespace = Integer.valueOf(data); }catch(Exception e){ System.err.println(data); }
                }else if(hashtag.compareTo("page-id") == 0){
                    try{ out.pageid = Long.valueOf(data); }catch(Exception e){ System.err.println(data); }
                }else if(hashtag.compareTo("revision-id") == 0){
                    out.revid = Long.valueOf(data);
                }else if(hashtag.compareTo("revision-parentid") == 0){
                    out.parentid = Long.valueOf(data);
                }else if(hashtag.compareTo("contributor-id") == 0){
                    out.userid = Long.valueOf(data);
                }else if(hashtag.compareTo("contributor-username") == 0){
                    out.username = data;
                }else if(hashtag.compareTo("revision-timestamp") == 0){
                    out.timestamp = data;
                }else if(hashtag.compareTo("revision-comment") == 0){
                    out.comment = data;
                }          
                tags.remove(tag);
            }else if(open.matches()){
                String data = "";
                StringEscapeUtils.unescapeXml(open.group(2));
                
                tag = cleantag(open.group(1));
                tags.add(tag);
                if(tag.compareTo("text") == 0){
                    intext = true;
                    contents = data + "\n";
                }
            }else if(close.matches()){
                String data = "";
                StringEscapeUtils.unescapeXml(close.group(1));
                
                tag = cleantag(close.group(2));
                
                if(tag.compareTo("text") == 0){
                    intext = false;
                    contents = contents + data;
                }
                
                tags.remove(tag);
            }else{
                if(intext){
                    contents = contents + StringEscapeUtils.unescapeXml(line) + "\n";
                }
            }
        }
        out.text = contents;
        
        return out;
    }
    
    public void print(){
        System.out.println("Title: " + this.title + "\t (" + this.namespace + ":" + this.pageid + ")\nBy: " + this.username + " (" + this.userid + ")\t" + this.timestamp 
                + "\nComment: " + this.comment + "\n" + this.text);
    }
    
    
    private static String cleantag(String tag){
        if(tag.contains(" ")){ tag = tag.substring(0, tag.indexOf(" ")); }
        return tag;    
    }
    
}
