import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { EmptyState, primaryButtonClass } from '../components/ui.jsx';

export default function NotFoundPage() {
  return (
    <EmptyState
      icon={Compass}
      title="Page not found"
      text="The page you're looking for doesn't exist."
      action={
        <Link to="/" className={primaryButtonClass}>
          Back to shows
        </Link>
      }
    />
  );
}
