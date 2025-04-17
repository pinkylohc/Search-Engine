import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import SearchBar from '../components/search-engine/SearchBar';
import KeywordList from '../components/search-engine/KeywordList';  
import HotTopics from '../components/search-engine/HotTopics';
import { saveSearch } from '../utils/searchHistory';

const Search = () => {
  const navigate = useNavigate();
  const [selectedKeywords, setSelectedKeywords] = useState([]);
  const [manualQuery, setManualQuery] = useState('');
  const [keywords, setKeywords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [usePageRank, setUsePageRank] = useState(true); // New state for PageRank toggle
  const [searchOperator, setSearchOperator] = useState('AND');


  useEffect(() => {
    const fetchKeywords = async () => {
      try {
        const response = await axios.get('http://localhost:8080/search/keywords');
        setKeywords(response.data);
      } catch (error) {
        console.error('Error fetching keywords:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchKeywords();
  }, []);

  const handleSearch = async (queryOverride) => {
    const searchTerms = queryOverride || [...selectedKeywords, manualQuery].filter(Boolean).join(' ');
    
    try {
      const response = await axios.get('http://localhost:8080/search/query', {
        params: {
          query: searchTerms,
          usePageRank: usePageRank
        },
      });
      
      saveSearch(searchTerms, response.data);
      navigateToResults(searchTerms, response.data, 'standard');
    } catch (error) {
      console.error('Search error:', error);
    }
  };

  const handleExtendedSearch = async (queryOverride) => {
    const searchTerms = queryOverride || [...selectedKeywords, manualQuery].filter(Boolean).join(' ');
    
    try {
      const response = await axios.get('http://localhost:8080/search/extended-boolean', {
        params: {
          query: searchTerms,
          operator: searchOperator,
          usePageRank: usePageRank
        },
      });
      
      saveSearch(searchTerms, response.data);
      navigateToResults(searchTerms, response.data, 'extended');
    } catch (error) {
      console.error('Extended search error:', error);
    }
  };

  const navigateToResults = (query, results, searchType) => {
    navigate('/results', {
      state: {
        searchData: {
          query,
          results,
          usedPageRank: usePageRank,
          operator: searchOperator,
          searchType
        }
      }
    });
  };

  return (
    <div className="bg-gradient-to-b from-blue-50 to-white flex flex-col items-center px-4">
      <main className="max-w-6xl mx-auto px-4 py-6">
        <div className="mb-8 w-full">
          <SearchBar 
            query={manualQuery}
            setQuery={setManualQuery}
            selectedKeywords={selectedKeywords}
            setSelectedKeywords={setSelectedKeywords}
            onSearch={handleSearch}
            onExtendedSearch={handleExtendedSearch}
            usePageRank={usePageRank}
            setUsePageRank={setUsePageRank}
            searchOperator={searchOperator}
            setSearchOperator={setSearchOperator}
          />
        </div>

        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1 lg:max-w-[70%]">
            <KeywordList 
              keywords={keywords}
              loading={loading}
              selectedKeywords={selectedKeywords}
              setSelectedKeywords={setSelectedKeywords}
            />
          </div>
          
          <div className="lg:w-[30%]">
            <HotTopics 
              onSelect={(query) => {
                setManualQuery(query);
                setSelectedKeywords([]);
                handleSearch(query);
              }}
            />
          </div>
        </div>
      </main>
    </div>
  );
};

export default Search;