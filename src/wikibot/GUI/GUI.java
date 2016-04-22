package wikibot.GUI;

import wikibot.bots.Bot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import wikibot.Wiki;

public class GUI {
    
    // add a button which gets the former paragraph, and one which gets the following.  so that we can expand the context when necessary    
    private static final Wiki wiki = new Wiki();
//    private static final Bot bot = new CiteBot(); // will be Bot type once CiteBot2 is done
    private Bot bot; // will be Bot type once CiteBot2 is done    
    private static boolean go = false;    
    
    public void run(){
        bot.login(wiki); // always log in
        // <editor-fold defaultstate="collapsed" desc="Form Elements">
        final Font font = new Font(Font.MONOSPACED, 0, 11);
        
        final JFrame main = new JFrame("WikiBot");
            main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            main.setLocation(450, 50);
            main.setSize(bot.getWidth(), bot.getHeight());
            main.setLayout(new BorderLayout());
            main.setFocusable(false);
          
        // Main content panel stuff
        final JPanel content = new JPanel();
        content.setBackground(new Color(240,240,240));
            content.setLayout(new GridLayout(1,2));
            main.add(content, BorderLayout.CENTER);
        
            
        final JTextArea previous = new JTextArea();
        previous.setBackground(new Color(200,200,210));
            previous.setMargin(new Insets(10,10,10,10));
            previous.setAlignmentY(JTextPane.LEFT_ALIGNMENT);
            previous.setText("Previous stuff.");
            previous.setWrapStyleWord(true);
            previous.setLineWrap(true);
            previous.setFont(font);
        final JScrollPane scrollPrevious = new JScrollPane(previous);
            if(bot.usePrev()){ content.add(scrollPrevious); } // only include the previous pain if the bot requires it
            
        final JTextArea contents = new JTextArea();
        contents.setBackground(new Color(250,250,250));
            contents.setMargin(new Insets(10,10,10,10));
            contents.setAlignmentY(JTextPane.LEFT_ALIGNMENT);
            contents.setText("Let's do this!");
            contents.setWrapStyleWord(true);
            contents.setLineWrap(true);
            contents.setFont(font);
            if(bot.useEditor()){
                contents.addCaretListener(new CaretListener() {
                    @Override
                    public void caretUpdate(CaretEvent e) {
                        // contentes = bot.highlightaction(String contents, int start, int end); // MAKE THIS!
                        Runnable doAction = new Runnable(){
                            @Override
                            public void run(){
                                String cText = contents.getText();
                                int start = contents.getSelectionStart(), end = contents.getSelectionEnd();
                                if(end - start > 0 && start >= 0 && end <= cText.length()){
                                    cText = bot.highlightAction(cText, start, end);
                                    contents.setText(cText);
                                }
                            }
                        };
                        SwingUtilities.invokeLater(doAction);
                    }
                });
            }
            
        final JScrollPane scrollContent = new JScrollPane(contents);
            scrollContent.getVerticalScrollBar().setValue(scrollContent.getVerticalScrollBar().getMinimum());
            content.add(scrollContent);       
        
        final JPanel pStatus = new JPanel();
            pStatus.setBackground(new Color(150,150,150));
            pStatus.setLayout(new BorderLayout());
            
        final JTextArea status = new JTextArea();
            status.setBackground(new Color(150,150,150));
            status.setMargin(new Insets(10,10,10,10));
            status.setAlignmentY(JTextPane.LEFT_ALIGNMENT);
            status.setText("");
            
            pStatus.add(status, BorderLayout.CENTER);
            main.add(pStatus, BorderLayout.NORTH);
            
        // Button Pane
        final JPanel pButtons = new JPanel();
            pButtons.setBackground(new Color(180,180,180));            
            main.add(pButtons, BorderLayout.SOUTH);
        // </editor-fold>
            
        final JButton bLogIn = new JButton("Log in (L)");
            bLogIn.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){ 
                    bot.login(wiki);
                    if(wiki.isLoggedIn()){
                        contents.setText("Successfully logged in as " + wiki.getUser() + ".");
                    }else{
                        contents.setText("Failed to login.");
                    }
                }
            });            
            
            pButtons.add(bLogIn);   
            
        final JButton bLoad = new JButton("Load (G)");
            bLoad.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){ 
                    bot.update();
//                    contents.setText(bot.getContents());
//                    previous.setText(bot.getPrevious());
//                    status.setText(bot.getStatus());
                    if(bot.useHighlighter()){
                        HashMap<String,Color> highlights = bot.highlights();
                        for(String highlight : highlights.keySet()){
                            highlight(previous, highlight, highlights.get(highlight));
                            highlight(contents, highlight, highlights.get(highlight));
                        }
                    }
                }
            });
            pButtons.add(bLoad);   
        
        final JSlider slider = new JSlider(JSlider.HORIZONTAL, 500, 60000, bot.getInterval());
            slider.addChangeListener(new ChangeListener(){
                @Override
                public void stateChanged(ChangeEvent e) {
                    if(!slider.getValueIsAdjusting()){ System.out.println("Bot interval is now: " + bot.getInterval()); }
                    bot.setInterval(slider.getValue());                       
                }    
            });
            pButtons.add(slider);  
        
        final JButton bPrev = new JButton("Prev (P)");
            bPrev.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){
                    bot.previous();
                    contents.setText(bot.getContents());
                    previous.setText(bot.getPrevious());
                    status.setText(bot.getStatus());
                    if(bot.useHighlighter()){
                        HashMap<String,Color> highlights = bot.highlights();
                        for(String highlight : highlights.keySet()){
                            highlight(previous, highlight, highlights.get(highlight));
                            highlight(contents, highlight, highlights.get(highlight));
                        }
                    }
                }
            });
            pButtons.add(bPrev); 
            
        final JButton bNext = new JButton("Next (N)");
            bNext.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){
                    bot.next();
                    while(bot.getContents().isEmpty()){ bot.next(); }
                    contents.setText(bot.getContents());
                    previous.setText(bot.getPrevious());
                    status.setText(bot.getStatus());
                    if(bot.useHighlighter()){
                        HashMap<String,Color> highlights = bot.highlights();
                        for(String highlight : highlights.keySet()){
                            highlight(previous, highlight, highlights.get(highlight));
                            highlight(contents, highlight, highlights.get(highlight));
                        }
                    }                 
                }
            });
            pButtons.add(bNext);     
          
        final JButton bPost = new JButton("Post (M)");
            bPost.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){ 
                    bot.setContents(contents.getText());
                    bot.post();
                    bNext.getActionListeners()[0].actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)); // click the next button
                }
            });
            pButtons.add(bPost);     
            
        final JButton bAuto = new JButton("Auto/Stop");
            bAuto.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){ 
                   go = !go;
                   System.out.println("Auto: " + go + "\tInterval: " + bot.getInterval());
                    Thread tt = new Thread(){                  
                        @Override
                        public void run(){                       
                            while(go){
                                try{
                                    go = go && bot.hasNext();
                                    Thread.sleep(bot.getInterval());
                                    if(go){ bot.post(); bot.next(); }
                                    contents.setText(bot.getContents());
                                    previous.setText(bot.getPrevious());
                                    status.setText(bot.getStatus());
                                    if(bot.useHighlighter()){
                                        HashMap<String,Color> highlights = bot.highlights();
                                        for(String highlight : highlights.keySet()){
                                            highlight(previous, highlight, highlights.get(highlight));
                                            highlight(contents, highlight, highlights.get(highlight));
                                        }
                                    }
                                }catch(InterruptedException e){}
                            }     
                        }
                    };
                    tt.start();
                }
            });
            pButtons.add(bAuto);     
            
             
        final JButton bStop = new JButton("Stop!");
            bStop.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){
                    go = false;
                }
            });
//            pButtons.add(bStop);  
                 
        final JButton bClose = new JButton("Close (X)");
            bClose.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){ 
                    bot.close();
                    System.exit(0); 
                }
            });
            pButtons.add(bClose);
            
        main.setVisible(true);
    }    
    public void run(Bot bot){
        this.bot = bot;
        run();
    }
    
    private void highlight(JTextArea jta, String word, Color color){
        DefaultHighlighter.DefaultHighlightPainter  hp = new DefaultHighlighter.DefaultHighlightPainter(color);        
        String contents = jta.getText();
//        word = "'''" + word + "'''";
        try{
            int c = 0, start, end;
            while(c < contents.length()){
                if(contents.substring(c).contains(word)){
                    start = contents.substring(c).indexOf(word) + c;
                    end = start + (word).length();
                    jta.getHighlighter().addHighlight(start, end, hp);                                    
                    c = end + 1;
                }else{ c = contents.length(); }
            }
        }catch(BadLocationException ex) {}
    }
}
