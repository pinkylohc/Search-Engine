package main.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

/**
 * DbManage.java
 * Manage all the jdbm database operations
 * create, access, and update the jdbm tables
 */


public class DbManage {
    private RecordManager recMan;

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

    public DbManage(String recman) throws IOException { // create all Htree tables
        recMan = RecordManagerFactory.createRecordManager(recman);
        pageMap = loadOrCreateHTree("pageMap"); // String -> Integer
        pageidMap = loadOrCreateHTree("pageidMap"); // Integer -> String
        pageidCounter = getTableSize(pageMap);

        pageIndex = loadOrCreateHTree("pageIndex"); // int -> pageInfo

        parentChildMap = loadOrCreateHTree("parentChildMap"); // int -> List<Integer>
        childParentMap = loadOrCreateHTree("childParentMap"); // int -> List<Integer>
        
        wordMap = loadOrCreateHTree("wordMap"); // String -> Integer
        wordidMap = loadOrCreateHTree("wordidMap"); // int -> String
        wordidCounter = getTableSize(wordMap);

        bodyIndex = loadOrCreateHTree("bodyIndex"); // int -> List<Posting>
        titleIndex = loadOrCreateHTree("titleIndex"); // int -> List<Posting>

        pageRank = loadOrCreateHTree("pageRank"); // int -> Double
        
    }

    public HTree loadOrCreateHTree(String name) throws IOException {   // Load/create the HTree from the database, if it does not exist, create a new one
        long recId = recMan.getNamedObject(name);
        if (recId == 0) {
            HTree htree = HTree.createInstance(recMan);
            recMan.setNamedObject(name, htree.getRecid());
            return htree;
        } else {
            return HTree.load(recMan, recId);
        }
    }

    public void finalise() throws IOException {
		recMan.commit();
		recMan.close();				
	}
    
    private int getTableSize(HTree hashTable) throws IOException { // get the id counter (for mapping table)
        FastIterator iter = hashTable.keys();
        int size = 0;
        while (iter.next() != null) {
            size++;
        }
        return size;
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

    /************* wordMap & wordidMap Operation ****************/
    public boolean containsWord(String word) throws IOException {
        return wordMap.get(word) != null;
    }

    public int getWordId(String word) throws IOException {
        return (int) wordMap.get(word);
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


    /************* parentChildMap && childParentMap Operation ****************/
        //parentChildMap: parent page id -> list of child page id
        //childParentMap: child page id -> parent page id

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
