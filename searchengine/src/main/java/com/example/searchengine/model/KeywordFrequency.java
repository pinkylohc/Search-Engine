package com.example.searchengine.model;

import java.io.Serializable;

public class KeywordFrequency implements Serializable{
    private static final long serialVersionUID = 100L; // Add a unique ID for serialization

    private String keyword;
    private Integer frequency;

    public KeywordFrequency(String keyword, Integer frequency) {
        this.keyword = keyword;
        this.frequency = frequency;
    }

    // Getters
    public String getKeyword() { return keyword; }
    public Integer getFrequency() { return frequency; }
}