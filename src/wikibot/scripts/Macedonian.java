package wikibot.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Dump;
import wikibot.Wiki;
import wikibot.objects.JSON_Pages;
import wikibot.objects.JSON_Pages.Page;
import wikibot.objects.JSON_Pages.Page.Revision;

public class Macedonian {
    Wiki wiki = new Wiki();       
    Dump dump = new Dump();
    ArrayList<String> bots = new ArrayList<>();
    ArrayList<String> safe = new ArrayList<>();
    
    public void run(){
        wiki.login();
        bots.addAll(Arrays.asList(new String[]{"MewBot","WingerBot","Rukhabot","Interwicket","Tbot","VolkovBot","RobotGMwikt","AutoFormat","MglovesfunBot","OpiBot","KassadBot"}));
        safe.addAll(Arrays.asList(new String[]{"Martin123xyz"}));

        TreeSet<String> titles = dump.getTitlesInLang("Macedonian", 100000);
//        TreeSet<String> titles = new TreeSet<>(); titles.addAll(Arrays.asList(new String[]{"лето", "липа", "лисица", "лук"}));
//        [а, бес, брашно, в, вата, вторник, господин, да, дом, е, и, камера, лето, липа, лисица, лук, небо, око, палец, по, погром, птица, работа, скот, слава, со, српски, царица, цена, час]
//        String title = "брашно";
        HashMap<String,String> out = new HashMap<>(); // sort into first char with the contents of the page, so we can write to my userspace
        
        int counter = 0;
        String curFC = "";
        
        for(String title : titles){
            JSON_Pages jp = wiki.getRevisions(title);
            String o = ""; String fc = title.substring(0,1);
            if(jp != null){
                Page p = jp.getPage();
                Revision[] revs = p.getRevisions();
                
                System.out.println(++counter + " of " + titles.size() + "\t" + title + "\t" + revs.length);
                
                String prev = "", cur = "", user = "", lastuser = "";
                if(out.containsKey(fc)){ o = out.get(fc); }

                o = o + "{| class=wikitable style=\"width:80%;\"\r\n"
                        + "! style=\"width:10%;\" | Word\r\n! style=\"width:15%;\" | Date\r\n! style=\"width:15%;\" | User\r\n! style=\"width:60%;\" | Comment\r\n";
                boolean update = false;
                if(p != null){
                    for(int r=revs.length-1; r >= 0; r--){
                        Revision rev = revs[r];
                        cur = clean(rev.getContent(), p.getTitle()).trim();
                        user = rev.getUser();
                        if(!bots.contains(user)){ //  && !safe.contains(user)
                            if(!cur.isEmpty() && cur.compareTo(prev) != 0){
                                update = true;
                                try{
                                    o = o + "|-\r\n"
                                        + "| [https://en.wiktionary.org/w/index.php?title=" + URLEncoder.encode(p.getTitle(),"UTF-8") + "&type=revision&diff=" + "next" + "&oldid=" + rev.getParent() + " " + title + "] "
                                        + "|| " + rev.getTimestamp() + " "
                                        + "|| " + rev.getUser() + " "
                                        + "|| <nowiki>" + rev.getComment() + "</nowiki>"
                                        + "\r\n";
                                }catch(UnsupportedEncodingException e){}
                            }
                            lastuser = user;
                        }
                        prev = cur;
                    }
                    o = o + "|}\r\n";
                    if(safe.contains(lastuser)){ update = false; }
                }
                if(update){ out.put(fc, o); }
            }
            String page = "User:TheDaveRoss/Macedonian/";
            if(!curFC.isEmpty() && curFC.compareTo(fc) != 0){
                try{
                    File f = new File("C:\\Macedonian\\" + curFC + ".txt");
                    if(curFC.matches("(\\p{Lu}+?)")){ f = new File("C:\\Macedonian\\" + curFC + " upper.txt"); }
                    
                    if(out.containsKey(curFC)){
                        if(!f.exists()){ f.createNewFile(); }
                        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                        bw.write(out.get(curFC));
                        bw.close();
                    }
                }catch(IOException e){ }   
                curFC = fc;
            }else{ curFC = fc; }         
        }

    }
    
    public String diff(String a, String b){
        String[] A = a.split("\n"), B = b.split("\n");
        int max = (A.length > B.length ? A.length : B.length);
        String aCur = "", bCur = "", out = "";
        
        for(int i=0; i<max; i++){         
            if(A.length > i){ aCur = A[i]; }else{ aCur = ""; }
            if(B.length > i){ bCur = B[i]; }else{ bCur = ""; }
            String d = (aCur.compareTo(bCur) != 0 ? "x" : "o");
            out = out + pad(aCur,100) + d + "\t" + pad(bCur,100) + "\n";            
        }
        return out;
    }
    
    private String clean(String in, String page){
        String out = "";
        String lang = "";
        
        Pattern iwiki = Pattern.compile("^\\s*\\[\\[(\\p{L}+?):" + page + "]]\\s*$");
        Pattern blank = Pattern.compile("^\\s*?$");
        Pattern L2 = Pattern.compile("^\\s*==\\s*([^=]+?)\\s*==\\s*$");        
        
        for(String line : in.split("\n")){
            Matcher iw = iwiki.matcher(line);
            Matcher bl = blank.matcher(line);
            Matcher l2 = L2.matcher(line);
            if(l2.matches()){ lang = l2.group(1); }
            if(lang.compareTo("Macedonian") == 0 && !iw.matches() && !bl.matches()){
                out = out + line + "\n";
            }
        }
        if(out.compareTo(in) != 0){ 
            return out; 
        }else{ return null; }
    }
    
    private String pad(String in, int p){
        String pad = "";
        for(int i=p; i>0; i--){ pad = pad + " "; }
        in = (in + pad).substring(0,p);
        return in;
    }
}
