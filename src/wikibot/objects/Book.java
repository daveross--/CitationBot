package wikibot.objects;

import java.io.File;

public class Book {

    
    private static final String work = "C:\\Users\\davidr\\Google Drive\\Java\\CitationBot\\text\\"; // work   
    private static final String home = "C:\\Users\\dave\\Google Drive\\Java\\CitationBot\\text\\"; // home    
    
    private String title;
    private String author;
    private String year;
    private String file;
    
    public Book(){}

    private final String root = root(); // chooses home or work based on what exists    
    
    public Book setTitle(String title){
        this.title = title;
        return this;
    }
    public Book setAuthor(String author){
        this.author = author;
        return this;
    }
    public Book setYear(String year){
        this.year = year;
        return this;
    }
    public Book setFile(String file){
        this.file = file;
        return this;
    }
    public String wpTitle(){
        return "[[w:" + title + "|" + title + "]]";
    }
    public String wpAuthor(){
        return "[[w:" + author + "|" + author + "]]";
    }
    public String wpSummary(){
        return "" + year + " — " + wpAuthor() + ". " + wpTitle() + ".";
    }
    public String getTitle(){
        return this.title;
    }
    public String getYear(){
        return this.year;
    }
    public String getAuthor(){
        return this.author;
    }
    public String getFile(){
        return this.file;
    }
    
    public static String root(){
        File h = new File(home);
        if(h.exists()){
            return home;
        }else{
            return work;
        }
    }    
}
