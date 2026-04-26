import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import Nav from './components/Nav';
import Footer from './components/Footer';

const Home = lazy(() => import('./routes/Home'));
const Install = lazy(() => import('./routes/Install'));
const Migration = lazy(() => import('./routes/Migration'));
const NotFound = lazy(() => import('./routes/NotFound'));

function RouteFallback() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <span
        className="size-10 animate-spin rounded-full border-2 border-outline/30 border-t-primary"
        aria-label="Loading"
      />
    </div>
  );
}

export default function App() {
  return (
    <div className="flex min-h-screen flex-col">
      <Nav />
      <main id="main" className="flex-1">
        <Suspense fallback={<RouteFallback />}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/install" element={<Install />} />
            <Route path="/migration" element={<Migration />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}
