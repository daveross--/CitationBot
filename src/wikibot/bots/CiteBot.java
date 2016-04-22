package wikibot.bots;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Wiki;
import wikibot.objects.Book;
import wikibot.objects.Citation;

public class CiteBot implements Bot {
    
    private int interval = 3000;  // ms delay
    private final int minLength = 128; // chars
    private final int maxLength = 512; // chars
    public static final int maxCites = 3; // right now this prevents words which occur in more than X sentences...
    private final boolean splitSentences = true;
    
    private Wiki wiki = new Wiki();
///*TEST*/ private final Book book = new Book().setAuthor("Test Author").setTitle("Test Book").setYear("2020").setFile(Book.root() + "TEST.txt");    
///* Complete */   private final Book book = new Book().setAuthor("John Bunyan").setTitle("The Pilgrim's Progress").setYear("1678").setFile(Book.root() + "Bunyan, The Pilgrim's Progress (1678).txt");
//    private final Book book = new Book().setAuthor("Daniel Defoe").setTitle("Robinson Crusoe").setYear("1719").setFile(Book.root() + "Defoe, Robinson Crusoe (1719).txt");
//    private final Book book = new Book().setAuthor("Jane Austen").setTitle("Pride and Prejudice").setYear("1813").setFile(Book.root() + "Austen, Pride and Prejudice (1813).txt");
//    private final Book book = new Book().setAuthor("Mary Shelley").setTitle("Frankenstein").setYear("1818").setFile(Book.root() + "Shelley, Frankenstein (1818).txt");
///* Complete */    private final Book book = new Book().setAuthor("Charles Dickens").setTitle("A Christmas Carol").setYear("1843").setFile(Book.root() + "Dickens, A Christmas Carol (1843).txt");
    private final Book book = new Book().setAuthor("Herman Melville").setTitle("Moby Dick").setYear("1851").setFile(Book.root() + "Melville, Moby Dick (1851).txt");
//    private final Book book = new Book().setAuthor("Bram Stoker").setTitle("Dracula").setYear("1897").setFile(Book.root() + "Stoker, Dracula (1897).txt");

    private String status;
    
    private LinkedHashMap<String, Citation> citations = new LinkedHashMap<>();
    private LinkedList<String> keys;
    private int cursor = -1;    
    private Citation current = null;    
    private final String skipTo = "";  // skip to this word
    
    // @todo: allow the bot to stop the gui if it encounters something it thinks is amiss, or skip, etc.
    // @todo: scroll panels
    
    @Override
    public void test(){
        // for testing...
        login(wiki);
        update();
    }
    
    @Override
    public void login(Wiki w){ // Log into the wiki, should be called to initiate the Bot
        this.wiki = w;
        wiki.login(); 
    } 
    @Override
    public void update(){ // update all of the fields to initiate the Bot
        // Parse the book into Citations HashMap
        if(parse()){ status = ("Parsed " + book.getTitle() + " successfully."); }else{ status = ("Failed to parse " + book.getTitle() + " successfully."); }
        // update the citations with their status on the wiki (citations page exists, citations page contents)
        if(checkWiki()){ status = ("Updated " + book.getTitle() + " cites successfully."); }else{ status = ("Failed to update " + book.getTitle() + " cites successfully."); }
        // create the new versions of citations pages
        if(createCites()){ status = ("Created new Citations content for " + book.getTitle() + "."); }else{ status = ("Failed somehow?"); }
        // remove words which are not going to be updated
        if(purgeWords()){ status = ("Purged uncitable words from list."); }else{ status = ("Purge failed."); }
        // finally, create the String[] which will allow us to move back and forth through the Citations
        if(createKeys()){ status = ("Created keys and cursor."); }else{ status = ("Failed to create keys and cursor."); }
    }
    @Override
    public void next(){ // get the next record to work on
        if(++cursor < keys.size()){
            current = citations.get(keys.get(cursor));
        }else if(keys.size() <= 0){
            cursor = 0;
            status = ("No more entries to process.");
            current = null;
        }else{
            status = ("Cursor:\t" + cursor + " is invalid; key count:\t" + keys.size() + " (next).");
            cursor = keys.size() - 1;
            current = citations.get(keys.get(cursor));
        }
    }
    @Override
    public void previous(){ // get the previous record to work on
        if(--cursor >= 0){
            current = citations.get(keys.get(cursor));
        }else{
            status = ("Cursor:\t" + cursor + " is invalid; key count:\t" + keys.size() + " (prev).");
            cursor = 0;
            current = citations.get(keys.get(cursor));
        }
    }
    @Override
    public void post(){ // process the current page
        if(current != null && current.getUpdate() != null){
            if(current.isCited()){
                status = (wiki.edit("Citations:" + current.getWord(), current.getUpdate(), current.editSummary()));
            }else{
                status = (wiki.create("Citations:" + current.getWord(), current.getUpdate(), current.editSummary()));
            }
            keys.remove(cursor--);
        }else{
            status = ("Failed to update " + current.getWord()); 
        }
    }
    @Override
    public void close(){
        
    }
    
    @Override
    public String getContents(){ 
        String contents = current.getUpdate();
        String word = current.getWord();
        if(contents != null){
            return contents;
        }else if(word != null){
            status = "No valid citations for current entry:\t" + current.getWord() + ".";
        }else{
            status = "There isn't even a word..."; 
        }
        return null;
    }
    @Override
    public String getPrevious(){
        String previous = current.getPrevious();
        String word = current.getWord();
        if(previous != null){
            return previous;
        }else if(word != null){
            status = "Creating a new page for\t" + current.getWord() + ".";
        }else{
            status = "There isn't even a word..."; 
        }
        return null;
    }
    @Override
    public String getWord(){
        return this.current.getWord();
    }
    @Override
    public String getStatus(){ return this.status; }
    @Override
    public int getInterval(){ return this.interval; }
    @Override
    public int setInterval(int i){ 
        this.interval = i;
        return this.interval; 
    }
    @Override
    public int getHeight(){ return 900; }
    @Override
    public int getWidth(){ return 1400; }
    @Override
    public boolean useHighlighter(){ return false; }
    @Override
    public HashMap<String,Color> highlights(){ 
        HashMap<String,Color> map = new HashMap<>();
        
        return map; 
    }
    @Override 
    public boolean useEditor(){ return false; }
    @Override
    public String highlightAction(String text, int start, int end){
        return text;
    }
    @Override
    public boolean hasNext(){ if(this.cursor >= keys.size()){ return false; }else{ return true; } }
    
    @Override
    public void setContents(String in){ 
        if(current != null){ 
            current.setUpdate(in);
        }else{
            current = new Citation(wiki).setBook(book).setUpdate(in);
        }
    }
    @Override
    public boolean usePrev(){ return true; }
    
    // parses the book, updates the Citations map   
    private boolean parse(){       
        LinkedHashMap<String, Citation> wordMap = new LinkedHashMap<>();
        Pattern pTextBounds = Pattern.compile("(.*?)(\\*{3})(.*?)PROJECT GUTENBERG(.*?)(\\*{3})(.*?)"); Matcher mTextBounds;
        Pattern pBlank = Pattern.compile("^(\\s*?)$");        
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(book.getFile()));
            String line, paragraph = "";   
            boolean inText = false, go = true;
            
            while((line = reader.readLine()) != null && go){
                mTextBounds = pTextBounds.matcher(line);
                if(mTextBounds.matches() && inText){ inText = false; go = false; } // if we are in text and find another boundary, end text
                if(inText){
                    line = clean(line);
                    if(pBlank.matcher(line).matches()){
                        if(!paragraph.isEmpty()){ 
                            // Fix the linebreaks and spacing
                            paragraph = paragraph.replaceAll("%", "%0").replaceAll("\n{2,}", "%1").replaceAll("\n", " ");
                            paragraph = paragraph.replaceAll("\\s{2,}", " ").replaceAll("%1", "\n\n").replaceAll("%0", "%");
                            if(splitSentences){ // "(?<=(.{4,}?[\\.!?]))\\s+(?=\\p{Lu}.{4,}?)"
                                for(String pg : paragraph.split("(?<=([^\\s][\\.!?]))\\s+(?=\\p{Lu}[^\\s]|[AI]\\s)")){ // fix this so as not to split on common things like Mr., Mrs., Ms. etc.
                                    wordMap = add(wordMap, pg);
                                }
                            }else{ wordMap = add(wordMap, paragraph); }
                        }
                        paragraph = "";
                    }else{ paragraph = paragraph + line.trim() + " "; }                    
                }
                if(mTextBounds.matches() && !inText){ inText = true; } // if we are not in the text, but find a boundary, start reading
            }                        
        }catch(IOException e){ return false; }
        
        LinkedHashMap<String, Citation> out = new LinkedHashMap<>();
        
        for(String w : wordMap.keySet()){ //
            Citation c = wordMap.get(w);
            if(c.getQuotes().size() > 0 && c.getQuotes().size() <= maxCites){
                out.put(w,c); // only keep the words which have between 1 and MAX cites
            }
        }
        citations = wordMap;        
        return (citations.size() > 0); // return false if we found no Citations
    }  
    // checks the wiki for existing citations pages, and retains their contents when they exist
    private boolean checkWiki(){
        LinkedHashMap<String, Citation> cites = citations;
        LinkedList<String> titles = new LinkedList<>(cites.keySet()); // collection of page titles
        LinkedList<String> ctitles = new LinkedList<>(); // collection of citation page titles
            for(String title : titles){ // update citation pages
                ctitles.add("Citations:" + title); 
            } 
            
        LinkedHashMap<String, String> contents = wiki.readAll2(ctitles); // add the existing content for cites which already exists
            for(String title : contents.keySet()){
                Citation c = cites.get(title.replaceAll("Citations:", ""));
                if(cites.containsKey(c.getWord())){
                    c.setCiteExists(true);
                    c.setPrevious(contents.get(title));
                    c.setCurrent(contents.get(title));
                    cites.put(title.replaceAll("Citations:", ""), c);
                }
            }
            
//        for(String w : cites.keySet()){ System.out.println(cites.get(w).print()); } // TEST        
        citations = cites;
        return (citations.size() > 0); // return false if we found no Citations
    }
    // runs through all words and creates the updated versions of the citations pages
    private boolean createCites(){
        for(String word : citations.keySet()){
            Citation c = citations.get(word);
            String update = c.makeCite();
            if(update != null){ c = c.setUpdate(update); } // if we have a new or updated citations page, update the Citation           
            citations.put(word, c);
        }        
        return true;
    }
    // remove words which we will not be updating
    private boolean purgeWords(){
        LinkedHashMap<String, Citation> cites = new LinkedHashMap<>();
        String skip = "";
        for(String word : citations.keySet()){
            String update = citations.get(word).getUpdate();
            if(update != null){
                cites.put(word, citations.get(word));
            }else{
                skip = skip + word + ", ";
            }
        }
//        System.out.println("Skipping:\r\n" + skip.substring(0, skip.length()-2));
        citations = cites;
        return citations.size() > 0;
    }
    // creates the keys and cursor for iteration
    private boolean createKeys(){
        TreeSet<String> ts = new TreeSet<>(citations.keySet());
        this.keys = new LinkedList<>(ts);        
        this.cursor = 0;
        this.current = citations.get(keys.get(0));
        if(this.keys.size() > 0 && current != null){ return true; }else{ return false; }
    }
    
    // removes citations for words which are too complicated for now UPPER, Upper, to-many-hyphens, spaced words, c'ntractions
    private LinkedHashMap<String, Citation> add(LinkedHashMap<String, Citation> input, String paragraph){
        Pattern pUpprs = Pattern.compile("([\\s\\p{Punct}0-9]+?)([\\p{Lu}\\-']+?)([\\s\\p{Punct}0-9]+?)");  // all UPPERCASE characters
        Pattern pUpper = Pattern.compile("(.*?)(\\p{Lu}+?)(.*?)");  // any UPPERCASE characters
        Pattern pSpace = Pattern.compile("(.*?)(\\s+?)(.*?)");      // spaces within the word (replaced punctuation usually)
        Pattern pHyphs = Pattern.compile("(.*?)\\-(.+?)\\-(.*?)");  // more than one hyphen (advanced!)
        Pattern pAppos = Pattern.compile("(.*?)'(.*?)"); // no appos for now //Pattern.compile("(\\p{L}+?)'(\\p{L}+?)");  // appostrophe which is not at the beginning or the end (advanced!)
        Pattern pBlank = Pattern.compile("^(\\s*?)$"); // blank or whitespace only
        
        TreeSet<String> inwords = new TreeSet(Arrays.asList(paragraph.replaceAll("([\\.?!,\";:\\()\\{}\\[\\]])","").split("(\\s+?)")));
        TreeSet<String> words = new TreeSet<>();
        
        if(!skipTo.isEmpty()){
            for(String word : inwords){ if(word.compareTo(skipTo) >= 0){ words.add(word); } } // remove words before skipTo
        }else{ words = inwords; }
            
        for(String word : words){
            String w = word.replaceAll("([^\\p{L}\\-'])", " ").trim();
            Matcher mUpprs = pUpper.matcher(w);
            Matcher mSpace = pSpace.matcher(w);
            Matcher mHyphs = pHyphs.matcher(w);
            Matcher mAppos = pAppos.matcher(w);
            Matcher mBlank = pBlank.matcher(w);
            
            boolean matched = mUpprs.matches() || mSpace.matches() || mHyphs.matches() || mAppos.matches() || mBlank.matches(); // does anything match? //  mUpper.matches() || 
            if(!matched){ 
                Citation c = new Citation(wiki).setBook(book).setWord(w);
                if(input.containsKey(word)){ c = input.get(word); }
                if(minLength <= paragraph.length() && paragraph.length() <= maxLength){
                    c = c.addQuote(paragraph);
                }
                input.put(w, c);
            }
        }
        
        return input;
    }   
    // cleans lines from funky formatting specific to certain texts
    private String clean(String line){                  
        line = line.replaceAll("\\-\\-", " — ").trim(); // Repair any punctuation which makes the parsing harder...
        line = line.replaceAll("(\\s*?['\"]*?)_([\\p{L}\\s\\-]+?)(\\.*)(.+?)_([\\.,!?\"']*\\s*?)", "$1''$2$3$4''$5"); // italics in Pride are _word_
        line = line.replaceAll("([¹²³⁴⁵⁶⁷⁸⁹⁰]+?)",""); // replace superscript numbers from Paradise Lost
        line = line.replaceAll("\\{[0-9]+?}",""); // Pilgrim's Progress line numbering
        line = line.replaceAll("\\s{2,}", " "); // replace consecutive whitespace with single space
        line = line.replaceAll("[“”]","\"");
        
        return line;
    }  
}
