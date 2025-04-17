package com.example.searchengine.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

// structure for the result of one URL that display on the frontend

public class PageResult implements Serializable{
    private static final long serialVersionUID = 1L; // Add a unique ID for serialization

    private Integer id;
    private Double score;
    private String title;
    private String url;
    private Date lastModified;
    private int size;
    private List<KeywordFrequency> keywordsWithFrequency;
    private List<String> childLinks;
    private List<String> parentLinks;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public List<KeywordFrequency> getKeywordsWithFrequency() { return keywordsWithFrequency; }
    public void setKeywordsWithFrequency(List<KeywordFrequency> keywordsWithFrequency) { 
        this.keywordsWithFrequency = keywordsWithFrequency; 
    }
    public List<String> getChildLinks() { return childLinks; }
    public void setChildLinks(List<String> childLinks) { this.childLinks = childLinks; }
    public List<String> getParentLinks() { return parentLinks; }
    public void setParentLinks(List<String> parentLinks) { this.parentLinks = parentLinks; }


    

}

