import React from 'react';
import { Link } from 'react-router-dom';
import { FaGlobe, FaSpider, FaSearch, FaHistory } from 'react-icons/fa';

const Header = () => {
  return (
    <header className="bg-gray-900 text-white shadow-lg">
      <div className="container mx-auto px-4 py-3 flex items-center justify-between">
        {/* Logo and Portal Name */}
        <Link to="/" className="flex items-center space-x-2 hover:text-blue-300 transition-colors">
          <FaGlobe className="text-2xl" />
          <h1 className="text-xl font-bold">BrowseBot</h1>
        </Link>

        {/* Navigation Links */}
        <nav className="flex space-x-6">
          <Link 
            to="/" 
            className="flex items-center space-x-1 hover:text-blue-300 transition-colors"
          >
            <FaSearch />
            <span>Search</span>
          </Link>
          
          <Link 
            to="/crawler" 
            className="flex items-center space-x-1 hover:text-blue-300 transition-colors"
          >
            <FaSpider />
            <span>Crawler</span>
          </Link>
          
          <Link 
            to="/history" 
            className="flex items-center space-x-1 hover:text-blue-300 transition-colors"
          >
            <FaHistory />
            <span>History</span>
          </Link>
        </nav>
      </div>
    </header>
  );
};

export default Header;