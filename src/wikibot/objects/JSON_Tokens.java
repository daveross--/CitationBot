package wikibot.objects;

public class JSON_Tokens {
    boolean batchcomplete;
    Query query;
            
    public String getLogin(){ return this.query.tokens.logintoken; }
    public String getCSRF(){ return this.query.tokens.csrftoken; }    
    
    class Query {
        Tokens tokens;
        
        class Tokens{
            String logintoken;
            String csrftoken;
        }
    }
}
