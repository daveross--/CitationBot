package wikibot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reader {
    
    public Reader(){}
    
    
    public static void read(File f){
        try{

            BufferedReader br = new BufferedReader(new FileReader(f));
            String text = "";
            String line = "";
            boolean go = false;
            while((line = br.readLine())!= null){
                if(line.startsWith("End of the Project Gutenberg EBook")){ go = false; } // stop
                if(go){ text = text + "\r\n" + line; }
                if(line.startsWith("*** START OF THIS PROJECT GUTENBERG EBOOK")){ go = true; } // start

            }
            /* Format */
            text = text.replaceAll("[\\r|\\n]{3,}+", "XXXXXX"); // replace all instances of 3+ carriage returns with just two.
            text = text.replaceAll("[\\r|\\n]", " "); // replace all single carriage returns with just a space (formatting)
            text = text.replaceAll("  ", " "); // replace all double spaces with single
            text = text.replaceAll("XXXXXX", "\r\n\r\n"); // adding the two carriage returns for instances of 3+
            
            /* Segment */
            String[] segments = text.split("\\r\\n\\r\\n");
            
            /* Analyze */
            int c = 0;
            for(String segment : segments){
                boolean use = true; // if we find some exclusion criteria we will skip the paragraph.
                String s = segment.replaceAll("[^A-Za-z'\"]" ," ");
                s = s.replaceAll("\\s+", " ");
                List<String> words = Arrays.asList(s.split("\\s"));
                Collections.sort(words);
                ArrayList<String> uWords = new ArrayList<>();
                for(String word : words){
                    boolean addWord = true;
                    if(uWords.contains(word)){ addWord = false; }
                    if(word.length() <= 3){ addWord = false; }
                    if(word.contains("'") || word.contains("\"")){ addWord = false; }
                    Pattern p = Pattern.compile(".*?[A-Z]+.*?");
                    if(p.matcher(word).matches()){ addWord = false; } // no uppercase letters
                    // if no exclusion criteria are found, add the word
                    if(addWord){ uWords.add(word); }
                }
                String list = "";
                for(String word : uWords){ list = list + " " + word; }
                if(uWords.size() <= 8 || segment.length() <= 64){ use = false; } // if there are fewer than 8 unique words or 64 characters skip the paragraph
                if(use){
                    System.out.println(c++ + ": " + list.trim() 
                                    + "\r\n\r\n* '''1928''' - [[w:Leo Tolstoy|]], ''[[w:War and Peace|]]'' (''Translation: Louise and Aylmer Maude'')\r\n*: " + segment + "\r\n");
                }
            }
            
            

        }catch(Exception e){ e.printStackTrace(); }
    }
}
