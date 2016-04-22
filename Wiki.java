package wikibot;

import wikibot.objects.Result;
import wikibot.objects.JSON_Pages;
import wikibot.objects.JSON_Info;
import wikibot.objects.JSON_Category;
import wikibot.objects.JSON_Embeds;
import wikibot.objects.JSON_Module;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wikibot.objects.JSON_AllPages;
import wikibot.objects.JSON_Blocks;
import wikibot.objects.JSON_Login;
import wikibot.objects.JSON_Tokens;
import wikibot.tools.Spreadsheet;

public class Wiki {
    
    private boolean logged_in = false;
    private String wiki = "https://en.wiktionary.org/";
//    private final String uri = "en.wiktionary.org";
    private boolean bot = false;
    private String user = "", password = "";
//    private final String prefix = "enwiktionary";
    private String loginToken;
    private String sessionId;
    private String userId;
    private String wiki_session;
    private String namespaces = "0";
    private long startOfRun = 0;
    private long lastedit = 0;
    private long lastread = 0;
    private final long maxPageRequests = 350; // 500 should be OK, maybe a byte limit of 8000?
    private final long minReadInterval = 10l;
    private final long minEditInterval = 1000l;    
    
    java.net.CookieManager ckMgr = new java.net.CookieManager(); 
    CookieStore ckStr = ckMgr.getCookieStore();
            
    private final File pwfile = new File("");
    
    public Wiki(){}
    public Wiki(String url){
        wiki = "https://" + url + "/";
    }
    
    public void setBot(){ bot = true; }
    public void login(){
        if(bot){ 
            user = "TheDaveBot"; password = "tincat22";            
        }else{
            user = "TheDaveRoss"; password = "bl@st3d88.4xx5";            
        }
        if(!logged_in){ // works as of 2015-12-18
            try{            
                ckMgr = new java.net.CookieManager();   
                ckMgr.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                CookieHandler.setDefault(ckMgr);
                
                // Send the request
                URL url = new URL(wiki + "w/api.php?action=query&meta=tokens&type=login&formatversion=2&format=json&utf8=");
                String a = SendURL(url); 
//                System.out.println(a);
                
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(a)); rdr.setLenient(true);
                JSON_Tokens j = g.fromJson(rdr, JSON_Tokens.class); // was a in place of rdr
                
                // Send the confirmation
                URL url2 = new URL(wiki + "w/api.php?action=login&formatversion=2&format=json&utf8="
                        + "&lgname=" + user + "&lgpassword=" + password + "&lgtoken=" + URLEncoder.encode(j.getLogin(),"UTF-8"));
                a = SendURL(url2);
//                System.out.println(a);
                
                g = new Gson();
                rdr = new JsonReader(new StringReader(a)); rdr.setLenient(true);
                JSON_Login jl = g.fromJson(rdr, JSON_Login.class); // was a in place of rdr

                userId = jl.getUserID();
                sessionId = jl.getSession();
                wiki_session = sessionId;
                loginToken = jl.getLogin();
                
                //Output the response
                if(a.contains("Success")){
                    logged_in = true;
                    startOfRun = System.currentTimeMillis();
                    System.out.println("Successfully logged in as " + user  + ".");
                }else{ System.out.println("Login failed."); }
            }catch(IOException e){}
        }  
    } 
    
    public String read(String _page){
        StringBuilder answer = new StringBuilder();
        try{
            // Send the request
            URL url = new URL(wiki  + "w/api.php?action=query&titles=" + URLEncoder.encode(_page,"UTF-8") + ""
                                    + "&prop=revisions&rvprop=content"
                                    + "&formatversion=2&format=json&utf8=");

            String a = SendURL(url);            
            Gson g = new Gson();
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Pages j = g.fromJson(rdr, JSON_Pages.class); // was a in place of rdr
            
            return j.getFirst();
        }catch(IOException e){ }          
        return null; // fail
    }      
    public LinkedHashMap<String,String> readAll(LinkedList<String> _pages){
        LinkedHashMap<String,String> out = new LinkedHashMap<>();
        LinkedList<String> chunks = new LinkedList<>();
        
        try{
            // Converts the passed LinkedList to URL-ready string
            String _page = "";
            int pCount = 0;
            for(String p : _pages){
                pCount++;
                
                if(pCount < maxPageRequests){
                    _page = _page + URLEncoder.encode(p, "UTF-8") + "|";
                }else{
                    _page = _page.substring(0,_page.length()-1);
                    chunks.add(_page);
                    pCount = 0;
                    _page = "" + URLEncoder.encode(p, "UTF-8") + "|";
                }
            }            
            if(_page.length() > 2){
                _page = _page.substring(0,_page.length()-1);
                chunks.add(_page);                
            }
            
            for(String chunk : chunks){
                // Send the request
                URL url = new URL(wiki  + "w/api.php?action=query&titles=" + chunk 
                        + "&prop=revisions"
                        + "&rvprop=content"
                        + "&formatversion=2&format=json&utf8=");      

                String a = SendURL(url);
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(a));
                rdr.setLenient(true);
                JSON_Pages j = g.fromJson(rdr, JSON_Pages.class); // was a in place of rdr

                if(j.getContents() == null){ System.out.println("Null:\r\n" + _page); }
                out.putAll(j.getContents());
            } 
            return out;
           
        }catch(IOException e){ }  
        
        return null; // fail
    }
    public LinkedHashMap<String,String> readAll(LinkedList<String> _pages, String section){
        if(section.length() > 0){ section = section + "#"; }
        
        try{
            // Converts the passed LinkedList to URL-ready string
            String _page = "";
            for(String p : _pages){ 
                _page = _page + URLEncoder.encode(p + section,"UTF-8") + "|"; 
            }
            _page = _page.substring(0, _page.length()-1);
            
            // Send the request
            URL url = new URL(wiki  + "w/api.php?action=query&titles=" + _page
                                    + "&prop=revisions&rvprop=content"
                                    + "&formatversion=2&format=json&utf8=");
            
            String a = SendURL(url);            
            Gson g = new Gson();
            JSON_Pages j = g.fromJson(a, JSON_Pages.class);
            
            if(j.getContents() == null){ System.out.println("Null:\r\n" + _page); }
            
            return j.getContents(); 
        }catch(IOException e){ }  
        
        return null; // fail
    }    
    public LinkedHashMap<String,String> readAll2(LinkedList<String> _pages){
        LinkedHashMap<String,String> out = new LinkedHashMap<>();
        StringBuilder answer = new StringBuilder();
        
        // Split the page list into chunks small enough that we can send requests to the wiki
        int pCount = 0;
        String pages = "";
        LinkedList<String> chunks = new LinkedList<>();
        try{
            for(String page : _pages){
                if(pCount >= maxPageRequests){
                    chunks.add(pages.substring(0,pages.length()-1));
                    pages = "";
                    pCount = 0;
                }
                pages = pages + URLEncoder.encode(page,"UTF-8") + "|";
                pCount++;
            }
            if(pages.length() >= 2){ chunks.add(pages.substring(0,pages.length()-1)); }

            for(String chunk : chunks){
                URL url = new URL(wiki  + "w/api.php?action=query&titles=" + chunk 
                                + "&prop=revisions&rvprop=content"
                                + "&formatversion=2&format=json&utf8=");      

                String a = SendURL(url);
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(a));
                rdr.setLenient(true);
                JSON_Pages j = g.fromJson(rdr, JSON_Pages.class); // was a in place of rdr

                out.putAll(j.getContents());
                answer = new StringBuilder();            
            }
            
            return out;
        }catch(IOException | JsonIOException | JsonSyntaxException e){ }
        
        return null;
    }
    
    public JSON_Pages getRevisions(String _page){
        try{
            // Send the request
            URL url = new URL(wiki  + "w/api.php?action=query&titles=" + URLEncoder.encode(_page, "UTF-8") + ""
                                    + "&prop=revisions&rvprop=content"
                                    + "&rvprop=ids|user|flags|timestamp|comment|content"
                                    + "&rvlimit=max"
                                    + "&formatversion=2&format=json&utf8=");
                        
            String a = SendURL(url); //System.out.println(a);
            Gson g = new Gson();
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Pages j = null;
            try{ j = g.fromJson(rdr, JSON_Pages.class);
            }catch(JsonIOException | JsonSyntaxException e){ System.out.println(a); }
            
            return j;
        }catch(IOException e){ }          
        return null; // fail        
    }
    public JSON_Pages getRevsBetween(String _start, String _end){
        try{
//            _start = URLEncoder.encode(_start, "UTF-8");
//            _end = URLEncoder.encode(_end, "UTF-8");
            // Send the request
            URL url = new URL(wiki  + "w/api.php?action=query&list=allrevisions"
//                                    + "&arvprop=ids|flags|timestamp|user|size|parsedcomment"
                                    + "&arvstart=" + _start + "&arvend=" + _end
                                    + "&arvlimit=5000"
                                    + "&formatversion=2&format=json&utf8=");
                        
            String a = SendURL(url); System.out.println(url);
            Gson g = new Gson();
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Pages j = null;
            try{ j = g.fromJson(rdr, JSON_Pages.class);
            }catch(JsonIOException | JsonSyntaxException e){ System.out.println(a); }
            
            return j;
        }catch(IOException e){ System.err.println(e.getMessage()); }          
        return null; // fail        
    }
    
    public Result getCategory(String _cat, int _limit, String _start){
        try{
//            if(_cat.toLowerCase().startsWith("category:")){ _cat = _cat.replaceFirst("[Cc]ategory:", ""); }
            URL url = new URL(
                    wiki + "w/api.php?action=query"
                            + "&format=json&utf8="
                            + "&list=categorymembers"
                            + "&cmnamespace=0|114"
                            + "&cmtype=page"
                            + "&cmtitle=" + URLEncoder.encode(_cat,"UTF-8") + ""
//                            + "&cmdir=desc"
                            + "&cmlimit=" + _limit + ""
                            + (_start != null ? "&cmcontinue=" + _start : "")
                            );   
            
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "TheDaveBot");
//            conn.setRequestProperty("Cookie", cookie);
            while(lastread > 0 && System.currentTimeMillis() - lastread < minReadInterval){ TimeUnit.MILLISECONDS.sleep(minReadInterval - (System.currentTimeMillis() - lastread)); }
            conn.setDoOutput(true);            
            lastread = System.currentTimeMillis(); // record the last time we read from the Wiki
            
            // Get the response
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line);
                }
            }
            String a = answer.toString();
//            System.out.println(a); // testing
            
            Gson g = new Gson();            
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Category j = g.fromJson(rdr, JSON_Category.class);
            
            return new Result().setReference(_cat).setPages(j.getPages()).setContinueString(j.getContinue()).setComplete(j.getComplete());
                        
        }catch(IOException | InterruptedException e){ }
        return null; // null on fail
    }   
    public Result getEmbeds(String _page, int _limit, String _start){
        try{
            URL url = new URL(
                    wiki + "w/api.php?action=query"
                            + "&format=json&utf8="
                            + "&list=embeddedin"
                            + "&einamespace=0"
                            + "&eititle=" + URLEncoder.encode(_page,"UTF-8") + ""
                            + "&eilimit=" + _limit
                            + "&eifilterredir=nonredirects"
                            + (_start != null && !_start.isEmpty() ? "&eicontinue=" + _start : "")
                            );   
                        
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "TheDaveBot");
//            conn.setRequestProperty("Cookie", cookie);
            
            while(lastread > 0 && System.currentTimeMillis() - lastread < minReadInterval){ TimeUnit.MILLISECONDS.sleep(minReadInterval - (System.currentTimeMillis() - lastread)); }
            conn.setDoOutput(true);            
            lastread = System.currentTimeMillis(); // record the last time we read from the Wiki
            
            // Get the response
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line);
                }
            }
            String a = answer.toString();
            
            Gson g = new Gson();            
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Embeds j = g.fromJson(rdr, JSON_Embeds.class);            
//            System.out.println(j.getPages().size() + "\r\n" + j.getPages());
            
            return new Result().setReference(_page).setPages(j.getPages()).setContinueString(j.getContinue()).setComplete(j.getComplete());
            
        }catch(IOException | InterruptedException e){ }
        return null; // null on fail
    }
    public Result getNamespace(String _ns, int _limit, String _start){
        return getNamespace(_ns, _limit, _start, null);
    }
    public Result getNamespace(String _ns, int _limit, String _start, String _prefix){
        if(!(_start != null)){ _start = ""; }
        try{
            URL url = new URL(
                    wiki + "w/api.php?action=query"
                            + "&format=json&utf8="
                            + "&list=allpages"
                            + "&apnamespace=" + _ns
                            + "&apfrom=" + URLEncoder.encode(_start,"UTF-8")
                            + "&aplimit=" + _limit + ""
                            + "&apfilterredir=nonredirects"
                            + "&apcontinue=" + _start
                            + (_prefix != null ? "&apprefix=" + _prefix : "")
                            );   
            
//            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("User-Agent", "TheDaveBot");
////            conn.setRequestProperty("Cookie", cookie);
//            while(lastread > 0 && System.currentTimeMillis() - lastread < minReadInterval){ TimeUnit.MILLISECONDS.sleep(minReadInterval - (System.currentTimeMillis() - lastread)); }
//            conn.setDoOutput(true);            
//            lastread = System.currentTimeMillis(); // record the last time we read from the Wiki
//            
//            // Get the response
//            StringBuilder answer = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    answer.append(line);
//                }
//            }
//            String a = answer.toString();
            String a = SendURL(url); // System.out.println(a);
            
            Gson g = new Gson();            
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_AllPages j = g.fromJson(rdr, JSON_AllPages.class);
            return new Result().setReference("NS:" + _ns).setPages(j.getPages()).setContinueString(j.getContinue()).setComplete(j.getComplete());
                        
        }catch(IOException e){ }
        return null; // null on fail 
    }
    
    public String edit(String _page, String _content, String _summary){
        String page = _page;
        String edittoken;
        String pageid;
        String summary = _summary;// String createonly; // this can be used to only create new pages... // set bot flag!
        
        try{
            // Get edit token...
            URL url = new URL(wiki + "w/api.php?action=query&meta=tokens&format=json&type=csrf&titles=" + URLEncoder.encode(page,"UTF-8"));
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; ");
            conn.setRequestProperty("User-Agent", "TheDaveBot");
//            conn.setRequestProperty("Cookie", cookie);
            
            // Sleep until enough time has lapsed since the last edit.
            while(lastedit > 0 && System.currentTimeMillis() - lastedit < minEditInterval){ TimeUnit.MILLISECONDS.sleep(minEditInterval - (System.currentTimeMillis() - lastedit)); }
            conn.setDoOutput(true);
            lastedit = System.currentTimeMillis();
            
            // Get the response
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line);
                }
            }
            //{"batchcomplete":"","query":{"pages":{"1998818":{"pageid":1998818,"ns":2,"title":"User:TheDaveBot/test"}},"tokens":{"csrftoken":"7424f8e9e0df6090fe395781ece4bef45674be29+\\"}}}
            // @todo: convert all of this to a regex pattern with groups so it can absorb small changes
            Pattern p = Pattern.compile("\\{\"batchcomplete\""
                    + "(.+?)\"pageid\":([0-9]+)"
                    + "(.+?)\"csrftoken\":\"([0-9a-f+]+\\\\)"
                    + "(.+?)");
            Matcher m = p.matcher(answer);
            if(m.matches()){
                edittoken = m.group(4);
                pageid = m.group(2);   
            // Write to page...
                URI uri = new URI("https", 
                                  "en.wiktionary.org",
                                  "/w/api.php", 
                                  "action=edit"
                                          + "&bot"
                                          + "&minor"
                                          + "&pageid=" + URLEncoder.encode(pageid,"UTF-8") + ""
                                          + "&summary=" + summary + ""
                                          + "&format=json&utf8="
                                    ,
                                  null);
                URL wurl = new URL(uri.toString().replaceAll("\\+","%2B").replaceAll("\\\\","%5C"));
                
                String body = "text=" + URLEncoder.encode(_content, "UTF-8") 
                        + "&token=" + edittoken.replaceAll("\\+","%2B").replaceAll("\\\\","%5C");
//                PrintStream out = new PrintStream(System.out, true, "UTF-8"); out.println(_content);
                
                HttpURLConnection wconn = (HttpURLConnection)wurl.openConnection();
                wconn.setRequestMethod("POST");
                wconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                wconn.setRequestProperty("User-Agent", "TheDaveBot");
                wconn.setRequestProperty("Content-Length", Integer.toString(body.length()));
                wconn.setDoOutput(true);
                wconn.getOutputStream().write(body.getBytes("UTF-8")); // This adds the edittoken to the body of the request
                lastedit = System.currentTimeMillis();
                
                answer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(wconn.getInputStream(), "UTF-8"))) {
                    String line;
                    while((line = reader.readLine()) != null){ answer.append(line); }
                }
                if(answer.toString().contains("\"result\":\"Success\"")){
                    System.out.println("Successful write to " + page + "");
                    return "Successful write to " + page + ".";
                }else{                    
                    System.out.println("Failed to write to " + page + "");
                    System.out.println(answer.toString());
                    return ("Failed to write to " + page + " with resoponse:\r\n\t" + answer.toString());
                }
            }else{
                return ("Failed to correctly make a POST request to wiki.\r\n\r\n" + answer.toString());
            }
        }catch(IOException | InterruptedException | URISyntaxException e){ }
        return "Fail?";
    }  
    public String create(String _page, String _content, String _summary){
        String page = _page;
        String edittoken;
        String summary = _summary;// String createonly; // this can be used to only create new pages... // set bot flag!
        
        if(exists(page)){ return page + " already exists, cannot write."; }
        
        try{
            // Get edit token...
            URL url = new URL(wiki + "w/api.php?action=query&meta=tokens&format=json&type=csrf&titles=" + page);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; ");
            conn.setRequestProperty("User-Agent", "TheDaveBot");
            
            // Sleep until enough time has lapsed since the last edit.
            while(lastedit > 0 && System.currentTimeMillis() - lastedit < minEditInterval){ TimeUnit.MILLISECONDS.sleep(minEditInterval - (System.currentTimeMillis() - lastedit)); }
            conn.setDoOutput(true);
            lastedit = System.currentTimeMillis();
            
            // Get the response
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    answer.append(line);
                }
            }
            // if the page is missing, just get the CSRF token
            // {"batchcomplete":"","query":{"pages":{"-1":{"ns":2,"title":"User:TheDaveRoss/Citations:strait-waistcoat","missing":""}},"tokens":{"csrftoken":"cdbdaeeb9fa44c243d883b3476c2424c568c355c+\\"}}}
            Pattern p = Pattern.compile("\\{\"batchcomplete\""
                    + "(.+?)\"csrftoken\":\"([0-9a-f+]+\\\\)"
                    + "(.+?)");
            Matcher m = p.matcher(answer);
            if(m.matches()){
                edittoken = m.group(2);
            // Write to page...
                URI uri = new URI("https", 
                                  "en.wiktionary.org",
                                  "/w/api.php", 
                                  "action=edit"
                                          + "&createonly"
                                          + "&recreate"
                                          + "&bot"
                                          + "&minor"
                                          + "&title=" + _page.replaceAll(" ", "_") + "" // URLEncoder.encode(_page,"UTF-8")
                                          + "&summary=" + summary + ""
                                          + "&format=json&utf8="
                                    ,
                                  null);
                URL wurl = new URL(uri.toString().replaceAll("\\+","%2B").replaceAll("\\\\","%5C"));
                
                String body = "text=" + URLEncoder.encode(_content, "UTF-8") 
                        + "&token=" + edittoken.replaceAll("\\+","%2B").replaceAll("\\\\","%5C");
                
                HttpURLConnection wconn = (HttpURLConnection)wurl.openConnection();
                wconn.setRequestMethod("POST");
                wconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                wconn.setRequestProperty("User-Agent", "TheDaveBot");
                wconn.setRequestProperty("Content-Length", Integer.toString(body.length()));
                wconn.setDoOutput(true);
                wconn.getOutputStream().write(body.getBytes("UTF-8")); // This adds the edittoken to the body of the request
                lastedit = System.currentTimeMillis();
                
                answer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(wconn.getInputStream(), "UTF-8"))) {
                    String line;
                    while((line = reader.readLine()) != null){ answer.append(line); }
                }
//                System.out.println(answer.toString());
                if(answer.toString().contains("\"result\":\"Success\"")){
                    System.out.println("Successful creation of " + page + "");
                    return ("Successful write to " + page + "");
                }else{                    
                    System.out.println("Failed to create " + page + "");
                    System.err.println(answer.toString());
                    return ("Failed to write to " + page + " with resoponse:\r\n\t" + answer.toString());
                }
            }else{
                return ("Failed to correctly make a POST request to wiki.\r\n\r\n" + answer.toString());
            }
        }catch(IOException | InterruptedException | URISyntaxException e){ }
        
        return "General Failure in Wiki.create for " + page + ".";
    }     
    public String write(String _page, String _content, String _summary){
        if(this.exists(_page)){
            return edit(_page, _content, _summary);
        }else{
            return create(_page, _content, _summary);
        }
    }
    public void move(String _from, String _to, String _summary){
        try{
            URL url = new URL(wiki + "w/api.php?action=query&meta=tokens&format=json&type=csrf&titles=" + URLEncoder.encode(_from,"UTF-8"));
            String a = SendURL(url);
            
            Gson g = new Gson();
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Tokens j = g.fromJson(rdr, JSON_Tokens.class); // was a in place of rdr
            
            url = new URL(wiki + "w/api.php?action=move"
                    + "&bot"
                    + "&movetalk"
                    + "&from=" + URLEncoder.encode(_from, "UTF-8")
                    + "&to=" + URLEncoder.encode(_to, "UTF-8")
                    + "&reason=" + URLEncoder.encode(_summary ,"UTF-8")
                    + "&format=json&utf8="
            );
            String body = "token=" + URLEncoder.encode(j.getCSRF(), "UTF-8");
            a = SendURL(url, body); // System.out.println(a);           
            System.out.println("Moved " + _from + " to " + _to);
        }catch(UnsupportedEncodingException | MalformedURLException e){}
    }
    
    public void unblock(long _id, String _user, String _summary){
        try{
            URL url = new URL(wiki + "w/api.php?action=query&meta=tokens&format=json&type=csrf");
            String a = SendURL(url);
            
            Gson g = new Gson();
            JsonReader rdr = new JsonReader(new StringReader(a));
            rdr.setLenient(true);
            JSON_Tokens j = g.fromJson(rdr, JSON_Tokens.class); // was a in place of rdr
            
            url = new URL(wiki + "w/api.php?action=unblock"
                    + (_id > 0 ? "&id=" + _id : "")
                    + (_id <= 0 ? "&user=" + _user : "")
                    + "&formatversion=2&format=json&utf8="
            );
            String body = ""
                    + "reason=" + URLEncoder.encode(_summary,"UTF-8")
                    + "&token=" + URLEncoder.encode(j.getCSRF(), "UTF-8");
            a = SendURL(url, body); // System.out.println(a);  
            if(a.contains("cantunblock")){ System.err.println("FAIL: " + _user + "\t" + a); 
            }else{ System.out.println("Unblocked " + _id + " (" + _user + ")"); }
        }catch(UnsupportedEncodingException | MalformedURLException e){}
    }
    
    public boolean interwiki(String _code, String _page){
        // Check if the page exists on the FL wiki
        try{
            // Send the request
            URL url = new URL("https://" + _code + ".wiktionary.org/"
                                + "w/api.php?action=query&titles=" + URLEncoder.encode(_page,"UTF-8")
                                + "&prop=info"
                                + "&formatversion=2"
                                + "&format=json&utf8="
            );
            String a = SendURL(url); //System.out.println(a);
            
            Gson g = new Gson();
            if(a.startsWith("{")){
                JSON_Info j = g.fromJson(a, JSON_Info.class);
                return j.exists();
            }else{
                System.out.println("Bad response from: " + url.toString());
                return false;
            }                        
        }catch(IOException e){ }        
        return false; // fail
    }
    public String normalize(String _lang, String _page){
        try{
            String code = ""
                    + "local interwiki_langs = { " // this stuff is hardcoded in the Module...
                    + " [\"nds-de\"] = \"nds\", "
                    + " [\"nds-nl\"] = \"nds\", "
                    + " [\"pdt\"] = \"nds\", "
                    + " } "                    
                    + "lang = require(\"Module:languages\").getByCanonicalName(\"" + _lang +"\") "                    
                    + "if interwiki_langs[lang:getCode()] then "
                        + "wmlangs = {require(\"Module:wikimedia languages\").getByCode(interwiki_langs[lang:getCode()])} "
                    + "else "
                        + "wmlangs = lang:getWikimediaLanguages() "
                    + "end "                    
                    + "print(lang:makeEntryName(\"" + _page + "\")) "
                    + "";

            // Send the request
            URL url = new URL("https://" + "en" + ".wiktionary.org/"
                                + "w/api.php?action=scribunto-console"
                                + "&format=json&utf8="
                                + "&title=" + URLEncoder.encode("Module:translations","UTF-8")
                                + "&question=" + URLEncoder.encode(code,"UTF-8")
            );
            
            String a = SendURL(url);
            Gson g = new Gson();
            if(a.startsWith("{")){
                JSON_Module j = g.fromJson(a, JSON_Module.class);
                return j.results()[0].trim();                
            }else{ System.out.println("Bad response from: " + url.toString()); }
        }catch(IOException e){ }
        return null; // null on failure
    }
    
    public LinkedList<LinkedHashMap<String,String>> getIndefIPBlocks(){
        
        LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
            
        try{
            int limit = 5000;
            boolean go;
            String cont = "";
            do{
                URL url = new URL(wiki + "w/api.php?action=query&list=blocks"
                        + "&bkdir=newer"
                        + "&bkprop=user|id|range|timestamp|by|expiry|reason"
                        + "&bklimit=" + limit
                        + (cont.length() > 0 ? "&bkcontinue=" + cont : "")
                        + "&formatversion=2&format=json&utf8="
                );
                String response = SendURL(url);
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(response));
                rdr.setLenient(true);
                JSON_Blocks j = g.fromJson(rdr, JSON_Blocks.class); // was a in place of rdr
                
                out.addAll(j.getIndefIPBlocks());     
                cont = j.cont();
                go = j.cont().length() > 3;  
            }while(go);
            
        }catch(MalformedURLException | JsonIOException | JsonSyntaxException e){}
        System.out.println("Found " + out.size() + " indef IP blocks.");
        return out;
    }
    public void blockList(int limit){
        try{
            boolean go;
            LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
            String cont = "";
            do{
                URL url = new URL(wiki + "w/api.php?action=query&list=blocks"
                        + "&bkdir=newer"
                        + "&bkprop=user|id|range|timestamp|by|expiry|reason"
                        + "&bklimit=" + limit
                        + (cont.length() > 0 ? "&bkcontinue=" + cont : "")
                        + "&formatversion=2&format=json&utf8="
                );
                String response = SendURL(url);
//                System.out.println(response);
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(response));
                rdr.setLenient(true);
                JSON_Blocks j = g.fromJson(rdr, JSON_Blocks.class); // was a in place of rdr
                
                out.addAll(j.getBlocks());     
                cont = j.cont();
//                System.out.println(cont);
                go = j.cont().length() > 3;  
//                go = false;
            }while(go);
            
            Pattern ipv4 = Pattern.compile("(([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/[0-9]{1,2})*");
            Pattern ipv6 = Pattern.compile("([0-9A-F]{0,4}:){7}[0-9A-F]{0,4}(/[0-9]{1,2})*");
            LinkedList<LinkedHashMap<String,String>> iponly = new LinkedList<>();
            
            for(LinkedHashMap<String,String> lhm : out){
//                System.out.println(lhm);
                String u = lhm.get("user");
                Matcher v4 = ipv4.matcher(u);
                Matcher v6 = ipv6.matcher(u);
                if(v4.matches()){
//                    System.out.println("V4:\t" + u);
                    iponly.add(lhm);
                }else if(v6.matches()){
//                    System.out.println("V6:\t" + u);                    
                    iponly.add(lhm);
                }else{
//                    System.out.println("No:\t" + u);
                }
            }
            
            Spreadsheet.printer("IP Block Log for enwiktionary", iponly);
            
        }catch(MalformedURLException | JsonIOException | JsonSyntaxException e){}
    }
    
    public void globalBlockList(int limit){
        try{
            boolean go;
            LinkedList<LinkedHashMap<String,String>> out = new LinkedList<>();
            String cont = "";
            do{
                URL url = new URL(wiki + "w/api.php?action=query&list=globalblocks"
                        + "&bgdir=newer"
                        + "&bgprop=address|id|range|timestamp|by|expiry|reason"
                        + "&bglimit=" + limit
                        + (cont.length() > 0 ? "&bgstart=" + cont : "")
                        + "&formatversion=2&format=json&utf8="
                );
                String response = SendURL(url);
//                System.out.println(response);
                Gson g = new Gson();
                JsonReader rdr = new JsonReader(new StringReader(response));
                rdr.setLenient(true);
                JSON_Blocks j = g.fromJson(rdr, JSON_Blocks.class); // was a in place of rdr
                
                out.addAll(j.getBlocks());     
                cont = j.cont();
                System.out.println("Cont:\t" + cont);
                go = j.cont().length() > 3;     
//                go = false;
            }while(go);
            
//            System.out.println(out);
            Spreadsheet.printer("Block Log for Global", out);
            
        }catch(MalformedURLException | JsonIOException | JsonSyntaxException e){}
    }
    
    public boolean exists(String _page){
        // fast check if a page exists
        try{
            // Send the request
            URL url = new URL(wiki  + "w/api.php?action=query&titles=" + URLEncoder.encode(_page,"UTF-8") + ""
                                    + "&prop=info"
                                    + "&formatversion=2&format=json&utf8=");
            
            String a = SendURL(url);
            //{"batchcomplete":true,"query":{"pages":[{"pageid":101243,"ns":114,"title":"Citations:hirsute","contentmodel":"wikitext","pagelanguage":"en","pagelanguagehtmlcode":"en","pagelanguagedir":"ltr","touched":"2016-01-04T05:35:05Z","lastrevid":26839623,"length":1124}]}}
            //{"batchcomplete":true,"query":{"pages":[{"ns":114,"title":"Citations:hairsuite","missing":true,"contentmodel":"wikitext","pagelanguage":"en","pagelanguagehtmlcode":"en","pagelanguagedir":"ltr"}]}}
             
            // quick and dirty
            return !a.contains("\"missing\":true");
           
        }catch(IOException e){ }  
        
        return false; // fail
    }
    public HashMap<String,Boolean> exists(List<String> _pages){
        // checks whether a bunch of pages exist, and returns a map of true/falses
        int chunkSize = 200; // maximum titles to check in a single query...
        HashMap<String,Boolean> out = new HashMap<>();
        String chunk = "";
        ArrayList<String> chunks = new ArrayList<>();
        int count = 0;
        for(String _page : _pages){
            if(count++ >= chunkSize){
                chunks.add(chunk.substring(0,chunk.length()-1));               
                chunk = "" + _page + "|"; count = 1;
            }else{
                chunk = chunk + _page + "|";
            }
        }
        if(chunk.compareTo("") != 0){ chunks.add(chunk.substring(0,chunk.length()-1)); } // add any remainder
        
        try{
            for(String titles : chunks){
                URL url = new URL(wiki  + "w/api.php?action=query&titles=" + URLEncoder.encode(titles, "UTF-8")
                                        + "&prop=info&formatversion=2&format=json&utf8=");

                String a = SendURL(url);
                Gson g = new Gson();
                if(a.startsWith("{")){ 
                    JSON_Info j = g.fromJson(a, JSON_Info.class); out.putAll(j.exist());
                }else{ System.out.println("Bad response from: " + url.toString()); }  
            }            
            
            return out;           
        }catch(IOException e){ }  
        
        return null; // fail
    }
    public HashMap<String,Boolean> citesExist(List<String> _pages){
        // check if cites exist for pages
        List<String> out = new ArrayList<>();
        for(String s : _pages){ out.add("Citations:" + s); }
        return exists(out);
    }    
    public void setNamespace(String ns){ namespaces = ns; }    
    public String SendURL(URL url){
        return SendURL(url, null);
    }
    public String SendURL(URL url, String body){          
        StringBuilder a = new StringBuilder();
        
        try{
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();                
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "User-Agent", "TheDaveBot" );
            conn.setDoOutput(true);
            if(body != null){ conn.getOutputStream().write(body.getBytes("UTF-8")); } // include body if it is passed

            // Get the response
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
                String line;
                while ((line = reader.readLine()) != null) {
                    a.append(line);
                }
            }catch(Exception e){ return "Failed to read result."; }
        }catch(IOException e){ e.printStackTrace(); return "Failed to connect to wiki."; }
        
        return a.toString();
    }
    
    // General Getters
    public String getUser(){ return this.user; }
    public boolean isLoggedIn(){
        return this.logged_in;
    }
}
