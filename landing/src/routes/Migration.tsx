import { Link } from 'react-router-dom';

const rows: Array<{ old: string; current: string }> = [
  { old: 'Retrostash.install(builder, context)', current: 'RetrostashOkHttpAndroid.install(builder, context)' },
  { old: 'Retrostash.from(client)', current: 'RetrostashOkHttpBridge.from(client)' },
  { old: 'Retrostash.clear(context)', current: 'RetrostashOkHttpAndroid.clear(context)' },
  { old: 'RetrostashConfig', current: 'RetrostashOkHttpConfig (OkHttp) or RetrostashConfig (Ktor)' },
  { old: 'PostResponseCacheStore', current: 'RetrostashStore + InMemoryRetrostashStore / AndroidRetrostashStore' },
  {
    old: 'NetworkCachePolicyInterceptor, CacheControlInterceptor',
    current: 'merged into RetrostashOkHttpInterceptor',
  },
  {
    old: 'JitPack: com.github.logickoder:retrostash:<v>',
    current: 'Maven Central: dev.logickoder:retrostash-{core,annotations,ktor,okhttp}:<v>',
  },
];

export default function Migration() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:py-24">
      <div className="scroll-reveal">
        <span className="text-xs font-medium uppercase tracking-wider text-primary">
          Upgrade path
        </span>
        <h1 className="mt-2 text-4xl font-semibold tracking-tight sm:text-5xl">
          Migrating from 0.0.4
        </h1>
        <p className="mt-4 text-lg text-on-surface-variant">
          0.0.5 is a structural rename, not a breaking semantic change. Annotate queries, annotate
          mutations — same contract, new module names.
        </p>
      </div>

      <div className="mt-10 scroll-reveal overflow-hidden rounded-card border border-outline/30">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-secondary-container/40 text-on-surface-variant">
            <tr>
              <th className="px-4 py-3 font-medium">0.0.4</th>
              <th className="px-4 py-3 font-medium">0.0.5</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr
                key={r.old}
                className="border-t border-outline/20 transition-colors duration-150 hover:bg-secondary-container/20"
              >
                <td className="px-4 py-3 font-mono text-xs text-on-surface-variant">
                  {r.old}
                </td>
                <td className="px-4 py-3 font-mono text-xs text-on-surface">
                  {r.current}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-8 rounded-card border border-outline/30 bg-secondary-container/20 p-5 scroll-reveal">
        <p className="text-sm text-on-surface-variant">
          The legacy <code className="rounded bg-surface-variant/40 px-1.5 py-0.5 font-mono text-xs">com.github.logickoder:retrostash</code>{' '}
          coord on JitPack is frozen at 0.0.4. New releases publish per-module to Maven Central.{' '}
          <Link className="font-medium text-primary hover:underline" to="/install">
            See Install →
          </Link>
        </p>
      </div>
    </div>
  );
}
