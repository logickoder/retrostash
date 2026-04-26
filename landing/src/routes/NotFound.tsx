import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import SEO from '../components/SEO';

export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-32 text-center">
      <SEO title="Page not found" />
      <span
        className="gradient-text text-7xl font-bold sm:text-8xl"
        style={{ animation: 'var(--animate-fade-up)' }}
      >
        404
      </span>
      <h1
        className="mt-4 text-2xl font-semibold text-on-surface"
        style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.1s' }}
      >
        Page not found
      </h1>
      <p
        className="mt-3 text-on-surface-variant"
        style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.2s' }}
      >
        That route doesn't exist on the Retrostash site.
      </p>
      <Link
        to="/"
        className="mt-8 inline-flex items-center gap-2 rounded-chip bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-transform hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/30"
        style={{ animation: 'var(--animate-fade-up)', animationDelay: '0.3s' }}
      >
        <ArrowLeft className="size-4" aria-hidden />
        Back to overview
      </Link>
    </div>
  );
}
