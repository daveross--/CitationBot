package wikibot.tools;

import java.math.BigInteger;
import java.security.MessageDigest;

public class MD5 {
    
    public static String hash(String in){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(in.getBytes(), 0, in.length());
            return new BigInteger(1, md.digest()).toString(16);
        }catch(Exception e){ return null; }
    }    
}
