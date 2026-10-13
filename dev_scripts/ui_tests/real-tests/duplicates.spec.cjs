const { test, expect } = require('playwright/test');

// No route interception: the visible journey runs against actual Spring REST
// controllers and the configured database, using synthetic records only.
test('review and merge real persisted persons, retain audit and show survivor once (AC-021/022)', async ({ page, request }) => {
    const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const create = async (name, nationality) => {
        const response = await request.post('/person', { data: {
            userName: `${name}-${suffix}`, firstName: `Test${suffix}`, lastName: 'DuplicateJourney',
            email: `${name}-${suffix}@example.test`, birthDate: '1990-01-01', nationality
        } });
        expect(response.status()).toBe(201); return response.json();
    };
    const left = await create('one', 'French'), right = await create('two', 'German');
    await page.goto('/');
    await page.getByRole('button', { name: 'Duplicate Management', exact: true }).click();
    const scanned = page.waitForResponse(response => response.url().endsWith('/duplicate-cases/scan') && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Scan for duplicates' }).click();
    expect((await scanned).status()).toBe(200);
    const response = await request.get('/duplicate-cases?status=OPEN&size=100'); expect(response.status()).toBe(200);
    const candidate = (await response.json()).content.find(c => c.leftPersonId === Math.min(left.id, right.id) && c.rightPersonId === Math.max(left.id, right.id));
    expect(candidate).toBeTruthy();
    await page.getByRole('button', { name: `Review case ${candidate.id}`, exact: true }).click();
    await page.getByLabel('Canonical person (surviving record)').selectOption(String(left.id));
    for (const field of ['userName', 'firstName', 'lastName', 'email', 'birthDate', 'nationality'])
        await page.locator(`#source-${field}`).selectOption(String(field === 'nationality' ? left.id : right.id));
    await expect(page.locator('#mergePreview')).toContainText(right.userName);
    await expect(page.locator('#mergePreview')).toContainText('French');
    await page.getByLabel('Resolution reason (10–500 characters)').fill('Confirmed synthetic duplicate records');
    page.once('dialog', dialog => dialog.accept());
    const merged = page.waitForResponse(response => response.url().endsWith(`/duplicate-cases/${candidate.id}/merge`) && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Merge persons', exact: true }).click();
    const mergeResponse = await merged; expect(mergeResponse.status()).toBe(200);
    const audit = await mergeResponse.json();
    await expect(page.getByRole('heading', { name: `Case ${candidate.id}: MERGED` })).toBeVisible();
    await expect(page.getByRole('button', { name: `Review case ${candidate.id}`, exact: true })).toHaveCount(0);
    await page.getByRole('button', { name: 'Persons', exact: true }).click();
    const row = page.locator('#personTable tbody tr').filter({ hasText: right.userName });
    await expect(row).toHaveCount(1); await expect(row).toContainText('French');
    const retired = await request.get(`/person/${right.id}`); expect(retired.status()).toBe(410);
    expect((await retired.json()).canonicalPersonId).toBe(left.id);
    const history = await request.get(`/person-merges/${audit.id}`); expect(history.status()).toBe(200);
    const evidence = (await history.json()).evidence;
    expect(evidence.leftBefore.id).toBe(Math.min(left.id, right.id));
    expect(evidence.canonicalAfter.id).toBe(left.id); expect(evidence.canonicalAfter.nationality).toBe('French');
    // Nationality search and edit still use the actual backend after the merge.
    const saved = await request.get(`/person/${left.id}`); const survivor = await saved.json();
    survivor.firstName = 'Updated synthetic person';
    expect((await request.put('/person', { data: survivor })).status()).toBe(200);
    const nationality = await request.get('/person/nationality/french'); expect(nationality.status()).toBe(200);
    expect((await nationality.json()).filter(p => p.id === left.id)).toHaveLength(1);
    expect((await request.delete(`/person/${left.id}`)).status()).toBe(200);
    expect((await request.get(`/person-merges/${audit.id}`)).status()).toBe(200);
});
