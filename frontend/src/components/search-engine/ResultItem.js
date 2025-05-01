import React, { memo, useState } from 'react';

const ResultItem = memo(({ result, index, onSimilarPagesClick }) => {
  const [showAllParentLinks, setShowAllParentLinks] = useState(false);
  const [showAllChildLinks, setShowAllChildLinks] = useState(false);

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      timeZoneName: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handlePageClick = () => {
    if (result.keywordsWithFrequency) {
      // Get current profile from localStorage
      const currentProfile = JSON.parse(localStorage.getItem('searchProfile')) || { 
        keywords: [],
        lastUpdated: Date.now() // Add timestamp for the entire profile
      };
      
      // Get top 5 keywords from this result
      const topKeywords = result.keywordsWithFrequency.slice(0, 5);
      
      // Update profile with new keywords
      const updatedKeywords = [...currentProfile.keywords];
      
      const currentTime = Date.now();
      
      topKeywords.forEach(kw => {
        const existingIndex = updatedKeywords.findIndex(item => item.keyword === kw.keyword);
        if (existingIndex >= 0) {
          // Keyword exists - update frequency and check if we should update URL
          updatedKeywords[existingIndex].frequency += 1;
          
          // If same frequency as others, update to most recent URL
          if (updatedKeywords[existingIndex].frequency === kw.frequency) {
            updatedKeywords[existingIndex] = {
              ...updatedKeywords[existingIndex],
              lastUrl: result.url, // Store the most recent URL
              lastUpdated: currentTime // Store when this was updated
            };
          }
        } else {
          // New keyword - add with initial data
          updatedKeywords.push({ 
            keyword: kw.keyword,
            frequency: 1,
            lastUrl: result.url, // Store the URL where this keyword was found
            lastUpdated: currentTime // Store when this was added
          });
        }
      });
  
      // Sort by frequency (descending) and then by lastUpdated (descending)
      const sortedKeywords = updatedKeywords.sort((a, b) => {
        // First sort by frequency (higher frequency comes first)
        if (b.frequency !== a.frequency) {
          return b.frequency - a.frequency;
        }
        // If frequencies are equal, sort by most recent
        return b.lastUpdated - a.lastUpdated;
      }).slice(0, 10); // Keep top 20 keywords
      
      // Save back to localStorage
      localStorage.setItem('searchProfile', JSON.stringify({ 
        keywords: sortedKeywords,
        lastUpdated: currentTime
      }));
    }
  };

  // handle get similar page button
  const handleSimilarPages = () => {
    if (result.keywordsWithFrequency && result.keywordsWithFrequency.length > 0) {
      const topKeywords = result.keywordsWithFrequency
        .slice(0, 5)
        .map(kw => kw.keyword);
      onSimilarPagesClick(topKeywords);
    }
  };

  return (
    <div 
      className="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow mb-4 border border-gray-100"
    >
      <div className="flex justify-between items-start mb-4">
        <div className="flex-1 min-w-0"> {/* Added min-w-0 to prevent overflow issues */}
          <div className="flex items-center mb-1">
            <span className="bg-blue-100 text-blue-800 text-sm font-medium px-2.5 py-0.5 rounded-full mr-2">
              #{index + 1}
            </span>
            <h2 className="text-xl font-semibold text-blue-700 hover:text-blue-800 transition-colors truncate">
              <a href={result.url} target="_blank" rel="noopener noreferrer" className="flex items-center" onClick={handlePageClick}>
                {result.title}
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 ml-1 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
              </a>
            </h2>
          </div>
          
          {/* URL with better overflow handling */}
          <div className="mb-3"
          onClick={handlePageClick}>
            <a 
              href={result.url} 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline text-sm break-all block truncate"
              title={result.url}
            >
              {result.url}
            </a>
          </div>
          
          {/* Metadata in compact grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-sm text-gray-600 mb-4">
            <div className="flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span className="truncate">{formatDate(result.lastModified)}</span>
            </div>
            <div className="flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
              </svg>
              <span>{result.size} Bytes</span>
            </div>
            <div className="flex items-center col-span-2 sm:col-span-1">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
              </svg>
              <span>Score: <span className="font-medium">{result.score}</span></span>
            </div>
          </div>
        </div>
      </div>

      {result.keywordsWithFrequency && (
        <div className="mb-4">
          <div className="flex justify-between items-center mb-2">
            <p className="text-sm font-medium text-gray-700">Top Keywords:</p>
            <button
              onClick={handleSimilarPages}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-full text-xs font-medium transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7v8a7 7 0 0014 0v-8a7 7 0 00-14 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 11a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
              Get similar pages
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {result.keywordsWithFrequency.slice(0, 5).map((kw, i) => (
              <span key={i} className="bg-gray-100 text-gray-800 text-xs px-2.5 py-1 rounded-full flex items-center">
                <span className="font-medium">{kw.keyword}</span>
                <span className="text-gray-500 text-xs ml-1">({kw.frequency})</span>
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="space-y-4">
        {result.parentLinks && result.parentLinks.length > 0 && (
          <div className="border-t pt-3">
            <div className="flex justify-between items-center mb-2">
              <p className="text-sm font-medium text-gray-700">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                </svg>
                Parent Links ({result.parentLinks.length})
              </p>
              {result.parentLinks.length > 5 && (
                <button 
                  onClick={() => setShowAllParentLinks(!showAllParentLinks)}
                  className="text-blue-600 hover:text-blue-800 text-xs flex items-center"
                >
                  {showAllParentLinks ? (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                      </svg>
                      Show Less
                    </>
                  ) : (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                      Show All
                    </>
                  )}
                </button>
              )}
            </div>
            <ul className="space-y-1.5">
              {(showAllParentLinks ? result.parentLinks : result.parentLinks.slice(0, 5)).map((link, i) => (
                <li key={i} className="flex">
                  <span className="text-gray-400 text-xs mr-2 mt-1">{i + 1}.</span>
                  <a 
                    href={link} 
                    target="_blank" 
                    rel="noopener noreferrer" 
                    className="text-blue-600 hover:underline text-sm break-all flex-1"
                    title={link}
                  >
                    {link}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}

        {result.childLinks && result.childLinks.length > 0 && (
          <div className="border-t pt-3">
            <div className="flex justify-between items-center mb-2">
              <p className="text-sm font-medium text-gray-700">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 5l7 7-7 7M5 5l7 7-7 7" />
                </svg>
                Child Links ({result.childLinks.length})
              </p>
              {result.childLinks.length > 5 && (
                <button 
                  onClick={() => setShowAllChildLinks(!showAllChildLinks)}
                  className="text-blue-600 hover:text-blue-800 text-xs flex items-center"
                >
                  {showAllChildLinks ? (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                      </svg>
                      Show Less
                    </>
                  ) : (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                      Show All
                    </>
                  )}
                </button>
              )}
            </div>
            <ul className="space-y-1.5">
              {(showAllChildLinks ? result.childLinks : result.childLinks.slice(0, 5)).map((link, i) => (
                <li key={i} className="flex">
                  <span className="text-gray-400 text-xs mr-2 mt-1">{i + 1}.</span>
                  <a 
                    href={link} 
                    target="_blank" 
                    rel="noopener noreferrer" 
                    className="text-blue-600 hover:underline text-sm break-all flex-1"
                    title={link}
                  >
                    {link}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
});

export default ResultItem;