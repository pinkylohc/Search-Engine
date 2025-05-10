package com.example.searchengine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import org.springframework.stereotype.Service;

import com.example.searchengine.model.PageInfo;
import com.example.searchengine.model.PageResult;
import com.example.searchengine.model.Posting;
import com.example.searchengine.service.utils.StopStem;
import com.example.searchengine.database.DbManage;
import com.example.searchengine.model.KeywordFrequency;

import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

@Service
public class SearchService {
    private DbManage dbManage;
    private final StopStem stopStem;

    public SearchService(DbManage dataService) {
        this.dbManage = dataService;
        this.stopStem = new StopStem("stopwords.txt"); 
    }

    public List<PageResult> search(String query, boolean usePageRank) throws IOException {
        List<PageResult> results = new ArrayList<>(); // return rersults list
        
        if(usePageRank){    // only store cache result for using pageRank
            dbManage.printCache(); // print the cache for debug
            List<PageResult> cachedResults = dbManage.getCachedResults(query);
            if (cachedResults != null) {
                System.out.println("Returning cached results for: " + query);
                return cachedResults;
            }
        }

        int totalCrawledPages = dbManage.getPageCount();
        if (totalCrawledPages == 0) return results; // No pages to search

        // parse the query
        List<Object> parsedQuery = parseQuery(query);
        List<String> terms = new ArrayList<>(); // for individual terms
        List<List<String>> phrases = new ArrayList<>(); // for phrases
        processParsedQuery(parsedQuery, terms, phrases);

        ///////////////////////////////// debugging output
        System.out.println("Terms:");
        for (String term : terms) {
            System.out.println(" - " + term);
        }

        System.out.println("Phrases:");
        for (List<String> phrase : phrases) {
            System.out.println(" - " + String.join(" ", phrase));
        }
        ////////////////////////////// end debugging output
        
        Map<Integer, Double> idfMap = computeIDFs(terms); // Precompute IDF for each term (Map<wordId, idf>)
        Map<Integer, Map<Integer, Double>> docTfidfMap = computeTermWeight(terms, idfMap); // compute tf score for each doc
        
        // Calculate cosine similarity scores
        Map<Integer, Double> scores = computeCosineSimilarity(docTfidfMap, idfMap, terms); // <doc id, scores>

        // support phrase search
        applyPhraseBoosts(phrases, scores);
        
        // Incorporate PageRank if enabled
        if (usePageRank) { incorporatePageRank(scores); }
        
        results = queryResult(scores);    // store the page into pageResult object for return
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore())); // sort by score
        if (results.size() > 50) results = new ArrayList<>(results.subList(0, 50)); 

        if(usePageRank) dbManage.putCachedResults(query, results);
        return results;
    }

    
    private void incorporatePageRank(Map<Integer, Double> scores) throws IOException {
        HTree pageRankTable = dbManage.getPageRankMap();
        
        // Get the max PageRank score for normalization
        double maxPageRank = 1.0; // Default if no pages
        FastIterator iter = pageRankTable.keys();
        Integer pageId;
        while ((pageId = (Integer) iter.next()) != null) {
            Double pr = (Double) pageRankTable.get(pageId);
            if (pr != null && pr > maxPageRank) {
                maxPageRank = pr;
            }
        }

        // Combine scores (70% content score, 30% PageRank)
        final double pageRankWeight = 0.3;
        final double contentWeight = 0.7;
        final double finalMaxPageRank = maxPageRank;
        
        scores.replaceAll((docId, contentScore) -> {
            try {
                Double pageRankScore = (Double) pageRankTable.get(docId);
                if (pageRankScore == null) {
                    return contentScore * contentWeight;
                }
                
                // Normalize PageRank score and combine with content score
                double normalizedPR = pageRankScore / finalMaxPageRank;
                return (contentScore * contentWeight) + (normalizedPR * pageRankWeight);
            } catch (IOException e) {
                e.printStackTrace();
                return contentScore * contentWeight;
            }
        });
    }


    // split for phrases and individual terms
    private List<Object> parseQuery(String query) {
        List<Object> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
        Matcher matcher = pattern.matcher(query);
        
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Handle quoted phrases - convert each word to lowercase
                List<String> phraseWords = Arrays.stream(matcher.group(1).split("\\s+"))
                    .map(String::toLowerCase)  // Convert each word to lowercase
                    .filter(word -> !word.isEmpty())
                    .collect(Collectors.toList());
                
                if (!phraseWords.isEmpty()) {
                    result.add(phraseWords);
                }
            } else {
                // Handle individual terms - convert to lowercase
                String term = matcher.group(2).toLowerCase();
                if (!term.isEmpty()) {
                    result.add(term);
                }
            }
        }
        return result;
    }

    // stop word removal & stemming for keywords and sperately store th terms/phrases
    private void processParsedQuery(List<Object> parsedQuery, List<String> terms, List<List<String>> phrases) throws IOException {
        for (Object item : parsedQuery) {
            if (item instanceof String) { // individual term
                String term = (String) item;
                if (!stopStem.isStopWord(term)) {
                    terms.add(stopStem.stem(term));
                }
            } else if (item instanceof List) { // phrase
                @SuppressWarnings("unchecked")
                List<String> phrase = (List<String>) item;
                List<String> processedPhrase = new ArrayList<>();
                for (String t : phrase) {
                    if (!stopStem.isStopWord(t)) {
                        processedPhrase.add(stopStem.stem(t));
                    }
                }
                if (!processedPhrase.isEmpty()) {
                    phrases.add(processedPhrase);
                }
            }
        }
    }


    private Map<Integer, Double> computeIDFs(List<String> terms) throws IOException {
        int totalDocs = dbManage.getPageCount();
        //System.out.println("Total number of documents: " + totalDocs);
        Map<Integer, Double> idfMap = new HashMap<>(); // for store idf score
        Set<Integer> docsWithTerm = new HashSet<>(); // for count the doc frequency

        for (String term : terms) {
            Integer wordId = dbManage.getWordId(term);
            if (wordId == -1 || idfMap.containsKey(wordId)) { // term not exist in the database or already computed
                continue;
            }

            // for each term, get the doc frequency (from body & title) -> cal. idf
            docsWithTerm.clear();
            List<Posting> titlePostings  = dbManage.getTitlePosting(wordId);
            List<Posting> bodyPostings = dbManage.getBodyPosting(wordId);
            addDocsFromPostings(docsWithTerm, titlePostings);
            addDocsFromPostings(docsWithTerm, bodyPostings);
            double idf = Math.log(1 + (double) totalDocs / (docsWithTerm.size())); // +1 for avoid div by 0 and smoothing effect
            idfMap.put(wordId, idf);

            ///////////////////// Debug output showing both ID and term
            //System.out.printf("[DEBUG] Term: %s (ID:%d), DocFreq: %d, IDF: %.4f%n",
            //term, wordId, docsWithTerm.size(), idf);
        }
        return idfMap;
    }

    private void addDocsFromPostings(Set<Integer> docs, List<Posting> postings) {
        if (postings != null) {
            postings.forEach(p -> docs.add(p.getId()));
        }
    }

    /**
     * @param terms: list of term
     * @return Map<doc id, Map<word id, tf score>>
     * @throws IOException
     */
    private Map<Integer, Map<Integer, Double>> computeTermWeight(List<String> terms, Map<Integer, Double> idfMap) throws IOException {
        Map<Integer, Map<Integer, Double>> docTfMap = new HashMap<>();
        Set<String> termSet = new HashSet<>(); // for avoid double exist term

        for (String term : terms) {
            if(termSet.contains(term)) continue; // avoid double exist term
            termSet.add(term);

            Integer wordId = dbManage.getWordId(term); 
            if (wordId == -1) continue; //! not handle double exist term !!

            List<Posting> titlePostings  = dbManage.getTitlePosting(wordId);
            List<Posting> bodyPostings = dbManage.getBodyPosting(wordId);
            processPostings(docTfMap, titlePostings, wordId, 3.0); // boost title tf score 
            processPostings(docTfMap, bodyPostings, wordId, 1.0);
        }


        // find the maxtf across the document -> cal. tf/maxtf
        for (Map.Entry<Integer, Map<Integer, Double>> entry : docTfMap.entrySet()) {
            int docId = entry.getKey();
            Map<Integer, Double> termFreq = entry.getValue();
            Map<Integer, Integer> textList =  dbManage.getBodyWordList(docId);
            double maxTf = 1.0;  // init to 1 to avoid div by zero

            if (textList != null) {
                for (Map.Entry<Integer, Integer> wordEntry : textList.entrySet()) {
                    double tf = (double) wordEntry.getValue();
                    if (tf > maxTf) {
                        maxTf = tf;
                    }
                }
            }
            //System.out.printf("\nDocument %d - Before normalization (maxTf=%.2f):\n", docId, maxTf); 
            final double finalMaxTf = maxTf;
            termFreq.replaceAll((wordId, tf) -> {
                //System.out.printf("before nor: Word ID: %d, TF: %.2f -> ", wordId, tf);
                double normalizedTf = 0.5 + 0.5 * tf / finalMaxTf;   // improved term weights (favors terms with small tf)
                //System.out.printf("after nor tfidf: %.2f\n", normalizedTf * idfMap.get(wordId));
                return normalizedTf * idfMap.get(wordId);
            });         
        }
        return docTfMap;
    }

    // create the vector (word id, tf score) for each document
    private void processPostings(Map<Integer, Map<Integer, Double>> docTfMap, List<Posting> postings, Integer wordId, double weight) {
        if (postings == null) return;
        for (Posting p : postings) {
            int docId = p.getId();
            Map<Integer, Double> termFreq = docTfMap.computeIfAbsent(docId, k -> new HashMap<>());

            // Calculate weighted frequency and accumulate
            double weightedFreq = p.getFreq() * weight;
            termFreq.merge(wordId, weightedFreq, Double::sum);
        }
    }


    // compute cosine similarity for each document with the query
    private Map<Integer, Double> computeCosineSimilarity(Map<Integer, Map<Integer, Double>> docTfidfMap, Map<Integer, Double> idfMap, List<String> terms) throws IOException {
        Map<Integer, Double> scores = new HashMap<>(); // <doc id, score>
        
        // 1. Create proper query vector with TF-IDF weights
        Map<Integer, Double> queryVector = new HashMap<>();
        for (String term : terms) {
            Integer wordId = dbManage.getWordId(term);
            if (wordId != -1) {
                // Calculate query TF (term frequency in query)
                double queryTf = Collections.frequency(terms, term);
                // Query IDF is already in idfMap
                queryVector.merge(wordId, queryTf * idfMap.get(wordId), Double::sum);
            }
        }
        double queryNorm = Math.sqrt(queryVector.values().stream()
                                .mapToDouble(v -> v * v)
                                .sum());
        
        for (Map.Entry<Integer, Map<Integer, Double>> entry : docTfidfMap.entrySet()) { 
            int docId = entry.getKey(); // doc id
            Map<Integer, Double> docVector = entry.getValue();  // <word id, tf*idf score>

            // Calculate dot product (sum of doc's TF-IDF scores for query terms)
            double dotProduct = 0.0;
            for (Map.Entry<Integer, Double> queryEntry : queryVector.entrySet()) {
                Integer wordId = queryEntry.getKey();
                double queryWeight = queryEntry.getValue();
                double docWeight = docVector.getOrDefault(wordId, 0.0);
                //System.out.println("wordId: " + wordId + ", queryWeight: " + queryWeight + ", docWeight: " + docWeight);
                dotProduct += queryWeight * docWeight;
            }

            // Calculate document norm
            double docNorm = Math.sqrt(docVector.values().stream()
            .mapToDouble(v -> v * v)
            .sum());

            double cosine = (dotProduct) / (Math.sqrt(docNorm) * Math.sqrt(queryNorm) + 1e-8);
            scores.put(docId, cosine);
            //System.out.printf("Doc %d - Cosine: %.4f (Dot: %.2f, DocNorm: %.2f, QueryNorm: %.2f)%n",
            //docId, cosine, dotProduct, docNorm, queryNorm);
        }
        return scores;
    }

    /////////////////// support phrase search //////////////////////
    private void applyPhraseBoosts(List<List<String>> phrases, Map<Integer, Double> scores) throws IOException {
        for (List<String> phrase : phrases) {
            Set<Integer> candidateDocs = findDocumentsWithAllTerms(phrase);
            for (int docId : candidateDocs) {
                // Count phrase occurrences in both title and body
                int titleCount = countPhraseOccurrences(docId, phrase, true);
                int bodyCount = countPhraseOccurrences(docId, phrase, false);
                int totalPhraseOccurrences = titleCount + bodyCount;
    
                if (totalPhraseOccurrences > 0) {
                    double currentScore = scores.getOrDefault(docId, 0.0);
                    double boost = (titleCount * 2.0) + bodyCount;
                    double phraseScore = boost * 3; // Apply logarithmic scaling to prevent over-boosting
                    
                    // Additive boost rather than multiplicative to handle zero scores
                    scores.put(docId, currentScore + phraseScore);
    
                    /*System.out.printf("Phrase '%s' in doc %d: %d title, %d body, boost=%.2f, score %.2f→%.2f%n",
                        String.join(" ", phrase), docId, titleCount, bodyCount,
                        phraseScore, currentScore, scores.get(docId));*/
                }
            }
        }
    }
    
    private int countPhraseOccurrences(int docId, List<String> phrase, boolean inTitle) throws IOException {
        List<Integer> wordIds = new ArrayList<>();
        for (String term : phrase) {
            Integer wordId = dbManage.getWordId(term);
            if (wordId == null || wordId == -1) return 0; // If any term is missing, no occurrences
            wordIds.add(wordId);
        }
    
        // Get positions for each term in the document section (title or body)
        Map<Integer, List<Integer>> termPositions = new HashMap<>();
        for (int i = 0; i < wordIds.size(); i++) {
            List<Posting> postings = inTitle ? 
                dbManage.getTitlePosting(wordIds.get(i)) : 
                dbManage.getBodyPosting(wordIds.get(i));
            termPositions.put(i, getPositionsForDoc(docId, postings));
        }
    
        // Count consecutive sequences
        int phraseCount = 0;
        List<Integer> firstTermPositions = termPositions.get(0);
        
        for (int pos : firstTermPositions) {
            boolean fullMatch = true;
            for (int i = 1; i < wordIds.size(); i++) {
                if (!termPositions.get(i).contains(pos + i)) {
                    fullMatch = false;
                    break;
                }
            }
            if (fullMatch) {
                phraseCount++;
            }
        }
        
        return phraseCount;
    }

    // get all doc with the phrase term exist
    private Set<Integer> findDocumentsWithAllTerms(List<String> phrase) throws IOException {
        Set<Integer> docs = new HashSet<>(); // doc id - contain all the phrase terms
        boolean firstTerm = true;
        
        for (String term : phrase) {
            Integer wordId = dbManage.getWordId(term);
            if (wordId == -1) return Collections.emptySet();
            
            Set<Integer> termDocs = new HashSet<>(); // temp set - current term exist
            addDocsFromPostings(termDocs, dbManage.getTitlePosting(wordId));
            addDocsFromPostings(termDocs, dbManage.getBodyPosting(wordId));
            
            if (firstTerm) {
                docs.addAll(termDocs);
                firstTerm = false;
            } else {
                docs.retainAll(termDocs); // Intersection of documents
            }
            if (docs.isEmpty()) return docs; // Early exit if no common docs
        }   
        return docs;
    }

    // Helper method to extract positions for a specific document
    private List<Integer> getPositionsForDoc(int docId, List<Posting> postings) {
        List<Integer> positions = new ArrayList<>();
        if (postings != null) {
            for (Posting p : postings) {
                if (p.getId() == docId) {
                    positions.addAll(p.getPositions());
                }
            }
        }
        return positions;
    }
    /////////////////////// end of phrase search support //////////////////////////

    
    // handle the return of search result
    public List<PageResult> queryResult(Map<Integer, Double> scores) throws IOException {
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

    // display all stemmed keywords for select
    public List<String> getAllKeywords() throws IOException {
        List<String> keywords = new ArrayList<>();
        HTree wordidMap = dbManage.getWordidMap();
        FastIterator iter = wordidMap.keys();
        Integer key;
        while ((key = (Integer) iter.next()) != null) {
            String word = dbManage.getWord(key);
            if (word != null) {
                keywords.add(word);
            }
        }
        return keywords;
    }


    // get hot topic from cache table
    public List<Map<String, Object>> getHotTopic() {
        try {
            List<Map.Entry<String, Integer>> topEntries = dbManage.getTopQueries(5);
            
            List<Map<String, Object>> response = topEntries.stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("query", entry.getKey());
                    item.put("frequency", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
            
            return response;
        } catch (IOException e) {
            return null;
        }
    }

    // clean the cache data
    public void cleanCache() throws IOException {
        dbManage.clearCache();
    }
} 