import html from '../index.html?raw';
import { beforeEach, it, expect, vi } from 'vitest';
import { personPayload } from '../src/personForm.js';
import { renderTable, loadPersons, searchPersons, savePerson, editPerson } from '../src/app.js';
const response = body => ({ status: 200, ok: true, text: async () => JSON.stringify(body) });
beforeEach(() => { document.body.innerHTML = html; vi.stubGlobal('fetch', vi.fn(async () => response([]))); vi.stubGlobal('alert', vi.fn()); });
it('maps create and edit forms without introducing validation/normalization', () => {
    for (const [id, value] of Object.entries({ userName: ' User ', firstName: 'First', lastName: 'Last', email: 'mail@example.test', birthDate: '1990-01-01' })) document.getElementById(id).value = value;
    expect(personPayload(document.getElementById('personForm'))).toEqual({ userName: ' User ', firstName: 'First', lastName: 'Last', email: 'mail@example.test', birthDate: '1990-01-01', nationality: null });
    document.getElementById('personId').value = '7'; document.getElementById('nationality').value = 'French';
    expect(personPayload(document.getElementById('personForm'))).toMatchObject({ id: 7, nationality: 'French' });
});
it('renders rows as literal text and clears empty results', () => {
    const markup = '<img src=x onerror="window.executed=true"><script>alert(1)</script>';
    renderTable([{ id: 1, userName: markup, nationality: markup }]);
    expect(document.querySelector('tbody').textContent).toContain(markup);
    expect(document.querySelectorAll('tbody img, tbody script')).toHaveLength(0);
    expect(window.executed).toBeUndefined();
    renderTable([]); expect(document.querySelectorAll('tbody tr')).toHaveLength(0);
});
it('shows loading, empty and error states without retaining stale rows', async () => {
    let finish; fetch.mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const loading = loadPersons(); expect(document.getElementById('searchStatus').textContent).toBe('Loading...');
    finish(response([])); await loading;
    expect(document.getElementById('searchStatus').textContent).toBe('No persons found');
    fetch.mockRejectedValueOnce(new TypeError('offline')); await loadPersons();
    expect(document.getElementById('searchStatus').textContent).toContain('Failed to load');
    expect(document.getElementById('personTable').getAttribute('aria-busy')).toBe('false');
});
it('ignores older search completions', async () => {
    let finish; fetch.mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const older = loadPersons(); document.getElementById('searchInput').value = 'French';
    fetch.mockResolvedValueOnce(response([{ id: 2, userName: 'newer' }])).mockResolvedValueOnce(response([]));
    await searchPersons(); finish(response([{ id: 1, userName: 'older' }])); await older;
    expect(document.querySelector('tbody').textContent).toContain('newer'); expect(document.querySelector('tbody').textContent).not.toContain('older');
});
it('prevents duplicate submissions, refreshes only after success and keeps failed forms open', async () => {
    let finish; fetch.mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const saving = savePerson(); await savePerson(); expect(fetch).toHaveBeenCalledTimes(1);
    expect(document.querySelector('#personForm button').disabled).toBe(true);
    finish(response({ id: 1 })); await saving;
    expect(fetch).toHaveBeenCalledTimes(2); expect(document.querySelector('#personForm button').disabled).toBe(false);
    document.getElementById('personModal').style.display = 'block';
    fetch.mockResolvedValueOnce({ status: 400, ok: false, text: async () => '{"message":"Invalid person"}' });
    await savePerson(); expect(alert).toHaveBeenCalledWith('Invalid person');
    expect(document.getElementById('personModal').style.display).toBe('block'); expect(fetch).toHaveBeenCalledTimes(3);
});
it.each([[404, 'no longer available'], [410, 'merged']])('explains unavailable person status %s and refreshes without opening the edit form', async (status, message) => {
    document.getElementById('personModal').style.display = 'none';
    renderTable([{ id: 1, userName: 'stale' }]);
    fetch.mockResolvedValueOnce({ status, ok: false, text: async () => '{"message":"Person unavailable"}' });
    await editPerson(1);
    expect(alert).toHaveBeenCalledWith(expect.stringContaining(message));
    expect(document.getElementById('personModal').style.display).toBe('none');
    expect(document.getElementById('personId').value).toBe('');
    expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/person/1', '/person']);
    expect(document.querySelectorAll('tbody tr')).toHaveLength(0);
});
