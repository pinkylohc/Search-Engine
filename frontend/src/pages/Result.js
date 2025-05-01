import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import ResultItem from '../components/search-engine/ResultItem';
import SearchBar from '../components/search-engine/SearchBar';
import axios from 'axios';
import { saveSearch } from '../utils/searchHistory';
import { FiFilter, FiSearch, FiX, FiCalendar, FiHardDrive, FiStar, FiLink } from 'react-icons/fi';

const Result = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchData = location.state?.searchData || {};
  const { 
    query = '', 
    results: initialResults = [], 
    usedPageRank = true,
    operator = 'AND',
    searchType = 'standard' 
  } = searchData;
  
  const [Requery, setQuery] = useState(query || '');
  const [showFilters, setShowFilters] = useState(false);
  const [filteredResults, setFilteredResults] = useState(initialResults);
  const [usePageRank, setUsePageRank] = useState(true);
  const [searchOperator, setSearchOperator] = useState(operator);
  const [currentSearchType, setCurrentSearchType] = useState(searchType);
  const [triggerSearch, setTriggerSearch] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [initialPageRank, setInitialPageRank] = useState(usedPageRank);
  const [initialOperator, setInitialOperator] = useState(operator);

  // Filter states
  const [filters, setFilters] = useState({
    minSize: '',
    maxSize: '',
    dateRange: 'all',
    minScore: '',
    hasParentLinks: false,
    hasChildLinks: false,
    keywordFilter: ''
  });

  // Apply filters whenever filters or initialResults change
  useEffect(() => {
    const filtered = initialResults.filter(result => {
      // Size filter
      if (filters.minSize && result.size < filters.minSize) return false;
      if (filters.maxSize && result.size > filters.maxSize) return false;
      
      // Date filter
      if (filters.dateRange !== 'all') {
        const resultDate = new Date(result.lastModified);
        const now = new Date();
        let cutoffDate = new Date();
        
        switch (filters.dateRange) {
          case 'day': cutoffDate.setDate(now.getDate() - 1); break;
          case 'week': cutoffDate.setDate(now.getDate() - 7); break;
          case 'month': cutoffDate.setMonth(now.getMonth() - 1); break;
          case 'year': cutoffDate.setFullYear(now.getFullYear() - 1); break;
          case '2years': cutoffDate.setFullYear(now.getFullYear() - 2); break;
          default: break;
        }
        
        if (resultDate < cutoffDate) return false;
      }
      
      // Score filter
      if (filters.minScore && result.score < parseFloat(filters.minScore)) return false;
      
      // Link filters
      if (filters.hasParentLinks && (!result.parentLinks || result.parentLinks.length === 0)) return false;
      if (filters.hasChildLinks && (!result.childLinks || result.childLinks.length === 0)) return false;
      
      // Keyword filter
      if (filters.keywordFilter) {
        const searchTerm = filters.keywordFilter.toLowerCase();
        const inTitle = result.title.toLowerCase().includes(searchTerm);
        const inKeywords = result.keywordsWithFrequency?.some(kw => 
          kw.keyword.toLowerCase().includes(searchTerm)
        );
        if (!inTitle && !inKeywords) return false;
      }
      
      return true;
    });
    
    setFilteredResults(filtered);
  }, [initialResults, filters]);

  const handleSearch = useCallback(async () => {
    setIsLoading(true);
    try {
      const endpoint = currentSearchType === 'extended' 
        ? '/search/extended-boolean' 
        : '/search/query';
      
      const params = {
        query: Requery,
        usePageRank: usePageRank
      };

      if (currentSearchType === 'extended') {
        params.operator = searchOperator;
      }

      const response = await axios.get(`http://localhost:8080${endpoint}`, { params });
      
      saveSearch(Requery, response.data);

      setInitialPageRank(usePageRank);
      setInitialOperator(searchOperator);
      
      navigate('/results', { 
        state: { 
          searchData: { 
            query: Requery, 
            results: response.data,
            usedPageRank: usePageRank,
            operator: searchOperator,
            searchType: currentSearchType
          } 
        },
        replace: true
      });
    } catch (error) {
      console.error('Search error:', error);
    } finally {
      setIsLoading(false);
    }
  }, [Requery, navigate, usePageRank, searchOperator, currentSearchType]);

  useEffect(() => {
    if (triggerSearch) {
      handleSearch();
      setTriggerSearch(false);
    }
  }, [Requery, triggerSearch, handleSearch]);

  const handleSimilarPagesClick = useCallback((keywords) => {
    const combinedQuery = `${Requery} ${keywords.join(' ')}`.trim();
    setQuery(combinedQuery);
    setTriggerSearch(true);
  }, [Requery]);

  const resetFilters = () => {
    setFilters({
      minSize: '',
      maxSize: '',
      dateRange: 'all',
      minScore: '',
      hasParentLinks: false,
      hasChildLinks: false,
      keywordFilter: ''
    });
  };

  const formatResultCount = useMemo(() => {
    if (filteredResults.length === initialResults.length) {
      return `${initialResults.length} ${initialResults.length === 1 ? 'result' : 'results'}`;
    }
    return `${filteredResults.length} of ${initialResults.length} results`;
  }, [filteredResults.length, initialResults.length]);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header Section */}
      <div className="bg-gradient-to-b from-blue-50 to-white shadow-sm">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          {/* Search Bar with extended search controls */}
          <div className="mb-4">
            <SearchBar 
              query={Requery}
              setQuery={setQuery}
              onSearch={() => {
                setCurrentSearchType('standard');
                setTriggerSearch(true);
              }}
              onExtendedSearch={() => {
                setCurrentSearchType('extended');
                setTriggerSearch(true);
              }}
              showTitle={false}
              usePageRank={usePageRank}
              setUsePageRank={setUsePageRank}
              searchOperator={searchOperator}
              setSearchOperator={setSearchOperator}
              isLoading={isLoading}
            />
          </div>

          {/* Results Summary and Controls */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Results for: <span className="text-blue-600">'{query}'</span>
              </h1>
              <div className="flex flex-wrap items-center gap-2 mt-1">
                {isLoading ? (
                  <span className="text-sm text-gray-600 flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Loading new results...
                  </span>
                ) : (
                  <>
                    <span className="text-sm text-gray-600">
                      {formatResultCount}
                    </span>
                    <span className="text-sm px-2 py-1 rounded bg-blue-100 text-blue-800">
                      {currentSearchType === 'extended' ? 'Extended Boolean' : 'Standard'} Search
                    </span>
                    {currentSearchType === 'extended' && (
                      <span className="text-sm px-2 py-1 rounded bg-purple-100 text-purple-800">
                        {initialOperator} operator
                      </span>
                    )}
                    <span className="text-sm px-2 py-1 rounded bg-green-100 text-green-800">
                      PageRank {initialPageRank ? 'ON' : 'OFF'}
                    </span>
                    {filteredResults.length !== initialResults.length && (
                      <button 
                        onClick={resetFilters}
                        className="text-sm text-blue-600 hover:underline"
                      >
                        (Clear filters)
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>
            
            <div className="flex gap-2">
              <button
                onClick={() => setShowFilters(!showFilters)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg ${
                  showFilters ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 border border-gray-300'
                } shadow-sm hover:shadow-md transition-all`}
              >
                <FiFilter size={16} />
                <span className="text-sm font-medium">
                  {showFilters ? 'Hide Filters' : 'Filters'}
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Filter Panel */}
        {showFilters && (
          <div className="bg-white p-6 rounded-lg shadow-md mb-8 border border-gray-200">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* Size Filter */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <FiHardDrive className="text-gray-500" /> Size (Bytes)
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <input
                      type="number"
                      placeholder="Min"
                      value={filters.minSize}
                      onChange={(e) => setFilters({...filters, minSize: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                      min="0"
                      step="1"
                    />
                  </div>
                  <div>
                    <input
                      type="number"
                      placeholder="Max"
                      value={filters.maxSize}
                      onChange={(e) => setFilters({...filters, maxSize: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                      min="0"
                      step="1"
                    />
                  </div>
                </div>
              </div>

              {/* Date Filter */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <FiCalendar className="text-gray-500" /> Date Modified
                </label>
                <select
                  value={filters.dateRange}
                  onChange={(e) => setFilters({...filters, dateRange: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="all">All time</option>
                  <option value="day">Last 24 hours</option>
                  <option value="week">Last week</option>
                  <option value="month">Last month</option>
                  <option value="year">Last year</option>
                  <option value="2years">Last 2 years</option>
                </select>
              </div>

              {/* Score Filter */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <FiStar className="text-gray-500" /> Minimum Score
                </label>
                <input
                  type="number"
                  placeholder="0.0"
                  min="0"
                  step="0.1"
                  value={filters.minScore}
                  onChange={(e) => setFilters({...filters, minScore: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              {/* Link Filters */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <FiLink className="text-gray-500" /> Links
                </label>
                <div className="flex flex-col gap-2">
                  <label className="flex items-center gap-2 text-sm text-gray-700">
                    <input
                      type="checkbox"
                      checked={filters.hasParentLinks}
                      onChange={(e) => setFilters({...filters, hasParentLinks: e.target.checked})}
                      className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    Has Parent Links
                  </label>
                  <label className="flex items-center gap-2 text-sm text-gray-700">
                    <input
                      type="checkbox"
                      checked={filters.hasChildLinks}
                      onChange={(e) => setFilters({...filters, hasChildLinks: e.target.checked})}
                      className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    Has Child Links
                  </label>
                </div>
              </div>

              {/* Keyword Filter */}
              <div className="space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <FiSearch className="text-gray-500" /> Keyword in Results
                </label>
                <input
                  type="text"
                  placeholder="Filter by keyword"
                  value={filters.keywordFilter}
                  onChange={(e) => setFilters({...filters, keywordFilter: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              {/* Reset Button */}
              <div className="flex items-end">
                <button
                  onClick={resetFilters}
                  className="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors text-sm font-medium"
                >
                  <FiX size={16} /> Reset All Filters
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Results List */}
        <div className="space-y-4">
          {isLoading ? (
            <div className="bg-white p-12 rounded-lg shadow-sm border border-gray-200 text-center">
              <div className="flex flex-col items-center justify-center">
                <svg className="animate-spin h-12 w-12 text-blue-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <h3 className="text-lg font-medium text-gray-700">Searching for '{Requery}'</h3>
                <p className="text-gray-500 mt-2">Please wait while we fetch your results...</p>
              </div>
            </div>
          ) : filteredResults.length > 0 ? (
            filteredResults.map((result, index) => (
              <ResultItem 
                key={index} 
                result={result} 
                index={index} 
                onSimilarPagesClick={handleSimilarPagesClick}
              />
            ))
          ) : (
            <div className="bg-white p-8 rounded-lg shadow-sm border border-gray-200 text-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3 className="text-lg font-medium text-gray-700 mt-4">
                {initialResults.length === 0 ? 'No results found' : 'No results match your filters'}
              </h3>
              <p className="text-gray-500 mt-2 max-w-md mx-auto">
                {initialResults.length === 0 
                  ? 'Try different keywords or check your spelling.'
                  : 'Try adjusting your filters or clear them to see all results.'
                }
              </p>
              {initialResults.length > 0 && (
                <button
                  onClick={resetFilters}
                  className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors text-sm font-medium"
                >
                  Clear All Filters
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Result;