import { Helmet } from 'react-helmet-async';
import { useLocation } from 'react-router-dom';

interface SEOProps {
  title?: string;
  description?: string;
  keywords?: string;
  image?: string;
  type?: string;
}

const SITE_NAME = 'Retrostash';
const DEFAULT_TITLE =
  'Retrostash — Annotation-driven query caching for Kotlin Multiplatform';
const DEFAULT_DESCRIPTION =
  'Cache POST queries, invalidate on mutation. Same API across Android, JVM, iOS, and the web. Works with Retrofit, OkHttp, and Ktor.';
const DEFAULT_KEYWORDS =
  'retrostash, kotlin multiplatform, kmp, retrofit, okhttp, ktor, query cache, post caching, mutation invalidation, android, ios, kotlin, maven central';
const AUTHOR = 'Jeffery Orazulike';
const TWITTER_HANDLE = '@logickoder';
const CANONICAL_ORIGIN = 'https://logickoder.dev';
const BASE_PATH = '/retrostash';

export default function SEO({
  title,
  description,
  keywords,
  image,
  type = 'website',
}: SEOProps) {
  const location = useLocation();

  const origin =
    typeof window !== 'undefined' ? window.location.origin : CANONICAL_ORIGIN;
  const canonicalUrl = `${CANONICAL_ORIGIN}${BASE_PATH}${location.pathname === '/' ? '' : location.pathname}`;
  const defaultImage = `${origin}${BASE_PATH}/og-image.png`;

  const metaTitle = title ? `${title} · ${SITE_NAME}` : DEFAULT_TITLE;
  const metaDescription = description ?? DEFAULT_DESCRIPTION;
  const metaKeywords = keywords ?? DEFAULT_KEYWORDS;
  const metaImage = image
    ? image.startsWith('http')
      ? image
      : `${origin}${image}`
    : defaultImage;

  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareSourceCode',
    name: SITE_NAME,
    description: metaDescription,
    url: `${CANONICAL_ORIGIN}${BASE_PATH}/`,
    codeRepository: 'https://github.com/logickoder/retrostash',
    programmingLanguage: 'Kotlin',
    runtimePlatform: ['Android', 'JVM', 'iOS', 'wasmJs'],
    license: 'https://www.apache.org/licenses/LICENSE-2.0',
    author: {
      '@type': 'Person',
      name: AUTHOR,
      url: 'https://logickoder.dev',
    },
    keywords: metaKeywords,
  };

  return (
    <Helmet>
      <title>{metaTitle}</title>
      <meta name="description" content={metaDescription} />
      <meta name="keywords" content={metaKeywords} />
      <meta name="author" content={AUTHOR} />
      <link rel="canonical" href={canonicalUrl} />

      <meta property="og:type" content={type} />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:title" content={metaTitle} />
      <meta property="og:description" content={metaDescription} />
      <meta property="og:image" content={metaImage} />
      <meta property="og:site_name" content={SITE_NAME} />
      <meta property="og:locale" content="en_US" />

      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:site" content={TWITTER_HANDLE} />
      <meta name="twitter:creator" content={TWITTER_HANDLE} />
      <meta name="twitter:url" content={canonicalUrl} />
      <meta name="twitter:title" content={metaTitle} />
      <meta name="twitter:description" content={metaDescription} />
      <meta name="twitter:image" content={metaImage} />

      <meta name="theme-color" content="#1C1B1F" />
      <meta name="apple-mobile-web-app-capable" content="yes" />
      <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
      <meta name="apple-mobile-web-app-title" content={SITE_NAME} />

      <script type="application/ld+json">{JSON.stringify(structuredData)}</script>
    </Helmet>
  );
}
