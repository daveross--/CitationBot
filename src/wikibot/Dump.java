package wikibot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
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

public class Dump {

    private final String xml = "enwiktionary-20160407-pages-articles";
    public final String dump = System.getProperty("user.home") + "\\Google Drive\\Java\\" + xml + ".xml";
    
    private static long cursor = 0, previous = 0;
    private static final Pattern pStart = Pattern.compile("^\\s*?<page>\\s*?$");
    private static final Pattern pEnd = Pattern.compile("^\\s*?</page>\\s*?$");
    private static final LinkedList<Long> index = new LinkedList<>();
    private static final LinkedList<Long> history = new LinkedList<>();
    private static int hIndex = 0;
    private boolean save = false;
    
    
    public Dump(){}
    public String getTitle(){ return dump.substring(dump.lastIndexOf("\\")+1, dump.lastIndexOf(".xml")); }
    
    public void setCursor(long c){ cursor = c; }
    public long getCursor(){ return cursor; }
    
    public XML_Page get(long cur){
        return get(cur, "", 1).getFirst();
    }
    public String getXML(){ return this.xml; }
    public void setSave(boolean b){ save = b; }
    public LinkedList<XML_Page> get(long cur, String ptrn, int max){
        history.add(cur); hIndex++;
        LinkedList<XML_Page> out = new LinkedList<>();     
        Pattern pattern = Pattern.compile(ptrn);
        long count = cursor = cur; // -1 for full read
        boolean go = true, inPage = false;        
        String line, data = "";
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(dump));
            reader.skip(cursor); // skip to the last place we stopped then read again
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
                        cursor = count + line.length();
                        go = false;
                    }
                }else{
                    data = data + line + "\n";
                }
            }            
            if(out.size() >= max){ return out; }
        }catch(IOException e){}        
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
        long cur = 0L;
        String title = "^(\\s*)<title>(.+?)</title>(\\s*)$";
        language = "^(.*?)==\\s*" + language + "\\s*==(.*?)$";
        String namespace = "^(\\s*)<ns>(.+?)</ns>(\\s*)$";
        TreeSet<String> out = new TreeSet<>();
        LinkedList<LinkedHashMap<String,String>> results = new LinkedList<>();
        String backup = System.getProperty("user.home") + "\\Google Drive\\Java\\" + MD5.hash(xml + language + max) + ".xlsx";
        File file = new File(backup);
        
        boolean go = true;
        try{
            if(file.exists()){
                results = Spreadsheet.read(file);
                for(LinkedHashMap<String,String> lhm : results){
                    out.add(lhm.get("title"));
                }
            }else{
                int counter = 0;
                String line, ns = "";
                BufferedReader reader = new BufferedReader(new FileReader(dump));
                Pattern T = Pattern.compile(title);
                Pattern P = Pattern.compile(language);
                Pattern N = Pattern.compile(namespace);
                while(go && ((line = reader.readLine()) != null)){
                    LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
                    go = (out.size() < max || max < 0);
                    Matcher t = T.matcher(line);
                    Matcher p = P.matcher(line);
                    Matcher n = N.matcher(line);
                    if(t.matches()){
                        title = t.group(2);
                    }else if(n.matches()){
                        ns = n.group(2);
                    }else if(p.matches()){
                        if(ns.compareTo("0") == 0){ 
//                            System.out.println(++counter + "\t" + title + "\t" + cur);
                            out.add(title); 
                            lhm.put("title", title);
                            lhm.put("cursor", Long.toString(cur));
                            results.add(lhm);
                        }                        
                    }
                    cur = cur + line.length();
                }
                Spreadsheet.printerX(file, results);
            }
        }catch(IOException e){ }
        
        
        return out;
    }
    public TreeMap<String,String> getPagesMatchingInLanguage(String language, String[] patterns, int max){
        TreeMap<String,String> out = new TreeMap<>();
        LinkedList<LinkedHashMap<String,String>> results = new LinkedList<>();
        String backup = System.getProperty("user.home") + "\\Google Drive\\Java\\" + MD5.hash(xml + patterns + max) + ".xlsx";
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
    
    public LinkedList<LinkedHashMap<String,String>> getTransToLang(String code, int max){
        LinkedList<LinkedHashMap<String,String>> list = new LinkedList<>();
        long cur = 0L;
        String title = "^(\\s*)<title>(.+?)</title>(\\s*)$";
        String namespace = "^(\\s*)<ns>(.+?)</ns>(\\s*)$";
        String trans = "^(.*?)={3,6}\\s*" + "[Tt]ranslation(s?)" + "\\s*={3,6}(.*?)$";
        String elex = "^\\s*={2,6}\\s*([^=]+?)\\s*={2,6}\\s*$";
        String eltwo = "^\\s*==\\s*([^=]+?)\\s*==\\s*$";
        String transline = "(.+?)\\{\\{t[+-]?\\|" + code + "\\|([^\\\\}|]+)(.+?)}}(.*?)";
        String glossline = "(.*?)\\{\\{trans-top(\\|(.+?))?}}(.*?)";        
        
        TreeSet<String> out = new TreeSet<>();
        TreeSet<String> POSs = new TreeSet<>(Arrays.asList(new String[]{"Noun","Verb","Adverb","Adjective","Participle","Article","Counter","Determiner","Interjection"
                ,"Particle", "Preposition", "Pronoun", "Proper noun","Numeral","Posposition","Contraction","Conjunction","Circumposition","Ambiposition","Classifier"}));
        LinkedList<LinkedHashMap<String,String>> results = new LinkedList<>();
        String backup = System.getProperty("user.home") + "\\Google Drive\\Java\\" + MD5.hash(xml + code + max) + ".xlsx";
        File file = new File(backup);
        
        boolean go = true;
        try{
            if(file.exists()){
                results = Spreadsheet.read(file);
                for(LinkedHashMap<String,String> lhm : results){
                    out.add(lhm.get("title"));
                }
                list = results;
            }else{
                int counter = 0;
                String line, ns = "", pos = "", gloss = "";
                BufferedReader reader = new BufferedReader(new FileReader(dump));
//                reader.skip(0); // testing
                Pattern TITLE = Pattern.compile(title);
                Pattern TRANS = Pattern.compile(trans);
                Pattern NS = Pattern.compile(namespace);
                Pattern LX = Pattern.compile(elex);
                Pattern L2 = Pattern.compile(eltwo);
                Pattern TL = Pattern.compile(transline);
                Pattern GL = Pattern.compile(glossline);
                boolean inTrans = false, ns0 = false;
                
                while(go && ((line = reader.readLine()) != null)){
                    LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
                    Matcher t = TITLE.matcher(line);
                    Matcher tr = TRANS.matcher(line);
                    Matcher n = NS.matcher(line);
                    Matcher lx = LX.matcher(line);
                    Matcher tl = TL.matcher(line);
                    Matcher gl = GL.matcher(line);
                    Matcher l2 = L2.matcher(line);
                    
                    if(t.matches()){ inTrans = false;
                        title = t.group(2);
                    }else if(n.matches()){
                        ns = n.group(2);
                        ns0 = ns.compareTo("0") == 0;
                    }else if(tr.matches()){
                        inTrans = true;
                    }else if(l2.matches()){
                        inTrans = false;
                        pos = "";
                    }else if(lx.matches()){
                        if(POSs.contains(lx.group(1))){ pos = lx.group(1); }
                        inTrans = false;
                    }else if(inTrans){
                        if(gl.matches()){
                            gloss = gl.group(3);
                        }else if(ns0 && tl.matches()){ 
                            counter++;
                            results.addAll(getTransToLang_split(title, pos, line, gloss));
                        }                        
                    }
                    cur = cur + line.length();
                    go = (results.size() < max || max < 0);
                }
                Spreadsheet.printerX(file, results);
                list = results;
            }
        }catch(IOException e){ }
        
        return list;
    }
    private LinkedList<LinkedHashMap<String,String>> getTransToLang_split(String title, String pos, String line, String gloss){     
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
        while(line.contains("{{t")){
            String sub = line.substring(line.indexOf("{{t"), line.indexOf("}}", line.indexOf("{{t"))+2);
            sub = sub.replaceAll("(\\{|\\})","");
            String[] s = sub.split("\\|");
            LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
            lhm.put("title", title);
            lhm.put("pos", pos);
            if(s.length > 2){ lhm.put("trans", s[2]); }else{ System.err.println("BAD: " + line); return new LinkedList<>(); }
            if(s.length > 3){ lhm.put("g1", s[3]); }else{ lhm.put("g1",""); }
            if(s.length > 4){ lhm.put("g2", s[4]); }else{ lhm.put("g2",""); }
            if(s.length > 5){ lhm.put("g3", s[5]); }else{ lhm.put("g3",""); }
            lhm.put("gloss", gloss);
            lhm.put("line", line);
            line = line.substring(line.indexOf("{{t") + sub.length());
            out.add(lhm);
        }        
        return out;
    }
    
    public TreeSet<String> transSectionNoTemplates(int max){
        long cur = 0L;
        String title = "^(\\s*)<title>(.+?)</title>(\\s*)$";
        String TRp = "^\\s*={3,5}\\s*[Tt]ranslation(s?)\\s*={3,5}\\s*$";
        String LXp = "^\\s*={2,6}\\s*(.+?)\\s*={2,6}\\s*$";
        String namespace = "^(\\s*)<ns>(.+?)</ns>(\\s*)$";
        TreeSet<String> out = new TreeSet<>();
        LinkedList<LinkedHashMap<String,String>> results = new LinkedList<>();
        String backup = System.getProperty("user.home") + "\\Google Drive\\Java\\" + MD5.hash(xml + "trans no temp" + max) + ".xlsx";
        File file = new File(backup);
        
        boolean go = true;
        try{
            if(file.exists()){
                results = Spreadsheet.read(file);
                for(LinkedHashMap<String,String> lhm : results){
                    out.add(lhm.get("title"));
                }
            }else{
                int counter = 0;
                String line, ns = "";
                BufferedReader reader = new BufferedReader(new FileReader(dump));
                Pattern T = Pattern.compile(title);
                Pattern TR = Pattern.compile(TRp);
                Pattern LX = Pattern.compile(LXp);
                Pattern N = Pattern.compile(namespace);
                boolean inTrans = false, temps = false;
                while(go && ((line = reader.readLine()) != null)){
                    LinkedHashMap<String,String> lhm = new LinkedHashMap<>();
                    go = (out.size() < max || max < 0);
                    Matcher t = T.matcher(line);
                    Matcher n = N.matcher(line);
                    Matcher tr = TR.matcher(line);
                    Matcher lx = LX.matcher(line);
                    if(t.matches()){
                        inTrans = false; temps = false;
                        title = t.group(2);
                    }else if(n.matches()){
                        ns = n.group(2);
                    }else if(tr.matches()){
                        inTrans = true;
                        if(ns.compareTo("0") == 0){ 
                            System.out.println(++counter + "\t" + title + "\t" + cur);
                            out.add(title); 
                            lhm.put("title", title);
                            lhm.put("cursor", Long.toString(cur));
                            results.add(lhm);
                        }                        
                    }else if(lx.matches()){ 
                        if(inTrans && temps){
                            
                        }
                        
                        inTrans = false; temps = false;
                    }
                    cur = cur + line.length();
                }
                Spreadsheet.printerX(file, results);
            }
        }catch(IOException e){ }        
        
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
}

/*
<mediawiki xmlns="http://www.mediawiki.org/xml/export-0.10/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd" version="0.10" xml:lang="en">
  <siteinfo>
    <sitename>Wiktionary</sitename>
    <dbname>enwiktionary</dbname>
    <base>https://en.wiktionary.org/wiki/Wiktionary:Main_Page</base>
    <generator>MediaWiki 1.27.0-wmf.7</generator>
    <case>case-sensitive</case>
    <namespaces>
      <namespace key="0" case="case-sensitive" />
     </namespaces>
  </siteinfo>
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

/*  
        Document dom;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            FileInputStream fsr = new FileInputStream(dump);
            DocumentBuilder db = dbf.newDocumentBuilder();
            
            dom = db.parse(fsr);

            Element doc = dom.getDocumentElement();
            NodeList nodes = doc.getChildNodes();
            
            for(int i=0; i<nodes.getLength(); i++){
                System.out.println(nodes.item(i));
            }
            
        }catch(Exception e){}
 */

/*
      <namespace key="-2" case="case-sensitive">Media</namespace>
      <namespace key="-1" case="first-letter">Special</namespace>
      <namespace key="0" case="case-sensitive" />
      <namespace key="1" case="case-sensitive">Talk</namespace>
      <namespace key="2" case="first-letter">User</namespace>
      <namespace key="3" case="first-letter">User talk</namespace>
      <namespace key="4" case="case-sensitive">Wiktionary</namespace>
      <namespace key="5" case="case-sensitive">Wiktionary talk</namespace>
      <namespace key="6" case="case-sensitive">File</namespace>
      <namespace key="7" case="case-sensitive">File talk</namespace>
      <namespace key="8" case="first-letter">MediaWiki</namespace>
      <namespace key="9" case="first-letter">MediaWiki talk</namespace>
      <namespace key="10" case="case-sensitive">Template</namespace>
      <namespace key="11" case="case-sensitive">Template talk</namespace>
      <namespace key="12" case="case-sensitive">Help</namespace>
      <namespace key="13" case="case-sensitive">Help talk</namespace>
      <namespace key="14" case="case-sensitive">Category</namespace>
      <namespace key="15" case="case-sensitive">Category talk</namespace>
      <namespace key="90" case="case-sensitive">Thread</namespace>
      <namespace key="91" case="case-sensitive">Thread talk</namespace>
      <namespace key="92" case="case-sensitive">Summary</namespace>
      <namespace key="93" case="case-sensitive">Summary talk</namespace>
      <namespace key="100" case="case-sensitive">Appendix</namespace>
      <namespace key="101" case="case-sensitive">Appendix talk</namespace>
      <namespace key="102" case="case-sensitive">Concordance</namespace>
      <namespace key="103" case="case-sensitive">Concordance talk</namespace>
      <namespace key="104" case="case-sensitive">Index</namespace>
      <namespace key="105" case="case-sensitive">Index talk</namespace>
      <namespace key="106" case="case-sensitive">Rhymes</namespace>
      <namespace key="107" case="case-sensitive">Rhymes talk</namespace>
      <namespace key="108" case="case-sensitive">Transwiki</namespace>
      <namespace key="109" case="case-sensitive">Transwiki talk</namespace>
      <namespace key="110" case="case-sensitive">Wikisaurus</namespace>
      <namespace key="111" case="case-sensitive">Wikisaurus talk</namespace>
      <namespace key="114" case="case-sensitive">Citations</namespace>
      <namespace key="115" case="case-sensitive">Citations talk</namespace>
      <namespace key="116" case="case-sensitive">Sign gloss</namespace>
      <namespace key="117" case="case-sensitive">Sign gloss talk</namespace>
      <namespace key="828" case="case-sensitive">Module</namespace>
      <namespace key="829" case="case-sensitive">Module talk</namespace>
      <namespace key="2300" case="case-sensitive">Gadget</namespace>
      <namespace key="2301" case="case-sensitive">Gadget talk</namespace>
      <namespace key="2302" case="case-sensitive">Gadget definition</namespace>
      <namespace key="2303" case="case-sensitive">Gadget definition talk</namespace>
      <namespace key="2600" case="first-letter">Topic</namespace>
*/