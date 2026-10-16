export function normalizeApiBase(value = '') {
    const base = String(value).trim().replace(/\/+$/, '');
    if (!base) return ''; // Explicit same-origin reverse-proxy deployment.
    const url = new URL(base);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || base.includes('?') || base.includes('#'))
        throw new Error('VITE_API_BASE_URL must be an HTTP(S) base URL without credentials, query or fragment.');
    return base;
}

export const apiBaseUrl = normalizeApiBase(import.meta.env.VITE_API_BASE_URL);
