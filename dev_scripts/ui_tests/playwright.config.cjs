const { defineConfig } = require('playwright/test');

module.exports = defineConfig({
    testDir: './tests',
    fullyParallel: true,
    retries: 0,
    workers: process.env.CI ? 2 : undefined,
    reporter: [['list'], ['html', { open: 'never' }]],
    use: { baseURL: 'http://127.0.0.1:4173', browserName: 'chromium', trace: 'retain-on-failure' },
    webServer: {
        command: 'node server.cjs',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: false
    }
});
