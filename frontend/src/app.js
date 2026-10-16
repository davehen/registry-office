import * as api from './civilRegistryApi.js';
import { personPayload } from './personForm.js';

let listRequestVersion = 0;
let mutationPending = false;

function beginListRequest() {
    const version = ++listRequestVersion;
    renderTable([]);
    document.getElementById('searchStatus').textContent = 'Loading...';
    document.getElementById('personTable').setAttribute('aria-busy', 'true');
    return version;
}
function finishListRequest(version, persons, message) {
    if (version !== listRequestVersion) return;
    renderTable(persons);
    document.getElementById('searchStatus').textContent = message;
    document.getElementById('personTable').setAttribute('aria-busy', 'false');
}
export async function loadPersons() {
    const version = beginListRequest();
    try {
        const persons = await api.listPeople();
        if (!Array.isArray(persons)) throw new Error('Invalid persons response');
        finishListRequest(version, persons, persons.length ? '' : 'No persons found');
    } catch (_) { finishListRequest(version, [], 'Failed to load persons from server.'); }
}
export async function searchPersons() {
    const text = document.getElementById('searchInput').value;
    if (!text.trim()) return loadPersons();
    const version = beginListRequest();
    try {
        const persons = await api.searchPeople(text);
        finishListRequest(version, persons, persons.length ? '' : 'No persons found');
    } catch (_) { finishListRequest(version, [], 'Search failed. Please try again.'); }
}
export function renderTable(persons) {
    const tbody = document.querySelector('#personTable tbody');
    tbody.replaceChildren();
    for (const person of persons) {
        const row = document.createElement('tr');
        for (const value of [person.id, person.userName, person.firstName, person.lastName,
            person.email, person.birthDate, person.nationality || 'Not specified']) {
            const cell = document.createElement('td');
            cell.textContent = value == null ? '' : String(value);
            row.appendChild(cell);
        }
        const actions = document.createElement('td');
        for (const [label, className, action] of [['Edit', 'btn-edit', editPerson], ['Delete', 'btn-delete', deletePerson]]) {
            const button = document.createElement('button');
            button.textContent = label; button.className = className;
            button.addEventListener('click', () => action(person.id));
            actions.appendChild(button);
        }
        row.appendChild(actions); tbody.appendChild(row);
    }
}
async function mutate(action) {
    if (mutationPending) return;
    mutationPending = true;
    const buttons = [...document.querySelectorAll('header button, #personsView button, .modal button')];
    const previous = buttons.map(button => button.disabled);
    buttons.forEach(button => { button.disabled = true; });
    try { await action(); }
    catch (error) { alert(error.message || 'Unable to complete the operation.'); }
    finally {
        mutationPending = false;
        buttons.forEach((button, index) => { button.disabled = previous[index]; });
    }
}
export async function savePerson() {
    return mutate(async () => {
        await api.savePerson(personPayload(document.getElementById('personForm')));
        closeModal('personModal');
        document.getElementById('personForm').reset();
        await loadPersons();
    });
}
export async function deletePerson(id) {
    if (mutationPending || !confirm('Are you sure you want to delete this person?')) return;
    return mutate(async () => { await api.deletePerson(id); await loadPersons(); });
}
export async function deleteAllPersons() {
    if (mutationPending || !confirm('WARNING: This will delete ALL persons from the registry. Are you sure?')) return;
    return mutate(async () => { await api.deleteAll(); await loadPersons(); });
}
export async function deleteByEmail() {
    const email = document.getElementById('deleteEmail').value;
    if (mutationPending) return;
    if (!email) { alert('Please enter an email address.'); return; }
    if (!confirm('Are you sure you want to delete the person with email: ' + email + '?')) return;
    return mutate(async () => {
        await api.deleteByEmail(email);
        closeModal('deleteEmailModal');
        document.getElementById('deleteEmail').value = '';
        await loadPersons();
    });
}
export function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
    if (modalId === 'personModal') {
        document.getElementById('modalTitle').textContent = 'Add New Person';
        document.getElementById('personId').value = '';
        document.getElementById('personForm').reset();
    }
}
export function closeModal(modalId) { document.getElementById(modalId).style.display = 'none'; }
export async function editPerson(id) {
    if (mutationPending) return;
    try {
        const person = await api.getPerson(id);
        if (!person || typeof person !== 'object' || !person.id) throw new Error('Server returned invalid person details.');
        openModal('personModal');
        document.getElementById('modalTitle').textContent = 'Edit Person';
        document.getElementById('personId').value = person.id;
        for (const field of ['userName', 'firstName', 'lastName', 'email', 'birthDate', 'nationality'])
            document.getElementById(field).value = person[field] ?? '';
    } catch (error) {
        const unavailableMessages = {
            404: 'Person no longer available. Refresh the person list.',
            410: 'This person has been merged. Refresh the person list.'
        };
        alert(unavailableMessages[error.status] || error.message || 'Unable to load person details.');
        if (error.status === 404 || error.status === 410) await loadPersons();
    }
}
export function initPersons() {
    document.querySelector('[data-action="search"]').addEventListener('click', searchPersons);
    document.querySelector('[data-action="delete-all"]').addEventListener('click', deleteAllPersons);
    document.querySelector('[data-action="delete-email"]').addEventListener('click', deleteByEmail);
    document.querySelectorAll('[data-open-modal]').forEach(button =>
        button.addEventListener('click', () => openModal(button.dataset.openModal)));
    document.querySelectorAll('[data-close-modal]').forEach(button =>
        button.addEventListener('click', () => closeModal(button.dataset.closeModal)));
    document.getElementById('personForm').addEventListener('submit', event => { event.preventDefault(); savePerson(); });
    window.addEventListener('click', event => { if (event.target.classList?.contains('modal')) event.target.style.display = 'none'; });
    loadPersons();
}
