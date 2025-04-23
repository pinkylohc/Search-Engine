import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSearchHistory, clearSearchHistory, getUserProfilo } from '../utils/searchHistory';
import { 
  FiClock, FiSearch, FiTrash2, FiArrowRight, 
  FiPlus, FiMinus, FiFilter, 
  FiX, FiInfo,FiUser
} from 'react-icons/fi';

const History = () => {
  const navigate = useNavigate();
  const [searchHistory, setSearchHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedSearches, setSelectedSearches] = useState([]);
  const [searchWithinQueries, setSearchWithinQueries] = useState({});
  const [activeOperation, setActiveOperation] = useState(null);
  const [profileKeywords, setProfileKeywords] = useState([]);

  // Load history from localStorage
  useEffect(() => {
    const history = getSearchHistory();

    const validHistory = history.filter(search => 
      search && search.query && Array.isArray(search.results));
    setSearchHistory(validHistory);
    
    // Load profile data
    const profile = getUserProfilo();
    setProfileKeywords(profile.keywords);

    setIsLoading(false);
  }, []);

  // Clear profile data
  const clearProfile = () => {
    localStorage.removeItem('searchProfile');
    setProfileKeywords([]);
    //alert('Your search profile has been cleared');
  };

  // Toggle search selection
  const toggleSearchSelection = (index) => {
    setSelectedSearches(prev => 
      prev.includes(index) 
        ? prev.filter(i => i !== index)
        : [...prev, index]
    );
  };

  // Merge selected searches (union)
  const mergeSelectedSearches = () => {
    if (selectedSearches.length < 2) return;
    setActiveOperation('merge');
    
    const mergedResults = [];
    const seenUrls = new Set();
    const queries = [];
    
    selectedSearches.forEach(index => {
      const search = searchHistory[index];
      queries.push(search.query);
      
      search.results.forEach(result => {
        if (!seenUrls.has(result.url)) {
          seenUrls.add(result.url);
          // Preserve all result data
          mergedResults.push({
            ...result,
          });
        }
      });
    });

    // Sort merged results by score in descending order
    mergedResults.sort((a, b) => (b.score || 0) - (a.score || 0));
    
    navigate('/results', {
      state: {
        searchData: {
          query: `Combined from: ${queries.join(', ')}`,
          results: mergedResults,
          isHistoryOperation: true,
          operationType: 'merge'
        }
      }
    });
  };

  // Find common results (intersection)
  const intersectSelectedSearches = () => {
    if (selectedSearches.length < 2) return;
    setActiveOperation('intersect');
    
    const urlCounts = new Map();
    const queries = [];
    
    selectedSearches.forEach(index => {
      const search = searchHistory[index];
      queries.push(search.query);
      
      const seenInThisSearch = new Set();
      search.results.forEach(result => {
        if (!seenInThisSearch.has(result.url)) {
          seenInThisSearch.add(result.url);
          urlCounts.set(result.url, (urlCounts.get(result.url) || 0) + 1);
        }
      });
    });
    
    const commonResults = searchHistory[selectedSearches[0]].results
      .filter(result => urlCounts.get(result.url) === selectedSearches.length)
      .map(result => ({
        ...result,
      }));

      // Sort merged results by score in descending order
    commonResults.sort((a, b) => (b.score || 0) - (a.score || 0));
    
    navigate('/results', {
      state: {
        searchData: {
          query: `Common in: ${queries.join(', ')}`,
          results: commonResults,
          isHistoryOperation: true,
          operationType: 'intersect'
        }
      }
    });
  };

  // Search within specific results
  const searchWithinResults = (searchIndex) => {
    const search = searchHistory[searchIndex];
    const query = searchWithinQueries[searchIndex] || '';
    
    if (!query.trim()) return;
    setActiveOperation('filter');
    
    const filteredResults = search.results
      .filter(result => 
        result.title.toLowerCase().includes(query.toLowerCase()) ||
        (result.keywordsWithFrequency?.some(kw => 
          kw.keyword.toLowerCase().includes(query.toLowerCase())))
      )
      .map(result => ({
        ...result
      }));
    
    navigate('/results', {
      state: {
        searchData: {
          query: `Filtered "${query}" within: ${search.query}`,
          results: filteredResults,
          isHistoryOperation: true,
          operationType: 'filter'
        }
      }
    });
  };

  // Handle input change for search within
  const handleSearchWithinChange = (index, value) => {
    setSearchWithinQueries(prev => ({
      ...prev,
      [index]: value
    }));
  };

  // Clear all history
  const handleClearHistory = () => {
    clearSearchHistory();
    setSearchHistory([]);
  };

  // View original results with all data
  const handleSearchAgain = (search) => {
    setActiveOperation('view');
    navigate('/results', {
      state: {
        searchData: {
          query: search.query,
          results: search.results || [],
          isHistoryOperation: true,
          operationType: 'view'
        }
      }
    });
  };

  // Format date display
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown date';
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return 'Invalid date';
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Search History</h1>
          <div className="flex gap-4">
            {searchHistory.length > 0 && (
              <button
                onClick={handleClearHistory}
                className="flex items-center gap-2 text-red-600 hover:text-red-800 transition-colors"
              >
                <FiTrash2 /> Clear History
              </button>
            )}
            {profileKeywords.length > 0 && (
              <button
                onClick={clearProfile}
                className="flex items-center gap-2 text-purple-600 hover:text-purple-800 transition-colors"
              >
                <FiUser /> Clear Profile
              </button>
            )}
          </div>
        </div>

        {/* Profile Display Section */}
        {profileKeywords.length > 0 && (
          <div className="bg-purple-50 p-4 rounded-lg mb-6 border border-purple-100">
            <div className="flex justify-between items-start">
              <div>
                <h3 className="font-medium text-purple-800 mb-2 flex items-center gap-2">
                  <FiUser /> Your Search Profile
                </h3>
                <p className="text-sm text-purple-600 mb-3">
                  Based on pages you've clicked, showing your most frequent keywords
                </p>
                <div className="flex flex-wrap gap-2">
                  {profileKeywords.map((kw, i) => (
                    <span 
                      key={i} 
                      className="bg-purple-100 text-purple-800 px-3 py-1 rounded-full text-sm flex items-center"
                    >
                      <span className="font-medium">{kw.keyword}</span>
                      <span className="text-xs ml-1 bg-purple-200 text-purple-800 rounded-full px-1.5 py-0.5">
                        {kw.frequency}
                      </span>
                    </span>
                  ))}
                </div>
              </div>
              
            </div>
          </div>
        )}

        {/* Operation Guidance */}
        {activeOperation && (
          <div className="bg-blue-50 p-4 rounded-lg mb-6 flex items-start gap-3">
            <FiInfo className="text-blue-600 mt-0.5 flex-shrink-0" />
            <div>
              {activeOperation === 'merge' && (
                <p>Showing combined results from multiple searches (all unique pages)</p>
              )}
              {activeOperation === 'intersect' && (
                <p>Showing only pages that appeared in all selected searches</p>
              )}
              {activeOperation === 'filter' && (
                <p>Showing filtered results based on your additional search term</p>
              )}
              {activeOperation === 'view' && (
                <p>Showing original search results</p>
              )}
            </div>
          </div>
        )}

        {/* Operation Controls */}
        {selectedSearches.length > 0 && (
          <div className="bg-blue-50 p-4 rounded-lg mb-6">
            <h3 className="font-medium text-blue-800 mb-3 flex items-center gap-2">
              <FiInfo /> Operations for {selectedSearches.length} selected searches
            </h3>
            <div className="flex flex-wrap gap-3">
              <button
                onClick={mergeSelectedSearches}
                className="flex items-center gap-2 px-3 py-1.5 bg-blue-100 hover:bg-blue-200 text-blue-800 rounded-md text-sm font-medium"
                title="Combine all results from selected searches"
              >
                <FiPlus /> Combine Results
              </button>
              <button
                onClick={intersectSelectedSearches}
                className="flex items-center gap-2 px-3 py-1.5 bg-blue-100 hover:bg-blue-200 text-blue-800 rounded-md text-sm font-medium"
                title="Show only pages that appear in all selected searches"
              >
                <FiMinus /> Find Common Pages
              </button>
              <button
                onClick={() => setSelectedSearches([])}
                className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-md text-sm font-medium"
              >
                <FiX /> Clear Selection
              </button>
            </div>
          </div>
        )}

        {/* History List */}
        {searchHistory.length === 0 ? (
          <div className="bg-white p-8 rounded-lg shadow-md text-center">
            <FiClock className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-700">No search history yet</h3>
            <p className="text-gray-500 mt-2">Your search queries will appear here once you start searching.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {searchHistory.map((search, index) => {
              const resultCount = search.results?.length || 0;
              const isSelected = selectedSearches.includes(index);
              
              return (
                <div 
                  key={index} 
                  className={`bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow border ${
                    isSelected ? 'border-blue-500 bg-blue-50' : 'border-transparent'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <button
                          onClick={() => toggleSearchSelection(index)}
                          className={`w-5 h-5 rounded border flex items-center justify-center ${
                            isSelected ? 'bg-blue-500 border-blue-500 text-white' : 'border-gray-300'
                          }`}
                          aria-label={isSelected ? 'Deselect search' : 'Select search'}
                        >
                          {isSelected && '✓'}
                        </button>
                        <FiSearch className="text-gray-400" />
                        <h3 className="font-medium text-gray-900">{search.query || 'Unknown query'}</h3>
                      </div>
                      <p className="text-sm text-gray-500 mb-2">
                        {resultCount} results • {formatDate(search.timestamp)}
                      </p>
                      
                      {/* Independent search input for each item */}
                      <div className="flex gap-2 mt-2">
                        <input
                          type="text"
                          placeholder="Search within these results stemmed keywords..."
                          value={searchWithinQueries[index] || ''}
                          onChange={(e) => handleSearchWithinChange(index, e.target.value)}
                          className="flex-1 px-3 py-1 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                        />
                        <button
                          onClick={() => searchWithinResults(index)}
                          className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 flex items-center gap-1"
                          disabled={!searchWithinQueries[index]}
                        >
                          <FiFilter size={16} /> Filter
                        </button>
                      </div>
                    </div>
                    
                    <div className="flex flex-col gap-2">
                      <button
                        onClick={() => handleSearchAgain(search)}
                        className="flex items-center gap-1 text-blue-600 hover:text-blue-800 transition-colors"
                        title="View original results with all details"
                      >
                        View <FiArrowRight />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default History;