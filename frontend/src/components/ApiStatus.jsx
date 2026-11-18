import { useEffect, useState } from 'react';
import { isApiUp } from '../lib/api.js';
import { Badge } from './ui.jsx';

const STATES = {
  checking: { tone: 'neutral', dot: 'bg-gray-400 animate-pulse', label: 'Connecting…' },
  up: { tone: 'green', dot: 'bg-emerald-500', label: 'API online' },
  down: { tone: 'red', dot: 'bg-red-500', label: 'API offline' },
};

// Small pill in the navbar. The free server sleeps when idle, so this check also wakes it up.
export default function ApiStatus() {
  const [state, setState] = useState('checking');

  useEffect(() => {
    let active = true;
    isApiUp().then((up) => active && setState(up ? 'up' : 'down'));
    return () => {
      active = false;
    };
  }, []);

  const { tone, dot, label } = STATES[state];
  return (
    <Badge tone={tone} role="status">
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} aria-hidden="true" />
      {label}
    </Badge>
  );
}
