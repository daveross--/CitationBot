package wikibot.objects;

public class JSON_Login {
    Login login;
    
    class Login{
        String result;
        int lguserid;
        String lgusername;
        String lgtoken;
        String cookieprefix;
        String sessionid;
    }
    
    public String getUserID(){ return Integer.toString(login.lguserid); }
    public String getUserName(){ return login.lgusername; }
    public String getLogin(){ return login.lgtoken; }
    public String getSession(){ return login.sessionid; }
}
