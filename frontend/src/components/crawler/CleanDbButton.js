import React, { useState } from 'react';
import axios from 'axios';
import { FaInfoCircle, FaTrash, FaSync, FaChevronDown, FaChevronUp } from 'react-icons/fa';

const CleanDatabaseButton = () => {
  const [isCleaning, setIsCleaning] = useState(false);
  const [message, setMessage] = useState('');
  const [isSectionVisible, setIsSectionVisible] = useState(true); // Toggle state for the section

  const handleCleanDatabase = async () => {
    if (!window.confirm('Are you sure you want to clean the database? This will delete all crawled data and require reindexing.')) {
      return;
    }

    setIsCleaning(true);
    setMessage('');

    try {
      const response = await axios.post('http://localhost:8080/clean-db');
      setMessage(response.data);
    } catch (error) {
      setMessage(error.response?.data || 'Failed to clean database');
    } finally {
      setIsCleaning(false);
    }
  };

  return (
    <div className="p-4 bg-white rounded-lg shadow">
      {/* Toggleable Header */}
      <button
        onClick={() => setIsSectionVisible(!isSectionVisible)}
        className="flex items-center justify-between w-full text-left focus:outline-none"
      >
        <div className="flex items-center space-x-3">
          <FaTrash className="text-red-500" />
          <span className="text-lg font-medium text-gray-700">Database Management</span>
        </div>
        {isSectionVisible ? (
          <FaChevronUp className="text-gray-500" />
        ) : (
          <FaChevronDown className="text-gray-500" />
        )}
      </button>

      {/* Toggleable Content */}
      {isSectionVisible && (
        <div className="mt-4 space-y-4">
          
          {/* Information Box */}
          <div className="bg-blue-50 border-l-4 border-blue-400 p-4 rounded-md">
            <div className="flex">
              <div className="flex-shrink-0">
                <FaInfoCircle className="h-5 w-5 text-blue-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-blue-800">Initial Status</h3>
                <div className="mt-2 text-sm text-blue-700">
                  <p>
                    No pages have been crawled initially. Please use the crawl form below to start indexing pages.
                  </p>
                  <p className="mt-1">
                    To reindex pages, first clean the database and then initiate a new crawl using the form below.
                  </p>
                  <p className="mt-1 text-red-600 font-semibold">
                    Warning: Clicking the "Clean Database" button will delete all database records. Please proceed with caution.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Clean Database Button */}
          <div className="flex items-center space-x-4">
            <button
              onClick={handleCleanDatabase}
              disabled={isCleaning}
              className={`inline-flex items-center px-4 py-2 rounded-md shadow-sm text-white ${
                isCleaning
                  ? 'bg-gray-500 cursor-not-allowed'
                  : 'bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500'
              }`}
            >
              {isCleaning ? (
                <>
                  <FaSync className="animate-spin mr-2" />
                  Cleaning...
                </>
              ) : (
                <>
                  <FaTrash className="mr-2" />
                  Clean Database
                </>
              )}
            </button>

            {message && (
              <div
                className={`px-4 py-2 rounded-md ${
                  message.includes('success') ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                }`}
              >
                {message}
              </div>
            )}
          </div>

          {/* Additional Guidance */}
          <p className="text-sm text-gray-500 italic">
            After cleaning the database, refresh the crawled pages below for checking and use the crawler form to reindex pages.
          </p>
        </div>
      )}
    </div>
  );
};

export default CleanDatabaseButton;