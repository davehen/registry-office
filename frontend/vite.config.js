import { defineConfig } from 'vite';
export default defineConfig({
  envDir: false, // Public build configuration comes from the process environment.
  server: { host: '127.0.0.1', strictPort: true },
  preview: { host: '127.0.0.1', strictPort: true },
  build: { outDir: 'dist', emptyOutDir: true }
});
