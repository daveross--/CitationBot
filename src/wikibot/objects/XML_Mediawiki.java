package wikibot.objects;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XML_Mediawiki {
    
    Mediawiki mediawiki;
    
    public XML_Mediawiki(){}
    
    private class Mediawiki{
        Siteinfo siteinfo;
    }    
    
    private class Siteinfo{
        String SITENAME;
        String DBNAME;
        String BASE;
        String GENERATOR;
        String CASE;
        Namespace[] NAMESPACES;
        
        private class Namespace{
            String KEY;
            String CASE;
        }        
    }
    
    private class Page{

    }
    
}


/*
<mediawiki xmlns="http://www.mediawiki.org/xml/export-0.10/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.10/ http://www.mediawiki.org/xml/export-0.10.xsd" version="0.10" xml:lang="en">
  <siteinfo>
    <sitename>Wiktionary</sitename>
    <dbname>enwiktionary</dbname>
    <base>https://en.wiktionary.org/wiki/Wiktionary:Main_Page</base>
    <generator>MediaWiki 1.27.0-wmf.7</generator>
    <case>case-sensitive</case>
    <namespaces>
      <namespace key="0" case="case-sensitive" />
     </namespaces>
  </siteinfo>
  <page>
    <title>Wiktionary:Welcome, newcomers</title>
    <ns>4</ns>
    <id>6</id>
    <restrictions>edit=autoconfirmed:move=sysop</restrictions>
    <revision>
      <id>33678382</id>
      <parentid>33678346</parentid>
      <timestamp>2015-07-28T19:44:36Z</timestamp>
      <contributor>
        <username>-sche</username>
        <id>444485</id>
      </contributor>
      <minor />
      <comment>Reverted edits by [[Special:Contributions/Glory of Space|Glory of Space]]. If you think this rollback is in error, please leave a message on my talk page.</comment>
      <model>wikitext</model>
      <format>text/x-wiki</format>
      <text xml:space="preserve">CONTENT...
        CONTENT...       
      CONTENT</text>
      <sha1>pdgvb0p8s6p28xkxyz0holcmfurq77p</sha1>
    </revision>
  </page>
*/