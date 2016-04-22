package wikibot;

import java.util.HashMap;
import java.util.LinkedList;
import wikibot.GUI.GUI;
import wikibot.bots.*;
import wikibot.objects.JSON_Pages;
import wikibot.objects.XML_Page;
import wikibot.scripts.*;
import wikibot.tools.*;

public class WikiBot {

    public static void main(String[] args) {
//        Wiki wiki = new Wiki(); wiki.login();

//        new GUI().run();
        
//        CONNECTOR TEST
//        Wiki.login(); //works
//        wiki.write("User:TheDaveBot/test2","Test ++ Test & ... \"","Testing"); // works
                
        // Bots
//        new GUI().run(new Wikify());
//        new GUI().run(new CiteBot());
//        new GUI().run(new GlossBot());
//        new GUI().run(new QuoteFixBot());
//        new GUI().run(new QuoteFixBot_redo1());
//        new GUI().run(new FormOfBot());
//        new GUI().run(new TemplateBot());
//        new GUI().run(new BalanceBot());
//        new GUI().run(new MoveBot());
//        new GUI().run(new SpanishBot());
//        new GUI().run(new UnblockBot()); // unblocks indef blocked IPs
        
        // Run Scripts
//        TBot.embeds();
//        Balancer.embeds();
//        new Macedonian().run();
//        WikipediaLinks.run();
        
        // TESTS
        Dump_RAF draf = new Dump_RAF();
        draf.setCursor(123456789);
        draf.next().print();
//        Test.run();
//        wiki.blockList(5000);  
//        new QuoteFixBot_redo1().test();
//        Dump1 dump = new Dump1();
//        String[] templates = new String[]{"{{quote-book","{{quote-journal","{{quote-handsard","{{quote-song","{{quote-magazine","{{quote-newsgroup","{{quote-web","{{quote-us-patent","{{quote-video","{{quote-wikipedia"};
//        HashMap<String,Integer> count = dump.strFreq(templates, 0);
//        for(String str : count.keySet()){
//            System.out.println(str + "\t" + count.get(str));
//        }
    }
}


// Possible cleanup
// Part of Speech sections out of order?  (alphabetical?)
// Add glosses to translation tables, especially when there is only one sense... [[Category:Translation_table_header_lacks_gloss]] is the cat which has them


// * Update instances of &c. to etc.
// * Derived/Related terms - if there is a derived terms section check if the {{der- templates are in use, if not add them if there are sufficient items
// * Context tags with commas in them, mostly they should be split into two items with a |
// * Run a list of definition lines which have no wikification, single and two-word lines can be wikified automagically?
// * Find ref tags with no ref sections?  Can we clean that up?
// ** Category:Language code missing    (/hyphenation)