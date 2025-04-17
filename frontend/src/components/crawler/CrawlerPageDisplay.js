import React, { useState, useEffect } from "react";
import axios from "axios";
import { FaSync, FaChevronDown, FaChevronUp, FaList } from "react-icons/fa";

const CrawledPageDisplay = () => {
  const [crawledPages, setCrawledPages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [isSectionVisible, setIsSectionVisible] = useState(false);

  const fetchCrawledPages = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await axios.get("http://localhost:8080/crawled-pages");
      setCrawledPages(response.data);
    } catch (error) {
      console.error("Error fetching crawled pages:", error);
      setError("Failed to fetch crawled pages. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCrawledPages();
  }, []);

  return (
    <div className="p-4 bg-white rounded-lg shadow mt-4">
      {/* Header with identical layout to Database Management */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <FaList className="text-blue-500" />
          <span className="text-lg font-medium text-gray-700">
            Crawled Pages ({crawledPages.length})
          </span>
        </div>
        
        <div className="flex items-center space-x-4">
          <button
            onClick={(e) => {
              e.stopPropagation();
              fetchCrawledPages();
            }}
            disabled={loading}
            className={`flex items-center px-3 py-1 rounded-md text-sm ${
              loading
                ? "bg-gray-200 text-gray-500 cursor-not-allowed"
                : "bg-blue-50 text-blue-600 hover:bg-blue-100"
            }`}
          >
            {loading ? (
              <FaSync className="animate-spin mr-1" />
            ) : (
              <FaSync className="mr-1" />
            )}
            Refresh
          </button>
          
          <button
            onClick={() => setIsSectionVisible(!isSectionVisible)}
            className="text-gray-500 focus:outline-none"
          >
            {isSectionVisible ? <FaChevronUp /> : <FaChevronDown />}
          </button>
        </div>
      </div>

      {/* Content - identical structure to Database Management */}
      {isSectionVisible && (
        <div className="mt-4 space-y-4">
          {error && (
            <div className="px-4 py-2 rounded-md bg-red-100 text-red-800">
              {error}
            </div>
          )}

          {crawledPages.length > 0 ? (
            <div className="border rounded-md divide-y max-h-96 overflow-y-auto">
              {crawledPages.map((page, index) => (
                <div key={index} className="p-3 hover:bg-gray-50">
                  <div className="flex items-start">
                    <span className="inline-block py-0.5 px-2 mr-2 text-xs font-medium rounded bg-blue-100 text-blue-800">
                      {index + 1}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {page.url}
                      </p>
                      {page.title && (
                        <p className="text-sm text-gray-500 truncate">
                          {page.title}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-4 text-gray-500">
              {loading ? "Loading..." : "No crawled pages found"}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CrawledPageDisplay;