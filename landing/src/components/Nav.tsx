import { NavLink } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';

const linkBase =
  'group relative rounded-chip px-3 py-1.5 text-sm font-medium transition-colors duration-200';

function ActiveDot() {
  return (
    <span
      aria-hidden
      className="absolute -bottom-0.5 left-1/2 h-0.5 w-6 -translate-x-1/2 rounded-full bg-primary"
    />
  );
}

const internal = ({ isActive }: { isActive: boolean }) =>
  `${linkBase} ${
    isActive
      ? 'text-on-surface'
      : 'text-on-surface-variant hover:text-on-surface'
  }`;

const external =
  `${linkBase} inline-flex items-center gap-1 text-on-surface-variant hover:text-on-surface`;

export default function Nav() {
  return (
    <header className="sticky top-0 z-20 border-b border-outline/20 bg-surface/80 backdrop-blur-md">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:rounded-chip focus:bg-primary focus:px-3 focus:py-1.5 focus:text-sm focus:font-semibold focus:text-on-primary"
      >
        Skip to content
      </a>
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <NavLink to="/" className="group flex items-center gap-2">
          <img
            src="/retrostash/favicon.svg"
            alt=""
            aria-hidden
            className="size-8 transition-transform duration-300 group-hover:rotate-6 group-hover:scale-110"
          />
          <span className="text-base font-semibold tracking-tight text-on-surface">
            Retrostash
          </span>
        </NavLink>

        <div className="hidden items-center gap-1 sm:flex">
          <NavLink to="/" end className={internal}>
            {({ isActive }) => (
              <>
                Overview
                {isActive && <ActiveDot />}
              </>
            )}
          </NavLink>
          <NavLink to="/install" className={internal}>
            {({ isActive }) => (
              <>
                Install
                {isActive && <ActiveDot />}
              </>
            )}
          </NavLink>
          <NavLink to="/migration" className={internal}>
            {({ isActive }) => (
              <>
                Migration
                {isActive && <ActiveDot />}
              </>
            )}
          </NavLink>
          <a className={external} href="/retrostash/playground/">
            Playground
            <ArrowUpRight className="size-3.5 transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </a>
          <a className={external} href="/retrostash/api/">
            API
            <ArrowUpRight className="size-3.5 transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </a>
          <a
            className={external}
            href="https://github.com/logickoder/retrostash"
            rel="noreferrer noopener"
            target="_blank"
          >
            GitHub
            <ArrowUpRight className="size-3.5 transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </a>
        </div>
      </nav>
    </header>
  );
}
