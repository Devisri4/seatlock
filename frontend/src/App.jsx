import { Clock, Radio, ShieldCheck } from 'lucide-react';
import Footer from './components/Footer.jsx';
import Navbar from './components/Navbar.jsx';
import { Card, PageHeader } from './components/ui.jsx';

const FEATURES = [
  {
    icon: Radio,
    title: 'Live seat map',
    text: 'The moment someone holds a seat, it turns amber in every open browser.',
  },
  {
    icon: Clock,
    title: '5-minute hold',
    text: 'Your seats are yours while you pay. If you walk away, they are released automatically.',
  },
  {
    icon: ShieldCheck,
    title: 'Never double booked',
    text: 'A seat is sold once, even when hundreds of people click at the same moment.',
  },
];

const LEGEND = [
  { label: 'Available', className: 'bg-seat-available' },
  { label: 'Held', className: 'bg-seat-held' },
  { label: 'Sold', className: 'bg-seat-sold' },
];

export default function App() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="page-container flex-1 py-12 sm:py-16">
        <PageHeader
          eyebrow="Shows and events"
          title="Pick a seat. Keep it. Pay once."
          description="SeatLock is a ticket booking system where many people can race for the same seat and only one of them gets it."
        />

        <div className="grid gap-5 sm:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, text }) => (
            <Card key={title} className="p-6 transition hover:shadow-card-hover">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                <Icon className="h-5 w-5" aria-hidden="true" />
              </span>
              <h2 className="mt-4 text-base font-semibold text-gray-900">{title}</h2>
              <p className="mt-1.5 text-sm text-gray-500">{text}</p>
            </Card>
          ))}
        </div>

        <ul className="mt-10 flex flex-wrap gap-5 text-sm text-gray-600" aria-label="Seat colours">
          {LEGEND.map(({ label, className }) => (
            <li key={label} className="flex items-center gap-2">
              <span className={`h-3.5 w-3.5 rounded ${className}`} aria-hidden="true" />
              {label}
            </li>
          ))}
        </ul>
      </main>
      <Footer />
    </div>
  );
}
