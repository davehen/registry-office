import { apiBaseUrl } from './runtimeConfig.js';

export class ApiError extends Error {
    constructor(status, message) { super(message); this.name = 'ApiError'; this.status = status; }
}

export function createHttpClient(base, fetcher = (...args) => globalThis.fetch(...args)) {
    return async function request(path, { method = 'GET', body, allowEmpty = false, notFoundValue } = {}) {
        let response;
        try {
            response = await fetcher(`${base}${path}`, {
                method, credentials: 'omit',
                ...(body === undefined ? {} : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
            });
        } catch (_) { throw new ApiError(0, 'Network error. Check the connection and try again.'); }
        if (response.status === 404 && notFoundValue !== undefined) return notFoundValue;
        let text;
        try { text = await response.text(); }
        catch (_) { throw new ApiError(0, 'Network error while reading the server response.'); }
        let data;
        try { data = text.trim() ? JSON.parse(text) : null; }
        catch (_) {
            if (response.ok) throw new ApiError(response.status, 'Server returned an invalid response.');
        }
        if (!response.ok) {
            const fallback = { 400: 'Invalid request. Check the form and try again.',
                404: 'Record no longer available.', 409: 'Conflict: a record changed or an identifier is unavailable.',
                410: 'This person has been merged. Refresh the person list.' };
            throw new ApiError(response.status, typeof data?.message === 'string' ? data.message : fallback[response.status] || 'Server error. Please try again.');
        }
        if (data === null && !allowEmpty) throw new ApiError(response.status, 'Server returned an empty response.');
        return data;
    };
}

export const request = createHttpClient(apiBaseUrl);
