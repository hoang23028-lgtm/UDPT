import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'udpt-favicon-redirect',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url && req.url.startsWith('/favicon.ico')) {
            res.statusCode = 302;
            res.setHeader('Location', '/favicon.svg');
            res.end();
            return;
          }
          next();
        });
      },
    },
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost', changeOrigin: true },
    },
  },
});
