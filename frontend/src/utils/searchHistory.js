// utils/searchHistory.js
export const saveSearch = (query, results) => {
    try {
        // First try to save normally
        return trySaveSearch(query, results);
    } catch (e) {
        if (e.name === 'QuotaExceededError') {
            // If storage is full, make space and try again
            return handleStorageFull(query, results);
        }
        console.error('Error saving search history:', e);
    }
};

const trySaveSearch = (query, results) => {
    const searches = getSearchHistory();
    
    if (!query || !Array.isArray(results) || results.length == null) {
        //console.error('Invalid search data - not saving to history');
        return;
    }

    const newSearch = {
        query: String(query),
        // Store only minimal result data
        results: results || [],
        timestamp: new Date().toISOString()
    };
    
    const updatedSearches = [newSearch, ...searches];
    const trimmedSearches = updatedSearches.slice(0, 50); // Keep reasonable limit
    
    localStorage.setItem('searchHistory', JSON.stringify(trimmedSearches));
    return trimmedSearches;
};

const handleStorageFull = (query, results) => {
    let searches = getSearchHistory();
    
    // Remove 10% of oldest records or at least 1
    const removeCount = Math.max(1, Math.floor(searches.length * 0.5));
    const trimmedSearches = searches.slice(0, searches.length - removeCount);
    
    // Try to save again with reduced history
    localStorage.setItem('searchHistory', JSON.stringify(trimmedSearches));
    
    // Now attempt to save the new search again
    return trySaveSearch(query, results);
};

export const getSearchHistory = () => {
    try {
        const history = localStorage.getItem('searchHistory');
        return history ? JSON.parse(history) : [];
    } catch (e) {
        console.error('Error parsing search history', e);
        return [];
    }
};

export const clearSearchHistory = () => {
    localStorage.removeItem('searchHistory');
};