package com.example.searchengine.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
// a simple code to test the whole program work
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.searchengine.dto.CrawlRequest;
import com.example.searchengine.model.PageInfo;
import com.example.searchengine.service.CrawlerService;

@RestController
public class CrawlerController {

    @Autowired
    CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/")
    public String initTest() {
        return "Hello World";
    }
    
    
    // start crawling
    @PostMapping("/crawl")
    public ResponseEntity<Void> crawl(@RequestBody CrawlRequest request) throws IOException {
        crawlerService.crawl(request.getStartingUrl(), request.getMaxIndexPage());
        return ResponseEntity.ok().build();
    } 

    // get crawled page
    @GetMapping("/crawled-pages")
    public ResponseEntity<List<PageInfo>> getCrawledPages() throws IOException {
        List<PageInfo> crawledPages = crawlerService.getCrawledPages();
        return ResponseEntity.ok(crawledPages);
    }

    // delete the .db file
    @PostMapping("/clean-db")
    public ResponseEntity<String> cleanDatabase() {
        try {
            crawlerService.cleanDatabase();
            return ResponseEntity.ok("Database cleaned successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clean database: " + e.getMessage());
        }
    }

    @GetMapping("/check-db-status")
    public ResponseEntity<Map<String, Object>> checkDatabaseStatus() {
        try {
            Map<String, Object> status = crawlerService.checkDatabaseStatus();
            return ResponseEntity.ok(status);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check database status");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(errorResponse);
        }
    }


}
