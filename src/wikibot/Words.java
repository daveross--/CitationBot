package wikibot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Words {

    public Words(){}
    
    public static void run(File f){
        try{

            BufferedReader br = new BufferedReader(new FileReader(f));

            HashMap<String,Integer> words = new HashMap<>();
            String line;
            boolean go = false;
            while((line = br.readLine())!= null){
                if(line.startsWith("End of the Project Gutenberg EBook")){ go = false; } // stop
                if(go){ 
                    line = line.replaceAll("[^A-Za-z' ]"," ");
                    String[] W = line.split(" ");
                    for(String w : W){
                        if(words.containsKey(w)){ 
                            Integer I = words.get(w) + 1;
                            words.put(w,I);
                        }else{ words.put(w, new Integer(1)); }
                    }
                }
                if(line.startsWith("*** START OF THIS PROJECT GUTENBERG EBOOK")){ go = true; } // start

            }
            for(String W : words.keySet()){
                System.out.println(W + "\t" + words.get(W));
            }

        }catch(Exception e){ e.printStackTrace(); }   
        
    }
    
}
