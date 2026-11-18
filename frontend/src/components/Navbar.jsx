import { Link, NavLink } from 'react-router-dom';
import { CalendarDays, Ticket } from 'lucide-react';
import ApiStatus from './ApiStatus.jsx';
import Logo from './Logo.jsx';

const LINKS = [
  { to: '/', label: 'Shows', icon: CalendarDays },
  { to: '/bookings', label: 'My bookings', icon: Ticket },
];

const linkClass = ({ isActive }) =>
  `inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
  }`;

export default function Navbar() {
  return (
    <header className="sticky top-0 z-40 border-b border-gray-200/80 bg-white/90 backdrop-blur-md">
      <div className="page-container flex h-16 items-center gap-4 sm:gap-6">
        <Link to="/" aria-label="SeatLock home" className="shrink-0 rounded-lg">
          <Logo />
        </Link>
        <nav className="flex items-center gap-1" aria-label="Main">
          {LINKS.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} end className={linkClass}>
              <Icon className="h-4 w-4" aria-hidden="true" />
              <span className="hidden sm:inline">{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="ml-auto hidden sm:block">
          <ApiStatus />
        </div>
      </div>
    </header>
  );
}
