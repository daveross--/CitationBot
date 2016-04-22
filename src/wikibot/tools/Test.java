package wikibot.tools;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.*;
import wikibot.Wiki;
import wikibot.objects.Result;
import wikibot.tools.*;

public class Test {

    static int get = 50;
    
    public static void run(){
        Wiki wiki = new Wiki();
//        wiki.setBot();
        wiki.login(); // 100, 101 (app/talk) 118, 119 (recon/talk)        
        
//        boolean go;
//        String cont = "";
//        do{
//            Result r = wiki.getNamespace("100", 100, cont, "Proto");
//            LinkedHashMap<String,String> contents = wiki.readAll(r.getPages());
//            
//            for(String key : contents.keySet()){
//                String content = contents.get(key);
//                if(!content.contains("{{reconstruct")){
//                    System.out.println("* [[" + key + "]]");
//                }else{
//                    try{
//                        String from = key, to = key.replace("Appendix:", "Reconstruction:");
//                        System.out.println(from + "\t" + to);
//                        System.in.read();
//                        wiki.move(from, to, "Testing fancy new move script");                    
//                    }catch(Exception e){}
//                }
//            }
//            
//            cont = r.getContinueString();
//            go = cont.compareTo("") != 0;
////            System.out.println(cont);
//        }while(go);
        
//        wiki.create("User:TheDaveRoss/testpage", "testing content", "testing");
//        wiki.globalBlockList(5000);

        
    }
    
    private static int count(String full, String part){
        int cur = 0, count = 0;
        try{
            int lastIndex = 0;
            while(lastIndex != -1){

                lastIndex = full.indexOf(part,lastIndex);

                if(lastIndex != -1){
                    count ++;
                    lastIndex += part.length();
                }
            }
        }catch(Exception e){ return count; }
        return count;
    }
}