package wikibot;

import java.io.File;
import java.io.RandomAccessFile;
import wikibot.objects.Page;

public class Dump_RAF {
    public final String xml = "enwiktionary-20160407-pages-articles";
    private final String filename = System.getProperty("user.home") + "\\Google Drive\\Java\\" + xml + ".xml";
    private final File file = new File(filename);
    private RandomAccessFile RAF;
    
    private final int chunksize = 4096; // how many bytes to read at a time
    private long cursor = 0;
    
    public Dump_RAF(){
        try{
            RAF = new RandomAccessFile(file, "r");
        }catch(Exception e){ System.out.println("Failed to create random access file."); }
    }
    
    public void read(long c){
        c = 0;
             
    }   
    
    public void setCursor(long c){ cursor = c; }
    public Page next(){
        byte[] bytes = new byte[chunksize];
        try{
            RAF.seek(cursor);
            RAF.read(bytes);
            String text = new String(bytes, "UTF8");
            
            boolean done = false;
            
            while(!done){
                if(text.contains("<page>")){
                    if(text.substring(text.indexOf("<page>")).contains("</page>")){
                        int n = text.indexOf("<page>") + text.substring(text.indexOf("<page>")).indexOf("</page>") + "</page>".length(); // find the end of the first full <page> section
                        String sub = text.substring(0, n);
                        cursor += (long)(sub.getBytes("UTF8").length); // update cursor with new closing position
                        text = sub.substring(sub.indexOf("<page>"));
                        done = true;
                    }else{ 
                        RAF.read(bytes);
                        text = text + new String(bytes, "UTF8");
                    }                
                }else{                  
                    RAF.read(bytes);
                    text = text + new String(bytes, "UTF8");
                }
            }
            
            return Page.parse(text);
            
        }catch(Exception e){ System.err.println("Failed to read dump (RAF)."); e.printStackTrace(); return null; }   
    }
    public Page prev(){
        byte[] bytes = new byte[chunksize];
        int chunksback = 1;
        try{
            RAF.seek(cursor - chunksize - 5);
            RAF.read(bytes);
            String text = new String(bytes, "UTF8");
            
            boolean done = false;
            
            while(!done){
                if(text.contains("<page>")){
                    if(text.substring(text.indexOf("<page>")).contains("</page>")){
                        int n = text.indexOf("<page>") + text.substring(text.indexOf("<page>")).indexOf("</page>") + "</page>".length(); // find the end of the first full <page> section
                        String sub = text.substring(0, n);
                        cursor += (long)(sub.getBytes("UTF8").length); // update cursor with new closing position
                        text = sub.substring(sub.indexOf("<page>"));
                        done = true;
                    }else{ 
                        RAF.seek(cursor - (chunksize * chunksback++) - 5);
                        RAF.read(bytes);
                        text = new String(bytes, "UTF8") + text;
                    }                
                }else{ 
                    RAF.seek(cursor - (chunksize * chunksback++) - 5);                 
                    RAF.read(bytes);
                    text = new String(bytes, "UTF8") + text;
                }
            }
            
            return Page.parse(text);
            
        }catch(Exception e){ System.err.println("Failed to read dump (RAF)."); return null; }         
    }
    
}
