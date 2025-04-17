import React from 'react';
import CrawlerForm from '../components/crawler/CrawlerForm';
import CleanDatabaseButton from '../components/crawler/CleanDbButton';
import CrawledPageDisplay from '../components/crawler/CrawlerPageDisplay';

function Crawler() {
  return (
    <div className="max-w-4xl mx-auto p-6 bg-gray-100 rounded-lg shadow-md mt-5">
      <div className="space-y-6">
        
        <CleanDatabaseButton initialUrl={"https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"} />

        <CrawledPageDisplay />


        <div className="p-4 bg-white rounded-lg shadow">
          <CrawlerForm />
        </div>

        

      </div>

      
    </div>
  );
}

export default Crawler;