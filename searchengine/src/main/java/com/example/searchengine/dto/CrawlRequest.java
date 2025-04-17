package com.example.searchengine.dto;

public class CrawlRequest {
    private String startingUrl;
    private int maxIndexPage;

    // Default constructor (required for JSON deserialization)
    public CrawlRequest() {}

    // Getters and setters
    public String getStartingUrl() {
        return startingUrl;
    }

    public void setStartingUrl(String startingUrl) {
        this.startingUrl = startingUrl;
    }

    public int getMaxIndexPage() {
        return maxIndexPage;
    }

    public void setMaxIndexPage(int maxIndexPage) {
        this.maxIndexPage = maxIndexPage;
    }
}