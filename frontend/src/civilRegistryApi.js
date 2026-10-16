import { request } from './httpClient.js';

const segment = value => encodeURIComponent(String(value));
export const listPeople = () => request('/person');
export const getPerson = id => request(`/person/${segment(id)}`);
export const findByNationality = value => request(`/person/nationality/${segment(value)}`);
export const findByText = value => request(`/person/search/${segment(value)}`, { notFoundValue: [] });
export async function searchPeople(value) {
    if (!value.trim()) return listPeople();
    const matches = await Promise.all([findByText(value), findByNationality(value.trim())]);
    if (matches.some(value => !Array.isArray(value))) throw new Error('Invalid search response');
    return [...new Map(matches.flat().map(person => [person.id, person])).values()];
}
export const savePerson = person => request('/person', { method: person.id ? 'PUT' : 'POST', body: person });
export const deletePerson = id => request(`/person/${segment(id)}`, { method: 'DELETE', allowEmpty: true });
export const deleteByEmail = email => request(`/person/email/${segment(email)}`, { method: 'DELETE', allowEmpty: true });
export const deleteAll = () => request('/person', { method: 'DELETE', allowEmpty: true });
export const duplicateRequest = async (path, body) => {
    if (!/^\/(duplicate-cases(?:[/?]|$)|person-merges\/)/.test(path)) throw new Error('Invalid duplicate API path');
    try { return await request(path, body === undefined ? {} : { method: 'POST', body }); }
    catch (error) {
        const messages = { 400: 'Invalid request. Check the reason and all selections.',
            404: 'Record no longer available.', 409: 'Conflict: a record changed. Review the refreshed case before submitting again.',
            410: 'This person has been merged. Refresh the person list.' };
        if (error.status) error.message = messages[error.status] || 'Server error. Please try again.';
        throw error;
    }
};
