package wikibot.objects;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import wikibot.Wiki;
import wikibot.bots.Bot;
import wikibot.bots.CiteBot;

public class Citation {

    Wiki wiki;
    private String word;
    private boolean wordExists = false;
    private boolean citeExists = false;
    private Book book;
    private LinkedList<String> quotes = new LinkedList<>();
    private String cite;
    private String current = null;                                              // the current contents of the page on wiki
    private String previous = null;
    private String update = null;                                               // the contents which the page should be updated to
    
    public Citation(Wiki wiki){
        this.wiki = wiki;
    }
    
    public Citation setWord(String word){
        this.word = word;
        return this;
    }
    public String getWord(){
        return this.word;
    }
    
    public Citation setWordExists(boolean b){
        this.wordExists = b;
        return this;
    }
    public boolean getWordExists(){ return this.wordExists; }
    
    public Citation setCiteExists(boolean b){
        this.citeExists = b;
        return this;
    }
    public boolean getCiteExists(){ return this.citeExists; }
    
    public Citation setBook(Book book){
        this.book = book;
        return this;
    }
    public Book getBook(){
        return this.book;
    }
    
    public Citation setCurrent(String cur){
        this.current = cur;
        return this;
    }
    public String getCurrent(){
        return this.current;
    }
    
    public Citation setPrevious(String prev){
        this.previous = prev;
        return this;
    }
    public String getPrevious(){
        return this.previous;
    }
    
    public Citation setQuotes(LinkedList<String> quotes){
        this.quotes = quotes;
        return this;
    }
    public Citation addQuote(String quote){
        this.quotes.add(quote);
        return this;
    }
    public LinkedList<String> getQuotes(){
        return this.quotes;
    }
    
    public Citation setUpdate(String u){
        this.update = u;
        return this;
    }
    public String getUpdate(){ return update; }
    
    public String makeCite(){           
        if(citeExists){
            this.cite = rebuild();
        }else{
            this.cite = newCite();
        }
        if(this.cite != null){ return this.cite; }else{ return null; }
    } 
    private String title(){
        return "* '''" + book.getYear() + "''' — " + book.wpAuthor() + ". ''" + book.wpTitle() + "''.\n";
    }   
    private String graphs(){
        String out = "";
        bestCites();
        if(quotes.size() < 1){ return null; }
        for(String graph : quotes){
            if(graph.length() > 1){
                graph = graph.replaceAll("(\\s+?)([\"'\\[\\(0-9]*?)(" + word + ")([\\.,\\:;!?\"'\\)\\]]*?)(\\s*?$|\\s+?.)", "$1$2'''$3'''$4$5"); // '''bolds''' the subject word
                out = out + "*: " + graph + "\n";
            }
        }    
        if(out.length() > 1){ return out.trim(); }else{ return null; }
    }
    private void bestCites(){
        // choose only the best cites, up to the maximum number
        LinkedList<String> q = quotes;
        if(q.size() >= CiteBot.maxCites){ // right now just take the first maxCites
            q = new LinkedList<>(q.subList(0,CiteBot.maxCites));
        }
        quotes = q;
    }
    private String newCite(){ // if the cites page doesn't exist, create from scratch
        String g = graphs();
        if(g != null){
            String c = ""
                    + "{{citations|lang=en}}\n\n"
                    + "{{timeline\n"
                    + "|" + book.getYear().substring(0,2) + "00s=" + book.getYear() + "\n"
                    + "}}\n\n";

            c = c + title() + g;
            return c;   
        }else{ return null; } // return null if we have no good cites
    }
    private String rebuild(){ // if the cites page already exists, add the new cite to the existing stuff...
        String original = current; //wiki.read("Citations:" + word);        
        String graphs = graphs();
        if(graphs != null){}else{ return null; } 
        
        Pattern pOtherTemps = Pattern.compile("(.*?)(\\{\\{(?!(citations|timeline)))(.*?)");
        Pattern pLX = Pattern.compile("(\\s*?)(={2,})(\\s*)(\\p{L}+?)(\\s*)(={2,})(.+?)");
        Pattern pLists = Pattern.compile("([#\\*\\:]+)(.+?)");
        Pattern pYear = Pattern.compile("([#\\*]{1,2})([^\\p{L}]+?)([0-9]{4})(.+?)"); // checks for a 4 digit number before any text in a list
//        Pattern pCitations = Pattern.compile("(\\s*?)(\\{\\{citations)(\\|lang=en}}|}})(\\s*?)");
//        Pattern pTimeline = Pattern.compile("(\\s*?)(\\{\\{timeline)([\\|]??)(\\s*?)");
        
        TreeMap<String,String> cites = new TreeMap<>();
        String year = "";
        String quote = "";        
        
        // return null if the current work is already cited
        if(original.contains(book.getTitle()) || original.contains(book.getYear())){ return null; }
        
        String[] lines = original.split("\n");        
        for(String line : lines){     
            Matcher mYear = pYear.matcher(line); // check for year  
            Matcher mLX = pLX.matcher(line); // check for L2+ headers
            Matcher mLists = pLists.matcher(line); // check for list structure
            Matcher mOtherTemps = pOtherTemps.matcher(line);
            
            if(mOtherTemps.matches()){ return null; }
            if(mLX.matches()){ return null; }
            
            // Update the quotations section.
            if(mLists.matches()){ // if we are in a list of some kind                
                if(mYear.matches()){
                    if(year.length() != 4){
                        year = mYear.group(3);
                        quote = line.trim() + "\n";
                    }else{
                        cites.put(year, quote);
                        year = mYear.group(3);
                        quote = line.trim() + "\n";
                    }
                }else{ quote = quote + line.trim() + "\n"; }                
            }            
        }
        cites.put(year, quote); // add the remaining cites
        cites.put(this.book.getYear(), this.title() + this.graphs());
        
        String out = "{{citations|lang=en}}\n\n" + makeTimeline(cites.keySet()) + "";        
        
        for(String c : cites.keySet()){ out = out + cites.get(c) + "\n\n\n"; }
        
        out = out.replaceAll("(\n{2,})", "\n\n");
        
        return out;
    }
    
    public boolean isCited(){
        return this.citeExists;
    }    
    public String editSummary(){
        return book.getYear() + " — " + book.wpAuthor() + ". " + book.wpTitle() + ". [[" + word + "]]";
    }
        
    public void highlight(JTextArea jta){
        DefaultHighlighter.DefaultHighlightPainter  hp = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        String w = this.word;
        String contents = this.makeCite();
        try{
            int c = 0, start, end;
            while(c < this.makeCite().length()){
                if(contents.contains(w)){
                    start = contents.indexOf(w) + c;
                    end = start + (w).length();
                    jta.getHighlighter().addHighlight(start, end, hp);                                    
                    c = end + 1;
                }else{ c = contents.length(); }
            }
        }catch(BadLocationException ex) {}
    }    
    private String makeTimeline(Set<String> years){
        TreeMap<String,String> centuries = new TreeMap<>();
        try{
            for(String year : years){
                if(year.length() == 4){
                    String prefix = year.substring(0,2);
                    String suffix = "";
                    if(centuries.containsKey(prefix)){
                        suffix = centuries.get(prefix);
                    }
                    centuries.put(prefix, suffix + year + "<br>");
                }
            }
        }catch(Exception e){
            System.err.println(years);
        }
        String out = "{{timeline\n";
        for(String prefix : centuries.keySet()){
            String suffix = centuries.get(prefix);
            suffix = suffix.substring(0,suffix.length()-4);
            out = out + "|" + prefix + "00s=" + suffix + "\n"; 
        }
        out = out + "}}\n\n";
        
        return out;
    }
    
    public String print(){ // simple oneline printer to check on cites 
        return (this.word + "                         ").substring(0,24) + "\t" 
                + this.book.getTitle() + "\t" 
                + this.citeExists + "\t" 
                + (this.current != null ? this.current.length() : 0) + "\t" 
                + quotes.size();
    }
}
