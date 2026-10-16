import { describe, it, expect, vi } from 'vitest';
import { normalizeApiBase } from '../src/runtimeConfig.js';
import { createHttpClient } from '../src/httpClient.js';
const response = (status, text) => ({ status, ok: status >= 200 && status < 300, text: async () => text });
describe('configured REST boundary', () => {
    it('normalizes base URLs and rejects unsafe configuration', () => {
        expect(normalizeApiBase(' https://api.example.test/root/// ')).toBe('https://api.example.test/root');
        expect(normalizeApiBase('')).toBe('');
        for (const value of ['file:///tmp', 'javascript:alert(1)', 'https://user:pass@api.test', 'https://api.test?q=1', 'https://api.test#fragment', 'https://api.test?', 'https://api.test#'])
            expect(() => normalizeApiBase(value)).toThrow();
    });
    it('builds configured URLs, omits credentials and parses JSON', async () => {
        const fetcher = vi.fn().mockResolvedValue(response(200, '[{"id":1}]'));
        expect(await createHttpClient('https://api.example.test', fetcher)('/person')).toEqual([{ id: 1 }]);
        expect(fetcher).toHaveBeenCalledWith('https://api.example.test/person', { method: 'GET', credentials: 'omit' });
    });
    it('encodes JSON mutation body and sends the content type', async () => {
        const fetcher = vi.fn().mockResolvedValue(response(201, '{"id":1}'));
        await createHttpClient('', fetcher)('/person', { method: 'POST', body: { firstName: 'Name' } });
        expect(fetcher.mock.calls[0][1]).toMatchObject({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"firstName":"Name"}' });
    });
    it('preserves backend error status and message as data', async () => {
        await expect(createHttpClient('', async () => response(409, '{"message":"Duplicate username"}'))('/person'))
            .rejects.toMatchObject({ status: 409, message: 'Duplicate username' });
    });
    it('handles non-JSON errors, invalid or empty success bodies and network failures', async () => {
        await expect(createHttpClient('', async () => response(500, '<html>bad</html>'))('/person')).rejects.toMatchObject({ status: 500, message: 'Server error. Please try again.' });
        for (const body of ['', '<html>not json</html>', 'null'])
            await expect(createHttpClient('', async () => response(200, body))('/person')).rejects.toThrow();
        await expect(createHttpClient('', async () => { throw new TypeError('offline'); })('/person')).rejects.toMatchObject({ status: 0 });
        await expect(createHttpClient('', async () => ({ status: 200, ok: true, text: async () => { throw new TypeError('disconnected'); } }))('/person')).rejects.toMatchObject({ status: 0 });
    });
    it('permits intentionally empty delete responses and missing text searches only', async () => {
        expect(await createHttpClient('', async () => response(204, ''))('/person/1', { method: 'DELETE', allowEmpty: true })).toBeNull();
        expect(await createHttpClient('', async () => response(404, '{}'))('/person/search/none', { notFoundValue: [] })).toEqual([]);
        await expect(createHttpClient('', async () => response(404, '{}'))('/person/1')).rejects.toMatchObject({ status: 404 });
    });
});
