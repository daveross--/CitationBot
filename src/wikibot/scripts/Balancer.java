package wikibot.scripts;

import wikibot.Wiki;
import wikibot.objects.Result;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Balancer {
    private static final Wiki wiki = new Wiki();
    
    private static final Pattern EN     = Pattern.compile("(\\s*?)={2,}(\\s*English\\s*)={2,}(\\s*?)");
    private static final Pattern LX     = Pattern.compile("(\\s*?)={2,}([\\p{L}\\s\\-]+?)={2,}(\\s*?)");    
    private static final Pattern L2     = Pattern.compile("(\\s*?)={2}([\\p{L}\\s\\-]+?)={2}(\\s*?)");    
    private static final Pattern Blank  = Pattern.compile("^(\\s*?)$");    
    private static final int limit = 1;
    private static final List<String> checkSec = Arrays.asList(new String[]{"Translation","Derived terms","Related terms"});
    // Balances derived terms, related terms and translation sections.  Also updates templates as needed.
    
    public Balancer(){}
    public static void embeds(){
        String _page = "Template:t";
        wiki.login();
        run(wiki.getEmbeds(_page, limit, "0|100000"));  // water=3749997, hoi polloi=100000
    }
    public static void run(Result result){
        result = result.retrieve(wiki, 100);
        LinkedList<String> pages = result.getPages(); 
        LinkedHashMap<String,String> contents = result.getContents();
        String output = "";
        
        for(String page : pages){            
            boolean update = false; // if we make changes which need to be saved, set this to true.            
            boolean inSec  = false;
            boolean engSec = false;
            
            String section = "";
            
            String content = contents.get(page);
            for(String line : content.split("\n")){
                Matcher en = EN.matcher(line);
                Matcher l2 = L2.matcher(line);
                Matcher lx = LX.matcher(line);
                
                if(en.matches()){
                    engSec = true;
                    output = output + "ENG\t" + line + "\n";                                        
                }else if(l2.matches()){
                    engSec = false;
                    output = output + "\t" + line + "\n";  
                }else if(lx.matches()){
                    section = lx.group(2).trim();
                    if(checkSec.contains(section)){
                        inSec = true;
                    }else{
                        inSec = false; // this will break if one section follows the other immediately...
                    }
                    output = output + "\t" + line + "\n";  
                }else if(engSec && inSec){
                    
                }else if(engSec){
                    output = output + "ENG\t" + line + "\n";                      
                }else{
                    output = output + "\t" + line + "\n";
                }
            }
        }
        System.out.println(output);
    }
}
