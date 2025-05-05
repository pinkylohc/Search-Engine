package com.example.searchengine.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
//import java.util.HashSet;
import java.util.List;
//import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
//import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.example.searchengine.database.DbManage;
import com.example.searchengine.model.PageInfo;

@Service
public class CrawlerService implements DisposableBean {
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>(); // thread-safe queue
    private final Set<String> visitedUrl = ConcurrentHashMap.newKeySet(); // thread-safe set
    private final IndexerService indexer;
    private final DbManage dbManage;
    private final PageRankService pageRankService;
    private final ExecutorService executorService;
    private final AtomicInteger pagesCrawled = new AtomicInteger(0);
    private final ReentrantLock dbLock = new ReentrantLock();
    private volatile boolean isCrawling = false;

    public CrawlerService(DbManage dbManage, IndexerService indexer, PageRankService pageRankService) {
        this.dbManage = dbManage;
        this.indexer = indexer;
        this.pageRankService = pageRankService;
        // Create a thread pool with number of threads equal to available processors
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void crawl(String startingUrl, int maxPages) throws IOException {
        if (isCrawling) {
            throw new IllegalStateException("Crawling is already in progress");
        }

        // Clear previous crawl results
        visitedUrl.clear();
        urlQueue.clear();
        pagesCrawled.set(0);
        isCrawling = true;

        // Add starting URL to queue
        urlQueue.add(startingUrl);
        List<Future<?>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isProcessing = new AtomicBoolean(true);
        AtomicInteger activeThreads = new AtomicInteger(0);

        // Start a thread to monitor the crawling progress
        executorService.submit(() -> {
            try {
                while (isProcessing.get() && pagesCrawled.get() < maxPages) {
                    String currentUrl = urlQueue.poll(1, TimeUnit.SECONDS);
                    if (currentUrl == null) {
                        // Only break if we have no active threads and no URLs in queue
                        if (activeThreads.get() == 0 && urlQueue.isEmpty()) {
                            break;
                        }
                        continue;
                    }

                    if (!visitedUrl.contains(currentUrl)) {
                        activeThreads.incrementAndGet();
                        Future<?> future = executorService.submit(() -> {
                            try {
                                processUrl(currentUrl, maxPages);
                            } catch (Exception e) {
                                System.err.println("Error processing URL " + currentUrl + ": " + e.getMessage());
                            } finally {
                                activeThreads.decrementAndGet();
                            }
                        });
                        futures.add(future);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                isProcessing.set(false);
                latch.countDown();
            }
        });

        try {
            // Wait for the crawling to complete or timeout after 1 minute
            if (!latch.await(1, TimeUnit.MINUTES)) {
                System.out.println("Crawling timed out after 1 minute");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Wait for all submitted tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Error waiting for crawl task: " + e.getMessage());
            }
        }

        // Final check to ensure we've processed all URLs
        while (!urlQueue.isEmpty() && pagesCrawled.get() < maxPages) {
            String currentUrl = urlQueue.poll();
            if (currentUrl != null && !visitedUrl.contains(currentUrl)) {
                try {
                    processUrl(currentUrl, maxPages);
                } catch (Exception e) {
                    System.err.println("Error in final URL processing: " + e.getMessage());
                }
            }
        }

        System.out.println("Final pages crawled: " + pagesCrawled.get());
        
        // Final database commit and verification
        try {
            dbManage.commit();
            System.out.println("Final database commit completed");
            
            // Verify the number of pages in the database
            int pagesInDb = dbManage.getPageCount();
            System.out.println("Pages in database: " + pagesInDb);
            
            /*if (pagesInDb != pagesCrawled.get()) {
                System.err.println("Warning: Discrepancy between crawled pages (" + pagesCrawled.get() + 
                                 ") and pages in database (" + pagesInDb + ")");
            }*/
        } catch (Exception e) {
            System.err.println("Error during final database commit: " + e.getMessage());
        }
        
        pageRankService.computePageRank();
        isCrawling = false;
    }

    private void processUrl(String currentUrl, int maxPages) throws IOException {
        // First check if URL is already visited or max pages reached
        if (visitedUrl.contains(currentUrl)) {
            return;
        }

        // Check max pages limit before processing
        if (pagesCrawled.get() >= maxPages) {
            return;
        }

        System.out.println("Visiting: " + currentUrl + " (Pages crawled: " + pagesCrawled.get() + ")");
        PageInfo pageInfo = new PageInfo(currentUrl);
        try {
            pageInfo.extractInfo();
        } catch (IOException e) {
            System.err.println("Failed to extract page info from " + currentUrl + ": " + e.getMessage());
            visitedUrl.add(currentUrl);
            return;
        }

        // Use a single lock for all database operations to ensure atomicity
        dbLock.lock();
        try {
            // Check again if URL is visited (in case another thread processed it)
            if (visitedUrl.contains(currentUrl)) {
                return;
            }

            // Check max pages again (in case another thread reached the limit)
            if (pagesCrawled.get() >= maxPages) {
                return;
            }

            int docId;
            if (dbManage.containsIndexedUrl(currentUrl)) {
                docId = dbManage.getPageId(currentUrl);
                Date storedLastModified = dbManage.getLastModified(docId);
                Date getLastModified = pageInfo.getLastModified();
                if (storedLastModified != null && getLastModified != null && !getLastModified.after(storedLastModified)) {
                    visitedUrl.add(currentUrl);
                    return;
                }
            } else {
                docId = dbManage.addPage(currentUrl);
            }

            // Ensure docId is valid
            if (docId < 0) {
                System.err.println("Failed to get valid docId for URL: " + currentUrl);
                return;
            }

            // Index the page and update database
            try {
                indexer.indexPage(currentUrl, pageInfo.getTitle(), docId, pageInfo);
                dbManage.addPageIndex(docId, pageInfo);
                
                // Verify the page was added to the database
                if (!dbManage.containsIndexedUrl(currentUrl)) {
                    System.err.println("Warning: Page was not properly added to database: " + currentUrl);
                    return;
                }

                // Extract and process links
                List<String> links = extractLinks(currentUrl);
                for (String link : links) {
                    if (!visitedUrl.contains(link) && pagesCrawled.get() < maxPages) {
                        urlQueue.offer(link);
                    }
                }
                dbManage.updateParentChildMap(links, currentUrl);

                // Only increment counter and mark as visited after successful database operations
                visitedUrl.add(currentUrl);
                pagesCrawled.incrementAndGet();

                // Periodically commit changes
                if (pagesCrawled.get() % 10 == 0) {
                    dbManage.commit();
                }
            } catch (Exception e) {
                System.err.println("Error during page processing for " + currentUrl + ": " + e.getMessage());
                return;
            }
        } finally {
            dbLock.unlock();
        }
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

    @Override
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}

