package wikibot.scripts;

import java.io.File;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import wikibot.Dump;
import wikibot.Wiki;
import wikibot.objects.XML_Page;
import wikibot.tools.Spreadsheet;

public class RedLinks {
 
    static String language = "Latin";
    static String code = "la";
    static final Wiki wiki = new Wiki();
    
    static int size = -1;
    static final Dump dump = new Dump();
    static long cursor = 280000L;
        
    public static void run(){
        TreeSet<String> dictionary = getWords(language, size);
        dump.setCursor(cursor);
        XML_Page page;
        TreeSet<String> words;
        TreeMap<String,TreeSet<String>> check = new TreeMap<>();
        
        int max = 1000;
        while(max != 0 && (page = dump.next()) != null){
            String contents = page.getContents();
            words = search(contents);
            
            for(String word : words){
                TreeSet<String> t = new TreeSet<>();
                if(check.containsKey(word)){ t = check.get(word); }
                t.add(page.getTitle());
                check.put(word, t);
            }             
            max--;
        }
        
        TreeMap<String,TreeSet<String>> missing = new TreeMap<>();
        for(String k : check.keySet()){
            if(!dictionary.contains(k)){
                missing.put(k, check.get(k));
            }
        }
        
        String write = "";
        for(String k : missing.keySet()){
            String p = "";
            for(String pg : missing.get(k)){ p = p + "[[" + pg + "]] "; }
            write = write + ("# {{m|" + code + "|" + k + "}} (" + p.trim() + ")\n");
        }
        
        wiki.setBot();
        wiki.login();
        wiki.write("User:TheDaveRoss/Missing/" + code, write, "testing");        
    }
    
    private static TreeSet<String> search(String contents){
        TreeSet<String> out = new TreeSet<>();
        
        boolean go = true;
        while(go){
            int[] v = {
                contents.indexOf("{{m|" + code + "|"), contents.indexOf("{{term|" + code + "|"), contents.indexOf("{{mention|" + code + "|")
                , contents.indexOf("{{t|" + code + "|"), contents.indexOf("{{t+|" + code + "|"), contents.indexOf("{{t-check|" + code + "|")
                , contents.indexOf("{{l|" + code + "|")
            };        
            int first = min(v);
            if(first > 0){
                contents = contents.substring(first);
                String sub = decon(contents.substring(0, contents.indexOf("\n")));
//                sub = sub.substring(1, sub.lastIndexOf("}}")).substring(sub.indexOf("|")-1).substring(sub.indexOf("|")); // removes leading and trailing {}, removes template and lang code
                
                sub = sub.substring(2, sub.lastIndexOf("}}"));
                sub = sub.substring(sub.indexOf("|")+1);
                sub = sub.substring(sub.indexOf("|")+1);                
                if(sub.contains("|")){ sub = sub.substring(0, sub.indexOf("|")); }
                
                if(!sub.matches("(.*?)[\\{}\\[\\]\\|](.*?)")){
                    out.add(clean(sub));
                }
                
                contents = contents.substring(1);
            }else{
                go = false;
            }
        }
        
        return out;
    }
    private static String clean(String in){
        // remove all marks which are not used in page titles
        String out = Normalizer.normalize(in, Normalizer.Form.NFD).replaceAll("[^\\p{L}\\p{Z}\\-]", ""); // this is overkill, need to restrict to codepoints that are removed by MW
        
        return out;
    }
    private static int min(int[] args){
        int out = -1;
        for(int arg : args){
            if(arg >= 0){
                if(out < 0){ out = arg; }else if(out > arg){ out = arg; }
            }            
        }
        return out;
    }
    private static String decon(String in){
        int openBraces = 0;
        int closeBraces = 0;
        int openBrackets = 0;
        int closeBrackets = 0;
        
        char[] chars = in.toCharArray();
        int end = 0;
        for(char c : chars){
            end++;
            if(c == '{'){ openBraces++; }
            if(c == '}'){ closeBraces++; }
            if(c == '['){ openBrackets++; }
            if(c == ']'){ closeBrackets++; }
            if(openBraces == closeBraces && openBrackets == closeBrackets){
                break;
            }
        }
        
//        if(in.compareTo(in.substring(0, end)) != 0){ System.out.println("DECON:\n\t" + in + "\n\t" + in.substring(0, end)); }
        return in.substring(0, end);
    }
    private static TreeSet<String> getWords(String lang, int limit){        
        TreeSet<String> words = new TreeSet<>();
        
        String title = dump.getXML() + " (" + lang + ") (" + limit + ")";
        String name = System.getProperty("user.home") + "\\Google Drive\\Java\\" + title + ".xlsx";
        try{
            File file = new File(name);
            if(file.exists()){           
                words = LLtoTS(Spreadsheet.read(file));
                System.out.println("Retrieved " + words.size() + " " + lang + " words from cached file.");
            }else{
                words = dump.getTitlesInLang(lang, limit);
                Spreadsheet.printer(file, words, "word");
                System.out.println("Retrieved " + words.size() + " " + lang + " words from dump.");
            }
        }catch(Exception e){ e.printStackTrace(); }
        return words;
    }    
    private static TreeSet<String> LLtoTS(LinkedList<LinkedHashMap<String,String>> in){
        TreeSet<String> out = new TreeSet<>();
        LinkedHashMap<String,String> first = in.get(0);
        String k = (String)first.keySet().toArray()[0];
        for(LinkedHashMap<String,String> lhm : in){
            out.add(lhm.get(k));
        }
        
        return out;
    }
}
