import { defineConfig } from 'orval';

// Génère le client HTTP typé à partir du spec OpenAPI produit par Quarkus.
// Lancer : npm run api:gen  (après ./mvnw package côté backend).
export default defineConfig({
  releveDlc: {
    input: {
      target: '../src/main/openapi/openapi.json',
    },
    output: {
      mode: 'single',
      target: 'src/api/generated.ts',
      client: 'axios',
      httpClient: 'axios',
      override: {
        mutator: {
          path: 'src/api/http.ts',
          name: 'http',
        },
      },
    },
  },
});
