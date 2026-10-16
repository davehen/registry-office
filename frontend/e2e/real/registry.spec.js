import { test, expect } from '@playwright/test';

const backend = 'http://127.0.0.1:18081';
test.beforeEach(async ({ request }) => {
    const response = await request.delete(`${backend}/person`);
    expect(response.status()).toBe(200);
});

async function fillPerson(page, values) {
    for (const [label, value] of Object.entries(values)) await page.getByLabel(label, { exact: true }).fill(value);
}

test('real separate-origin CRUD, nationality search, backend errors and deletion actions', async ({ page }) => {
    const requests = [];
    page.on('request', request => { if (new URL(request.url()).pathname.startsWith('/person')) requests.push(request); });
    await page.goto('/');
    await expect(page.locator('#searchStatus')).toHaveText('No persons found');
    const unique = `browser-${Date.now()}`;
    await page.getByRole('button', { name: 'Add New Person', exact: true }).click();
    await fillPerson(page, { Username: unique, 'First Name': 'Real', 'Last Name': 'Browser', Email: `${unique}@example.test`, 'Birth Date': '1990-01-01', Nationality: 'French' });
    const created = page.waitForResponse(response => response.url() === `${backend}/person` && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Save Person', exact: true }).click();
    const createResponse = await created; expect(createResponse.status()).toBe(201);
    expect(createResponse.headers()['access-control-allow-origin']).toBe('http://127.0.0.1:4174');
    expect(createResponse.headers()['access-control-expose-headers']).toContain('Location');
    const row = page.locator('#personTable tbody tr').filter({ hasText: unique });
    await expect(row).toContainText('French');
    await row.getByRole('button', { name: 'Edit', exact: true }).click();
    await page.getByLabel('First Name', { exact: true }).fill('Updated');
    await page.getByRole('button', { name: 'Save Person', exact: true }).click();
    await expect(row).toContainText('Updated');
    await page.getByRole('textbox', { name: 'Search persons', exact: true }).fill(' french ');
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(row).toContainText('French');
    await page.getByRole('textbox', { name: 'Search persons', exact: true }).fill('NoSuchRecord');
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(page.locator('#searchStatus')).toHaveText('No persons found');
    await page.getByRole('textbox', { name: 'Search persons', exact: true }).fill('');
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(row).toHaveCount(1);
    // Duplicate POST reaches the real backend and its error is visible to the user.
    await page.getByRole('button', { name: 'Add New Person', exact: true }).click();
    await fillPerson(page, { Username: unique, 'First Name': 'Other', 'Last Name': 'Person', Email: `other-${unique}@example.test`, 'Birth Date': '1990-01-01' });
    const dialog = new Promise(resolve => page.once('dialog', async error => {
        const message = error.message(); await error.accept(); resolve(message);
    }));
    await page.getByRole('button', { name: 'Save Person', exact: true }).click();
    expect(await dialog).toMatch(/username|user.?name|conflict|duplicate/i);
    await expect(page.locator('#personModal')).toBeVisible();
    await page.locator('#personModal .close').click();
    page.once('dialog', dialog => dialog.accept());
    await row.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(row).toHaveCount(0);
    for (const action of ['email', 'all']) {
        await page.getByRole('button', { name: 'Add New Person', exact: true }).click();
        await fillPerson(page, { Username: `${unique}-${action}`, 'First Name': 'Delete', 'Last Name': 'Example', Email: `${unique}-${action}@example.test`, 'Birth Date': '1990-01-01' });
        await page.getByRole('button', { name: 'Save Person', exact: true }).click();
        await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
        if (action === 'email') {
            await page.getByRole('button', { name: 'Delete by Email', exact: true }).click();
            await page.getByLabel('Email Address', { exact: true }).fill(`${unique}-email@example.test`);
            page.once('dialog', dialog => dialog.accept());
            await page.getByRole('button', { name: 'Delete Person', exact: true }).click();
        } else {
            page.once('dialog', dialog => dialog.accept());
            await page.getByRole('button', { name: 'Delete All', exact: true }).click();
        }
        await expect(page.locator('#personTable tbody tr')).toHaveCount(0);
    }
    expect(requests.length).toBeGreaterThan(5);
    expect(requests.every(request => new URL(request.url()).origin === backend)).toBe(true);
    expect(requests.some(request => request.method() === 'PUT')).toBe(true);
});

test('a foreign frontend origin is not granted CORS access', async ({ request }) => {
    const response = await request.get(`${backend}/person`, { headers: { Origin: 'http://unapproved.example.test' } });
    expect(response.status()).toBe(403);
    expect(response.headers()['access-control-allow-origin']).toBeUndefined();
});
