package wikibot.bots;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.Dump;
import wikibot.Wiki;
import wikibot.objects.XML_Page;

public class Wikify implements Bot{

    Wiki wiki = new Wiki();
    Dump dump = new Dump();
    
    XML_Page current = null;
    
    long cursor = 18289193;
    int chunkSize = 100;
    String status = "No status has yet been set.";
    
//    String pattern = "(?s)(.*?==\\s*English\\s*==(?!----).*?)\n(#[^#*:]*?[^\\[\\]{}]+?)\n(.*?)";
    String pattern = "(?s)(.*?==\\s*English\\s*==(?!----).*?)\n(#[^#*:][^\\[\\]{}]+?)\n(.*?)";
    // the contents in chunks, so that we can only show the part of the entry which needs fixing
    String before = null;
    String contents = null;
    String after = null;
    // end of contents
    String editSummary = "wikify";
    
    @Override
    public void test(){ }
    
    @Override
    public void login(Wiki wiki){ this.wiki = wiki; this.wiki.login(); dump.setCursor(cursor); }
    @Override
    public void update(){ 
        dump.setCursor(cursor);
    }
    @Override
    public void next(){ 
        current = dump.getXMatches(1, pattern).getFirst(); 
        cursor = dump.getCursor();
        status = current.getTitle();
    }
    @Override
    public void previous(){ System.out.println("No previous yet."); }
    @Override
    public void post(){  // process the current page
        System.out.println("Cursor:\t" + String.format("%15d",cursor) + "\t" + current.getTitle() + "\t" + contents);
        wiki.edit(current.getTitle(), before + "\n" + contents + "\n" + after, editSummary);
        next();
        getContents();
    }
    @Override
    public void close(){}

    @Override
    public String getContents(){
//        return current.getRevisions().getFirst().getText();
        String z = wiki.read(current.getTitle());
        Pattern ptrn = Pattern.compile(pattern);
        Matcher m = ptrn.matcher(z);
        if(m.matches()){
            before = m.group(1);
            contents = m.group(2);
            after = m.group(3);
        }else{
            contents = null; before = null; after = null;
            return ""; // return blank
        }
        return contents;
    }
    @Override
    public void setContents(String text){ contents = text; }
    @Override
    public boolean hasNext(){ return false; }
    @Override
    public boolean usePrev(){ return false; }    
    @Override
    public String getPrevious(){ return null; }
    @Override
    public String getWord(){ return null; }
    @Override
    public String getStatus(){ return status; }
    @Override
    public int getInterval(){ return 3000; } 
    @Override
    public int setInterval(int i){ return 3000; }
    @Override
    public int getHeight(){ return 400; }
    @Override
    public int getWidth(){ return 1000; }
    @Override
    public boolean useHighlighter(){ return true; }
    @Override
    public HashMap<String,Color> highlights(){ 
        HashMap<String,Color> map = new HashMap<>();
        
        return map; 
    }
    @Override 
    public boolean useEditor(){ return true; }
    @Override
    public String highlightAction(String text, int start, int end){
        String a = text.substring(start, end);   
        String b = a;
        if(start >= 2 && end < text.length()-2){ b = text.substring(start-2, end+2); }
        
        if(a.contains("[[")){ 
            return text.substring(0, start) + a.replaceAll("[\\[\\]]", "") + text.substring(end);
        }else if(b.contains("[[")){
            return text.substring(0, start-2) + b.replaceAll("[\\[\\]]", "") + text.substring(end+2);
        }else{
            return text.substring(0, start) + "[[" + a + "]]" + text.substring(end);
        }        
    }
    
    private String testcontents(){
        return "==English==\n" +
            "\n" +
            "===Etymology===\n" +
            "From {{etyl|la|en}} [[centum]] ''hundred'' + [[annus]] ''year'' + [[-al]]\n" +
            "\n" +
            "===Adjective===\n" +
            "{{en-adj|-}}\n" +
            "\n" +
            "# Relating to, or associated with, the [[commemoration]] of an event that happened a hundred years before.\n" +
            "#: ''a '''centennial''' ode''\n" +
            "# Happening once in a {{w|hundred}} years.\n" +
            "#: ''a '''centennial''' jubilee; a '''centennial''' celebration''\n" +
            "# Lasting or aged a hundred years.\n" +
            "#* Longfellow\n" +
            "#*: That opened through long lines / Of sacred ilex and '''centennial''' pines.\n" +
            "\n" +
            "===Noun===\n" +
            "{{wikipedia}}\n" +
            "{{en-noun}}\n" +
            "\n" +
            "# The hundredth [[anniversary]] of an event or happening.\n" +
            "\n" +
            "====Derived terms====\n" +
            "* [[semicentennial]]\n" +
            "* [[sesquicentennial]]\n" +
            "\n" +
            "====Translations====\n" +
            "{{trans-top|100th anniversary}}\n" +
            "* Armenian: {{t+|hy|հարյուրամյակ}}\n" +
            "* Bulgarian: {{t|bg|стогодишнина|f}}\n" +
            "* Czech: {{t|cs|sté výročí}}\n" +
            "* Danish: {{t|da|hundredårsdag}}\n" +
            "* Dutch: {{t+|nl|eeuwfeest}}\n" +
            "* Finnish: {{t|fi|satavuotisjuhla}}\n" +
            "* French: {{t+|fr|centenaire|m}}\n" +
            "* German: {{t|de|Hundertjahrfeier|f}}, {{t|de|hundertster Jahrestag|m}}\n" +
            "* Greek: {{t+|el|εκατονταετηρίδα|f}}\n" +
            "* Hungarian: {{t+|hu|centenárium}}\n" +
            "* Italian: {{t+|it|centenario|m}}\n" +
            "* Norman: {{t|nrf|chent'naithe|m}}\n" +
            "* Latvian: {{t|lv|simtgadu}}\n" +
            "{{trans-mid}}\n" +
            "* Lithuanian: {{t+|lt|šimtmetis}}\n" +
            "* Norwegian: {{t|no|hundreårsdag}}\n" +
            "* Polish: {{t+|pl|stulecie|n}}\n" +
            "* Portuguese: {{t+|pt|centenário|m}}\n" +
            "* Romanian: {{t+|ro|centenar}}\n" +
            "* Russian: {{t+|ru|столе́тие|n}}, {{t|ru|[[столетний|столе́тняя]] [[годовщи́на]]|f}}\n" +
            "* Scottish Gaelic: {{t|gd|cuimhneachan nan ceud bliadhna|m}}\n" +
            "* Serbo-Croatian:\n" +
            "*: Cyrillic: {{t|sh|стогодишњица|f|sc=Cyrl}}, {{t|sh|столеће|n|sc=Cyrl}}\n" +
            "*: Roman: {{t+|sh|stogodišnjica|f}}, {{t|sh|stoleće|n}}\n" +
            "* Spanish: {{t+|es|centenario|m}}\n" +
            "* Swedish: {{t|sv|hundraårsdag}}\n" +
            "* Turkish: {{t|tr|yüzüncü yıldönümü}}\n" +
            "{{trans-bottom}}\n" +
            "\n" +
            "[[Category:en:Hundred]]\n" +
            "[[Category:en:Time]]\n" +
            "\n" +
            "[[fr:centennial]]\n" +
            "[[ko:centennial]]\n" +
            "[[hy:centennial]]\n" +
            "[[io:centennial]]\n" +
            "[[it:centennial]]\n" +
            "[[ku:centennial]]\n" +
            "[[hu:centennial]]\n" +
            "[[mg:centennial]]\n" +
            "[[ml:centennial]]\n" +
            "[[pl:centennial]]\n" +
            "[[fi:centennial]]\n" +
            "[[sv:centennial]]\n" +
            "[[ta:centennial]]\n" +
            "[[vi:centennial]]\n" +
            "[[zh:centennial]]";
    }
}
