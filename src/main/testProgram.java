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

public class testProgram {
    String url = null; // Initialize to null, or a default URL
    int numPages = 30;
    Crawler webCrawler = new Crawler(numPages);
    DbManage dbManage = new DbManage("crawlerDB");

    public static void main(String[] args) {  // Added main method
        testProgram program = new testProgram();
        try { // Added try-catch block to handle potential exceptions
            FileWriter fileOut = new FileWriter("spiderResult.txt");
            fileOut.write("");

            program.webCrawler.crawl(program.url);

            FastIterator iter = program.dbManage.get(pageIndex).keys();
            Integer pageId;
            while ((pageId = (Integer) iter.next()) != null) {

                PageInfo pageInfo = (PageInfo) program.dbManage.pageIndex.get(pageId);

                String title = pageInfo.getTitle();
                String url = pageInfo.getUrl();
                Date lastModified = pageInfo.getLastModified();
                int size = pageInfo.getSize();

                fileOut.append(title + '\n');
                fileOut.append(url);
                
                fileOut.append(lastModified + ", ");
                fileOut.append(String.valueOf(size) + '\n');

                if (pageInfo.bodyWordList != null){
                    StringBuilder bodyKeywordFreqBuilder = new StringBuilder();
                    for (Map.Entry<Integer, Integer>entry: pageInfo.bodyWordList.entrySet()){
                        int wordId = entry.getKey();
                        int freq = entry.getValue();
                        String word = (String) program.dbManage.wordidMap.get(wordId);
                        if (word != null){
                            bodyKeywordFreqBuilder.append(word).append(" ").append(freq).append("; ");
                        }
                    }
                    if (bodyKeywordFreqBuilder.length() >0){
                        bodyKeywordFreqBuilder.setLength(bodyKeywordFreqBuilder.length() - 2);
                    }
                    fileOut.append(bodyKeywordFreqBuilder.toString());
                }

                List<Integer> childPageIds = (List<Integer>) program.dbManage.parentChildMap.get(pageId);
                if (childPageIds != null) {
                    for (int childPageId : childPageIds) {
                        String childUrl = (String) program.dbManage.pageidMap.get(childPageId);
                        if (childUrl != null) {
                            fileOut.append(childUrl + '\n');
                        }
                    }
                }
                
                fileOut.append("-----------------\n"); // Added newline for better formatting
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