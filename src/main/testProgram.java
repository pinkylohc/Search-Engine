package main;

import java.awt.DefaultKeyboardFocusManager;
import java.awt.image.IndexColorModel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.stream.events.EntityReference;

import jdbm.helper.FastIterator;

import org.htmlparser.util.ParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.database.DbManage;
import main.database.PageInfo;
import main.utils.Indexer;
import jdbm.htree.HTree;
import java.util.Map;

public class testProgram {
    String url = null; // Initialize to null, or a default URL
    int numPages = 30;
    Crawler webCrawler;
    DbManage dbManage;

    public testProgram() { // Constructor to initialize webCrawler and dbManage
        try {
            webCrawler = new Crawler(numPages);
            dbManage = new DbManage("crawlerDB");
        } catch (IOException e) {
            System.err.println("Error creating Crawler and dbManage: " + e.getMessage());
            // Handle the error appropriately - maybe exit the program,
            // or set webCrawler and dbManage to null and handle it later.
            webCrawler = null;
            dbManage = null;
        }
    }

    public static void main(String[] args) {  // Added main method
        testProgram program = new testProgram();

        //Check if crawler and dbManage were initialized successfully
        if (program.webCrawler == null || program.dbManage == null){
            System.err.println("Crawler or DbManage failed to initialize. Exiting.");
            return;
        }
  

        try{
            FileWriter fileOut = new FileWriter("spiderResult.txt");
            fileOut.write("");
            program.webCrawler.crawl(program.url);
        }catch (IOException e){
            System.err.println("Error during crawling: " + e.getMessage());
            return;
        }

            // FastIterator iter = program.dbManage.get(pageIndex).keys();
            FastIterator iter = program.dbManage.loadOrCreateHTree("pageIndex").keys();
            Integer pageId;
            while ((pageId = (Integer) iter.next()) != null) {

                // PageInfo pageInfo = (PageInfo) program.dbManage.pageIndex.get(pageId);
                PageInfo pageInfo = (PageInfo) program.dbManage.loadOrCreateHTree("pageIndex").get(pageId);

                String title = pageInfo.getTitle();
                String url2 = pageInfo.getUrl();
                Date lastModified = pageInfo.getLastModified();
                int size = pageInfo.getSize();

                fileOut.append(title + '\n');
                fileOut.append(url2 + '\n'); //Added newline

                fileOut.append(lastModified + ", ");
                fileOut.append(String.valueOf(size) + '\n');

                if (pageInfo.bodyWordList != null){
                    StringBuilder bodyKeywordFreqBuilder = new StringBuilder();
                    for (Map.Entry<Integer, Integer>entry: pageInfo.bodyWordList.entrySet()){
                        int wordId = entry.getKey();
                        int freq = entry.getValue();
                        // String word = (String) program.dbManage.wordidMap.get(wordId);
                        String word = (String) program.dbManage.loadOrCreateHTree("wordidMap").get(wordId);
                        if (word != null){
                            bodyKeywordFreqBuilder.append(word).append(" ").append(freq).append("; ");
                        }
                    }
                    if (bodyKeywordFreqBuilder.length() >0){
                        bodyKeywordFreqBuilder.setLength(bodyKeywordFreqBuilder.length() - 2);
                    }
                    fileOut.append(bodyKeywordFreqBuilder.toString() + '\n'); //Added newline
                }

                // List<Integer> childPageIds = (List<Integer>) program.dbManage.parentChildMap.get(pageId);
                List<Integer> childPageIds = (List<Integer>) program.dbManage.loadOrCreateHTree("parentChildMap").get(pageId);

                if (childPageIds != null) {
                    for (int childPageId : childPageIds) {
                        // String childUrl = (String) program.dbManage.pageidMap.get(childPageId);
                        String childUrl = (String) program.dbManage.loadOrCreateHTree(("pageidMap")).get(childPageId);
                        if (childUrl != null) {
                            fileOut.append(childUrl + '\n');
                        }
                    }
                }

                fileOut.append("-----------------\n"); // Added newline for better formatting
            }

            fileOut.close();

        // } catch (IOException e) {
        //     System.err.println("An error occurred: " + e.getMessage()); // Handle exceptions properly
        //     e.printStackTrace();
        }
    }
/* Page title
URL
Last modification date, size of page
Keyword1 freq1; Keyword2 freq2; Keyword3 freq3; ... ...
Child Link1
Child Link2 ... ...
——————————————– (The separator line should be a line of hyphens, i.e. -)*/