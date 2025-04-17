import React from 'react';
import { FiSearch, FiX, FiBarChart2, FiZap } from 'react-icons/fi';

const SearchBar = ({ 
  query, 
  setQuery, 
  selectedKeywords = [], 
  setSelectedKeywords = () => {}, 
  onSearch,
  onExtendedSearch,
  showTitle = true,
  usePageRank,
  setUsePageRank,
  searchOperator,
  setSearchOperator
}) => {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch();
  };

  const handleExtendedSearch = (e) => {
    e.preventDefault();
    onExtendedSearch();
  };

  const removeKeyword = (keyword) => {
    setSelectedKeywords(prev => prev.filter(k => k !== keyword));
  };

  return (
    <div className="relative max-w-3xl mx-auto">
      {showTitle && (
        <div className="text-center mb-4">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">
            <span className="text-blue-600">Browse</span>Bot
          </h1>
          <p className="text-lg text-gray-600">Search across indexed pages</p>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Search input container */}
        <div className="relative flex items-center bg-white rounded-full shadow-md border border-gray-200 hover:border-blue-300 transition-colors duration-200 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-200">
          {/* Search icon */}
          <div className="absolute left-4 text-gray-400">
            <FiSearch size={24} />
          </div>
          
          {/* Input field */}
          <div className="flex-1 flex items-center flex-wrap gap-2 pl-12 pr-4 py-3">
            {selectedKeywords.map(keyword => (
              <div key={keyword} className="bg-blue-50 px-3 py-1 rounded-full flex items-center group">
                <span className="text-blue-700 text-sm">{keyword}</span>
                <button
                  type="button"
                  onClick={() => removeKeyword(keyword)}
                  className="ml-1.5 text-blue-500 hover:text-blue-700 transition-colors"
                >
                  <FiX size={16} />
                </button>
              </div>
            ))}
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={selectedKeywords.length ? '' : "Search anything..."}
              className="flex-1 min-w-[100px] outline-none text-gray-800 placeholder-gray-400 bg-transparent"
            />
          </div>

          {/* Action buttons */}
          <div className="flex items-center pr-2 gap-2">
            {query && (
              <button
                type="button"
                onClick={() => setQuery('')}
                className="p-1 text-gray-400 hover:text-gray-600"
              >
                <FiX size={20} />
              </button>
            )}
            <button
              type="submit"
              className="p-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 transition-colors"
            >
              <FiSearch size={20} />
            </button>
          </div>
        </div>

        {/* Visual divider */}
        <div className="relative my-3">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-200"></div>
          </div>
          <div className="relative flex justify-center">
            <span className="px-2 bg-white text-xs text-gray-500">
              Advanced Search Options
            </span>
          </div>
        </div>

        {/* Compact options row */}
        <div className="flex flex-wrap items-center justify-between gap-3 mt-3">
          {/* Boolean operator section */}
          <div className="bg-blue-100 border border-blue-200 rounded-lg p-2 flex-1">
            <div className="flex items-center">
              <div className="flex items-center space-x-2 mr-3">
                <span className="text-sm font-medium text-blue-800 whitespace-nowrap flex items-center">
                  <FiZap className="mr-1" /> Boolean Mode:
                </span>
                <label className="inline-flex items-center">
                  <input
                    type="radio"
                    checked={searchOperator === 'AND'}
                    onChange={() => setSearchOperator('AND')}
                    className="form-radio h-4 w-4 text-blue-600"
                  />
                  <span className="ml-1 text-sm whitespace-nowrap">AND</span>
                </label>
                <label className="inline-flex items-center">
                  <input
                    type="radio"
                    checked={searchOperator === 'OR'}
                    onChange={() => setSearchOperator('OR')}
                    className="form-radio h-4 w-4 text-blue-600"
                  />
                  <span className="ml-1 text-sm whitespace-nowrap">OR</span>
                </label>
              </div>
              <button
                onClick={handleExtendedSearch}
                className="px-3 py-1 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm font-medium whitespace-nowrap flex items-center"
              >
                <FiZap className="mr-1" /> Perform Soft Boolean Search
              </button>
            </div>
            <p className="text-xs text-blue-600 mt-1 pl-4">
              Select AND/OR, then click to apply soft boolean search (phrase not supported)
            </p>
          </div>

          {/* PageRank section */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-2">
            <label className="inline-flex items-center">
              <input
                type="checkbox"
                checked={usePageRank}
                onChange={() => setUsePageRank(!usePageRank)}
                className="form-checkbox h-4 w-4 text-blue-600 rounded"
              />
              <span className="ml-1 text-sm flex items-center whitespace-nowrap">
                <FiBarChart2 className="mr-1" /> <strong>PageRank</strong> (default active)
              </span>
            </label>
            <p className="text-xs text-gray-500 mt-1">
              Improves ranking for all searches
            </p>
          </div>
        </div>

        
      </form>
    </div>
  );
};

export default SearchBar;