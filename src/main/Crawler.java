package main;

/**
 * Crawler.java
 * Main class for the web crawler
 * start to index pages, and create all the database tables
 */

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.htmlparser.util.ParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.database.DbManage;
import main.database.PageInfo;
import main.utils.Indexer;


public class Crawler {
    private int numIndexPage; // number of pages to be indexed (30 for phase 1, 300 for phase 2)

    // keep track of the visited URLs to prevent cyclic links
    private Set<String> visitedUrl = new HashSet<String>();

    // key track of the URLs to be visited (from BFS)
    private Queue<String> urlQueue = new ArrayDeque<String>();

    private DbManage dbManage; // for access the jdbm tables
    private Indexer indexer; // for extract and index the page content

    // Constructor
    Crawler(int numIndexPage) throws IOException { 
        this.numIndexPage = numIndexPage;
        this.dbManage = new DbManage("crawlerDB");
        this.indexer = new Indexer();
    }


    // extract links in url and return them (for BFS)
    public List<String> extractLinks(String url) throws IOException {
        List<String> links = new ArrayList<>();
        Document doc = Jsoup.connect(url).get();
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement : linkElements) {
            String link = linkElement.attr("abs:href");
            // Check if the link starts with "http" or "https"
            if (link.startsWith("http://") || link.startsWith("https://")) {
                links.add(link);
        }
        }
        return links; 
    }

    // Crawler function - index the web pages from startingUrl
    public void crawl(String startingUrl) throws IOException, ParserException{
        urlQueue.add(startingUrl);
        int docId = 0;

        while (!urlQueue.isEmpty() && numIndexPage > 0) {
            String currentUrl = urlQueue.poll();
            
            // check if visited URL (prevent cyclic links)
            if(visitedUrl.contains(currentUrl)){ continue; }

            // extract the page header (last modified, title, size)
            // System.out.println("Visiting: " + currentUrl);
            PageInfo pageInfo = new PageInfo(currentUrl);
            try {
                pageInfo.extractInfo();
            } catch (IOException e) {
                System.err.println("Failed to extract page info from " + currentUrl + ": " + e.getMessage());
                visitedUrl.add(currentUrl);
                continue;
            }  

            // check if the URL is already in the inverted file
            if(dbManage.containsIndexedUrl(currentUrl)) { 
                docId = dbManage.getPageId(currentUrl);
                Date storedLastModified = dbManage.getLastModified(docId); // get the stored date
                Date getLastModified = pageInfo.getLastModified(); // get the current date
                if (storedLastModified != null && getLastModified != null && !getLastModified.after(storedLastModified)) {
                    continue;  // skip for repeated page
                }

            } else { 
                docId = dbManage.addPage(currentUrl);  // create page record
            } 

            indexer.indexPage(currentUrl, pageInfo.getTitle(), dbManage, docId, pageInfo); // pass to indexer (extract word & store into inverted file)
            dbManage.addPageIndex(docId, pageInfo); // update db PageIndex

            // extract links from the current page and add them to the queue
            try{
                List<String> links = extractLinks(currentUrl);  
                for (String link : links) { // add newly extracted links to URLsToVisit
                    urlQueue.add(link);
                } 
                dbManage.updateParentChildMap(links, currentUrl);
            } catch (IOException e) {
                System.err.println("Failed to extract links from " + currentUrl + ": " + e.getMessage());
                continue;
            }
            visitedUrl.add(currentUrl);
            numIndexPage--;
        }


        // stop jdbm access
        //dbManage.printPageMap();
        //dbManage.printwordMap();
        //dbManage.printwordidmap();
        //dbManage.printBodyIndex(); 
        //dbManage.printTitleIndex();
        //dbManage.printPageIndex();
        dbManage.finalise();

    }


    
}
