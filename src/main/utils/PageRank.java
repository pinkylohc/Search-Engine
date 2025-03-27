package main.utils;


public class PageRank {
    /* a new empty table `pageRank` is created in DbManage.java 
     page id (int) -> pageRank score (double) */
    // can refer to L07 - Google PageRank Algorithm 

    // private final DbManage dbManage; // for access the jdbm
    // private HTree pageRank;   // for modify the pageRank table
    // private HTree parentChildMap;    // for access the parent -> child relationship
    // private HTree childParentMap;    // for access the child -> parent relationship
    // private HTree pageIndex; // page ID -> page info


    public PageRank() {
        // this.dbManage = new DbManage("crawlerDB");
        // this.pageRank = dbManage.loadOrCreateHTree("pageRank");
        // this.parentChildMap = dbManage.loadOrCreateHTree("parentChildMap");
        // this.childParentMap = dbManage.loadOrCreateHTree("childParentMap");
        // this.pageIndex = dbManage.loadOrCreateHTree("pageIndex");
    }

    
    /**
     * Update the PageRank table
     * This function is run after the crawler in Main.java has finished 
     */
    public void calculatePageRank() {
        ///////////////////// Example Flow //////////////////////
        // 1. Initialize the pageRank table
        // 2. iterative update the pageRank table (handle dangling nodes and other....)
            // only need to calculate the pageRank score for page in pageIndex table
            // only consider the parent & child page ids that exist in the pageIndex table
        ///////////////////// Example Flow //////////////////////
    }


}
