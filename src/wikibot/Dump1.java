package wikibot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.objects.XML_Page;
import wikibot.tools.MD5;
import wikibot.tools.Spreadsheet;

public class Dump1{

    public static String dump = System.getProperty("user.home") + "\\Google Drive\\Java\\enwiktionary-20160407-pages-articles.xml";
    
    private static long cursor = 0, previous = 0;
    private static final Pattern pStart = Pattern.compile("^\\s*?<page>\\s*?$");
    private static final Pattern pEnd = Pattern.compile("^\\s*?</page>\\s*?$");
    private static final LinkedList<Long> index = new LinkedList<>();
    private static final LinkedList<Long> history = new LinkedList<>();
    private static int hIndex = 0;
    
    public Dump1(){}
    public String getTitle(){ return dump.substring(dump.lastIndexOf("\\")+1, dump.lastIndexOf(".xml")); }
    
    public void setCursor(long c){ cursor = c; }
    public long getCursor(){ return cursor; }
    
    public XML_Page get(long cur){
        return get(cur, "", 1).getFirst();
    }
    public LinkedList<XML_Page> get(long cur, String ptrn, int max){
        history.add(cur); hIndex++;
        LinkedList<XML_Page> out = new LinkedList<>();     
        Pattern pattern = Pattern.compile(ptrn);
        long count = cursor = cur; // -1 for full read
        boolean go = true, inPage = false;        
        String line, data = "";
        
        try{
            FileInputStream fis = new FileInputStream(dump);
            fis.skip(cursor);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
//            reader.skip(cursor); // skip to the last place we stopped then read again
            while(go && (line = reader.readLine()) != null){    
                count += line.length();                
                Matcher mStart = pStart.matcher(line), mEnd = pEnd.matcher(line);                
                if(mStart.matches()){
                    inPage = true;
                    data = line + "\n";
                }else if(inPage && mEnd.matches()){
                    data = data + line + "\n";
                    Matcher m = pattern.matcher(data);
                    if(m.matches() || ptrn.isEmpty()){ 
                        XML_Page xmlp = new XML_Page().build(data);
                        if(xmlp.getNS() == 0){ out.add(new XML_Page().build(data)); } // only keep NS:0 for now
                    }                    
                    if(out.size() >= max){
                        cursor = count + line.getBytes("UTF-8").length;
                        go = false;
                    }
                }else{
                    data = data + line + "\n";
                }
            }            
            if(out.size() >= max){ return out; }
        }catch(IOException e){}   
        if(out.size() == 0){ return null; } // there is a better way to find out if we are at the end, I am sure.
        return out;
    }    
    public LinkedList<XML_Page> nextX(int x){
        return get(cursor, "", x);
    }    
    public LinkedList<XML_Page> getXMatches(int x, String p){
        return get(cursor, p, x);
    }
    public void index(){ // read the whole dump and index it so that we can more quickly seek later
        try{
            String line;
            long cur = 0;
            BufferedReader reader = new BufferedReader(new FileReader(dump));
            while((line = reader.readLine()) != null){
                if(line.trim().compareTo("<page>") == 0){
                    index.add(cur);
                }
                cur += line.length();
            }
            System.out.println("Indexed " + index.size() + " pages.");
        }catch(IOException e){}
    }
    
    public TreeSet<String> getTitlesMatching(String pattern, int group){
//        pattern = "^(\\s*)<title>(-?[0-9,.]+?)</title>(\\s*)$";
        pattern = "^(\\s*)<title>((one|two|three|four|five|six|seven|eight|nine"
                                + "|ten|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety"
                                + "|hundred|thousand|million|billion|trillion|quadrillion|quintillion|sextillion|septillion|octillion|nonillion"
                                + "|decillion|undecillion|duodecillion|tresdecillion|-|\\s)+?)</title>(\\s*)$";
        TreeSet<String> out = new TreeSet<>();
        try{
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(dump));
            Pattern p = Pattern.compile(pattern);
            while((line = reader.readLine()) != null){
                Matcher m = p.matcher(line);
                if(m.matches()){
                    out.add(m.group(group));
                }
            }
        }catch(IOException e){
            
        }
        return out;
    }
    public TreeSet<String> getTitlesInLang(String language, int max){
        String title = "^(\\s*)<title>(.+?)</title>(\\s*)$";
        language = "^(.*?)==\\s*" + language + "\\s*==(.*?)$";
        String namespace = "^(\\s*)<ns>(.+?)</ns>(\\s*)$";
        TreeSet<String> out = new TreeSet<>();
        boolean go = true;
        try{
            String line, ns = "";
            BufferedReader reader = new BufferedReader(new FileReader(dump));
            Pattern T = Pattern.compile(title);
            Pattern P = Pattern.compile(language);
            Pattern N = Pattern.compile(namespace);
            while(go && ((line = reader.readLine()) != null)){
                go = (out.size() < max || max < 0);
                Matcher t = T.matcher(line);
                Matcher p = P.matcher(line);
                Matcher n = N.matcher(line);
                if(t.matches()){
                    title = t.group(2);
                }else if(n.matches()){
                    ns = n.group(2);
                }else if(p.matches()){
                    if(ns.compareTo("0") == 0){ out.add(title); }
                }
            }
        }catch(IOException e){ }
        return out;
    }
    public TreeMap<String,String> getPagesMatchingInLanguage(String language, String[] patterns, int max){
        TreeMap<String,String> out = new TreeMap<>();
        LinkedList<LinkedHashMap<String,String>> results = new LinkedList<>();
        String backup = System.getProperty("user.home") + "\\Google Drive\\Java\\" + MD5.hash(dump + patterns + max) + ".xlsx";
        File file = new File(backup);
        try{
            if(file.exists()){
                results = Spreadsheet.read(file);
                for(LinkedHashMap<String,String> lhm : results){
                    out.put(lhm.get("title"), lhm.get("contents"));
                }
            }else{
                Pattern ptrn = Pattern.compile(patterns[0]);
                while(max != 0){ // -1 returns all results
                    XML_Page page = next();
                    String title = page.getTitle();
                    String text = page.getRevisions().getFirst().getText();
                    Matcher mchr = ptrn.matcher(text);
                    if(mchr.matches()){ 
                        max--; 
                        out.put(title, mchr.group(2));
                        LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
                        lhm.put("title", title);
                        lhm.put("contents", mchr.group(2));
                        results.add(lhm);
                    }
                }
                Spreadsheet.printerX(file, results);
            }
        }catch(Exception e){}        
        
        return out;
    }
    
    public boolean diff(String a, String b){
        return a.compareTo(b) != 0;
    }
    
    public XML_Page first(){
        cursor = 0;
        return get(cursor);
    }
    public XML_Page next(){
        return get(cursor);
    }
    public XML_Page previous(){
        System.out.println("History: " + history.size() + "\tIndex: " + hIndex);
        if(hIndex > 0){
            return get(history.get(--hIndex));
        }else{
            return get(0);
        }
    }
    
    public int strFreq(String str, int ns){
        int counter = 0;
        XML_Page page;
        String contents;
        while((page = next()) != null){
            contents = page.getContents();
            if(contents.contains(str)){
                counter += count(contents, str);
                System.out.println(page.getTitle() + "\t" + counter);
            }
        }        
        return counter;
    }
    public HashMap<String,Integer> strFreq(String[] list, int ns){
        XML_Page page;
        String contents;
        HashMap<String,Integer> map = new HashMap<>();
        while((page = next()) != null){
            contents = page.getContents();
            for(String str : list){
                if(contents.contains(str)){
                    Integer i = (int)Spreadsheet.GD(map, str);
                    i += count(contents, str);
                    map.put(str, i);
                }
            }
        }        
        return map;
    }
    private int count(String text, String str){
        int cur = 0, count = 0;
        while(cur >= 0 && cur < text.length()){
            if(text.substring(cur).contains(str)){
                count++;
                cur = text.indexOf(str, cur)+1;
            }else{
                cur = -1;
            }
        }
        return count;
    }
}