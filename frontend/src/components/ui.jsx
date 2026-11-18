// Shared design system: cards, badges and page headers. Every page builds on these so the app looks consistent.

export const cardClass = 'rounded-2xl border border-gray-200 bg-white shadow-card';

export function Card({ className = '', children }) {
  return <div className={`${cardClass} ${className}`}>{children}</div>;
}

const BADGE_TONES = {
  neutral: 'bg-gray-100 text-gray-700 ring-gray-200',
  brand: 'bg-brand-50 text-brand-700 ring-brand-100',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-100',
  amber: 'bg-amber-50 text-amber-700 ring-amber-100',
  red: 'bg-red-50 text-red-700 ring-red-100',
};

export function Badge({ tone = 'neutral', className = '', children, ...rest }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${BADGE_TONES[tone]} ${className}`}
      {...rest}
    >
      {children}
    </span>
  );
}

export function PageHeader({ eyebrow, title, description }) {
  return (
    <div className="mb-10 max-w-2xl">
      {eyebrow && <p className="eyebrow mb-2">{eyebrow}</p>}
      <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">{title}</h1>
      {description && <p className="mt-3 text-base text-gray-500 sm:text-lg">{description}</p>}
    </div>
  );
}
