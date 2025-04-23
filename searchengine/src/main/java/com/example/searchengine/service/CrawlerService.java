package com.example.searchengine.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.example.searchengine.database.DbManage;
import com.example.searchengine.model.PageInfo;

@Service
public class CrawlerService {
    private Queue<String> urlQueue = new ArrayDeque<String>(); // key track of the URLs to be visited (for BFS)
    private Set<String> visitedUrl = new HashSet<String>(); // // keep track of the visited URLs to prevent cyclic links
    private IndexerService indexer; // call indexer service
    private DbManage dbManage; // for access the jdbm tables
    private final PageRankService pageRankService;  // create page rank table after crawl
    

    public CrawlerService(DbManage dbManage, IndexerService indexer, PageRankService pageRankService) {
        this.dbManage = dbManage;
        this.indexer = indexer;
        this.pageRankService = pageRankService;
    }

    public void crawl(String startingUrl, int maxPages) throws IOException {
        // Clear previous crawl results
        visitedUrl.clear();
        urlQueue.clear();

        // Add starting URL to queue
        urlQueue.add(startingUrl);
        int pagesCrawled = 0;
        int docId = 0;

        while (!urlQueue.isEmpty() && pagesCrawled < maxPages) {
            String currentUrl = urlQueue.poll();
            
            // check if visited URL (prevent cyclic links)
            if(visitedUrl.contains(currentUrl)){ continue; }

            // extract the page header (last modified, title, size)
            System.out.println("Visiting: " + currentUrl);
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

            indexer.indexPage(currentUrl, pageInfo.getTitle(), docId, pageInfo); // pass to indexer (extract word & store into inverted file)
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
            pagesCrawled++;
        }
        pageRankService.computePageRank(); // compute page rank after crawling
        //dbManage.printPageRank(); // print page rank
        dbManage.commit(); // commit the changes to the database

    }

    // extract links in url and return them (for BFS)
    public List<String> extractLinks(String url) throws IOException {
        List<String> links = new ArrayList<>();
        int maxRetries = 3; // Maximum number of retries
        int retryCount = 0;
        boolean success = false;

        while (retryCount < maxRetries && !success) {
            try {
                Document doc = Jsoup.connect(url).get();
                Elements linkElements = doc.select("a[href]");
                for (Element linkElement : linkElements) {
                    String link = linkElement.attr("abs:href");
                    // Check if the link starts with "http" or "https"
                    if (link.startsWith("http://") || link.startsWith("https://")) {
                        links.add(link);
                    }
                }
                success = true; // Mark as successful if no exception occurs
            } catch (IOException e) {
                retryCount++;
                System.err.println("Failed to extract links from " + url + " (Attempt " + retryCount + " of " + maxRetries + "): " + e.getMessage());
                if (retryCount >= maxRetries) {
                    throw e; // Rethrow the exception after max retries
                }
            }
        }
        return links;
    }

    // get crawled page
    public List<PageInfo> getCrawledPages() throws IOException {
        return dbManage.getCrawledPages();
    }


    // delete the .db file
    public void cleanDatabase() throws IOException {
        Path dbPath = Paths.get("crawlerDb.db");
        if (Files.exists(dbPath)) {
            Files.delete(dbPath);
        }
        dbManage.recreateDatabase(); // Recreate the database after deletion
    }

    // check the if the .db file exists
    public Map<String, Object> checkDatabaseStatus() throws IOException {
        Path dbPath = Paths.get("crawlerDb.db");
        if (Files.exists(dbPath)) {
            return Map.of("status", "Database exists");
        } else {
            return Map.of("status", "Database does not exist");
        }
    }
}

