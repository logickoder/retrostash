import { useState } from 'react';
import { Check, Copy } from 'lucide-react';

interface CodeBlockProps {
  lang?: string;
  children: string;
}

export default function CodeBlock({ lang, children }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const onCopy = async () => {
    try {
      await navigator.clipboard.writeText(children);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* noop */
    }
  };

  const Icon = copied ? Check : Copy;

  return (
    <div className="group relative overflow-hidden rounded-card border border-outline/30 bg-secondary-container/30 transition-colors hover:border-primary/40">
      {lang ? (
        <div className="flex items-center justify-between border-b border-outline/20 px-4 py-2">
          <span className="text-[10px] font-medium uppercase tracking-wider text-on-surface-variant">
            {lang}
          </span>
          <button
            type="button"
            onClick={onCopy}
            className="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium text-on-surface-variant opacity-0 transition-opacity duration-200 hover:bg-surface-variant hover:text-on-surface group-hover:opacity-100 focus:opacity-100"
            aria-label={copied ? 'Copied' : 'Copy to clipboard'}
          >
            <Icon className="size-3.5" />
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={onCopy}
          className="absolute right-2 top-2 z-10 inline-flex items-center gap-1 rounded-md bg-surface-variant/80 px-2 py-1 text-xs font-medium text-on-surface-variant opacity-0 backdrop-blur transition-opacity duration-200 hover:text-on-surface group-hover:opacity-100 focus:opacity-100"
          aria-label={copied ? 'Copied' : 'Copy to clipboard'}
        >
          <Icon className="size-3.5" />
        </button>
      )}
      <pre className="overflow-x-auto p-4">
        <code className="font-mono text-sm leading-relaxed text-on-surface">{children}</code>
      </pre>
    </div>
  );
}
