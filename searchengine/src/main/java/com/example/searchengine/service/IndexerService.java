package com.example.searchengine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.example.searchengine.database.DbManage;
import com.example.searchengine.model.PageInfo;
import com.example.searchengine.model.Posting;
import com.example.searchengine.service.utils.StopStem;

/**
 * Indexer.java
 * For the Crawler to extract the page content and index it
 * store data into inverted index table
 */

@Service
public class IndexerService {
    private DbManage dbManage;

    public IndexerService(DbManage dbManage) {
        this.dbManage = dbManage;
    }

    public void indexPage(String url, String title, int docId, PageInfo pageInfo) throws IOException {
        // extract the page content
        List<String> bodyWords = extractBodyWords(url);
        List<String> titleWords = tokenizeTitle(title);

        // stop word removal & stemming (body & title)
        StopStem stopStem = new StopStem("stopwords.txt"); // stop word removal and stemming
        List<String> bodyStem = bodyWords.stream()   
                            .filter(word -> !stopStem.isStopWord(word))
                            .map(stopStem::stem)
                            .collect(Collectors.toList());

        List<String> titleStem = titleWords.stream()
                            .filter(word -> !stopStem.isStopWord(word))
                            .map(stopStem::stem)
                            .collect(Collectors.toList());

        // <wordId, freq> 
        Map<Integer, Integer> bodyPageIndex= updateIndex(bodyStem, docId, "body"); // store stem into bodyIndex
        Map<Integer, Integer> titlePageIndex= updateIndex(titleStem, docId, "title"); // store stem into titleIndex

        // add bodyPageIndex, titlePageIndex as the instance var of pageInfo to store in pageIndex table
        pageInfo.setBodyWordList(bodyPageIndex); 
        pageInfo.setTitleWordList(titlePageIndex);
    }


    // store the stem word into WordMap, WordidMap, BodyIndex, TitleIndex
    public Map<Integer, Integer> updateIndex(List<String> stemList, int docId, String type) throws IOException{
        Map<Integer, Posting> invertedIndex = new HashMap<>(); // return table for page info
        Map<Integer, Integer> forwardIndex = new HashMap<>(); // <word id, freq>

        for (int i = 0; i < stemList.size(); i++) {
            String stem = stemList.get(i);
            int wordId; 

            if(dbManage.containsWord(stem)){
                wordId = dbManage.getWordId(stem);

            } else {
                wordId = dbManage.addWord(stem); // add to wordMap && wordidMap
            }

            // update inverted index table
            Posting posting = invertedIndex.getOrDefault(wordId, new Posting(docId, 0, new ArrayList<>()));
            posting.addPosition(i);
            posting.freq++;
            invertedIndex.put(wordId, posting);  

            // update forward index table
            forwardIndex.put(wordId, forwardIndex.getOrDefault(wordId, 0) + 1);
        }
        
        // Batch update on the inverted index table
        if(type.equals("body")){
            dbManage.updateBodyIndex(invertedIndex);
        } else if(type.equals("title")){
            dbManage.updateTitleIndex(invertedIndex);
        }
        return forwardIndex;

    }

    // extract the word for page body from url
    public List<String> extractBodyWords(String url) throws IOException{
        // Fetch the HTML from a URL
        List<String> bodyWords = new ArrayList<>();
        Document doc = Jsoup.connect(url).get();
        Element body = doc.body();

        if (body != null) {
            String bodyText = body.text();

            // Tokenize the body text into words
            String[] words = bodyText.split("\\W+"); // Split by any non-letter characters
            for (String word : words) {
                if (!word.isEmpty()) {
                    bodyWords.add(word.toLowerCase());
                }
            }
        }
        return bodyWords;  
    }

    // tokenize the title into words
    public List<String> tokenizeTitle(String title){
        List<String> titleWords = new ArrayList<>();
        String[] words = title.split("\\W+"); // Split by any non-letter characters
        for (String word : words) {
            if (!word.isEmpty()) {
                titleWords.add(word.toLowerCase());
            }
        }
        return titleWords;
    }
}
