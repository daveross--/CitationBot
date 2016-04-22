package wikibot.objects;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XML_Page {
    public String TITLE;
    int NS;
    long ID; 
    String RESTRICTIONS;
    LinkedList<Revision> REVISIONS = new LinkedList<>();
  
    public XML_Page(){}
    
    public XML_Page build(String in){
        Pattern x = Pattern.compile("(?s)^([^<>]*?)$");
        Pattern a = Pattern.compile("(?s)^\\s*?<(.+?[^/]*)>(.+?)</(.+?)>\\s*?$"); // open and close
        Pattern b = Pattern.compile("(?s)^\\s*?<(.+?)/>\\s*?$"); // single open-close tag
        Pattern c = Pattern.compile("(?s)^\\s*?<([^/]+?)>([^<]*?)$"); // open and data, no close
        Pattern d = Pattern.compile("(?s)^(.*?)</(.+?)>\\s*?$"); // data and close, no open       
        Pattern e = Pattern.compile("(?s)^\\s*?</(.+?)>\\s*?$"); // close, no open        
        
        String data = "";
        boolean inRev = false;
        Revision rev = new Revision();
        
        for(String line : in.split("\n")){            
            Matcher X = x.matcher(line);
            Matcher A = a.matcher(line);
            Matcher B = b.matcher(line);
            Matcher C = c.matcher(line);
            Matcher D = d.matcher(line);
            Matcher E = e.matcher(line);
            
            if(inRev && X.matches()){ data = data + line + "\n"; 
            }else if(A.matches()){
                String a1 = A.group(1).trim(), a2 = A.group(2).trim(), a3 = A.group(3).trim();
                if(inRev){
                    if(a1.compareTo("id") == 0){ 
                        rev.id = Long.valueOf(a2);
                    }else if(a1.compareTo("parentid") == 0){
                        rev.parentid = Integer.valueOf(a2);
                    }else if(a1.startsWith("timestamp")){
                        rev.timestamp = a2;
                    }else if(a1.startsWith("username")){
                        rev.username = a2;
                    }else if(a1.startsWith("ip")){
                        rev.username = a2;
                    }else if(a1.startsWith("comment")){
                        rev.comment = a2;
                    }else if(a1.startsWith("text")){                      
                    }else{ }
                }else{
                    if(a1.compareTo("id") == 0){ 
                        this.ID = Long.valueOf(a2);
                    }else if(a1.compareTo("title") == 0){
                        try{
                            this.TITLE = URLDecoder.decode(a2,"UTF-8");
                        }catch(Exception ex){ this.TITLE = a2; }
                    }else if(a1.startsWith("ns")){
                        this.NS = Integer.valueOf(a2);
                    }else{ }
                }
            }else if(B.matches()){
                String b1 = B.group(1).trim();
                if(b1.startsWith("bot")){ rev.bot = true; // bot isn't flagged in the dumps?
                }else if(b1.startsWith("minor")){ rev.minor = true; }
            }else if(C.matches()){
                String c1 = C.group(1).trim(), c2 = C.group(2).trim();
                if(c1.compareTo("revision") == 0){ 
                    inRev = true;
                    rev = new Revision();
                }else if(c1.startsWith("text")){
                    data = c2 + "\n";
                }
            }else if(D.matches()){
                String d1 = D.group(1).trim(), d2 = D.group(2).trim();
                if(d2.compareTo("revision") == 0){ 
                    inRev = false;
                    rev.text = d1 + data;
                    this.REVISIONS.add(rev);
                }
            }else if(E.matches()){
                String e1 = E.group(1).trim();
            }
        }                       
        return this;
    }
    
    public String getWord(){ return getTitle(); }
    public String getTitle(){ return TITLE; }
    public String getContents(){
        return this.REVISIONS.get(0).getText();
    }
    public long getID(){ return this.ID; }
    public int getNS(){ return this.NS; }
    public LinkedList<Revision> getRevisions(){ return this.REVISIONS; }
    public void printAll(){
        for(Revision rev : this.REVISIONS){
            System.out.println("TT:\t" + this.TITLE);
            rev.print();
        }
    }    
    public class Revision{
        long id;
        long parentid;
        String timestamp;
        String username;
        String comment;
        String text;
        boolean minor;
        boolean bot;
        
        void print(){
            System.out.println(
            "ID:\t" + this.id + "\n"
//            + "PID:\t" + this.parentid + "\n"
            + "TS:\t" + this.timestamp + "\n"
            + "UN:\t" + this.username + "\n"
            + "MN:\t" + this.minor + "\n"
//            + "BOT:\t" + this.bot + "\n"
            + "CM:\t" + this.comment + "\n"
//           + "TX:\t" + this.text.replaceAll("\n", " ") + "\n"
            );
        }
        
        public long getID(){ return this.id; }
        public String getTimestamp(){ return this.timestamp; }
        public String getUsername(){ return this.username; }
        public String getComment(){ return this.comment; }
        public String getText(){ return this.text; }
        
        void setID(long id){ this.id = id; }
        void setParentID(long id){ this.parentid = id; }
        void setTimestamp(String ts){ this.timestamp = ts; }
        void setUsername(String un){ this.username = un; }
        void setComment(String cm){ this.comment = cm; }
        void setText(String tx){ this.text = tx; }
    }
}
/*
  <page>
    <title>Wiktionary:Welcome, newcomers</title>
    <ns>4</ns>
    <id>6</id>
    <restrictions>edit=autoconfirmed:move=sysop</restrictions>
    <revision>
      <id>33678382</id>
      <parentid>33678346</parentid>
      <timestamp>2015-07-28T19:44:36Z</timestamp>
      <contributor>
        <username>-sche</username>
        <id>444485</id>
      </contributor>
      <minor />
      <comment>Reverted edits by [[Special:Contributions/Glory of Space|Glory of Space]]. If you think this rollback is in error, please leave a message on my talk page.</comment>
      <model>wikitext</model>
      <format>text/x-wiki</format>
      <text xml:space="preserve">CONTENT...
        CONTENT...       
      CONTENT</text>
      <sha1>pdgvb0p8s6p28xkxyz0holcmfurq77p</sha1>
    </revision>
  </page>
*/