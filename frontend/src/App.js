import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Crawler from './pages/Crawler';
import Header from './components/Header';
import Search from './pages/Search';
import Result from './pages/Result';
import History from './pages/History';

function App() {
  return (
    <Router>
        <Header />
        <Routes>
          <Route path="/" element={<Search />} />
          <Route path="/crawler" element={<Crawler />} />
          <Route path="/results" element={<Result />} />
          <Route path="/history" element={<History />} />
          <Route path="*" element={<Search />} />
        </Routes>
    </Router>
  );
}

export default App;
