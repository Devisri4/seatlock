import ApiStatus from './ApiStatus.jsx';
import Logo from './Logo.jsx';

export default function Navbar() {
  return (
    <header className="sticky top-0 z-40 border-b border-gray-200/80 bg-white/90 backdrop-blur-md">
      <div className="page-container flex h-16 items-center gap-6">
        <a href="/" aria-label="SeatLock home" className="shrink-0 rounded-lg">
          <Logo />
        </a>
        <div className="ml-auto">
          <ApiStatus />
        </div>
      </div>
    </header>
  );
}
