import { beforeEach, it, expect, vi } from 'vitest';
import * as api from '../src/civilRegistryApi.js';
beforeEach(() => vi.stubGlobal('fetch', vi.fn(async () => ({ status: 200, ok: true, text: async () => '[]' }))));
it('preserves every person REST path, method and encoded segment', async () => {
    await api.listPeople(); await api.getPerson(7); await api.findByText('A/B é'); await api.findByNationality('French');
    await api.savePerson({ userName: 'new' }); await api.savePerson({ id: 7, userName: 'changed' });
    await api.deletePerson(7); await api.deleteByEmail('a+b@example.test'); await api.deleteAll();
    expect(fetch.mock.calls.map(([url, options]) => [url, options.method])).toEqual([
        ['/person', 'GET'], ['/person/7', 'GET'], ['/person/search/A%2FB%20%C3%A9', 'GET'], ['/person/nationality/French', 'GET'],
        ['/person', 'POST'], ['/person', 'PUT'], ['/person/7', 'DELETE'], ['/person/email/a%2Bb%40example.test', 'DELETE'], ['/person', 'DELETE']
    ]);
    expect(JSON.parse(fetch.mock.calls[4][1].body)).toEqual({ userName: 'new' });
});
it('merges and deduplicates nationality/text results while preserving exact/partial API semantics', async () => {
    fetch.mockResolvedValueOnce({ status: 200, ok: true, text: async () => '[{"id":1},{"id":2}]' })
        .mockResolvedValueOnce({ status: 200, ok: true, text: async () => '[{"id":2},{"id":3}]' });
    expect(await api.searchPeople(' French ')).toEqual([{ id: 1 }, { id: 2 }, { id: 3 }]);
    expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/person/search/%20French%20', '/person/nationality/French']);
});
it('empty search lists all and a failed search rejects the combined result', async () => {
    await api.searchPeople('  '); expect(fetch.mock.calls[0][0]).toBe('/person');
    fetch.mockRejectedValue(new TypeError('offline'));
    await expect(api.searchPeople('French')).rejects.toMatchObject({ status: 0 });
});
it('all duplicate API calls share the configured HTTP client', async () => {
    await api.duplicateRequest('/duplicate-cases?status=OPEN');
    await api.duplicateRequest('/duplicate-cases/1/merge', { reason: 'Confirmed', canonicalPersonId: 1 });
    await api.duplicateRequest('/person-merges/3');
    expect(fetch.mock.calls.map(([url, opts]) => [url, opts.method])).toEqual([
        ['/duplicate-cases?status=OPEN', 'GET'], ['/duplicate-cases/1/merge', 'POST'], ['/person-merges/3', 'GET']
    ]);
    await expect(api.duplicateRequest('https://foreign.test')).rejects.toThrow('Invalid duplicate API path');
});
