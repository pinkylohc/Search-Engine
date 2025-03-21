package main;

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

import jdbm.helper.FastIterator;

import org.htmlparser.util.ParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.database.DbManage;
import main.database.PageInfo;
import main.utils.Indexer;

public class testProgram {
    String url = null; // Initialize to null, or a default URL
    int numPages = 30;
    Crawler webCrawler = new Crawler(numPages);

    public static void main(String[] args) {  // Added main method
        testProgram program = new testProgram();
        try { // Added try-catch block to handle potential exceptions
            FileWriter fileOut = new FileWriter("spiderResult.txt");
            fileOut.write("");

            program.webCrawler.crawl(program.url);
            StringTokenizer lt = new StringTokenizer(program.webCrawler.visitedUrl); // Corrected this line

            FastIterator iter = program.webCrawler.dbManage.keys;
            String key;
            while ((key = (String) iter.next()) != null) {
                String currentURL = key;
                int docId = program.webCrawler.dbManage.getPageId(key);
                fileOut.append(program.webCrawler.indexPage.getTitle(key) + '\n');
                fileOut.append(program.webCrawler.dbManage.getLastModified(docId) + ", ");
                // fileOut.append(webCrawler.indexPage.get + '\n')
                // fileOut.append(webCrawler.dbManage.getWordId(key))
                FastIterator links = program.webCrawler.indexPage.getLinks(key);
                String link;
                while ((link = (String) links.next()) != null) {
                    fileOut.append(link + '\n');
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