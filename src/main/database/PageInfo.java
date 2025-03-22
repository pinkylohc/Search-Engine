package main.database;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class PageInfo implements Serializable{
    public String url;
    public String title;
    public int size;
    public Date lastModified;
    public Map<Integer, Integer> bodyWordList; // body word list
    public Map<Integer, Integer> titleWordList; // title word list

    public PageInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
    
    public String getTitle() {
        return title;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public int getSize() {
        return size;
    }

    
    public void setBodyWordList(Map<Integer, Integer> bodyWordList) {
        this.bodyWordList = bodyWordList;
    }

    public void setTitleWordList(Map<Integer, Integer> titleWordList) {
        this.titleWordList = titleWordList;
    }

    public void extractInfo() throws IOException {
        Document doc = Jsoup.connect(url).get();
        this.title = doc.title();
        this.lastModified = extractLastModified(doc);
        this.size = extractSize(doc);
        if(this.lastModified == null) {
            this.lastModified = new Date();
        }
    }

    private int extractSize(Document doc) {
        String contentLengthHeader = doc.connection().response().header("Content-Length");
        if (contentLengthHeader != null) {
            return Integer.parseInt(contentLengthHeader);
        } else {
            // Fallback to calculating the size if Content-Length is not available
            return doc.html().length();
        }
    }

    // Method to get the last modified date as a Date object
    private static Date extractLastModified(Document doc) {
        // Check the HTTP headers first
        String lastModifiedHeader = doc.connection().response().header("Last-Modified");
        if (lastModifiedHeader != null) {
            return parseHttpDate(lastModifiedHeader);
        }

        // If the header is not available, check for a <meta> tag
        Elements metaTags = doc.select("meta[name=last-modified]");
        if (!metaTags.isEmpty()) {
            String metaDate = metaTags.first().attr("content");
            return parseHttpDate(metaDate);
        }

        // If no last modified date is found, return null
        return null;
    }

    // Helper method to parse HTTP date strings 
    private static Date parseHttpDate(String dateString) {
        try {
            return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").parse(dateString);
        } catch (java.text.ParseException e) {
            System.err.println("Error parsing date: " + dateString);
            return null;
        }
    }


    // for debug
    public void printPageInfo() {
        System.out.println("URL: " + url);
        System.out.println("Title: " + title);
        System.out.println("Size: " + size + " bytes");
        System.out.println("Last Modified: " + (lastModified != null ? lastModified : "N/A"));

        // print the body and title map
        System.out.println("Body Word List:");
        if (bodyWordList != null) {
            for (Map.Entry<Integer, Integer> entry : bodyWordList.entrySet()) {
                System.out.println("Body: " + entry.getKey() + " -> " + entry.getValue());
            }
        }


        System.out.println("Title Word List:");
        if(titleWordList != null) {
            for (Map.Entry<Integer, Integer> entry : titleWordList.entrySet()) {
                System.out.println("Title: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        
    }

}
