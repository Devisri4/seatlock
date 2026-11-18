// Shared design system: buttons, cards, badges, headers, notices, empty and loading states.
// Every page builds on these so the whole app looks consistent.
import { AlertCircle, CheckCircle2, Info } from 'lucide-react';

const buttonBase =
  'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition ' +
  'active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:active:scale-100';

export const primaryButtonClass = `${buttonBase} bg-brand-700 text-white shadow-sm hover:bg-brand-800`;

export const secondaryButtonClass = `${buttonBase} border border-gray-300 bg-white text-gray-700 shadow-sm hover:border-gray-400 hover:bg-gray-50`;

export const cardClass = 'rounded-2xl border border-gray-200 bg-white shadow-card';

export function Card({ className = '', children, ...rest }) {
  return (
    <div className={`${cardClass} ${className}`} {...rest}>
      {children}
    </div>
  );
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

export function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="max-w-2xl">
        {eyebrow && <p className="eyebrow mb-2">{eyebrow}</p>}
        <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">{title}</h1>
        {description && <p className="mt-3 text-base text-gray-500 sm:text-lg">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 gap-3">{actions}</div>}
    </div>
  );
}

const NOTICE_STYLES = {
  error: { box: 'border-red-200 bg-red-50 text-red-700', icon: AlertCircle },
  success: { box: 'border-emerald-200 bg-emerald-50 text-emerald-700', icon: CheckCircle2 },
  info: { box: 'border-brand-100 bg-brand-50 text-brand-800', icon: Info },
};

export function Notice({ type = 'info', children }) {
  if (!children) return null;
  const { box, icon: Icon } = NOTICE_STYLES[type];
  return (
    <p role={type === 'error' ? 'alert' : 'status'} className={`flex items-start gap-2 rounded-lg border px-3.5 py-2.5 text-sm ${box}`}>
      <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </p>
  );
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="rounded-2xl border border-dashed border-gray-300 bg-white px-6 py-16 text-center">
      {Icon && (
        <div className="relative mx-auto flex h-16 w-16 items-center justify-center">
          <span className="absolute inset-0 rounded-full bg-brand-50" aria-hidden="true" />
          <span className="absolute inset-2 rounded-full bg-brand-100/70" aria-hidden="true" />
          <Icon className="relative h-7 w-7 text-brand-700" aria-hidden="true" />
        </div>
      )}
      <h2 className="mt-5 text-base font-semibold text-gray-900">{title}</h2>
      {text && <p className="mx-auto mt-1.5 max-w-sm text-sm text-gray-500">{text}</p>}
      {action && <div className="mt-6 flex justify-center">{action}</div>}
    </div>
  );
}

export function Skeleton({ className = '' }) {
  return <div className={`animate-pulse rounded-md bg-gray-200/80 ${className}`} aria-hidden="true" />;
}
