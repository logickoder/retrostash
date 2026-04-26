export default function Footer() {
  return (
    <footer className="mt-24 border-t border-(--color-outline)/30">
      <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-2 px-4 py-6 text-sm text-(--color-on-surface-variant) sm:flex-row">
        <p>
          Apache-2.0 ·{' '}
          <a
            className="text-(--color-primary) hover:underline"
            href="https://github.com/logickoder/retrostash"
            rel="noreferrer noopener"
            target="_blank"
          >
            github.com/logickoder/retrostash
          </a>
        </p>
        <p>
          Built with Kotlin Multiplatform · Compose Multiplatform · Ktor · OkHttp · Retrofit
        </p>
      </div>
    </footer>
  );
}
