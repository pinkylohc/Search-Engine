package main;


import java.io.IOException;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import main.database.DbManage;
import main.database.PageInfo;


public class testProgram {
    //String url = null; // Initialize to null, or a default URL
    //int numPages = 30;
    //Crawler webCrawler = new Crawler(numPages);
    //DbManage dbManage = new DbManage("crawlerDB");

    public static void main(String[] args) {  // Added main method
        //testProgram program = new testProgram();
        
        try { // Added try-catch block to handle potential exceptions
            DbManage dbManage = new DbManage("crawlerDB");
            FileWriter fileOut = new FileWriter("spider_result.txt");
            fileOut.write("");

            //program.webCrawler.crawl(program.url);
            HTree printPageIndex = dbManage.loadOrCreateHTree("pageIndex");
            HTree printWordidMap = dbManage.loadOrCreateHTree("wordidMap");
            HTree printParentChildMap = dbManage.loadOrCreateHTree("parentChildMap");
            HTree printPageidMap = dbManage.loadOrCreateHTree("pageidMap");
            

            FastIterator iter = printPageIndex.keys();
            Integer pageId;
            while ((pageId = (Integer) iter.next()) != null) {

                PageInfo pageInfo = (PageInfo) printPageIndex.get(pageId);

                String title = pageInfo.getTitle();
                String url = pageInfo.getUrl();
                Date lastModified = pageInfo.getLastModified();
                int size = pageInfo.getSize();

                fileOut.append(title + '\n');
                fileOut.append(url + '\n');
                
                fileOut.append(lastModified + ", ");
                fileOut.append(String.valueOf(size) + " bytes \n");

                if (pageInfo.bodyWordList != null){
                    StringBuilder bodyKeywordFreqBuilder = new StringBuilder();
                    // Sort the entries by frequency in descending order and limit to top 10
                    pageInfo.bodyWordList.entrySet().stream()
                        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                        .limit(10)
                        .forEach(entry -> {
                            int wordId = entry.getKey();
                            int freq = entry.getValue();
                            String word;
                            try {
                                word = (String) printWordidMap.get(wordId);
                                if (word != null) {
                                    bodyKeywordFreqBuilder.append(word).append(" ").append(freq).append("; ");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
                        });
                    fileOut.append(bodyKeywordFreqBuilder.toString());
                }

                fileOut.append('\n');

                @SuppressWarnings("unchecked")
                List<Integer> childPageIds = (List<Integer>) printParentChildMap.get(pageId);
                if (childPageIds != null) {
                    int count = 0;
                    for (int childPageId : childPageIds) {
                        if(count >= 10) break;
                        String childUrl = (String) printPageidMap.get(childPageId);
                        if (childUrl != null) {
                            fileOut.append(childUrl + '\n');
                        }
                        count++;
                    }
                }
                
                fileOut.append("-----------------------------------------------\n"); // Added newline for better formatting
            }

            fileOut.close();

        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage()); // Handle exceptions properly
            e.printStackTrace();
        }
    }
}
/* Page title
URL
Last modification date, size of page
Keyword1 freq1; Keyword2 freq2; Keyword3 freq3; ... ...
Child Link1
Child Link2 ... ...
——————————————– (The separator line should be a line of hyphens, i.e. -)*/