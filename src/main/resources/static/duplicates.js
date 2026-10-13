const mergeFields = ['userName', 'firstName', 'lastName', 'email', 'birthDate', 'nationality'];
const fieldLabels = { userName: 'Username', firstName: 'First Name', lastName: 'Last Name', email: 'Email', birthDate: 'Birth Date', nationality: 'Nationality' };
let casePage = 0;
let caseListVersion = 0;
let caseDetailVersion = 0;
let currentCase = null;
let resolutionPending = false;
const caseDrafts = new Map();

function duplicateElement(tag, text, className) {
    const element = document.createElement(tag);
    if (text != null) element.textContent = String(text);
    if (className) element.className = className;
    return element;
}

async function duplicateRequest(path, body) {
    let response;
    try {
        response = await fetch(path, body === undefined ? {} : {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        });
    } catch (_) {
        throw { status: 0, message: 'Network error. Check the connection and try again.' };
    }
    if (!response.ok) {
        const messages = {
            400: 'Invalid request. Check the reason and all selections.',
            404: 'Record no longer available.',
            409: 'Conflict: a record changed. Review the refreshed case before submitting again.',
            410: 'This person has been merged. Refresh the person list.'
        };
        throw { status: response.status, message: messages[response.status] || 'Server error. Please try again.' };
    }
    try { return await response.json(); }
    catch (_) { throw { status: 500, message: 'Server returned an invalid response.' }; }
}

async function loadDuplicateCases() {
    const version = ++caseListVersion;
    const status = document.getElementById('duplicateStatus');
    const rows = document.getElementById('duplicateRows');
    rows.replaceChildren();
    status.textContent = 'Loading cases...';
    document.getElementById('previousCases').disabled = true;
    document.getElementById('nextCases').disabled = true;
    const query = new URLSearchParams({ status: document.getElementById('caseStatus').value, page: casePage, size: 20 });
    const confidence = document.getElementById('caseConfidence').value;
    if (confidence) query.set('confidence', confidence);
    try {
        const result = await duplicateRequest(`/duplicate-cases?${query}`);
        if (version !== caseListVersion) return;
        if (!result.content.length && casePage > 0 && result.totalPages <= casePage) {
            casePage = Math.max(0, result.totalPages - 1);
            return loadDuplicateCases();
        }
        result.content.forEach(value => {
            const row = duplicateElement('tr');
            [value.id, `${value.leftPersonId} / ${value.rightPersonId}`, value.status, value.score, value.confidence, value.reasons.join(', ')]
                .forEach(text => row.appendChild(duplicateElement('td', text)));
            const action = duplicateElement('td');
            const button = duplicateElement('button', `Review case ${value.id}`);
            button.className = 'duplicate-review-button';
            button.disabled = resolutionPending;
            button.addEventListener('click', () => loadCaseDetail(value.id));
            action.appendChild(button); row.appendChild(action); rows.appendChild(row);
        });
        status.textContent = result.content.length ? '' : 'No duplicate cases found';
        document.getElementById('casePageSummary').textContent = `${result.totalElements} cases · Page ${result.totalPages ? casePage + 1 : 0} of ${result.totalPages}`;
        document.getElementById('previousCases').disabled = casePage === 0;
        document.getElementById('nextCases').disabled = casePage + 1 >= result.totalPages;
    } catch (error) {
        if (version === caseListVersion) status.textContent = error.message;
    }
}

async function scanDuplicateCases() {
    const button = document.getElementById('scanDuplicates');
    if (button.disabled || resolutionPending) return;
    button.disabled = true;
    const summary = document.getElementById('scanSummary');
    summary.textContent = 'Scanning...';
    try {
        const result = await duplicateRequest('/duplicate-cases/scan', {});
        summary.textContent = `${result.activePersons} active persons; ${result.evaluatedCandidatePairs} candidate pairs evaluated; ${result.matchedPairs} matches; ${result.createdCases} new, ${result.reopenedCases} reopened, ${result.obsoleteCases} obsolete cases.`;
        casePage = 0;
        if (currentCase) await loadCaseDetail(currentCase.duplicateCase.id, true);
        await loadDuplicateCases();
    } catch (error) { summary.textContent = error.message; }
    finally { button.disabled = false; }
}

async function loadCaseDetail(id, retainText = false, resolutionRefresh = false) {
    if (resolutionPending && !resolutionRefresh) return;
    const version = ++caseDetailVersion;
    if (retainText) caseDrafts.set(id, {
        reason: document.getElementById('resolutionReason')?.value || caseDrafts.get(id)?.reason || '',
        operator: document.getElementById('resolutionOperator')?.value || caseDrafts.get(id)?.operator || ''
    });
    const draft = caseDrafts.get(id) || { reason: '', operator: '' };
    const review = document.getElementById('caseReview');
    review.hidden = false;
    review.replaceChildren(duplicateElement('p', 'Loading case...'));
    currentCase = null;
    try {
        const detail = await duplicateRequest(`/duplicate-cases/${id}`);
        if (version !== caseDetailVersion) return;
        currentCase = detail;
        renderCaseReview(detail, draft.reason, draft.operator);
    } catch (error) {
        if (version !== caseDetailVersion) return;
        review.replaceChildren(duplicateElement('p', error.message, 'case-error'));
        if (caseDrafts.has(id)) {
            const fields = duplicateElement('div', null, 'resolution-controls');
            for (const [labelText, field, key, tag] of [
                ['Resolution reason (10–500 characters)', 'resolutionReason', 'reason', 'textarea'],
                ['Operator name (optional)', 'resolutionOperator', 'operator', 'input']
            ]) {
                const label = duplicateElement('label', labelText);
                const input = duplicateElement(tag); input.id = field; input.value = draft[key];
                input.addEventListener('input', () => caseDrafts.set(id, {
                    reason: document.getElementById('resolutionReason').value,
                    operator: document.getElementById('resolutionOperator').value
                }));
                label.appendChild(input); fields.appendChild(label);
            }
            review.appendChild(fields);
        }
        const retry = duplicateElement('button', 'Refresh case');
        retry.addEventListener('click', () => loadCaseDetail(id, true)); review.appendChild(retry);
    }
}

function renderCaseReview(detail, reasonText, operatorText) {
    const review = document.getElementById('caseReview');
    review.replaceChildren();
    const c = detail.duplicateCase;
    review.appendChild(duplicateElement('h3', `Case ${c.id}: ${c.status}`));
    review.appendChild(duplicateElement('p', `Score ${c.score} · ${c.confidence} · ${c.reasons.join(', ')}`));
    review.appendChild(duplicateElement('p', `First detected: ${c.firstDetectedAt} · Last detected: ${c.lastDetectedAt} · Case version: ${c.version}`));
    if (c.resolvedAt) review.appendChild(duplicateElement('p', `Resolved: ${c.resolvedAt} · ${c.resolutionReason} · ${c.operatorName || 'Operator not provided'}`));
    const pair = [detail.leftPerson, detail.rightPerson];
    const available = pair.every(p => p && p.status === 'ACTIVE');
    const canResolve = c.status === 'OPEN';
    const comparison = duplicateElement('table');
    comparison.setAttribute('aria-label', 'Person comparison');
    const headings = duplicateElement('tr');
    ['Field', pair[0] ? `Person ${pair[0].id} (version ${pair[0].version}, ${pair[0].status})` : 'Person unavailable',
        pair[1] ? `Person ${pair[1].id} (version ${pair[1].version}, ${pair[1].status})` : 'Person unavailable', 'Source for final record']
        .forEach(text => headings.appendChild(duplicateElement('th', text)));
    comparison.appendChild(headings);
    mergeFields.forEach(field => {
        const equal = pair[0]?.[field] === pair[1]?.[field];
        const row = duplicateElement('tr', null, equal ? 'fields-equal' : 'fields-different');
        row.appendChild(duplicateElement('th', `${fieldLabels[field]} (${equal ? 'equal' : 'different'})`));
        pair.forEach(p => row.appendChild(duplicateElement('td', p ? p[field] ?? 'Not specified' : 'Unavailable')));
        const selectionCell = duplicateElement('td');
        if (canResolve && available) {
            const select = duplicateElement('select');
            select.id = `source-${field}`;
            select.setAttribute('aria-label', `${fieldLabels[field]} source`);
            select.appendChild(new Option('Choose source', ''));
            pair.forEach(p => select.appendChild(new Option(`Person ${p.id}`, p.id)));
            select.addEventListener('change', updateMergePreview);
            selectionCell.appendChild(select);
        }
        row.appendChild(selectionCell); comparison.appendChild(row);
    });
    const container = duplicateElement('div', null, 'table-container');
    container.appendChild(comparison); review.appendChild(container);
    const feedback = duplicateElement('p', null, 'case-error');
    feedback.id = 'caseFeedback'; feedback.setAttribute('role', 'status');
    review.appendChild(feedback);
    if (canResolve) {
        const controls = duplicateElement('div', null, 'resolution-controls');
        if (available) {
            const label = duplicateElement('label', 'Canonical person (surviving record)');
            const canonical = duplicateElement('select'); canonical.id = 'canonicalPerson';
            canonical.setAttribute('aria-label', 'Canonical person (surviving record)');
            canonical.appendChild(new Option('Choose canonical person', ''));
            pair.forEach(p => canonical.appendChild(new Option(`Person ${p.id}`, p.id)));
            label.appendChild(canonical); controls.appendChild(label);
            canonical.addEventListener('change', updateMergePreview);
        } else controls.appendChild(duplicateElement('p', 'Merge unavailable: both records must be active and present.'));
        const reasonLabel = duplicateElement('label', 'Resolution reason (10–500 characters)');
        const reason = duplicateElement('textarea'); reason.id = 'resolutionReason'; reason.value = reasonText;
        reason.maxLength = 500; reasonLabel.appendChild(reason); controls.appendChild(reasonLabel);
        const operatorLabel = duplicateElement('label', 'Operator name (optional)');
        const operator = duplicateElement('input'); operator.id = 'resolutionOperator'; operator.value = operatorText;
        operator.maxLength = 100; operatorLabel.appendChild(operator); controls.appendChild(operatorLabel);
        const dismiss = duplicateElement('button', 'Dismiss case'); dismiss.id = 'dismissCase';
        dismiss.addEventListener('click', () => submitCaseResolution('dismiss')); controls.appendChild(dismiss);
        if (available) {
            controls.appendChild(duplicateElement('h4', 'Final record preview'));
            const preview = duplicateElement('dl'); preview.id = 'mergePreview'; controls.appendChild(preview);
            const merge = duplicateElement('button', 'Merge persons', 'btn-danger'); merge.id = 'mergeCase';
            merge.addEventListener('click', () => submitCaseResolution('merge')); controls.appendChild(merge);
        }
        review.appendChild(controls);
        const updateDraft = () => {
            caseDrafts.set(c.id, { reason: reason.value, operator: operator.value });
            updateMergePreview();
        };
        reason.addEventListener('input', updateDraft); operator.addEventListener('input', updateDraft);
        updateMergePreview();
    }
    review.appendChild(duplicateElement('h4', 'Event history'));
    const history = duplicateElement('ol');
    detail.events.forEach(event => history.appendChild(duplicateElement('li', `${event.occurredAt} · ${event.type}${event.reason ? ` · ${event.reason}` : ''}${event.operatorName ? ` · ${event.operatorName}` : ''}`)));
    review.appendChild(history);
    if (c.mergeAuditId) {
        const button = duplicateElement('button', 'View merge audit');
        button.addEventListener('click', async () => {
            try {
                const audit = await duplicateRequest(`/person-merges/${c.mergeAuditId}`);
                const pre = duplicateElement('pre', JSON.stringify(audit, null, 2));
                button.replaceWith(pre);
            } catch (error) { feedback.textContent = error.message; }
        });
        review.appendChild(button);
    }
}

function updateMergePreview() {
    const reason = document.getElementById('resolutionReason')?.value.trim() || '';
    const operatorInput = document.getElementById('resolutionOperator')?.value || '';
    const valid = reason.length >= 10 && reason.length <= 500 && (!operatorInput || (operatorInput.trim().length >= 1 && operatorInput.trim().length <= 100));
    const dismiss = document.getElementById('dismissCase');
    if (dismiss) dismiss.disabled = !valid || resolutionPending;
    const preview = document.getElementById('mergePreview');
    if (!preview) return;
    preview.replaceChildren();
    let complete = !!document.getElementById('canonicalPerson').value;
    mergeFields.forEach(field => {
        const value = document.getElementById(`source-${field}`).value;
        const source = [currentCase.leftPerson, currentCase.rightPerson].find(p => String(p.id) === value);
        if (!source) complete = false;
        preview.appendChild(duplicateElement('dt', fieldLabels[field]));
        preview.appendChild(duplicateElement('dd', source ? source[field] ?? 'Not specified' : 'Choose source'));
    });
    document.getElementById('mergeCase').disabled = !valid || !complete || resolutionPending;
}

async function submitCaseResolution(operation) {
    const button = document.getElementById(operation === 'merge' ? 'mergeCase' : 'dismissCase');
    if (!button || button.disabled || resolutionPending || !currentCase) return;
    const body = {
        expectedCaseVersion: currentCase.duplicateCase.version,
        reason: document.getElementById('resolutionReason').value.trim()
    };
    const operator = document.getElementById('resolutionOperator').value;
    if (operator) body.operatorName = operator.trim();
    if (operation === 'merge') {
        body.canonicalPersonId = Number(document.getElementById('canonicalPerson').value);
        body.expectedPersonVersions = Object.fromEntries([currentCase.leftPerson, currentCase.rightPerson].map(p => [p.id, p.version]));
        body.fieldSources = Object.fromEntries(mergeFields.map(field => [field, Number(document.getElementById(`source-${field}`).value)]));
    }
    if (!confirm(operation === 'merge' ? 'This merge is irreversible. Confirm the canonical person and every field in the final preview before continuing.' : 'Confirm dismissal of this duplicate case?')) return;
    resolutionPending = true; updateMergePreview();
    document.querySelectorAll('.duplicate-review-button').forEach(review => { review.disabled = true; });
    document.getElementById('scanDuplicates').disabled = true;
    const id = currentCase.duplicateCase.id;
    try {
        await duplicateRequest(`/duplicate-cases/${id}/${operation}`, body);
        caseDrafts.delete(id);
        await loadCaseDetail(id, false, true);
        await loadDuplicateCases();
        await loadPersons();
    } catch (error) {
        if (error.status === 409) await loadCaseDetail(id, true, true);
        const feedback = document.getElementById('caseFeedback') || document.getElementById('duplicateStatus');
        feedback.textContent = error.message;
    } finally {
        resolutionPending = false; updateMergePreview();
        document.querySelectorAll('.duplicate-review-button').forEach(review => { review.disabled = false; });
        document.getElementById('scanDuplicates').disabled = false;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const showView = duplicates => {
        document.getElementById('personsView').hidden = duplicates;
        document.getElementById('duplicatesView').hidden = !duplicates;
        document.getElementById('personsViewButton').setAttribute('aria-pressed', String(!duplicates));
        document.getElementById('duplicatesViewButton').setAttribute('aria-pressed', String(duplicates));
        if (duplicates) loadDuplicateCases();
    };
    document.getElementById('personsViewButton').addEventListener('click', () => showView(false));
    document.getElementById('duplicatesViewButton').addEventListener('click', () => showView(true));
    document.getElementById('scanDuplicates').addEventListener('click', scanDuplicateCases);
    for (const id of ['caseStatus', 'caseConfidence']) document.getElementById(id).addEventListener('change', () => { casePage = 0; loadDuplicateCases(); });
    document.getElementById('previousCases').addEventListener('click', () => { casePage--; loadDuplicateCases(); });
    document.getElementById('nextCases').addEventListener('click', () => { casePage++; loadDuplicateCases(); });
});
