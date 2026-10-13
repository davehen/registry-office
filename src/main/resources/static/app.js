const API_BASE_URL = '/person';
let listRequestVersion = 0;

async function personErrorMessage(response) {
    const messages = {
        400: 'Invalid person details. Check the form and try again.',
        404: 'Person no longer available. Refresh the person list.',
        409: 'Conflict: the person changed or an identifier is unavailable. Refresh and review before trying again.',
        410: 'This person has been merged. Refresh the person list.'
    };
    return messages[response.status] || 'Server error. Please try again.';
}

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

document.addEventListener('DOMContentLoaded', () => {
    loadPersons();

    document.getElementById('personForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await savePerson();
    });
});

async function loadPersons() {
    const version = beginListRequest();
    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error('Failed to load persons');
        const persons = await response.json();
        if (!Array.isArray(persons)) throw new Error('Invalid persons response');
        finishListRequest(version, persons, persons.length ? '' : 'No persons found');
    } catch (error) {
        console.error('Error loading persons:', error);
        finishListRequest(version, [], 'Failed to load persons from server.');
    }
}

async function searchPersons() {
    const text = document.getElementById('searchInput').value;
    if (!text.trim()) {
        await loadPersons();
        return;
    }
    const version = beginListRequest();
    try {
        const [textResponse, nationalityResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/search/${encodeURIComponent(text)}`),
            fetch(`${API_BASE_URL}/nationality/${encodeURIComponent(text.trim())}`)
        ]);
        if ((!textResponse.ok && textResponse.status !== 404) || !nationalityResponse.ok) {
            throw new Error('Search endpoint failed');
        }
        const [textMatches, nationalityMatches] = await Promise.all([
            textResponse.status === 404 ? [] : textResponse.json(),
            nationalityResponse.json()
        ]);
        if (!Array.isArray(textMatches) || !Array.isArray(nationalityMatches)) {
            throw new Error('Invalid search response');
        }
        const persons = [...new Map([...textMatches, ...nationalityMatches]
            .map(person => [person.id, person])).values()];
        finishListRequest(version, persons, persons.length ? '' : 'No persons found');
    } catch (error) {
        console.error('Error searching persons:', error);
        finishListRequest(version, [], 'Search failed. Please try again.');
    }
}

function renderTable(persons) {
    const tbody = document.querySelector('#personTable tbody');
    tbody.innerHTML = '';

    persons.forEach(person => {
        const row = document.createElement('tr');
        [person.id, person.userName, person.firstName, person.lastName,
            person.email, person.birthDate, person.nationality || 'Not specified'].forEach(value => {
            const cell = document.createElement('td');
            cell.textContent = value == null ? '' : String(value);
            row.appendChild(cell);
        });
        const actions = document.createElement('td');
        [['Edit', 'btn-edit', editPerson], ['Delete', 'btn-delete', deletePerson]].forEach(([label, className, action]) => {
            const button = document.createElement('button');
            button.textContent = label;
            button.className = className;
            button.addEventListener('click', () => action(person.id));
            actions.appendChild(button);
        });
        row.appendChild(actions);
        tbody.appendChild(row);
    });
}

async function savePerson() {
    const id = document.getElementById('personId').value;
    const person = {
        userName: document.getElementById('userName').value,
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        birthDate: document.getElementById('birthDate').value,
        nationality: document.getElementById('nationality').value || null
    };

    if (id) person.id = parseInt(id);

    const method = id ? 'PUT' : 'POST';
    try {
        const response = await fetch(API_BASE_URL, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(person)
        });

        if (response.ok) {
            closeModal('personModal');
            loadPersons();
            document.getElementById('personForm').reset();
        } else {
            alert(await personErrorMessage(response));
        }
    } catch (error) {
        console.error('Error saving person:', error);
        alert('A network error occurred.');
    }
}

async function deletePerson(id) {
    if (!confirm('Are you sure you want to delete this person?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE' });
        if (response.ok) {
            loadPersons();
        } else {
            alert(await personErrorMessage(response));
        }
    } catch (error) {
        console.error('Error deleting person:', error);
        alert('A network error occurred.');
    }
}

async function deleteAllPersons() {
    if (!confirm('WARNING: This will delete ALL persons from the registry. Are you sure?')) return;

    try {
        const response = await fetch(API_BASE_URL, { method: 'DELETE' });
        if (response.ok) {
            loadPersons();
        } else {
            alert(await personErrorMessage(response));
        }
    } catch (error) {
        console.error('Error deleting all persons:', error);
        alert('A network error occurred.');
    }
}

async function deleteByEmail() {
    const email = document.getElementById('deleteEmail').value;
    if (!email) {
        alert('Please enter an email address.');
        return;
    }

    if (!confirm(`Are you sure you want to delete the person with email: ${email}?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/email/${encodeURIComponent(email)}`, { method: 'DELETE' });
        if (response.ok) {
            closeModal('deleteEmailModal');
            loadPersons();
            document.getElementById('deleteEmail').value = '';
        } else {
            alert(await personErrorMessage(response));
        }
    } catch (error) {
        console.error('Error deleting by email:', error);
        alert('A network error occurred.');
    }
}

// UI Helpers
function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
    if (modalId === 'personModal') {
        document.getElementById('modalTitle').innerText = 'Add New Person';
        document.getElementById('personId').value = '';
        document.getElementById('personForm').reset();
    }
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

async function editPerson(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/${id}`);
        if (!response.ok) {
            alert(await personErrorMessage(response));
            if (response.status === 404 || response.status === 410) await loadPersons();
            return;
        }
        const person = await response.json();
        
        openModal('personModal');
        document.getElementById('modalTitle').innerText = 'Edit Person';
        document.getElementById('personId').value = person.id;
        document.getElementById('userName').value = person.userName;
        document.getElementById('firstName').value = person.firstName;
        document.getElementById('lastName').value = person.lastName;
        document.getElementById('email').value = person.email;
        document.getElementById('birthDate').value = person.birthDate;
        document.getElementById('nationality').value = person.nationality ?? '';
    } catch (error) {
        console.error('Error fetching person details:', error);
        alert('A network or response error occurred while loading person details.');
    }
}

// Close modals when clicking outside
window.onclick = function(event) {
    if (event.target.className === 'modal') {
        event.target.style.display = 'none';
    }
}
