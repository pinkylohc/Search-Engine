package com.example.searchengine.database;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.searchengine.model.PageInfo;
import com.example.searchengine.model.PageResult;
import com.example.searchengine.model.Posting;
import com.example.searchengine.service.RecordManagerService;


import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

/**
 * DbManage.java
 * Manage all the jdbm database operations
 * create, access, and update the jdbm tables
 */

@Service
public class DbManage {
    private final RecordManagerService recordManagerService;


    private HTree pageMap; // mapping table: URL -> page id
    private HTree pageidMap; // mapping table: page id -> URL
    int pageidCounter = 0; // keep track of the page id

    private HTree pageIndex; // page ID -> page info

    private HTree parentChildMap; // mapping table: parent page id -> list of child page id
    private HTree childParentMap; // mapping table: child page id -> parent page id

    private HTree wordMap; // mapping table: word -> word id
    private HTree wordidMap; // mapping table: word id -> word
    int wordidCounter = 0; // keep track of the word id

    private HTree bodyIndex; // word ID -> list of posting
    private HTree titleIndex; // word ID -> list of posting

    private HTree pageRank; // page id -> page rank score

    private HTree searchCache; // For storing cached search results
    private HTree cacheMetadata; // For storing cache metadata (access times, frequencies)
    private final int CACHE_SIZE = 10; // Maximum number of cached searches


    public DbManage(RecordManagerService recordManagerService) throws IOException {
        this.recordManagerService = recordManagerService;
        initializeTables();
    }
    
    // Initialize or recreate the tables
    private void initializeTables() throws IOException {
       // Initialize or load the tables
       pageMap = recordManagerService.getOrCreateHTree("pageMap"); // String -> Integer
       pageidMap = recordManagerService.getOrCreateHTree("pageidMap"); // Integer -> String
       pageidCounter = getTableSize(pageMap);

       pageIndex = recordManagerService.getOrCreateHTree("pageIndex"); // int -> pageInfo
       parentChildMap = recordManagerService.getOrCreateHTree("parentChildMap"); // int -> List<Integer>
       childParentMap = recordManagerService.getOrCreateHTree("childParentMap"); // int -> List<Integer>

       wordMap = recordManagerService.getOrCreateHTree("wordMap"); // String -> Integer
       wordidMap = recordManagerService.getOrCreateHTree("wordidMap"); // Integer -> String

       bodyIndex = recordManagerService.getOrCreateHTree("bodyIndex"); // word ID -> list of posting
       titleIndex = recordManagerService.getOrCreateHTree("titleIndex"); // word ID -> list of posting
       pageRank = recordManagerService.getOrCreateHTree("pageRank"); // page ID -> page rank score

       searchCache = recordManagerService.getOrCreateHTree("searchCache");
       cacheMetadata = recordManagerService.getOrCreateHTree("cacheMetadata");
    }

    // Cache entry class (for cache data structure)
    private static class CacheMetadata implements Serializable {
        private static final long serialVersionUID = 20L; // Add a unique ID for serialization
        long lastAccessed;
        int frequency;
        
        
        public CacheMetadata(long lastAccessed, int frequency) {
            this.lastAccessed = lastAccessed;
            this.frequency = frequency;
        }
    }

    private int getTableSize(HTree hashTable) throws IOException { // get the id counter (for mapping table)
        FastIterator iter = hashTable.keys();
        int size = 0;
        while (iter.next() != null) {
            size++;
        }
        return size;
    }

    // Recreate the database after deletion
    public void recreateDatabase() throws IOException {
        recordManagerService.reinitializeRecordManager(); // Reinitialize the RecordManager
        initializeTables(); // Reinitialize the tables
    }

    public void commit() throws IOException {
        recordManagerService.commit();
    }

    public void close() throws IOException {
        recordManagerService.close();
    }

    /****************** Cache Operation ***********************/
    @SuppressWarnings("unchecked")
    public List<PageResult> getCachedResults(String query) throws IOException {
        // Check if query is in cache
        List<PageResult> results = (List<PageResult>) searchCache.get(query);
        if (results != null) {
            // Update metadata - increment frequency and update last accessed time
            CacheMetadata metadata = (CacheMetadata) cacheMetadata.get(query);
            if (metadata == null) {
                metadata = new CacheMetadata(System.currentTimeMillis(), 1);
            } else {
                metadata = new CacheMetadata(System.currentTimeMillis(), metadata.frequency + 1);
            }
            cacheMetadata.put(query, metadata);
            return results;
        }
        return null;
    }

    public void putCachedResults(String query, List<PageResult> results){
        // First check if we need to evict
        try{
            System.out.println("Cache Size: " + getTableSize(searchCache));
            if (getTableSize(searchCache) >= CACHE_SIZE) {
                evictLeastUsed();
            }
            
            // Store the results
            searchCache.put(query, results);
            // Store/update metadata
            CacheMetadata metadata = (CacheMetadata) cacheMetadata.get(query);
            if (metadata == null) {
                metadata = new CacheMetadata(System.currentTimeMillis(), 1);
            } else {
                metadata = new CacheMetadata(System.currentTimeMillis(), metadata.frequency + 1);
            }
            cacheMetadata.put(query, metadata);
            commit();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void evictLeastUsed() throws IOException {
        FastIterator iter = cacheMetadata.keys();
        String leastUsedKey = null;
        long oldestAccess = Long.MAX_VALUE;
        int lowestFrequency = Integer.MAX_VALUE;
        String key;
        
        // Find the least recently used AND least frequently used entry
        while ((key = (String) iter.next()) != null) {
            CacheMetadata metadata = (CacheMetadata) cacheMetadata.get(key);
            // First check frequency, then recency
            if (metadata.frequency < lowestFrequency || 
                (metadata.frequency == lowestFrequency && metadata.lastAccessed < oldestAccess)) {
                leastUsedKey = key;
                oldestAccess = metadata.lastAccessed;
                lowestFrequency = metadata.frequency;
            }
        }
        
        if (leastUsedKey != null) {
            searchCache.remove(leastUsedKey);
            cacheMetadata.remove(leastUsedKey);
            commit();
        }
    }

    // clear cache table records
    public void clearCache() throws IOException {
        // First collect all keys
        List<String> cacheKeys = new ArrayList<>();
        FastIterator iter = searchCache.keys();
        String key;
        while ((key = (String) iter.next()) != null) {
            cacheKeys.add(key);
        }
        
        // Then remove them
        for (String cacheKey : cacheKeys) {
            searchCache.remove(cacheKey);
        }
        
        // Same for metadata
        List<String> metadataKeys = new ArrayList<>();
        FastIterator iter2 = cacheMetadata.keys();
        String key2;
        while ((key2 = (String) iter2.next()) != null) {
            metadataKeys.add(key2);
        }
        
        for (String metadataKey : metadataKeys) {
            cacheMetadata.remove(metadataKey);
        }
        
        commit();
    }

    // For debugging
    public void printCache() throws IOException {
        System.out.println("Search Cache Contents:");
        FastIterator iter = searchCache.keys();
        String key;
        while ((key = (String) iter.next()) != null) {
            CacheMetadata metadata = (CacheMetadata) cacheMetadata.get(key);
            System.out.printf("Query: %s, Last Accessed: %d, Frequency: %d%n",
                key, metadata.lastAccessed, metadata.frequency);
        }
    }


    // Get top N queries from the cache as hot topic
    public List<Map.Entry<String, Integer>> getTopQueries(int limit) throws IOException {
        List<Map.Entry<String, Integer>> topQueries = new ArrayList<>();
        FastIterator iter = cacheMetadata.keys();
        String key;
        
        while ((key = (String) iter.next()) != null) {
            CacheMetadata metadata = (CacheMetadata) cacheMetadata.get(key);
            if (metadata != null) {
                topQueries.add(new AbstractMap.SimpleEntry<>(key, metadata.frequency));
            }
        }
        
        // Sort by frequency in descending order, then by lastAccessed in descending order
        topQueries.sort((a, b) -> {
            int freqCompare = b.getValue().compareTo(a.getValue());
            if (freqCompare != 0) {
                return freqCompare;
            }
            // If frequencies are equal, compare by lastAccessed
            try {
                CacheMetadata metadataA = (CacheMetadata) cacheMetadata.get(a.getKey());
                CacheMetadata metadataB = (CacheMetadata) cacheMetadata.get(b.getKey());
                return Long.compare(metadataB.lastAccessed, metadataA.lastAccessed);
            } catch (IOException e) {
                // If there's an error accessing metadata, fall back to frequency comparison
                return freqCompare;
            }
        });
        
        // Return top N results
        return topQueries.stream().limit(limit).collect(Collectors.toList());
    }


    /****************** pageRank Operation ***********************/
    public HTree getPageRankMap() {
        return pageRank;
    }

    /************* pageMap && pageidMap Operation ****************/
    // Crawler to check if the url is already in the inverted file
    public boolean containsUrl(String url) throws IOException {
        return pageMap.get(url) != null;
    }

    public boolean containsIndexedUrl(String url) throws IOException {
        Integer pageId = (Integer) pageMap.get(url);
        return pageId != null && pageIndex.get(pageId) != null;
    }

    public int getPageId(String url) throws IOException {
        return (int) pageMap.get(url);
    }

    public String getUrl(int pageId) throws IOException {
        return (String) pageidMap.get(pageId);
    }

    public int addPage (String url) throws IOException{ // new URL -> add to pageMap, pageIndex
        if(containsUrl(url)) return getPageId(url);
        pageMap.put(url, pageidCounter);
        pageidMap.put(pageidCounter, url);
        pageidCounter++;
        return (pageidCounter-1);  
    }


    /************* pageIndex Operation ****************/
    public void addPageIndex(int pageId, PageInfo pageInfo) throws IOException {
        pageIndex.put(pageId, pageInfo);
    }

    public Date getLastModified(int pageId) throws IOException {
        PageInfo pageInfo = (PageInfo) pageIndex.get(pageId);
        return pageInfo.getLastModified();
    }

    public List<PageInfo> getCrawledPages() throws IOException { // for crawler page display
        List<PageInfo> crawledPages = new ArrayList<>();
        FastIterator iter = pageIndex.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            crawledPages.add((PageInfo) pageIndex.get(key));
        }
        return crawledPages;
    }

    public List<Map.Entry<Integer, PageInfo>> getCrawledPagesWithIds() throws IOException { // for search func
        List<Map.Entry<Integer, PageInfo>> crawledPages = new ArrayList<>();
        FastIterator iter = pageIndex.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            crawledPages.add(new AbstractMap.SimpleEntry<>(key, (PageInfo) pageIndex.get(key)));
        }
        return crawledPages;
    }

    public int getPageCount() throws IOException {
        return pageidCounter;
    }

    public PageInfo getPageInfo(int pageId) throws IOException {
        return (PageInfo) pageIndex.get(pageId);
    }

    public HTree getPageIndex() {
        return pageIndex;
    }

    public Map<Integer, Integer> getBodyWordList(int docId) throws IOException {
        try{
            PageInfo bodylist = (PageInfo) pageIndex.get(docId);
            if (bodylist == null) { return null;}  
            return bodylist.getBodyWordList();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
        
    }

    /************* wordMap & wordidMap Operation ****************/
    public boolean containsWord(String word) throws IOException {
        return wordMap.get(word) != null;
    }

    public HTree getWordidMap() throws IOException {
        return wordidMap;
    }
    
    public String getWord(int wordId) throws IOException {
        Object result = wordidMap.get(wordId);
        return result != null ? result.toString() : null;
    }

    public int getWordId(String word) throws IOException {
        Object result = wordMap.get(word);
        return result != null ? (int) result : -1;
    }

    public int addWord(String word) throws IOException {
        wordMap.put(word, wordidCounter);
        wordidMap.put(wordidCounter, word);
        wordidCounter++;
        return (wordidCounter-1);
    }

    /************* bodyIndex, titleIndex Operation ****************/
    // Htree - <int, List<posting>>

    @SuppressWarnings("unchecked")
    public void updateBodyIndex(Map<Integer, Posting> batchIndex) throws IOException {
        // loop through the batch inverted index table from Indexer
        for (Map.Entry<Integer, Posting> entry : batchIndex.entrySet()) { 
            int wordId = entry.getKey();
            Posting posting = entry.getValue();

            List<Posting> postings = (List<Posting>) bodyIndex.get(wordId);
            if (postings == null) {
                postings = new ArrayList<>(); 
            }
            postings.add(posting);
            bodyIndex.put(wordId, postings);

        }
    }

    @SuppressWarnings("unchecked")
    public void updateTitleIndex(Map<Integer, Posting> batchIndex) throws IOException {
        // loop through the batch inverted index table from Indexer
        for (Map.Entry<Integer, Posting> entry : batchIndex.entrySet()) { 
            int wordId = entry.getKey();
            Posting posting = entry.getValue();

            List<Posting> postings = (List<Posting>) titleIndex.get(wordId);
            if (postings == null) {
                postings = new ArrayList<>(); 
            }
            postings.add(posting);
            titleIndex.put(wordId, postings);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Posting> getBodyPosting(int wordId) throws IOException {
        return (List<Posting>) bodyIndex.get(wordId);
    }

    @SuppressWarnings("unchecked")
    public List<Posting> getTitlePosting(int wordId) throws IOException {
        return (List<Posting>) titleIndex.get(wordId);
    }


    /************* parentChildMap && childParentMap Operation ****************/
        //parentChildMap: parent page id -> list of child page id
        //childParentMap: child page id -> parent page id
        

    @SuppressWarnings("unchecked")
    public List<String> getChildLinks(int pageId) throws IOException {
        List<String> childUrls = new ArrayList<>();
        List<Integer> childPageIds = (List<Integer>) parentChildMap.get(pageId);
        
        if (childPageIds != null) {
            for (Integer childId : childPageIds) {
                String url = (String) pageidMap.get(childId);
                if (url != null) {
                    childUrls.add(url);
                }
            }
        }
        return childUrls;
    }

    @SuppressWarnings("unchecked")
    public List<String> getParentLinks(int pageId) throws IOException {
        List<String> parentUrls = new ArrayList<>();
        List<Integer> parentPageIds = (List<Integer>) childParentMap.get(pageId);
        
        if (parentPageIds != null) {
            for (Integer parentId : parentPageIds) {
                String url = (String) pageidMap.get(parentId);
                if (url != null) {
                    parentUrls.add(url);
                }
            }
        }
        return parentUrls;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCrawledParentPageIds(int pageId) throws IOException {
        List<Integer> parentPageIds = (List<Integer>) childParentMap.get(pageId);
        if (parentPageIds == null) return new ArrayList<>();
        
        List<Integer> result = new ArrayList<>();
        for (Integer parentId : parentPageIds) {
            try {
                if (pageIndex.get(parentId) != null) {
                    result.add(parentId);
                }
            } catch (IOException e) {
                // Log error and continue with other parents
                System.err.println("Error checking parent page: " + parentId);
                e.printStackTrace();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCrawledChildPageIds(int pageId) throws IOException {
        List<Integer> childPageIds = (List<Integer>) parentChildMap.get(pageId);
        if (childPageIds == null) return new ArrayList<>();
        
        List<Integer> result = new ArrayList<>();
        for (Integer childId : childPageIds) {
            try {
                if (pageIndex.get(childId) != null) {
                    result.add(childId);
                }
            } catch (IOException e) {
                // Log error and continue with other children
                System.err.println("Error checking child page: " + childId);
                e.printStackTrace();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void updateParentChildMap(List<String> links, String url) throws IOException {
        Integer parentPageId = this.getPageId(url);
        
        // Initialize the parent's list of child page IDs if it doesn't exist
        List<Integer> childPageIds = (List<Integer>) parentChildMap.get(parentPageId);
        if (childPageIds == null) {
            childPageIds = new ArrayList<>();
        }

        // Process each child URL
        for (String childUrl : links) {
            int childPageId;
            if (containsUrl(childUrl)) { // If the child URL is already in the pageMap
                childPageId = getPageId(childUrl);
            } else {
                childPageId = addPage(childUrl); // Add the child URL to the pageMap
            }
            // Add the child page ID to the parent's list of children
            if (!childPageIds.contains(childPageId)) {
                childPageIds.add(childPageId);
            }

            // Update the childParentMap to add the parent page ID to the child's list of parents (child -> parent)
            List<Integer> parentPageIds = (List<Integer>) childParentMap.get(childPageId);
            if (parentPageIds == null) {
                parentPageIds = new ArrayList<>();
            }
            if (!parentPageIds.contains(parentPageId)) {
                parentPageIds.add(parentPageId);
            }
            childParentMap.put(childPageId, parentPageIds);
        }

        // Update the parentChildMap with the modified list of child page IDs
        parentChildMap.put(parentPageId, childPageIds);
    }


/////////////////////////////////////////////////////////////////////////////////////////////
/// The following functions are for the debug purpose only

    public void printPageMap() throws IOException { // print for debug
        System.out.println("PageMap:");
        FastIterator iter = pageMap.keys();
        String key;
        while( (key = (String)iter.next()) != null) {
            System.out.println("URL: " + key + " PageID: " + pageMap.get(key));
        }
    }

    public void printwordMap() throws IOException { // print for debug
        System.out.println("WordMap:");
        FastIterator iter = wordMap.keys();
        String key;
        while( (key = (String)iter.next()) != null) {
            System.out.println("Word: " + key + " WordID: " + wordMap.get(key));
        }
    }

    public void printwordidmap() throws IOException { // print for debug
        System.out.println("WordidMap:");
        FastIterator iter = wordidMap.keys();
        Integer key;
        while( (key = (Integer)iter.next()) != null) {
            System.out.println("WordID: " + key + " Word: " + wordidMap.get(key));
        }
    }

    @SuppressWarnings("unchecked")
    public void printBodyIndex() throws IOException { // print for debug
        System.out.println("BodyIndex:");
        FastIterator iter = bodyIndex.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            System.out.println("WordID: " + key);
            List<Posting> postings = (List<Posting>) bodyIndex.get(key);
            for (Posting posting : postings) {
                System.out.println("  DocID: " + posting.getId() + ", Freq: " + posting.getFreq() + ", Positions: " + posting.getPositions());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void printTitleIndex() throws IOException { // print for debug
        System.out.println("TitleIndex:");
        FastIterator iter = titleIndex.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            System.out.println("WordID: " + key);
            List<Posting> postings = (List<Posting>) titleIndex.get(key);
            for (Posting posting : postings) {
                System.out.println("  DocID: " + posting.getId() + ", Freq: " + posting.getFreq() + ", Positions: " + posting.getPositions());
            }
        }
    }

    public void printPageIndex() throws IOException {
        System.out.println("PageIndex:");
        FastIterator iter = pageIndex.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            System.out.println("DocID: " + key);
            PageInfo pageInfo = (PageInfo) pageIndex.get(key);
            pageInfo.printPageInfo();
        }
    }

    public void printPageRank() throws IOException {
        System.out.println("PageRank:");
        FastIterator iter = pageRank.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            System.out.println("DocID: " + key + ", PageRank: " + pageRank.get(key));
        }
    }

    

    @SuppressWarnings("unchecked")
    public void printPageInfo() throws IOException {
        try {
            FastIterator iter = pageIndex.keys();
            Integer pageId;
            while ((pageId = (Integer) iter.next()) != null) {
                PageInfo pageInfo = (PageInfo) pageIndex.get(pageId);
                String url = pageInfo.getUrl();
                String title = pageInfo.getTitle();
                Date lastModified = pageInfo.getLastModified();
                int size = pageInfo.getSize();

                // Print page title
                System.out.println("Title: " + title);

                // Print URL
                System.out.println("URL: " + url);

                // Print last modification date and size of page
                System.out.println("Last Modified: " + (lastModified != null ? lastModified.toString() : "N/A") + ", Size: " + size + " bytes");

                // Print keywords and their frequencies from body
                if (pageInfo.bodyWordList != null) {
                    System.out.println("Body Keywords:");
                    StringBuilder bodyKeywordFreqBuilder = new StringBuilder();
                    for (Map.Entry<Integer, Integer> entry : pageInfo.bodyWordList.entrySet()) {
                        int wordId = entry.getKey();
                        int freq = entry.getValue();
                        String word = (String) wordidMap.get(wordId);
                        if (word != null) {
                            bodyKeywordFreqBuilder.append(word).append(" ").append(freq).append("; ");
                        }
                    }
                    if (bodyKeywordFreqBuilder.length() > 0) {
                        bodyKeywordFreqBuilder.setLength(bodyKeywordFreqBuilder.length() - 2); // Remove the last "; "
                    }
                    System.out.println(bodyKeywordFreqBuilder.toString());
                }

                // Print words from title index
                if (pageInfo.titleWordList != null) {
                    System.out.println("Title Keywords:");
                    StringBuilder titleKeywordFreqBuilder = new StringBuilder();
                    for (Map.Entry<Integer, Integer> entry : pageInfo.titleWordList.entrySet()) {
                        int wordId = entry.getKey();
                        int freq = entry.getValue();
                        String word = (String) wordidMap.get(wordId);
                        if (word != null) {
                            titleKeywordFreqBuilder.append(word).append(" ").append(freq).append("; ");
                        }
                    }
                    if (titleKeywordFreqBuilder.length() > 0) {
                        titleKeywordFreqBuilder.setLength(titleKeywordFreqBuilder.length() - 2); // Remove the last "; "
                    }
                    System.out.println(titleKeywordFreqBuilder.toString());
                }

                // Print child links
                System.out.println("Child Links:");
                List<Integer> childPageIds = (List<Integer>) parentChildMap.get(pageId);
                if (childPageIds != null) {
                    for (int childPageId : childPageIds) {
                        String childUrl = (String) pageidMap.get(childPageId);
                        if (childUrl != null) {
                            System.out.println(childUrl);
                        }
                    }
                }

                // Print parent links
                System.out.println("Parent Links:");
                List<Integer> parentPageIds = (List<Integer>) childParentMap.get(pageId);
                if (parentPageIds != null) {
                    for (int parentPageId : parentPageIds) {
                        String parentUrl = (String) pageidMap.get(parentPageId);
                        if (parentUrl != null) {
                            System.out.println(parentUrl);
                        }
                    }
                }

                // Print separator line
                System.out.println("——————————————–");
            }
        } catch (IOException e) {
            System.err.println("Error printing page info: " + e.getMessage());
            throw e;
        }
    }
///////////////////////////////////////////////////////////////////////////////////////////////
/// End of debug functions

}
