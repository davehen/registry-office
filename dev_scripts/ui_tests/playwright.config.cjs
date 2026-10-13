const { defineConfig } = require('playwright/test');
const path = require('node:path');
const os = require('node:os');
const results = process.env.REGISTRY_UI_RESULTS || path.join(os.tmpdir(), 'registry-office-ui-results');

module.exports = defineConfig({
    testDir: './tests',
    fullyParallel: true,
    retries: 0,
    workers: process.env.CI ? 2 : undefined,
    outputDir: path.join(results, 'test-results'),
    reporter: [['list'], ['html', { open: 'never', outputFolder: path.join(results, 'html') }]],
    use: { baseURL: 'http://127.0.0.1:4173', browserName: 'chromium', trace: 'retain-on-failure' },
    webServer: {
        command: 'node server.cjs',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: false
    }
});
