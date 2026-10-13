const { defineConfig } = require('playwright/test');
const path = require('node:path');
const os = require('node:os');
if (!process.env.REGISTRY_BASE_URL) throw new Error('REGISTRY_BASE_URL must identify the orchestrator-started Spring Boot app');
const results = process.env.REGISTRY_REAL_UI_RESULTS || path.join(os.tmpdir(), 'registry-office-real-ui-results');
module.exports = defineConfig({
    testDir: './real-tests',
    workers: 1,
    retries: 0,
    outputDir: path.join(results, 'test-results'),
    reporter: [['list'], ['html', { open: 'never', outputFolder: path.join(results, 'html') }]],
    use: { baseURL: process.env.REGISTRY_BASE_URL, browserName: 'chromium', trace: 'retain-on-failure' }
});
