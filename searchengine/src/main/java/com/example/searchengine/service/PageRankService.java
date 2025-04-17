package com.example.searchengine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.searchengine.database.DbManage;

import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

//page rank from quality and authority
//location information and visual presentation?

//page rank = (1-d) + d(sum of page rank/number of outgoing links, for each ingoing page)
//d: damping factor -> set as variable
// all start with initial value 1

//synchronous: update all at once
//asynchronous: update sequentially
//set as toggle?

//also base on anchor text??
//also base on hit list (properties of the hit??)

//table PRScore
//key: docID, value: score
//initialize at 1

//variable damp (0-1)

//iteration stops after either x times
//or when the order does not change
// or with a stopping criterion
//sum of the absolute change in values < 0.0001 or sth


@Service
public class PageRankService {
    private DbManage dbManage; // for access the jdbm tables
    private static final double DAMPING_FACTOR = 0.85; // default value in the pagerank paper
    private static final int MAX_ITERATIONS = 20;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    public PageRankService(DbManage dbManage) {
        this.dbManage = dbManage;
    }

    // modify the pagerank table
    public void computePageRank() throws IOException {
        HTree pageRankTable = dbManage.getPageRankMap();
        HTree pageIndex = dbManage.getPageIndex();

        // Clear and initialize only with crawled pages
        initializePageRank(pageRankTable, pageIndex);

        int totalCrawledPages = dbManage.getPageCount();
        if (totalCrawledPages == 0) return;

        //asynchronous
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Map<Integer, Double> newScores = new HashMap<>();
            double totalChange = 0.0;
            FastIterator iter = pageIndex.keys();
            Integer pageId;

            while ((pageId = (Integer) iter.next()) != null) {
                double sum = 0.0;

                // Get all parent pages that link to this page (using new helper method)
                List<Integer> parentPageIds = dbManage.getCrawledParentPageIds(pageId);
                
                for (Integer parentId : parentPageIds) {
                    // Get child links count for the parent (using new helper method)
                    List<Integer> childPageIds = dbManage.getCrawledChildPageIds(parentId);
                    int outboundLinks = childPageIds.size();
                    
                    if (outboundLinks > 0) {
                        Double parentScore = (Double) pageRankTable.get(parentId);
                        if (parentScore != null) {
                            sum += parentScore / outboundLinks;
                        }
                    }
                }

                // Calculate new score with damping factor
                double newScore = (1 - DAMPING_FACTOR) / totalCrawledPages + DAMPING_FACTOR * sum;
                Double currentScore = (Double) pageRankTable.get(pageId);
                
                if (currentScore != null) { totalChange += Math.abs(newScore - currentScore);}
                newScores.put(pageId, newScore);
            }

            // Update all scores
            newScores.forEach((id, score) -> {
                try {
                    pageRankTable.put(id, score);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            dbManage.commit();

            //System.out.printf("Iteration %d: Total change=%.6f%n", iteration + 1, totalChange);

            // Check for convergence
            if (totalChange < CONVERGENCE_THRESHOLD) {
                //System.out.println("PageRank converged after " + (iteration + 1) + " iterations");
                break;
            }

        }

        // print out the final pagerank table
        //dbManage.printPageRank();
    }


    private void initializePageRank(HTree pageRankTable, HTree pageIndex) throws IOException {
        // Clear existing data
        List<Integer> pageRankKeys = new ArrayList<>();
        FastIterator iter = pageRankTable.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            pageRankKeys.add(key);
        }
        for (Integer pageRankKey : pageRankKeys) {
            pageRankTable.remove(pageRankKey);
        }

        // Initialize only crawled pages
        iter = pageIndex.keys();
        Integer pageId;
        int totalPages = 0;
        double initialScore = 1.0; // Or 1.0/pageIndex.size() for normalized
        
        while ((pageId = (Integer) iter.next()) != null) {
            pageRankTable.put(pageId, initialScore);
            totalPages++;
        }
        
        System.out.println("Initialized PageRank for " + totalPages + " crawled pages");
    }



}
