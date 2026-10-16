const { test, expect } = require('@playwright/test');

const person = (id, nationality = null, userName = `user${id}`) => ({
    id, userName, firstName: 'First', lastName: 'Last',
    email: `${userName}@example.test`, birthDate: '1990-01-01', nationality
});

// Browser tests use the real static UI with controlled API responses. Existing
// Java API integration tests cover actual backend matching and persistence.
async function setup(page, initial = [person(1, 'French')], overrides = {}) {
    let people = initial.map(value => ({ ...value }));
    const requests = [];
    await page.route('**/person**', async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (!url.pathname.startsWith('/person')) return route.continue();
        const method = request.method();
        const pathname = decodeURIComponent(url.pathname);
        requests.push({ method, pathname, body: request.postDataJSON() });
        const reply = (body, status = 200) => route.fulfill({ status, json: body });
        if (pathname.startsWith('/person/search/')) {
            if (overrides.text) return overrides.text(route, pathname);
            const query = pathname.slice('/person/search/'.length).toLowerCase();
            const matches = people.filter(p => [p.userName, p.firstName, p.lastName, p.email]
                .some(value => value.toLowerCase().includes(query)));
            return reply(matches.length ? matches : { message: 'No persons found' }, matches.length ? 200 : 404);
        }
        if (pathname.startsWith('/person/nationality/')) {
            if (overrides.nationality) return overrides.nationality(route, pathname);
            const query = pathname.slice('/person/nationality/'.length).trim().toLowerCase();
            return reply(people.filter(p => p.nationality != null && p.nationality.toLowerCase() === query));
        }
        if (method === 'POST' || method === 'PUT') {
            const body = request.postDataJSON();
            if (method === 'POST') body.id = Math.max(0, ...people.map(p => p.id)) + 1;
            people = [...people.filter(p => p.id !== body.id), body];
            return reply(body, method === 'POST' ? 201 : 200);
        }
        if (method === 'DELETE') {
            if (pathname === '/person') people = [];
            else if (pathname.startsWith('/person/email/')) people = people.filter(p => p.email !== pathname.slice('/person/email/'.length));
            else people = people.filter(p => p.id !== Number(pathname.split('/').pop()));
            return reply({});
        }
        return reply(pathname === '/person' ? people : people.find(p => p.id === Number(pathname.split('/').pop())));
    });
    await page.goto('/');
    await expect(page.locator('#personTable')).toHaveAttribute('aria-busy', 'false');
    return requests;
}

async function search(page, value) {
    await page.getByRole('textbox', { name: 'Search persons', exact: true }).fill(value);
    await page.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(page.locator('#personTable')).toHaveAttribute('aria-busy', 'false');
}

async function fillNewPerson(page) {
    await page.getByRole('button', { name: 'Add New Person' }).click();
    for (const [label, value] of Object.entries({ Username: 'newuser', 'First Name': 'New', 'Last Name': 'Person', Email: 'new@example.test', 'Birth Date': '1990-01-01' })) {
        await page.getByLabel(label, { exact: true }).fill(value);
    }
}

test('create nationality, show saved value and reset for next person (AC-001/005)', async ({ page }) => {
    const requests = await setup(page, []);
    await fillNewPerson(page);
    await page.getByLabel('Nationality', { exact: true }).fill('French');
    await page.getByRole('button', { name: 'Save Person' }).click();
    await expect(page.locator('#personTable tbody')).toContainText('French');
    expect(requests.find(r => r.method === 'POST').body.nationality).toBe('French');
    await page.getByRole('button', { name: 'Add New Person' }).click();
    await expect(page.getByLabel('Nationality', { exact: true })).toHaveValue('');
});

test('empty optional field submits null (AC-002)', async ({ page }) => {
    const requests = await setup(page, []);
    await fillNewPerson(page);
    await page.getByRole('button', { name: 'Save Person' }).click();
    await expect(page.locator('#personTable tbody')).toContainText('Not specified');
    expect(requests.find(r => r.method === 'POST').body.nationality).toBeNull();
});

test('edit unrelated field preserves nationality; change and clear it (AC-003/004)', async ({ page }) => {
    const requests = await setup(page);
    await page.getByRole('button', { name: 'Edit', exact: true }).click();
    await expect(page.getByLabel('Nationality', { exact: true })).toHaveValue('French');
    await page.getByLabel('First Name', { exact: true }).fill('Updated');
    await page.getByRole('button', { name: 'Save Person' }).click();
    await expect(page.locator('#personTable tbody')).toContainText('Updated');
    expect(requests.filter(r => r.method === 'PUT').at(-1).body.nationality).toBe('French');
    await page.getByRole('button', { name: 'Edit', exact: true }).click();
    await page.getByLabel('Nationality', { exact: true }).fill('German');
    await page.getByRole('button', { name: 'Save Person' }).click();
    await expect(page.locator('#personTable tbody')).toContainText('German');
    await page.getByRole('button', { name: 'Edit', exact: true }).click();
    await page.getByLabel('Nationality', { exact: true }).fill('');
    await page.getByRole('button', { name: 'Save Person' }).click();
    await expect(page.locator('#personTable tbody')).toContainText('Not specified');
    expect(requests.filter(r => r.method === 'PUT').at(-1).body.nationality).toBeNull();
});

test('reopening create resets abandoned nationality and label supports keyboard (AC-005/006)', async ({ page }) => {
    await setup(page);
    await page.getByRole('button', { name: 'Add New Person' }).click();
    const nationality = page.getByLabel('Nationality', { exact: true });
    // Native date inputs have browser-dependent internal tab stops. Check
    // keyboard reachability from the adjacent submit button instead.
    await page.getByRole('button', { name: 'Save Person' }).focus();
    await page.keyboard.press('Shift+Tab');
    await expect(nationality).toBeFocused();
    await page.keyboard.type('Abandoned');
    await expect(nationality).toHaveValue('Abandoned');
    await page.locator('#personModal .close').click();
    await page.getByRole('button', { name: 'Add New Person' }).click();
    await expect(nationality).toHaveValue('');
});

test('nationality containing markup is literal text (AC-006)', async ({ page }) => {
    const markup = '<img src=x onerror="window.nationalityExecuted=true">';
    await setup(page, [person(1, markup)]);
    await expect(page.locator('#personTable tbody')).toContainText(markup);
    await expect(page.locator('#personTable tbody img')).toHaveCount(0);
    expect(await page.evaluate(() => window.nationalityExecuted)).toBeUndefined();
});

test('combined matches are merged and deduplicated by ID (AC-007/008)', async ({ page }) => {
    await setup(page, [person(1, 'German', 'FrenchName'), person(2, 'French'), person(3, 'French', 'FrenchBoth'), person(4, 'Spanish')]);
    await search(page, 'French');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(3);
    await expect(page.locator('#personTable tbody')).not.toContainText('user4');
});

test('exact nationality case/whitespace and text partial matching coexist (AC-009/010)', async ({ page }) => {
    await setup(page, [person(1, 'French'), person(2, 'German', 'FrenchName')]);
    await search(page, ' french ');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
    await expect(page.locator('#personTable tbody')).toContainText('user1');
    await search(page, 'Fren');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
    await expect(page.locator('#personTable tbody')).toContainText('FrenchName');
});

test('neither search matches clears old rows and shows empty state (AC-011)', async ({ page }) => {
    await setup(page);
    await search(page, 'Missing');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(0);
    await expect(page.getByRole('status')).toHaveText('No persons found');
});

test('empty and whitespace search show all people (AC-012)', async ({ page }) => {
    await setup(page, [person(1, 'French'), person(2, 'German')]);
    await search(page, 'French');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
    for (const query of ['', '   ']) {
        await search(page, query);
        await expect(page.locator('#personTable tbody tr')).toHaveCount(2);
    }
});

for (const endpoint of ['text', 'nationality']) {
    for (const failure of ['http', 'network', 'malformed']) {
        test(`${endpoint} ${failure} failure clears results and reports error (AC-013)`, async ({ page }) => {
            await setup(page, [person(1, 'French')], {
                [endpoint]: route => failure === 'network' ? route.abort() :
                    route.fulfill({ status: failure === 'http' ? 500 : 200, json: { message: 'failure' } })
            });
            await search(page, 'French');
            await expect(page.locator('#personTable tbody tr')).toHaveCount(0);
            await expect(page.getByRole('status')).toContainText('Search failed');
        });
    }
}

test('latest search wins over delayed older responses', async ({ page }) => {
    let release;
    const pending = new Promise(resolve => { release = resolve; });
    let started;
    const startedPromise = new Promise(resolve => { started = resolve; });
    await setup(page, [person(1, 'French'), person(2, 'German')], {
        text: async (route, pathname) => {
            if (pathname.endsWith('/French')) { started(); await pending; }
            await route.fulfill({ status: 404, json: { message: 'No matches' } });
        }
    });
    await page.getByRole('textbox', { name: 'Search persons', exact: true }).fill('French');
    const oldSearch = page.evaluate(async () => (await import('/src/app.js')).searchPersons());
    await startedPromise;
    await search(page, 'German');
    await expect(page.locator('#personTable tbody')).toContainText('German');
    release();
    await oldSearch;
    await expect(page.locator('#personTable tbody')).toContainText('German');
    await expect(page.locator('#personTable tbody')).not.toContainText('French');
});

test('form validation, text search and delete operations remain functional (AC-014)', async ({ page }) => {
    const requests = await setup(page, [person(1, null), person(2, 'German')]);
    await page.getByRole('button', { name: 'Add New Person' }).click();
    await page.getByRole('button', { name: 'Save Person' }).click();
    expect(requests.some(r => r.method === 'POST')).toBe(false);
    await page.locator('#personModal .close').click();
    await search(page, 'user1');
    await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
    page.on('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await expect(page.locator('#personTable tbody tr')).toHaveCount(1);
    await expect(page.locator('#personTable tbody')).toContainText('user2');
    await page.getByRole('button', { name: 'Delete by Email', exact: true }).click();
    await page.getByLabel('Email Address', { exact: true }).fill('user2@example.test');
    await page.getByRole('button', { name: 'Delete Person', exact: true }).click();
    await expect(page.locator('#personTable tbody tr')).toHaveCount(0);
    await page.getByRole('button', { name: 'Delete All', exact: true }).click();
    await expect(page.getByRole('status')).toHaveText('No persons found');
});
