package main;

import java.util.List;

// Please Run the Crawler first to create the jdbm file

public class SearchEngine {
    // private final DbManage dbManage; // for access the jdbm
    // private final HTree bodyIndex;   // for access the bodyIndex table
    // private final HTree titleIndex;  // for access the titleIndex table
    // private final Htree wordMap;     // for access the wordMap table
    // private final StopStem stopStem; // for stop words removal && stemming on query

    public SearchEngine() { // init the instance var. 
        // this.dbManage = new DbManage("crawlerDB");
        // this.bodyIndex = dbManage.loadOrCreateHTree("bodyIndex");
        // this.titleIndex = dbManage.loadOrCreateHTree("titleIndex");
        // this.wordMap = dbManage.loadOrCreateHTree("wordMap");
        // this.stopStem = new StopStem("src/stopwords.txt");
    }


    /**
     * Search endpoint that accepts HTTP GET requests
     * * Support Phrase Search (use the location info. in index tables)
     * * Favour Matches in titleIndex table
     * @param query: the search query send from web portal
     * @return a list of sorted page ids (top 50) based on vector space model
     */
    public List<Integer> search(String query){

        ///////////////////// Example Flow //////////////////////
        // 1. parse the query (can refer to tokenizeTitle() in indexer.java)
        // 2. stop words removal, stemming (below e.g. from indexer.java & lab3)
                /* List<String> bodyStem = bodyWords.stream()   
                                    .filter(word -> !stopStem.isStopWord(word))
                                    .map(stopStem::stem)
                                    .collect(Collectors.toList()); */
        // 3. access the jdbm to get all infomation needed
        // 4. Search Logic (term weight, cos similarity, phase search, favor title.....)
        ///////////////////// Example Flow //////////////////////
        

        // return a list of sorted page ids (top 50) based on vector space model
        return null;
    }


    /**
     * Search endpoint that accepts HTTP GET requests
     * * Support Soft Boolean Search (L06 - two opeators: ^, V) - optional for this phase
     * @param query
     * @return
     */
    public List<Integer> booleanSearch(String query){
        // return a list of sorted page ids (top 50) based on boolean search
        return null;
    }
    
}
