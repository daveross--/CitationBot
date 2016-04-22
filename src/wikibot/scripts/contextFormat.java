package wikibot.scripts;

import wikibot.objects.Result;
import wikibot.Wiki;
import wikibot.tools.Lang;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class contextFormat {
    private static final Wiki wiki = new Wiki();
//    private static String page = "User:TheDaveBot/test";
    private static String category;
    private static final int limit = 5; // how many pages to retrieve at a time
    private static final int chunkSize = 5; // how many pages to read at a time
    private static int counter = 0;
    private static Lang L = new Lang();    
    
    public contextFormat(){}
    public static void embeds(String page){
        wiki.login();
        run(wiki.getEmbeds(page, limit, "0|1000000")); // last continue for context scan: 0|4960554
    }
    public static void inCat(String category){
        wiki.login();
        run(wiki.getCategory(category, limit, "0|0")); 
    }
    public static void run(Result result){    
        TreeSet<String> missing = new TreeSet<>();
        
        // Bundle the pages into chunks to be read together (for efficient API calls).
        int chunked = 0;
        LinkedList<String> pages = result.getPages();
        LinkedList<LinkedList<String>> chunks = new LinkedList<>();
        while(chunked < pages.size()){
            if(pages.size() > chunked+chunkSize){
                chunks.add((new LinkedList<>(pages.subList(chunked, chunked+chunkSize))));
                chunked += chunkSize;
            }else{
                chunks.add(new LinkedList<>(pages.subList(chunked, pages.size())));
                chunked = pages.size();
            }
        }
                
        for(LinkedList<String> chunk : chunks){
            LinkedHashMap<String,String> pageMap;
            if((pageMap = wiki.readAll(chunk)) != null){
                System.out.println("Chunk: " + counter++ + "\t(Processed - " + (counter*chunkSize - chunkSize) + ", remaining - " + pageMap.size() + ")\t [skip: " + result.getContinueString() + "]");

                for(String page : pageMap.keySet()){
                    String contents = pageMap.get(page);
                    LinkedList<String> lines = new LinkedList<>(Arrays.asList(contents.split("\n")));

                    Pattern L2 = Pattern.compile("(\\s*?)==([\\p{L}\\s\\-]+?)==(\\s*?)");
                    Pattern L3 = Pattern.compile("(\\s*?)={3,}([\\p{L}\\s\\-]+?)={3,}(\\s*?)");

                    // improved context which understands when a context has a lang param
                    Pattern context = Pattern.compile("(.*?)" + "(\\{\\{context\\||\\{\\{cx\\|)"
        //                                            + "(?=.*?)((?!lang).)*" // working: (?=.*?)((?!lang).)*
    //                                                + "((?=[^\\}]*)((?!lang=).?)*?)"
                                                    + "(.+?)" + "(}})" + "(.*)(.*?)");

                    String currentLang = null, currentCode = null, currentL3 = null, out = "";
                    boolean update = false, L2skip = true, L3skip = true; 

                    // If we find context in skipL3 we replace it with {{qualifier|}}
                    List<String> skipL3 = Arrays.asList(new String[]{
                            "Translations","References","Etymology","Pronunciation","Synonyms","Antonyms","Anagrams"
                            , "Derived terms", "Related terms", "Usage notes","Declension"});

                    for(String line : lines){
                        Matcher l2 = L2.matcher(line);
                        Matcher l3 = L3.matcher(line);

                        if(l2.matches()){
                            currentLang = l2.group(2);
                            L2skip = ((currentCode = L.getCode(currentLang)) == null); // If we don't know the language we skip any contexts within the section
                        }else if(!L2skip && l3.matches()){
                            currentL3 = l3.group(2).trim();
                            L3skip = skipL3.contains(currentL3); // if we are within an L3+ which is not valid for context we will replace context with qualifier

                        }else if(!L2skip && !L3skip){ // If we have a good language and a good section
                            Matcher cx = context.matcher(line);
                            if(cx.matches()){ // Context line of some kind...
                                String rline = rebuild(page, line, currentCode);
//                                if(line.compareTo(rline) != 0){
                                    System.out.println("Possible change:\t" + page + "\t" + currentCode + "\t\r\n" + line + "\r\n" + rline);
//                                    update = true;
                                    line = rline;
//                                }
                            }
                        }                    
                        // Update the output with the initial line or the updated line
                        out = out + line + "\r\n";
                    }
                    if(update){
                        try{ System.out.println("Y/N?"); System.in.read(); }catch(IOException e){}
                        wiki.edit(page, out, "Updating [[Template:context|context]] with lang.");
                    }else{}            
                }
            }
        }
        
        if(!result.getComplete()){ 
             try{ System.out.println("\r\n\r\nGet the next set of pages?"); System.in.read(); }catch(IOException e){}            
            run(wiki.getEmbeds(result.getReference(), limit, result.getContinueString()));
        }
        if(missing.size() > 0){ System.err.println("Missing: " + missing); }
    }
    
    // This method figures out how to add the lang= param to the context line, if it is applicable
    public static String rebuild(String page, String in, String lang){
        // Check if the string contains {{context or {{cx, if not return input.
        if(!in.contains("{{context|") && !in.contains("{{cx|")){ return in; }
        
        // Split the line into a list of chunks which begin with either {{context| or {{cx|
        int cursor = 0;
        LinkedList<String> chunks = new LinkedList<>();
        while(cursor < in.length()){
            int ct = in.substring(cursor).indexOf("{{context|");
            int cx = in.substring(cursor).indexOf("{{cx|");
            int s = (ct >= 0 && (cx < 0 || ct < cx) ? ct : cx);
            if(s < 0 || s >= in.length()){
                cursor -= (cursor > 0 ? 1 : 0); // back up one if we aren't at the start to account for indexof behavior
                chunks.add(in.substring(cursor));
                cursor = in.length();
            }else{
                s += cursor;
                cursor -= (cursor > 0 ? 1 : 0); // back up one if we aren't at the start to account for indexof behavior
                chunks.add(in.substring(cursor, s));
                cursor = s+1;
            }
        }
       
        // Apply update logic to each chunk, then rebuild
        String out = "";
        for(String chunk : chunks){
            String temp = chunk;
            if(chunk.contains("{{context|") || chunk.contains("{{cx|")){ // only work on the chunk if it contains a context template
                LinkedHashMap<String,String> hash = new LinkedHashMap<>();
                int h = 1;

                hash.put("%0", "%"); 
                temp = temp.replaceAll("%", "%0");

                int ct = temp.indexOf("{{context|"), cx = temp.indexOf("{{cx|");
                int context = (ct >= 0 && (cx < 0 || ct < cx) ? ct : (cx >= 0 ? cx : temp.length()));

                boolean go = temp.contains("}}") && temp.contains("{{") && (context < temp.length());
                try{
                    while(go){
                        int close = temp.indexOf("}}");
                        int open = temp.substring(0,close).lastIndexOf("{{");
                        go = open != context;

                        if(go){
                            String sub = temp.substring(open, close + 2);
                            temp = temp.replaceAll(Pattern.quote(sub), "%"+h);
                            hash.put("%" + h++, sub);
                        }
                    }
                    if(!temp.contains("|lang=") && temp.contains("}}")){
                        temp = temp.substring(0, temp.indexOf("}}")) + "|lang=" + lang + temp.substring(temp.indexOf("}}"));
                    }           
                }catch(Exception e){ System.err.println(page + "\t" + chunk + "\t" + temp); e.printStackTrace(); System.exit(0); }

                // rebuild the string with nested templates
                String[] keys = hash.keySet().toArray(new String[hash.size()]);
                for(int k=keys.length-1; k>=0; k--){
                    String H = hash.get(keys[k]);
                    temp = temp.replaceAll(Pattern.quote(keys[k]), H);
                }
            }
            out = out + temp;
        }        
        return out;
    }
}

/* Original logic...
// Check the L2, if it is a language we know allow to continue, if not record the language and skip to the end
                    if(l2.matches()){
                        currentLang = l2.group(2);
                        L2skip = ((currentCode = Lang.getCode(currentLang)) == null); // If we don't know the language we skip any contexts within the section
                    }else if(!L2skip && l3.matches()){
                        currentL3 = l3.group(2).trim();
                        L3skip = skipL3.contains(currentL3); // if we are within an L3+ which is not valid for context we will replace context with qualifier
                    
                    // If we are in a language we know, and in an acceptable L3, and we match context templates
                    }else if(!L2skip && !L3skip && cx.matches()){
                        if(cx.group(3).contains("{")){ 
                            String rline = rebuild(line, currentCode);
                            if(line.compareTo(rline) != 0){
                                System.out.println("Tough line!\t" + line);  
                                System.out.println(page + "\t" + currentLang + "\t" + rline);
                            }
                        }else{
                            System.out.println(page + "\t" + currentLang + "\t" + line);
//                            line = cx.group(1) + cx.group(2) + cx.group(3) + "|lang=" + currentCode + cx.group(5) + cx.group(6);
                            line = rebuild(line, currentCode);
                            System.out.println(page + "\t" + currentLang + "\t" + line);
                            update = true;
                        }
                    
                    // If we are in a bad L3, even if we don't know the language
                    }else if(L3skip && cx.matches()){
                        if(cx.group(3).contains("{")){ 
                            String rline = rebuild(line, currentCode);
                            if(line.compareTo(rline) != 0){
                                System.out.println("Tough line!\t" + line);  
                                System.out.println(page + "\t" + currentLang + "\t" + rline);
                            }                       
                        }else{
                            System.out.println(page + "\t" + currentLang + "\t" + line);
                            line = cx.group(1) + cx.group(2).replaceAll("(context|cx)","qualifier") + cx.group(3) + "|lang=" + currentCode + cx.group(5) + cx.group(6);
                            System.out.println(page + "\t" + currentLang + "\t" + line);
                            update = true;
                        }

                    // If we don't know the language but we find a context template
                    }else if(L2skip && cx.matches() && currentLang != null){
                        missing.add(currentLang);
                    }

*/