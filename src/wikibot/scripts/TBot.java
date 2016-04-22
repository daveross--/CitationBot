package wikibot.scripts;

import wikibot.objects.Result;
import wikibot.Wiki;
import wikibot.tools.Lang;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

public class TBot { // Translation section cleanup and related tasks.
    private static final Wiki wiki = new Wiki();
    
    private static final int limit = 50;
    private static final Lang L = new Lang(); // this has the list of languages and codes
    private static final List<String> skip = Arrays.asList(new String[]{"zh","crh","sco","ba","dsb","nv","fur","ig","ceb"}); // These langs have funky normalizations...    
 
    private static final Pattern LX     = Pattern.compile("(\\s*?)={2,}([\\p{L}\\s\\-]+?)={2,}(\\s*?)");    
    private static final Pattern TSec   = Pattern.compile("(\\s*?)={3,}(\\s*)[Tt]ranslation[s]*(\\s*)={3,}(\\s*?)");    
    private static final Pattern Trans  = Pattern.compile("(\\s*?)(\\{\\{trans-(top|mid|bottom|see))(.*?)");
    private static final Pattern Blank  = Pattern.compile("^(\\s*?)$");
    private static final Pattern Language   = Pattern.compile("^([#\\:\\*\\s]*?)([\\p{IsAlphabetic}\\s\\-]+)(\\:)(.+?)$");
    private static final Pattern T          = Pattern.compile("(.*?)"
                                                            + "(\\{\\{)"
                                                            + "(t[\\+]*)"
                                                            + "(\\|)"
                                                            + "([A-Za-z]{2,})"
                                                            + "(\\|)"
                                                            + "([^\\|\\}]++)"
                                                            + "(.+?)");
    
    public TBot(){}
    public static void embeds(){
        String _page = "Template:t";
        wiki.login();
        run(wiki.getEmbeds(_page, limit, "0|500"));  // water=3749997, hoi polloi=100000
    }
    public static void run(Result result){ // hoi polloi is the first example to be used...
        result = result.retrieve(wiki, 100);
        LinkedList<String> pages = result.getPages(); 
        LinkedHashMap<String,String> contents = result.getContents();
        
        for(String page : pages){            
            boolean update = false; // if we make changes which need to be saved, set this to true.
            
            String content = contents.get(page);
            String[] sections = split(content);
            
            String lines = "";
            for(String line : sections[1].split("\n")){
                Matcher lx = LX.matcher(line);
                Matcher tr = Trans.matcher(line);
                Matcher bl = Blank.matcher(line);
                Matcher la = Language.matcher(line);
                // Skip lines we don't want to work on...
                if(lx.matches() || tr.matches() || bl.matches()){
                }else{                    
                    if(la.matches()){
                        String lang = la.group(2).trim();
                        LinkedList<String> chunks = chunks(la.group(4));                        
                        System.out.println(pad(page,24) + "\t" + pad(lang,16) + "\t" + L.getCode(lang) + "\t" + la.group(4));
                        
                        for(String chunk : chunks){
                            String o = "";
                            Matcher t = T.matcher(chunk);
                            if(t.matches()){
                                String temp = t.group(3);
                                String code = L.getWiki(t.group(5));
                                String term = t.group(7).trim();  
                                String norm = (Normalizer.normalize(term, Normalizer.Form.NFD).replaceAll("\\p{M}", ""));
                                String iwik = term;
                                 
                                if(L.getCode(lang) != null && !skip.contains(code)){
                                    boolean FLterm = wiki.interwiki(code, term);
                                    boolean FLnorm = false; 
                                    boolean IWterm = false;
                                    if(!FLterm){ FLnorm = wiki.interwiki(code, norm); } // We don't need to look again if the first look gets it
                                    if(!FLterm && !FLnorm){
                                        iwik = wiki.normalize(lang, term);
                                        IWterm = wiki.interwiki(code, iwik);
                                    }
                                    
                                    if(FLterm || FLnorm || IWterm){
                                        if(!temp.contains("+")){
                                            System.out.println("Wrong t: " + pad(page,20) + "\t" + temp + "\t" + code + "\t" + term + " (" + FLterm + ")\t" + norm + " (" + FLnorm + ")\t" + iwik + " (" + IWterm + ")");
                                        }
                                    }else{
                                        if(temp.contains("+")){
                                            System.out.println("Wrong t: " + pad(page,20) + "\t" + temp + "\t" + code + "\t" + term + " (" + FLterm + ")\t" + norm + " (" + FLnorm + ")\t" + iwik + " (" + IWterm + ")");
                                        }                                    
                                    }
                                }
                                // @todo: create a pageHasLang(Page, Language) method on Wiki so we can check if the local page for the translation has the relevant language (or exists at all)                                
                            }
                        }                
                    }
                }
            }
            
            // rebuild page if we made changes to the trans sections
            if(lines.compareTo(sections[1]) != 0){ content = sections[0] + lines + sections[2]; }            
            // Update the page if necessary.
            if(update){ page = "User:TheDaveBot/tbot";
                System.out.println(content);
//                Wiki.edit(page, sections[0] + sections[1] + sections[2], "Testing some new [[User:Tbot|Tbot]] functionality.");
            }
        }        
    }    
        
    private static String pageFromT(String t){
        /* {{t|
                1= language code (required)
                2= word (required) May contain markup, remove all non-chars/whitespace from this before working with it...
                3=,4=,5= (optional) gender, number, etc.
                sc= script (optional)
                tr= transliteration (optional)
                alt= alternate form (optional)
        */
        
        return t;
    }
    
    private static LinkedList<String> chunks(String in){
        LinkedList<String> chunks = new LinkedList<>();        
        if(!in.contains("{{t|") && !in.contains("{{t+|")){ chunks.add(in); return chunks; }
        
        // Split the line into a list of chunks which begin with either {{t| or {{t+|
        int cursor = 0;
        while(cursor < in.length()){
            int ct = in.substring(cursor).indexOf("{{t|");
            int cx = in.substring(cursor).indexOf("{{t+|");
            int s = (ct >= 0 && (cx < 0 || ct < cx) ? ct : cx);
            if(s < 0 || s >= in.length()){
                cursor -= (cursor > 0 ? 1 : 0); // back up one if we aren't at the start to account for indexof behavior
                chunks.add(in.substring(cursor).trim());
                cursor = in.length();
            }else{
                s += cursor;
                cursor -= (cursor > 0 ? 1 : 0); // back up one if we aren't at the start to account for indexof behavior
                chunks.add(in.substring(cursor, s).trim());
                cursor = s+1;
            }
        }   
        
        return chunks;
    }
    private static String[] split(String content){
        String pretrans = "", transSec = "", posttrans = ""; // store the sections of the page in these so we can modify and rebuild later
        boolean before = true, during = false, after = false;        
        
        for(String line : content.split("\n")){
            Matcher trans = TSec.matcher(line), lx = LX.matcher(line);
            // determine which section we are in
            if(trans.matches() && !after){ during = true; before = false; after = false;
            }else if(lx.matches() && during){ during = false; before = false; after = true; }

            // build sections
            if(before){ pretrans = pretrans + line + "\n";
            }else if(during){ transSec = transSec + line + "\n";
            }else{ posttrans = posttrans + line + "\n"; }
        }
        
        return new String[]{pretrans, transSec, posttrans};
    }
    private static String pad(String in, int width){
        String p = "";
        for(int i=0; i<width; i++){ p = p + " "; }
        return (in + p).substring(0,width);
    }
}

        // change {{t}} to {{t+}} if the foreign language wiki includes the term
        // validate language code, add if possible where missing (based on language stated in the line or in the super-line
        // add script if missing and can be recognized        
        // organize the section, including {{trans-top/bottom}} and balance with {{trans-mid}}

        // create lists in user space with all of the redlink words by language: * {{term|xxx}} - [[from page]], ''gloss''
        

