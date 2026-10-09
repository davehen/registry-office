const API_BASE_URL = '/person';

document.addEventListener('DOMContentLoaded', () => {
    loadPersons();

    document.getElementById('personForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await savePerson();
    });
});

async function loadPersons() {
    try {
        const response = await fetch(API_BASE_URL);
        const persons = await response.json();
        renderTable(persons);
    } catch (error) {
        console.error('Error loading persons:', error);
        alert('Failed to load persons from server.');
    }
}

async function searchPersons() {
    const text = document.getElementById('searchInput').value;
    if (!text) {
        loadPersons();
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/search/${encodeURIComponent(text)}`);
        if (response.status === 404) {
            alert('No persons found for search: ' + text);
            return;
        }
        const persons = await response.json();
        renderTable(persons);
    } catch (error) {
        console.error('Error searching persons:', error);
        alert('Search failed.');
    }
}

function renderTable(persons) {
    const tbody = document.querySelector('#personTable tbody');
    tbody.innerHTML = '';

    persons.forEach(person => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${person.id}</td>
            <td>${person.userName}</td>
            <td>${person.firstName}</td>
            <td>${person.lastName}</td>
            <td>${person.email}</td>
            <td>${person.birthDate}</td>
            <td>
                <button class="btn-edit" onclick="editPerson(${person.id})">Edit</button>
                <button class="btn-delete" onclick="deletePerson(${person.id})">Delete</button>
            </td>
        `;
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
        birthDate: document.getElementById('birthDate').value
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
            const errorData = await response.json();
            alert('Error: ' + (errorData.message || 'Failed to save person'));
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
            alert('Failed to delete person.');
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
            alert('Failed to delete all persons.');
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
            const errorData = await response.json();
            alert('Error: ' + (errorData.message || 'Person not found.'));
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
        const person = await response.json();
        
        openModal('personModal');
        document.getElementById('modalTitle').innerText = 'Edit Person';
        document.getElementById('personId').value = person.id;
        document.getElementById('userName').value = person.userName;
        document.getElementById('firstName').value = person.firstName;
        document.getElementById('lastName').value = person.lastName;
        document.getElementById('email').value = person.email;
        document.getElementById('birthDate').value = person.birthDate;
    } catch (error) {
        console.error('Error fetching person details:', error);
        alert('Failed to fetch person details.');
    }
}

// Close modals when clicking outside
window.onclick = function(event) {
    if (event.target.className === 'modal') {
        event.target.style.display = 'none';
    }
}