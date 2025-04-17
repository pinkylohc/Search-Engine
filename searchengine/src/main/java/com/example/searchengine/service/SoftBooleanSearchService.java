package com.example.searchengine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.searchengine.database.DbManage;
import com.example.searchengine.model.KeywordFrequency;
import com.example.searchengine.model.PageInfo;
import com.example.searchengine.model.PageResult;
import com.example.searchengine.model.Posting;
import com.example.searchengine.service.utils.StopStem;

import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

@Service
public class SoftBooleanSearchService {
    
    @Autowired
    private DbManage dbManage;
    
    private final StopStem stopStem;

    public SoftBooleanSearchService(DbManage dbManage) {
        this.dbManage = dbManage;
        this.stopStem = new StopStem("stopwords.txt");
    }

    public List<PageResult> extendedBooleanSearch(String query, boolean usePageRank, String operator) throws IOException {
        List<PageResult> results = new ArrayList<>(); // return rersults list
        System.out.println("Extended Boolean Search: " + query + ", operator: " + operator);

        // Parse the query into individual terms
        List<String> terms = parseQueryTerms(query);

        for (String term : terms){
            System.out.println("Parsed term: " + term);
        }
        
        int totalCrawledPages = dbManage.getPageCount();
        if (totalCrawledPages == 0) return results; // No pages to search

        // Get normalized scores for each term (0-1 range)
        Map<Integer, Map<String, Double>> docTermScores = new HashMap<>(); // <doc id, <term, score>>
        Map<String, Double> maxScores = new HashMap<>(); // <term, max score>
        
        // First pass: get all term scores and find max scores
        for (String term : terms) {
            List<PageResult> termResults = basicTermSearch(term); // get the tf score
            double maxScore = termResults.stream()
                .mapToDouble(PageResult::getScore)
                .max()
                .orElse(1.0);
            maxScores.put(term, maxScore);
            
            for (PageResult result : termResults) {
                docTermScores.computeIfAbsent(result.getId(), k -> new HashMap<>())
                    .put(term, result.getScore() / maxScore); // Normalize score
            }
        }
        
        // Apply extended Boolean model
        Map<Integer, Double> combinedScores = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Double>> entry : docTermScores.entrySet()) {
            int docId = entry.getKey();
            Map<String, Double> termScores = entry.getValue();
            
            double score;
            if ("OR".equalsIgnoreCase(operator)) {
                score = calculateOrScore(termScores, terms);
            } else { // AND
                score = calculateAndScore(termScores, terms);
            }
            
            combinedScores.put(docId, score);
        }
        
        // Apply PageRank if needed
        if (usePageRank) {
            applyPageRank(combinedScores);
        }
        
        results = convertToResults(combinedScores);
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore())); // sort by score
        if (results.size() > 50) results = new ArrayList<>(results.subList(0, 50)); 
        return results;
    }
    
    private double calculateOrScore(Map<String, Double> termScores, List<String> terms) {
        double sumOfSquares = 0.0;
        int m = terms.size();
        
        for (String term : terms) {
            double x = termScores.getOrDefault(term, 0.0);
            sumOfSquares += Math.pow(x, 2);
        }
        
        return Math.sqrt(sumOfSquares / m);
    }
    
    private double calculateAndScore(Map<String, Double> termScores, List<String> terms) {
        double sumOfSquares = 0.0;
        int m = terms.size();
        
        for (String term : terms) {
            double x = termScores.getOrDefault(term, 0.0);
            sumOfSquares += Math.pow(1 - x, 2);
        }
        
        return 1 - Math.sqrt(sumOfSquares / m);
    }
    
    
    private List<String> parseQueryTerms(String query) {
        // Similar to your existing parseQuery but simpler
        return Arrays.stream(query.split("\\s+"))
            .map(String::toLowerCase)
            .filter(term -> !stopStem.isStopWord(term))
            .map(stopStem::stem)
            .collect(Collectors.toList());
    }
    
    private List<PageResult> basicTermSearch(String term) throws IOException {
        // Use your existing single-term search logic
        Integer wordId = dbManage.getWordId(term);
        if (wordId == -1) return Collections.emptyList();
        
        // Get postings and compute scores (simplified version)
        List<Posting> titlePostings = dbManage.getTitlePosting(wordId);
        List<Posting> bodyPostings = dbManage.getBodyPosting(wordId);
        
        // Combine and score documents
        Map<Integer, Double> scores = new HashMap<>();
        processPostings(scores, titlePostings, 5.0); // title boost
        processPostings(scores, bodyPostings, 1.0);
        
        // Normalize by maximum term frequency in each document
        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            PageInfo pageInfo = dbManage.getPageInfo(entry.getKey());
            if (pageInfo != null && pageInfo.bodyWordList != null && !pageInfo.bodyWordList.isEmpty()) {
                // Find the maximum frequency in this document
                int maxFreq = pageInfo.bodyWordList.values().stream()
                    .max(Integer::compare)
                    .orElse(1);
                
                // Normalize the score by the max frequency
                double normalizedScore = entry.getValue() / maxFreq;
                entry.setValue(normalizedScore);
            }
        }

        return convertToResults(scores);
    }
    
    private void processPostings(Map<Integer, Double> scores, List<Posting> postings, double weight) {
        if (postings == null) return;
        for (Posting p : postings) {
            scores.merge(p.getId(), p.getFreq() * weight, Double::sum);
        }
    }
   
    private void applyPageRank(Map<Integer, Double> scores) throws IOException {
        HTree pageRankTable = dbManage.getPageRankMap();
        double maxPageRank = getMaxPageRank(pageRankTable);
        
        scores.replaceAll((docId, contentScore) -> {
            try {
                Double pageRankScore = (Double) pageRankTable.get(docId);
                if (pageRankScore == null) return contentScore * 0.7;
                
                double normalizedPR = pageRankScore / maxPageRank;
                return (contentScore * 0.7) + (normalizedPR * 0.3);
            } catch (IOException e) {
                return contentScore * 0.7;
            }
        });
    }
    
    private double getMaxPageRank(HTree pageRankTable) throws IOException {
        double max = 1.0;
        FastIterator iter = pageRankTable.keys();
        Integer pageId;
        while ((pageId = (Integer) iter.next()) != null) {
            Double pr = (Double) pageRankTable.get(pageId);
            if (pr != null && pr > max) max = pr;
        }
        return max;
    }
    
    private List<PageResult> convertToResults(Map<Integer, Double> scores) throws IOException {
        List<PageResult> results = new ArrayList<>();
        for(Integer docId : scores.keySet()) {
            PageInfo pageInfo = dbManage.getPageInfo(docId);
            if (pageInfo != null) {
                PageResult result = new PageResult();
                result.setId(docId);
                result.setScore(scores.get(docId));
                result.setTitle(pageInfo.getTitle());
                result.setUrl(pageInfo.getUrl());
                result.setLastModified(pageInfo.getLastModified());
                result.setSize(pageInfo.getSize());
                result.setChildLinks(dbManage.getChildLinks(docId));
                result.setParentLinks(dbManage.getParentLinks(docId)); // Parent links would need to be retrieved similarly
                
                // body word list for display
                if (pageInfo.bodyWordList != null) {
                    List<KeywordFrequency> keywords = new ArrayList<>();
                    for (Map.Entry<Integer, Integer> wordEntry : pageInfo.bodyWordList.entrySet()) {
                        String word = dbManage.getWord(wordEntry.getKey());
                        if (word != null) {
                            keywords.add(new KeywordFrequency(word, wordEntry.getValue()));
                        }
                    }
                    // Sort by frequency in descending order
                    keywords.sort((a, b) -> b.getFrequency().compareTo(a.getFrequency()));
                    result.setKeywordsWithFrequency(keywords.stream().collect(Collectors.toList()));
                }
                
                results.add(result);
            }
        }
        return results;
    }
}