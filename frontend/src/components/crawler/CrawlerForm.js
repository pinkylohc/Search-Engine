import React, { useState } from 'react';
import axios from 'axios';

const CrawlerForm = () => {
  const [formData, setFormData] = useState({
    startingUrl: '',
    maxPages: 300
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const validateForm = () => {
    if (!formData.startingUrl) {
      setError('Starting URL is required');
      return false;
    }
    
    try {
      new URL(formData.startingUrl);
    } catch (e) {
      setError('Please enter a valid URL');
      return false;
    }

    if (formData.maxPages <= 0) {
      setError('Max pages must be greater than 0');
      return false;
    }

    setError('');
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    setIsSubmitting(true);
    setSuccess('');
    
    try {
      const response = await axios.post('http://localhost:8080/crawl', {
        startingUrl: formData.startingUrl,
        maxIndexPage: parseInt(formData.maxPages)
      }, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      setSuccess('Crawler started successfully!');
      console.log('Server response:', response.data);
    } catch (err) {
      console.error('Error submitting form:', err);
      setError(err.response?.data?.message || 'Failed to start crawler. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-8 p-6 bg-white rounded-lg shadow-md">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Configure Web Crawler</h2>
      
      {error && (
        <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}
      
      {success && (
        <div className="mb-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded">
          {success}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="mb-6">
          <label htmlFor="startingUrl" className="block text-gray-700 font-medium mb-2">
            Starting URL:
          </label>
          <input
            type="url"
            id="startingUrl"
            name="startingUrl"
            value={formData.startingUrl}
            onChange={handleChange}
            placeholder="https://example.com"
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>
        
        <div className="mb-6">
          <label htmlFor="maxPages" className="block text-gray-700 font-medium mb-2">
            Maximum Pages to Crawl:
          </label>
          <input
            type="number"
            id="maxPages"
            name="maxPages"
            min="1"
            max="1000"
            value={formData.maxPages}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          />
        </div>
        
        <button
          type="submit"
          disabled={isSubmitting}
          className={`w-full py-2 px-4 rounded-md text-white font-medium ${
            isSubmitting
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-blue-600 hover:bg-blue-700'
          } focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2`}
        >
          {isSubmitting ? 'Starting Crawler...' : 'Start Crawler'}
        </button>
      </form>
    </div>
  );
};

export default CrawlerForm;