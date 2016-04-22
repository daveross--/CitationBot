package wikibot.deprecated;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.*;
import wikibot.Wiki;
import wikibot.objects.Book;
import wikibot.objects.Citation;

public class CiteBot_OLD {
    // Read a text, parse it into citation sized chunks and update citations pages with the citations for less-common words.
    private final String work = "C:\\Users\\davidr\\Google Drive\\Java\\CitationBot\\text\\"; // work   
    private final String home = "C:\\Users\\dave\\Google Drive\\Java\\CitationBot\\text\\"; // home
    private final String root = root(); // chooses home or work based on what exists
 
//  DONE (limited)   
    // 1600s
//    private final String file = root + "KJV.txt"; 1611 // no authors
// /*bad fmt*/   private final Book BOOK = new Book().setAuthor("John Milton").setTitle("Paradise Lost").setYear("1674").setFile(root + "Milton, Paradise Lost (1674).txt");
// /*done*/   private final Book BOOK = new Book().setAuthor("John Bunyan").setTitle("The Pilgrim's Progress").setYear("1678").setFile(root + "Bunyan, The Pilgrim's Progress (1678).txt");
    // 1700s
//    private final Book BOOK = new Book().setAuthor("Daniel Defoe").setTitle("Robinson Crusoe").setYear("1719").setFile(root + "Defoe, Robinson Crusoe (1719).txt");
    // 1800s
//    private final Book BOOK = new Book().setAuthor("Jane Austen").setTitle("Pride and Prejudice").setYear("1813").setFile(root + "Austen, Pride and Prejudice (1813).txt");
//    private final Book BOOK = new Book().setAuthor("Mary Shelley").setTitle("Frankenstein").setYear("1818").setFile(root + "Shelley, Frankenstein (1818).txt");
//    private final Book BOOK = new Book().setAuthor("Charles Dickens").setTitle("A Christmas Carol").setYear("1843").setFile(root + "Dickens, A Christmas Carol (1843).txt");
//    private final Book BOOK = new Book().setAuthor("Herman Melville").setTitle("Moby Dick").setYear("1851").setFile(root + "Melville, Moby Dick (1851).txt");
    private final Book BOOK = new Book().setAuthor("Bram Stoker").setTitle("Dracula").setYear("1897").setFile(root + "Stoker, Dracula (1897).txt");
    // 1900s
    
    private final int minLength = 128; // chars
    private final int maxLength = 512; // chars
    private final int maxCites = 3; // right now this prevents words which occur in more than X sentences...
    
    private final boolean splitSentences = true;
    private final boolean createOnly = false; // update existing cite pages?
    
    private final Wiki wiki = new Wiki();
    private LinkedHashMap<String,Citation> citations = new LinkedHashMap<>();
    
    public CiteBot_OLD(){}
    public void test(){
        String test = "15:51,52] The Pilgrims then, especially Christian, began to '''despond''' in their minds, and looked this way and that, but no way could be found by them by which they might escape the river.";
        Pattern ppCruft = Pattern.compile("(([^\\p{L}\"']++))(.+?)"); //15:51,52] The Pilgrims then, especially Christian, began to '''despond''' in their minds, and looked this way and that, but no way could be found by them by which they might escape the river.
        Matcher mppCruft = ppCruft.matcher(test);
        if(mppCruft.matches()){
            test = mppCruft.group(3);
        }
        System.out.println(test);
//        wiki.login();        
//        LinkedHashMap<String, Citation> cites = get(); //
//        TreeSet<String> words = new TreeSet<>(cites.keySet()); //        
//        for(String word : words){
////            System.out.println(word);
//            String cite = cites.get(word).makeCite();
////            if(cite != null){
////                String out = word + "\r\n" + cite + "\r\n";
////                System.out.println(out);
////                try{ System.in.read(); }catch(IOException e){}
////            }
//        }
    } 
    
    public LinkedHashMap<String, Citation> get(){
        // word => [cite, cite, cite]
        LinkedHashMap<String, Citation> parsed = parse(BOOK.getFile());
        System.out.println(parsed.size() + " parsed.");
        LinkedHashMap<String, Citation> missing = existance(parsed); // remove cited words?
        System.out.println(missing.size() + " missing.");
        LinkedHashMap<String, Citation> uncited  = cited(missing); // check the contents of those which exist to see if we have already cited this book
        System.out.println(uncited.size() + " uncited.");
        
        return uncited;
    }
    
    private LinkedHashMap<String, Citation> parse(String _file){
        LinkedHashMap<String, Citation> wordMap = new LinkedHashMap<>();
        
        String start = "*** START OF THIS PROJECT GUTENBERG";
        String end = "*** END OF THIS PROJECT GUTENBERG";
        Pattern pBlank = Pattern.compile("^(\\s*?)$");
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(_file));
            String line;
            String paragraph = "";   
            boolean inText = false;
            
            while((line = reader.readLine()) != null){
                if(line.startsWith(end)){ inText = false; }
                if(inText){
                    line = clean(line);
                    if(pBlank.matcher(line).matches()){
                        if(!paragraph.isEmpty()){ 
                            if(splitSentences){
                                for(String pg : paragraph.split("(?<=[\\.!?]\\s)")){
                                    wordMap = add(wordMap, pg); // pg.replaceAll("[^\\p{L}\\s\\-']","")??
                                }
                            }else{ wordMap = add(wordMap, paragraph); }
                        }
                        paragraph = "";
                    }else{ paragraph = paragraph + line.trim() + " "; }                    
                }
                if(line.startsWith(start)){ inText = true; }

            }                        
        }catch(IOException e){}
        
        LinkedHashMap<String, Citation> out = new LinkedHashMap<>();
        for(String w : wordMap.keySet()){
            Citation c = wordMap.get(w);
            if(c.getQuotes().size() > 0 && c.getQuotes().size() <= maxCites){
                out.put(w,c); // only keep the words which have between 1 and MAX cites
            }
        }
        return out;
    }
    private LinkedHashMap<String, Citation> add(LinkedHashMap<String, Citation> input, String paragraph){
        Pattern pUpprs = Pattern.compile("([\\s\\p{Punct}0-9]+?)([\\p{Lu}\\-']+?)([\\s\\p{Punct}0-9]+?)");  // any UPPERCASE characters
        Pattern pUpper = Pattern.compile("(.*?)(\\p{Lu}+?)(.*?)");  // any UPPERCASE characters
        Pattern pSpace = Pattern.compile("(.*?)(\\s+?)(.*?)");      // spaces within the word (replaced punctuation usually)
        Pattern pHyphs = Pattern.compile("(.*?)\\-(.+?)\\-(.*?)");  // more than one hyphen (advanced!)
        Pattern pAppos = Pattern.compile("(.*?)'(.*?)"); // no appos for now //Pattern.compile("(\\p{L}+?)'(\\p{L}+?)");  // appostrophe which is not at the beginning or the end (advanced!)
        
        TreeSet<String> words = new TreeSet(Arrays.asList(paragraph.replaceAll("([\\.?!,\";:\\()\\{}\\[\\]])","").split("(\\s+?)")));
        
        for(String word : words){
            String w = word.replaceAll("([^\\p{L}\\-'])", " ").trim();
            Matcher mUpprs = pUpper.matcher(w);
            Matcher mSpace = pSpace.matcher(w);
            Matcher mHyphs = pHyphs.matcher(w);
            Matcher mAppos = pAppos.matcher(w);
            
            boolean matched = mUpprs.matches() || mSpace.matches() || mHyphs.matches() || mAppos.matches(); // does anything match? //  mUpper.matches() || 
            if(!matched){ 
                Citation c = new Citation(wiki).setBook(BOOK).setWord(w);
                if(input.containsKey(word)){ c = input.get(word); }
                if(minLength <= paragraph.length() && paragraph.length() <= maxLength){
                    c = c.addQuote(paragraph);
                }
                input.put(w, c);
            }
        }
        
        return input;
    }    
    private LinkedHashMap<String, Citation> existance(LinkedHashMap<String, Citation> input){
        LinkedHashMap<String, Citation> out = new LinkedHashMap<>();
        ArrayList<String> words = new ArrayList<>();
        
        for(String word : input.keySet()){ words.add("Citations:" + word); }    
        HashMap<String,Boolean> exists = wiki.exists(words); 
        
        for(String word : exists.keySet()){
            if(!exists.get(word)){
                word = word.replaceAll("Citations:","");
                out.put(word, input.get(word).setCiteExists(false));
            }else{
                word = word.replaceAll("Citations:","");
                out.put(word, input.get(word).setCiteExists(true));
            }
        }
        
        return out;
    }    
    private LinkedHashMap<String, Citation> cited(LinkedHashMap<String, Citation> input){
        // review the cite pages for all of the citations, eliminate those which already have this work cited
        LinkedHashMap<String, Citation> out = new LinkedHashMap<>();        
        LinkedList<String> cites = new LinkedList<>();
        for(String word : input.keySet()){
            if(input.get(word).isCited()){ cites.add("Citations:" + word); } // keep the words which are cited
        }
        System.out.println("Checking wiki citations for " + cites.size() + " pages.");
        LinkedHashMap<String,String> pages = wiki.readAll(cites);     
        
        System.out.println("Citations remain for " + pages.size() + " pages.");
        for(String title : pages.keySet()){
            String contents = pages.get(title);
            if(!contents.contains(BOOK.getTitle()) && !contents.contains(BOOK.getYear())){ 
                String word = title.replaceAll("Citations:","");
                String cite = input.get(word).makeCite();
                if(cite != null){
                    out.put(word, input.get(word));
                }
            }               
        }        
        return out;
    }
    private String clean(String line){                  
        line = line.replaceAll("\\-\\-", " — ").trim(); // Repair any punctuation which makes the parsing harder...
        line = line.replaceAll("(\\s*?['\"]*?)_([\\p{L}\\s\\-]+?)(\\.*)(.+?)_([\\.,!?\"']*\\s*?)", "$1''$2$3$4''$5"); // italics in Pride are _word_
        line = line.replaceAll("([¹²³⁴⁵⁶⁷⁸⁹⁰]+?)",""); // replace superscript numbers from Paradise Lost
        line = line.replaceAll("\\{[0-9]+?}",""); // Pilgrim's Progress line numbering
        // remove junk from start of lines when references break lines
        Pattern ppCruft = Pattern.compile("(([^\\p{L}\"']++))\\s*(.+?)"); //15:51,52] The Pilgrims then, especially Christian, began to '''despond''' in their minds, and looked this way and that, but no way could be found by them by which they might escape the river.
        Matcher mppCruft = ppCruft.matcher(line); if(mppCruft.matches()){ line = mppCruft.group(3); }
        
        return line;
    }
    private String root(){
        File h = new File(home);
        if(h.exists()){
            return home;
        }else{
            return work;
        }
    }
    
}
