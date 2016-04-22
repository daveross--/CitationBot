package wikibot.bots;

import java.awt.Color;
import java.util.HashMap;
import wikibot.Wiki;

public interface Bot {
    
    public int getWidth();
    public int getHeight();
    
    // Bot abstraction so that the GUI can handle any type of Bot script generically
    public void test(); // test functionality
    public void login(Wiki wiki); // log in to wiki
    public void update(); // populate the bots fields
    public void next(); // move on to the next item
    public void previous(); // move to the previous item
    public void post(); // process the current item  
    public void close(); // action to perform on close (such as reporting an index)
    
    public void setContents(String text); // update the contents from the edit pane
    
    public boolean hasNext();
    public boolean usePrev();
    public boolean useEditor();
    
    public String getContents();
    public String getPrevious();
    public String getWord();
    public String getStatus();
    
    public boolean useHighlighter();
    public String highlightAction(String text, int start, int end);
    public HashMap<String, Color> highlights();
    
    public int getInterval();
    public int setInterval(int i);
}
