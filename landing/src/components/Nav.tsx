import { NavLink } from 'react-router-dom';

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
  `${linkBase} text-on-surface-variant hover:text-on-surface`;

export default function Nav() {
  return (
    <header className="sticky top-0 z-20 border-b border-outline/20 bg-surface/80 backdrop-blur-md">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <NavLink to="/" className="group flex items-center gap-2">
          <span
            aria-hidden
            className="relative inline-block size-7 overflow-hidden rounded-md bg-primary transition-transform duration-300 group-hover:rotate-6 group-hover:scale-110"
          >
            <span className="absolute inset-0 bg-gradient-to-br from-primary to-tertiary opacity-90" />
          </span>
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
            <span className="ml-1 inline-block transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5">↗</span>
          </a>
          <a className={external} href="/retrostash/api/">
            API
            <span className="ml-1 inline-block transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5">↗</span>
          </a>
          <a
            className={external}
            href="https://github.com/logickoder/retrostash"
            rel="noreferrer noopener"
            target="_blank"
          >
            GitHub
            <span className="ml-1 inline-block transition-transform duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5">↗</span>
          </a>
        </div>
      </nav>
    </header>
  );
}
