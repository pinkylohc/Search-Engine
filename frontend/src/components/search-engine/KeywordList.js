import React, { useState, useEffect, useMemo } from 'react';
import { FaSpinner, FaSearch, FaChevronLeft, FaChevronRight } from 'react-icons/fa';

const KeywordList = ({ keywords, loading, selectedKeywords, setSelectedKeywords }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(50);
  const [alphabetFilter, setAlphabetFilter] = useState(null);

  // Memoize filtered keywords to prevent unnecessary recalculations
  const filteredKeywords = useMemo(() => {
    if(!keywords) return [];
    return keywords.filter(keyword => {
      if (!keyword) return false;
      
      const matchesSearch = keyword.toLowerCase().includes(searchTerm.toLowerCase());
      
      if (!alphabetFilter) return matchesSearch;
      
      const firstChar = keyword.charAt(0).toLowerCase();
      
      if (alphabetFilter === '0-9') {
        return matchesSearch && /^[0-9]/.test(firstChar);
      } else if (alphabetFilter === 'symbols') {
        return matchesSearch && /^[^a-z0-9]/.test(firstChar);
      } else {
        return matchesSearch && firstChar === alphabetFilter.toLowerCase();
      }
    });
  }, [keywords, searchTerm, alphabetFilter]);

  // Calculate pagination data
  const paginationData = useMemo(() => {
    const totalItems = filteredKeywords.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const indexOfLastItem = currentPage * itemsPerPage;
    const indexOfFirstItem = indexOfLastItem - itemsPerPage;
    const currentItems = filteredKeywords.slice(indexOfFirstItem, indexOfLastItem);

    return {
      totalItems,
      totalPages,
      currentItems,
      indexOfFirstItem,
      indexOfLastItem
    };
  }, [filteredKeywords, currentPage, itemsPerPage]);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, alphabetFilter, itemsPerPage]);

  const toggleKeyword = (keyword) => {
    setSelectedKeywords(prev =>
      prev.includes(keyword)
        ? prev.filter(k => k !== keyword)
        : [...prev, keyword]
    );
  };

  // Generate navigation items including numbers and symbols
  const generateNavigationItems = () => {
    const letters = [...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'];
    const specialCategories = [
      { id: '0-9', label: '0-9' },
      { id: 'symbols', label: '#$%' }
    ];

    return [
      { id: null, label: 'All' },
      ...specialCategories,
      ...letters.map(letter => ({ id: letter.toLowerCase(), label: letter }))
    ];
  };

  const navigationItems = generateNavigationItems();

  // Handle page change with boundary checks
  const handlePageChange = (newPage) => {
    if (newPage < 1) newPage = 1;
    if (newPage > paginationData.totalPages) newPage = paginationData.totalPages;
    setCurrentPage(newPage);
  };

  // Generate pagination buttons with smart truncation
  const renderPaginationButtons = () => {
    const buttons = [];
    const { totalPages } = paginationData;
    const maxVisibleButtons = 5;
    let startPage, endPage;

    if (totalPages <= maxVisibleButtons) {
      startPage = 1;
      endPage = totalPages;
    } else {
      const maxVisibleBeforeCurrent = Math.floor(maxVisibleButtons / 2);
      const maxVisibleAfterCurrent = Math.ceil(maxVisibleButtons / 2) - 1;
      
      if (currentPage <= maxVisibleBeforeCurrent) {
        startPage = 1;
        endPage = maxVisibleButtons;
      } else if (currentPage + maxVisibleAfterCurrent >= totalPages) {
        startPage = totalPages - maxVisibleButtons + 1;
        endPage = totalPages;
      } else {
        startPage = currentPage - maxVisibleBeforeCurrent;
        endPage = currentPage + maxVisibleAfterCurrent;
      }
    }

    // First page button
    if (startPage > 1) {
      buttons.push(
        <button
          key={1}
          onClick={() => handlePageChange(1)}
          className="w-10 h-10 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          1
        </button>
      );
      if (startPage > 2) {
        buttons.push(<span key="left-ellipsis" className="px-2">...</span>);
      }
    }

    // Page number buttons
    for (let i = startPage; i <= endPage; i++) {
      buttons.push(
        <button
          key={i}
          onClick={() => handlePageChange(i)}
          className={`w-10 h-10 rounded ${currentPage === i ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          {i}
        </button>
      );
    }

    // Last page button
    if (endPage < totalPages) {
      if (endPage < totalPages - 1) {
        buttons.push(<span key="right-ellipsis" className="px-2">...</span>);
      }
      buttons.push(
        <button
          key={totalPages}
          onClick={() => handlePageChange(totalPages)}
          className="w-10 h-10 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          {totalPages}
        </button>
      );
    }

    return buttons;
  };

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-4">
        <h2 className="text-xl font-semibold text-gray-800">
          Stemmed Keywords
          {selectedKeywords.length > 0 && (
            <span className="ml-2 text-sm text-gray-500">
              ({selectedKeywords.length} selected)
            </span>
          )}
        </h2>
        
        <div className="relative w-full sm:w-64">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <FaSearch className="text-gray-400" />
          </div>
          <input
            type="text"
            placeholder="Search keywords..."
            className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg bg-white shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Enhanced Character Navigation */}
      <div className="flex flex-wrap gap-1 mb-4 overflow-x-auto py-2">
        {navigationItems.map(item => (
          <button
            key={item.id || 'all'}
            onClick={() => setAlphabetFilter(item.id)}
            className={`px-2 py-1 text-xs rounded min-w-[2rem] text-center ${
              alphabetFilter === item.id 
                ? 'bg-blue-600 text-white' 
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            } ${
              item.id === 'symbols' ? 'font-mono' : ''
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {/* Items per page selector */}
      <div className="flex items-center mb-4">
        <span className="text-sm text-gray-600 mr-2">Show:</span>
        <select
          value={itemsPerPage}
          onChange={(e) => setItemsPerPage(Number(e.target.value))}
          className="border border-gray-300 rounded px-2 py-1 text-sm"
        >
          <option value={25}>25</option>
          <option value={50}>50</option>
          <option value={100}>100</option>
          <option value={200}>200</option>
          <option value={500}>500</option>
        </select>
        <span className="text-sm text-gray-600 ml-2">items per page</span>
      </div>

      {loading ? (
        <div className="flex justify-center py-6">
          <FaSpinner className="animate-spin text-blue-500 text-2xl" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-2 mb-4">
            {paginationData.currentItems.length > 0 ? (
              paginationData.currentItems.map(keyword => (
                <button
                  key={keyword}
                  onClick={() => toggleKeyword(keyword)}
                  className={`p-2 rounded-lg text-sm transition-all ${
                    selectedKeywords.includes(keyword)
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {keyword}
                </button>
              ))
            ) : (
              <div className="col-span-3 text-center py-4 text-gray-500">
                No keywords found matching your criteria
              </div>
            )}
          </div>

          {/* Pagination controls */}
          {paginationData.totalItems > 0 && (
            <div className="flex flex-col sm:flex-row justify-between items-center mt-4 gap-4">
              <div className="text-sm text-gray-600">
                Showing {paginationData.indexOfFirstItem + 1}-{Math.min(paginationData.indexOfLastItem, paginationData.totalItems)} of {paginationData.totalItems} keywords
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  className={`p-2 rounded ${currentPage === 1 ? 'bg-gray-200 text-gray-400 cursor-not-allowed' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
                >
                  <FaChevronLeft />
                </button>
                
                {renderPaginationButtons()}

                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === paginationData.totalPages}
                  className={`p-2 rounded ${currentPage === paginationData.totalPages ? 'bg-gray-200 text-gray-400 cursor-not-allowed' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
                >
                  <FaChevronRight />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default KeywordList;