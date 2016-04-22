package wikibot.scripts;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Dump;
import wikibot.Wiki;
import wikibot.objects.XML_Page;

public class WikipediaLinks {
    static final Dump dump = new Dump();
    static final Wiki wikipedia = new Wiki("en.wikipedia.org");
    static final Wiki wiktionary = new Wiki("en.wiktionary.org");
    
    public static void run(){
//        update("predator bug");
        wikipedia.login(); wiktionary.login();
        int chunk = 100, max = 25 * chunk;
        boolean go = true;
        LinkedList<XML_Page> pages;
        TreeSet<String> missing = new TreeSet<>();
        
        do{
            LinkedList<String> titles = new LinkedList<>();
            pages = dump.getXMatches(chunk, "(?s)(.+?)\\{\\{wikipedia(.+?)");
            for(XML_Page page : pages){
                titles.add(page.getTitle());
            }
            System.out.println(titles);
            HashMap<String,Boolean> wp = wikipedia.exists(titles);
            for(String key : wp.keySet()){
                if(!wp.get(key)){
                    String lower = key.substring(0, 1).toLowerCase() + key.substring(1);
                    if(titles.contains(key)){
                        missing.add(key);
                        update(key);
                    }else if(titles.contains(lower)){
                        missing.add(lower);
                        update(lower);
                    }else{
                        System.err.println(key + " (" + lower + ") is missing from pedia and not in the titles?");
                    }
                }
            }
            max -= chunk;
        }while(chunk >= pages.size() && max != 0);
        System.out.println(missing.size() + "\t" + missing);
    }
    
    private static void update(String title){
        String content = wiktionary.read(title);
//        Pattern tw = Pattern.compile("\\[\\[\\p{L}{2,3}:" + title + "]]\\s*");
//        boolean before = true;
//        String a = "", b = "";
        
//        for(String line : content.split("\n")){
//            Matcher m = tw.matcher(line);
//            if(m.matches()){ before = false; }
//            if(before){ a = a + line + "\n"; }else{ b = b + line + "\n"; }            
//        }
//        String out = a + "\n\n[[Category:Wikipedia links to missing pages]]\n\n" + b;
//        out = out.replaceAll("(\\n{3,})", "\n\n");
        String out = content.replaceAll("\\{\\{wikipedia}}", "{{wikipedia|missing=true}}");
        System.out.println(title + "\n\n" + out);
    }
    
    private static String upper(String in){
        return in.substring(0, 1).toUpperCase() + in.substring(1);
    }
    
    class link{
        String wikt;
        String wpda;
        link(String wk, String wp){
            wikt = wk; wpda = wp;
            
        }        
    }
}
