import { Route, Routes } from 'react-router-dom';
import Footer from './components/Footer.jsx';
import Navbar from './components/Navbar.jsx';
import MyBookingsPage from './pages/MyBookingsPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ShowPage from './pages/ShowPage.jsx';
import ShowsPage from './pages/ShowsPage.jsx';

export default function App() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="page-container flex-1 py-10 sm:py-14">
        <Routes>
          <Route path="/" element={<ShowsPage />} />
          <Route path="/shows/:showId" element={<ShowPage />} />
          <Route path="/bookings" element={<MyBookingsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
