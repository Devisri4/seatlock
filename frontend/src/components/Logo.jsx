import { Armchair } from 'lucide-react';

export default function Logo() {
  return (
    <span className="flex items-center gap-2.5">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-600 to-brand-800 text-white shadow-sm">
        <Armchair className="h-5 w-5" aria-hidden="true" />
      </span>
      <span className="text-lg font-bold tracking-tight text-gray-900">
        Seat<span className="text-brand-600">Lock</span>
      </span>
    </span>
  );
}
