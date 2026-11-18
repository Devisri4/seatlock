import { Github } from 'lucide-react';
import Logo from './Logo.jsx';

export default function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-white">
      <div className="page-container flex flex-col gap-6 py-10 md:flex-row md:items-center md:justify-between">
        <div className="max-w-sm">
          <Logo />
          <p className="mt-3 text-sm text-gray-500">
            A ticket booking demo built around one hard problem: many people, one seat, exactly one winner.
          </p>
        </div>
        <a
          href="https://github.com/Devisri4/seatlock"
          className="inline-flex items-center gap-2 text-sm font-medium text-gray-600 transition hover:text-brand-700"
        >
          <Github className="h-4 w-4" aria-hidden="true" />
          Source on GitHub
        </a>
      </div>
    </footer>
  );
}
