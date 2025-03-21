package main;

/**
 * Crawler.java
 * Main class for the web crawler
 * start to index pages, and create all the database tables
 */

import java.io.IOException;
import java.net.URL;
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

import org.htmlparser.beans.StringBean;
import org.htmlparser.beans.LinkBean;
import java.util.Vector;
import java.util.StringTokenizer;
import jdbm.htree.HTree;


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
                    dbManage.updateParentChildMap(links, currentUrl);

                } 
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

    // public String getTitle(int key){
    //     //dbManage.get
    // }

    // public String getLastModified(int id){
    //     return dbManage.getLastModified(id);
    // }

    // public Vector<String> extractWords(int id){
    //     String url = dbManage.getUrl(id);
    //     StringBean sb = new StringBean();
    //     sb.setURL(url);

    //     Vector<String> words = new Vector<String>();
	// 	String strings = sb.getStrings();
	// 	StringTokenizer st = new StringTokenizer(strings);

    //     while (st.hasMoreTokens()){
	// 		words.add(st.nextToken());
	// 	}

	// 	return (words);
    // }

    // public HTree wordFreq(int id) throws ParserException{
    //     Vector<String> words = extractWords(id);
    //     FastIterator iter = words;
    //     String k;
    //     HTree wf = HTree.createInstance(dbManage);
    //     while ((k = (String)iter.next()) != null){
    //         if (wf.get(k)){
    //             wf.put(k, wf.get(k)+1);
    //         }
    //         else{
    //             wf.put(k,1);
    //         }
    //     }
    //     return wf;
    // }
    
    // public Vector<String> extractLinks(int id) throws ParserException{
    //     String url = dbManage.getUrl(id);
    //     LinkBean lb = new LinkBean();
	//     lb.setURL(url);

	// 	Vector<String> links = new Vector<String>();
	// 	URL[] URL_array = lb.getLinks();
	//     for(int i=0; i<URL_array.length; i++){
	//     	System.out.println(URL_array[i]);
	// 		links.add(URL_array[i].toString());
	//     }
	//     return links;
    // }

    // public Set<String> getVisited(){
    //     return visitedUrl;
    // }

    // public Vector<String> getKeys(){
    //     return dbManage.keys();
    //     dbManage.getD
    // }
    
    public String getTitle(){
        //url/page id (pageMap)
        int pageid = dbManage.pageMap
        //get pageID -> page index
        //get page title
    }

    public String getURL(){
        //pageid -> url (pageidmap)
        //get page index
        //get url
    }

    public Date getLastModification(){
        //url -> pageid (pageMap)
        //get page id -> page index
        //get last modification date
    }

    public Date getSize(){
        //url -> pageid (pageMap)
        //get page id -> page index
        //get size
    }

    public Map<Integer,Integer> getKeywords(){
        //url -> pageid (pageMap)
        //get page id -> page index
        //get body word list
        //wordid -> words
        //output as HTree?
    }

    public List<String> getChildren(){
        //url -> pageid (pageMap)
        //get parent page id -> page Index
        //get list of child page IDs
        //map to page urls via pageid index
    }
}
