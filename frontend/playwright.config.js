import { defineConfig } from '@playwright/test';

const backend = 'http://127.0.0.1:18081';
const mockFrontend = 'http://127.0.0.1:4173';
const realFrontend = 'http://127.0.0.1:4174';

export default defineConfig({
    testDir: './e2e', retries: 0, workers: 1,
    outputDir: 'target/playwright-results',
    reporter: [['list'], ['html', { open: 'never', outputFolder: 'target/playwright-report' }], ['json', { outputFile: 'target/playwright-results.json' }]],
    use: { browserName: 'chromium', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
    projects: [
        { name: 'mock', testDir: './e2e/mock', use: { baseURL: mockFrontend } },
        { name: 'real', testDir: './e2e/real', fullyParallel: false, use: { baseURL: realFrontend } }
    ],
    webServer: [
        { command: `java -jar ../backend/target/registry-office-backend-1.0.0.jar --server.port=18081 --server.address=127.0.0.1 --spring.datasource.url=jdbc:h2:mem:aidm185-browser --app.cors.allowed-origins=${realFrontend}`,
            url: `${backend}/person`, timeout: 90000, reuseExistingServer: false },
        { command: 'npm run dev -- --port 4173', url: mockFrontend, reuseExistingServer: false, env: { VITE_API_BASE_URL: '' } },
        { command: 'npm run dev -- --port 4174', url: realFrontend, reuseExistingServer: false, env: { VITE_API_BASE_URL: backend } }
    ]
});
