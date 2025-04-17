import React, { useState, useEffect } from 'react';
import { FiTrendingUp, FiInfo, FiTrash2 } from 'react-icons/fi';
import axios from 'axios';

const HotTopics = ({ onSelect }) => {
  const [hotTopics, setHotTopics] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchHotTopics = async () => {
      try {
        const response = await axios.get('http://localhost:8080/search/hot-topic');
        setHotTopics(response.data);
      } catch (error) {
        console.error('Error fetching hot topics:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchHotTopics();
  }, []);

  const clearCache = async () => {
    try {
      await axios.post('http://localhost:8080/search/clean-cache');
      setHotTopics([]);
    } catch (error) {
      console.error('Error clearing cache:', error);
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6">
        <div className="flex items-center gap-2 mb-4">
          <FiTrendingUp className="text-orange-500 text-xl" />
          <h2 className="text-xl font-semibold text-gray-800">Loading Trends...</h2>
        </div>
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="w-full p-3 rounded-lg bg-gray-100 animate-pulse h-16"></div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <div className="flex items-center gap-2 mb-4">
        <FiTrendingUp className="text-orange-500 text-xl" />
        <h2 className="text-xl font-semibold text-gray-800">Trending Searches</h2>
      </div>

      <div className="flex justify-between items-center pb-2 border-t border-gray-100">
        <div className="relative group">
          <button 
            className="flex items-center gap-1 text-gray-500 hover:text-gray-700 transition-colors text-sm"
            aria-label="Information about trending searches"
          >
            <FiInfo />
            <span>About these trends</span>
          </button>
          <div className="absolute bottom-full left-0 mb-2 w-48 px-3 py-2 bg-gray-800 text-white text-xs rounded-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
            Five most frequent search queries from cache (limited to normal search plus PageRank results).
          </div>
        </div>
        <button
          onClick={clearCache}
          className="flex items-center gap-1 text-sm text-red-600 hover:text-red-800 transition-colors"
          aria-label="Clear cache"
        >
          <FiTrash2 />
          <span>Clear cache</span>
        </button>
      </div>
      
      
      <div className="space-y-3 mb-4">
        {hotTopics.map((topic, index) => (
          <button
            key={index}
            onClick={() => onSelect(topic.query)}
            className="w-full p-3 text-left rounded-lg hover:bg-orange-50 transition-colors border border-orange-100"
          >
            <div className="flex justify-between items-center">
              <h3 className="font-medium text-gray-800">
                {index + 1}. {topic.query}
              </h3>
              <span className="text-xs bg-orange-100 text-orange-800 px-2 py-1 rounded-full">
                {topic.frequency} searches
              </span>
            </div>
          </button>
        ))}
      </div>

      
    </div>
  );
};

export default HotTopics;