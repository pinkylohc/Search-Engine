package com.example.searchengine.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.searchengine.model.PageResult;
import com.example.searchengine.service.SearchService;
import com.example.searchengine.service.SoftBooleanSearchService;


@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private SoftBooleanSearchService softBooleanSearchService;

    /**
     * Search with query
     * @param query
     * @return list of page results
     */
    @GetMapping("/query")
    public ResponseEntity<List<PageResult>> search(
        @RequestParam String query,
        @RequestParam(required = false, defaultValue = "true") boolean usePageRank) {
        try {
            System.out.println("Searching for: " + query + ", usePageRank: " + usePageRank);
            List<PageResult> results = searchService.search(query, usePageRank);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // display all stemmed keywords for select
    @GetMapping("/keywords")
    public ResponseEntity<?> getAllKeywords() {
        try {
            List<String> keywords = searchService.getAllKeywords();

            // Check if the list is empty
            if (keywords.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No keywords found.");
            }

            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving keywords.");
        }
    }

    // get hot topic from cache table
    @GetMapping("/hot-topic")
    public ResponseEntity<List<Map<String, Object>>> getHotTopic() {
        try {
            List<Map<String, Object>> hotTopic = searchService.getHotTopic();
            return ResponseEntity.ok(hotTopic);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // clean cache table
    @PostMapping("/clean-cache")
    public ResponseEntity<String> cleanCache() {
        try {
            searchService.cleanCache();
            return ResponseEntity.ok("Cache cleaned successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while cleaning the cache.");
        }
    }

    @GetMapping("/extended-boolean")
    public ResponseEntity<List<PageResult>> extendedBooleanSearch(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "AND") String operator,
            @RequestParam(required = false, defaultValue = "true") boolean usePageRank) {
        
        try {
            List<PageResult> results = softBooleanSearchService.extendedBooleanSearch(
                query, usePageRank, operator);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
