const { test, expect } = require('@playwright/test');

const fields = ['userName', 'firstName', 'lastName', 'email', 'birthDate', 'nationality'];
const person = id => ({ id, userName: `user${id}`, firstName: 'First', lastName: 'Last', email: `user${id}@example.test`, birthDate: '1990-01-01', nationality: id === 1 ? 'French' : 'German', status: 'ACTIVE', version: 0 });
const caseValue = (id = 1) => ({ id, version: 0, leftPersonId: 1, rightPersonId: 2, status: 'OPEN', score: 90, confidence: 'HIGH', reasons: ['FULL_NAME_BIRTH_DATE'], firstDetectedAt: '2026-01-01T00:00:00Z', lastDetectedAt: '2026-01-01T00:00:00Z' });

async function setup(page, options = {}) {
    let detail = { duplicateCase: caseValue(), leftPerson: person(1), rightPerson: person(2), events: [{ type: 'DETECTED', occurredAt: '2026-01-01T00:00:00Z' }] };
    if (options.markup) {
        detail.leftPerson.firstName = '<img src=x onerror="window.duplicateExecuted=true">';
        detail.events[0].reason = '<script>alert(1)</script>';
        detail.events[0].operatorName = '<b>Operator</b>';
    }
    let calls = [];
    let detailGets = 0;
    await page.route('**/person', route => route.fulfill({ json: [person(1), person(2)] }));
    await page.route('**/duplicate-cases**', async route => {
        const url = new URL(route.request().url());
        const pathname = url.pathname;
        const method = route.request().method();
        calls.push({ pathname, method, query: url.searchParams, body: method === 'POST' ? route.request().postDataJSON() : null });
        if (pathname.endsWith('/scan')) return route.fulfill({ json: { activePersons: 2, evaluatedCandidatePairs: 1, matchedPairs: 1, createdCases: 1, reopenedCases: 0, obsoleteCases: 0 } });
        if (pathname === '/duplicate-cases') {
            const content = options.pages ? [caseValue(Number(url.searchParams.get('page')) + 1)] : detail.duplicateCase.status === url.searchParams.get('status') ? [detail.duplicateCase] : [];
            return route.fulfill({ json: { content, page: Number(url.searchParams.get('page')), size: 20, totalElements: options.pages ? 21 : content.length, totalPages: options.pages ? 2 : content.length ? 1 : 0 } });
        }
        if (pathname.endsWith('/dismiss') || pathname.endsWith('/merge')) {
            if (options.failure === 'network') return route.abort();
            if (options.failure) {
                if (options.failure === 409) detail.duplicateCase.version++;
                return route.fulfill({ status: options.failure, json: { message: 'synthetic failure' } });
            }
            detail.duplicateCase.status = pathname.endsWith('/merge') ? 'MERGED' : 'DISMISSED';
            detail.events.push({ type: detail.duplicateCase.status, occurredAt: '2026-01-02T00:00:00Z' });
            return route.fulfill({ json: pathname.endsWith('/merge') ? { id: 1 } : detail });
        }
        detailGets++;
        if (options.refreshFailure && detailGets === 2) return route.fulfill({ status: 500, json: {} });
        if (options.detailFailure) return route.fulfill({ status: options.detailFailure, json: {} });
        return route.fulfill({ json: detail });
    });
    await page.goto('/');
    await page.getByRole('button', { name: 'Duplicate Management', exact: true }).click();
    await page.getByRole('button', { name: 'Review case 1', exact: true }).click();
    if (!options.detailFailure) await expect(page.getByRole('heading', { name: 'Case 1: OPEN' })).toBeVisible();
    return calls;
}

async function selectMerge(page) {
    await page.getByLabel('Canonical person (surviving record)').selectOption('1');
    for (const field of fields) await page.locator(`#source-${field}`).selectOption(field === 'nationality' ? '1' : '2');
    await page.getByLabel('Resolution reason (10–500 characters)').fill('Verified matching individuals');
}

test('scan, filters, pagination and comparison expose case state', async ({ page }) => {
    const calls = await setup(page, { pages: true });
    await expect(page.getByRole('table', { name: 'Person comparison' })).toContainText('different');
    await expect(page.getByRole('table', { name: 'Person comparison' })).toContainText('version 0');
    await page.getByRole('button', { name: 'Scan for duplicates' }).click();
    await expect(page.locator('#scanSummary')).toContainText('1 candidate pairs evaluated');
    await page.getByLabel('Confidence', { exact: true }).selectOption('HIGH');
    await expect.poll(() => calls.filter(c => c.pathname === '/duplicate-cases').at(-1)?.query.get('confidence')).toBe('HIGH');
    await page.getByRole('button', { name: 'Next', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Review case 2' })).toBeVisible();
    await page.getByRole('button', { name: 'Previous', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Review case 1' })).toBeVisible();
});

test('merge requires every source, canonical, reason, preview and explicit confirmation', async ({ page }) => {
    const calls = await setup(page);
    const merge = page.getByRole('button', { name: 'Merge persons', exact: true });
    await expect(merge).toBeDisabled();
    await page.getByLabel('Resolution reason (10–500 characters)').fill('Verified matching individuals');
    await expect(merge).toBeDisabled();
    await selectMerge(page);
    await expect(page.locator('#mergePreview')).toContainText('user2');
    await expect(page.locator('#mergePreview')).toContainText('French');
    await page.getByLabel('Operator name (optional)').fill('  ');
    await expect(merge).toBeDisabled();
    await page.getByLabel('Operator name (optional)').fill('Reviewer');
    await expect(merge).toBeEnabled();
    page.once('dialog', dialog => dialog.dismiss());
    await merge.click();
    expect(calls.filter(c => c.pathname.endsWith('/merge'))).toHaveLength(0);
    page.once('dialog', dialog => { expect(dialog.message()).toContain('irreversible'); return dialog.accept(); });
    await merge.click();
    await expect(page.getByRole('heading', { name: 'Case 1: MERGED' })).toBeVisible();
    await expect(page.locator('#duplicateRows tr')).toHaveCount(0);
    const body = calls.find(c => c.pathname.endsWith('/merge')).body;
    expect(body.canonicalPersonId).toBe(1);
    expect(body.fieldSources).toEqual({ userName: 2, firstName: 2, lastName: 2, email: 2, birthDate: 2, nationality: 1 });
    expect(body.expectedPersonVersions).toEqual({ 1: 0, 2: 0 });
});

test('dismissal validates reason and asks for confirmation', async ({ page }) => {
    const calls = await setup(page);
    const dismiss = page.getByRole('button', { name: 'Dismiss case', exact: true });
    await page.getByLabel('Resolution reason (10–500 characters)').fill('short');
    await expect(dismiss).toBeDisabled();
    await page.getByLabel('Resolution reason (10–500 characters)').fill('Confirmed separate identities');
    page.once('dialog', dialog => dialog.accept()); await dismiss.click();
    await expect(page.getByRole('heading', { name: 'Case 1: DISMISSED' })).toBeVisible();
    expect(calls.find(c => c.pathname.endsWith('/dismiss')).body.reason).toBe('Confirmed separate identities');
});

for (const failure of [400, 404, 409, 410, 500, 'network']) {
    test(`${failure} resolution failure keeps text and never automatically retries`, async ({ page }) => {
        const calls = await setup(page, { failure });
        await selectMerge(page);
        await page.getByLabel('Operator name (optional)').fill('Reviewer');
        page.once('dialog', dialog => dialog.accept());
        await page.getByRole('button', { name: 'Merge persons', exact: true }).click();
        await expect(page.locator('#caseFeedback')).toContainText(failure === 'network' ? 'Network error' : failure === 409 ? 'Conflict' : failure === 410 ? 'merged' : failure === 404 ? 'no longer available' : failure === 400 ? 'Invalid request' : 'Server error');
        await expect(page.getByLabel('Resolution reason (10–500 characters)')).toHaveValue('Verified matching individuals');
        await expect(page.getByLabel('Operator name (optional)')).toHaveValue('Reviewer');
        expect(calls.filter(c => c.pathname.endsWith('/merge'))).toHaveLength(1);
        if (failure === 409) {
            await expect(page.locator('#caseReview')).toContainText('Case version: 1');
            await expect(page.getByRole('button', { name: 'Merge persons', exact: true })).toBeDisabled();
        }
    });
}

test('markup in comparison, history and operator is literal text', async ({ page }) => {
    await setup(page, { markup: true });
    await expect(page.locator('#caseReview')).toContainText('<script>alert(1)</script>');
    await expect(page.locator('#caseReview')).toContainText('<b>Operator</b>');
    await expect(page.locator('#caseReview img, #caseReview script, #caseReview b')).toHaveCount(0);
    expect(await page.evaluate(() => window.duplicateExecuted)).toBeUndefined();
});

test('failed conflict refresh retains draft and recovers only after explicit refresh', async ({ page }) => {
    const calls = await setup(page, { failure: 409, refreshFailure: true });
    await selectMerge(page);
    await page.getByLabel('Operator name (optional)').fill('Reviewer');
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Merge persons', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Refresh case' })).toBeVisible();
    await expect(page.getByLabel('Resolution reason (10–500 characters)')).toHaveValue('Verified matching individuals');
    await expect(page.getByLabel('Operator name (optional)')).toHaveValue('Reviewer');
    await page.getByLabel('Resolution reason (10–500 characters)').fill('Draft amended while offline');
    await page.getByRole('button', { name: 'Refresh case' }).click();
    await expect(page.locator('#caseReview')).toContainText('Case version: 1');
    await expect(page.getByLabel('Resolution reason (10–500 characters)')).toHaveValue('Draft amended while offline');
    await expect(page.getByLabel('Operator name (optional)')).toHaveValue('Reviewer');
    await expect(page.getByRole('button', { name: 'Merge persons', exact: true })).toBeDisabled();
    expect(calls.filter(c => c.pathname.endsWith('/merge'))).toHaveLength(1);
});

test('pending resolution prevents another case replacing its draft context', async ({ page }) => {
    const calls = await setup(page, { pages: true });
    await selectMerge(page);
    await page.getByLabel('Operator name (optional)').fill('Reviewer of case one');
    let release;
    const held = new Promise(resolve => { release = resolve; });
    let started;
    const pending = new Promise(resolve => { started = resolve; });
    await page.route('**/duplicate-cases/1/merge', async route => {
        started(); await held;
        return route.fulfill({ status: 409, json: { message: 'Version conflict' } });
    });
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Merge persons', exact: true }).click();
    await pending;
    await page.getByRole('button', { name: 'Next', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Review case 2' })).toBeDisabled();
    await page.evaluate(async () => (await import('/src/duplicates.js')).loadCaseDetail(2));
    expect(calls.some(call => call.pathname === '/duplicate-cases/2')).toBe(false);
    release();
    await expect(page.locator('#caseFeedback')).toContainText('Conflict');
    await expect(page.getByLabel('Resolution reason (10–500 characters)')).toHaveValue('Verified matching individuals');
    await expect(page.getByLabel('Operator name (optional)')).toHaveValue('Reviewer of case one');
    await expect(page.getByRole('button', { name: 'Review case 2' })).toBeEnabled();
});

for (const retiredStatus of [404, 410]) {
    test(`stale person Edit handles ${retiredStatus} without opening a bogus form`, async ({ page }) => {
        await setup(page);
        await page.route('**/person/1', route => route.fulfill({ status: retiredStatus, json: { message: 'Person unavailable', canonicalPersonId: 2, canonicalUri: '/person/2' } }));
        await page.getByRole('button', { name: 'Persons', exact: true }).click();
        page.once('dialog', dialog => {
            expect(dialog.message()).toContain(retiredStatus === 410 ? 'merged' : 'no longer available');
            return dialog.accept();
        });
        const fetched = page.waitForResponse(response => response.url().endsWith('/person/1'));
        await page.locator('#personTable tbody tr').first().getByRole('button', { name: 'Edit', exact: true }).click();
        await fetched;
        await expect(page.locator('#personModal')).toBeHidden();
    });
}
