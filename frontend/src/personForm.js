export function personPayload(form) {
    const value = id => form.querySelector(`#${id}`).value;
    const person = Object.fromEntries(['userName', 'firstName', 'lastName', 'email', 'birthDate'].map(id => [id, value(id)]));
    person.nationality = value('nationality') || null;
    if (value('personId')) person.id = Number(value('personId'));
    return person;
}
