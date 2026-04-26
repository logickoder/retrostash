export default function Footer() {
  return (
    <footer className="mt-24 border-t border-outline/30">
      <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-2 px-4 py-6 text-sm text-on-surface-variant sm:flex-row">
        <p>
          © 2026{' '}
          <a
            className="text-on-surface hover:text-primary"
            href="https://logickoder.dev"
            rel="noreferrer noopener"
            target="_blank"
          >
            Jeffery Orazulike
          </a>
          . All rights reserved.
        </p>
        <p>
          Apache-2.0 ·{' '}
          <a
            className="text-primary hover:underline"
            href="https://github.com/logickoder/retrostash"
            rel="noreferrer noopener"
            target="_blank"
          >
            github.com/logickoder/retrostash
          </a>
        </p>
      </div>
    </footer>
  );
}
