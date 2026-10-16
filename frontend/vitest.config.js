import { defineConfig } from 'vitest/config';
export default defineConfig({ envDir: false, define: { 'import.meta.env.VITE_API_BASE_URL': JSON.stringify('') }, test: {
    environment: 'jsdom', include: ['tests/**/*.test.js'], restoreMocks: true, unstubGlobals: true,
    reporters: ['default', 'json'], outputFile: 'target/vitest-results.json'
} });
