package main;

import java.io.IOException;

import org.htmlparser.util.ParserException;

import main.database.DbManage;

/*
 * Main class: Run the crawler
 */

public class Main {

    public static void main(String[] args) {
        try {
            // run the crawler and create all the database tables
            Crawler crawler = new Crawler(30);
            crawler.crawl("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm");


            // Access database tables (after create .db file in crawl())
            // 1. create DbMange class instance || 2.directly access with recordManager

            // DbManage dbManage = new DbManage("crawlerDB");
            // dbManage.printPageInfo(); // a sample func to print the page infomation

            // RecordManager recMan = RecordManagerFactory.createRecordManager("CrawlerDB");
            // long recId = recMan.getNamedObject("pageMap");
            // HTree pageMap = HTree.load(recMan, recId);
            // System.out.println(pageMap.get("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"));



        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        
    }
    
}
