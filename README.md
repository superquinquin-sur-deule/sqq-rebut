# sqq-dlc — Relevé DLC

Application de **relevé des DLC des produits frais** pour SuperQuinquin.

- **Scannette** (`/scannette`, mobile/Zebra) : scan d'un code-barres → fiche produit → DLC (J-0/J-1/J-2 ou date exacte) + quantité → validation (bip + vibration).
- **Poste responsable** (`/poste`, desktop) : relecture du relevé partagé, édition/suppression, **« Envoyer les J-0 au rebut »** → création + validation de lignes `stock.scrap` dans Odoo (motif « DLC Dépassée »).

## Architecture

- **Backend** : Quarkus (REST + Panache/PostgreSQL). Schéma géré par **Flyway** (`src/main/resources/db/migration`, appliqué au démarrage) ; Hibernate est en `validate` (il vérifie le schéma, ne le modifie pas). Client JSON-RPC Odoo (`org.superquinquin.odoo.OdooClient`) : lookup produit par EAN (`product.product`) + mise au rebut (`stock.scrap` → `action_validate`). Spec OpenAPI exposée sur `/q/openapi` et écrite dans `src/main/openapi/`.
- **Frontend** : Vite + Vue 3 + TypeScript (`frontend/`). Le client HTTP est **généré par orval** depuis `src/main/openapi/openapi.json` (`npm run api:gen`). Servi par Quarkus via **Quinoa** : en dev Quinoa démarre Vite et proxifie tout sur `:8080` ; au build il exécute `npm run build` et embarque `dist/` dans le jar.

## Configuration Odoo (`.env` à la racine)

```
ODOO_URL=...            ODOO_DATABASE=...    ODOO_LOGIN=...    ODOO_PASSWORD=...
ODOO_BASIC_AUTH_USERNAME=...   ODOO_BASIC_AUTH_PASSWORD=...   # staging uniquement (couche HTTP Basic Auth en plus du login)
```

⚠️ `odoo.rebut.dry-run` vaut **`true`** par défaut partout (aucune écriture Odoo, payload loggé) — garde-fou anti-écriture accidentelle. Pour tester le **vrai** rebut **sur staging** : `./mvnw quarkus:dev -Dodoo.rebut.dry-run=false`.

## Lancer en local

```shell
# Dev — Quinoa démarre Vite et sert UI + API sur :8080 (Dev Services lance PostgreSQL via Docker)
./mvnw quarkus:dev                 # http://localhost:8080  ·  Swagger UI : /q/swagger-ui

# (Ré)générer le client orval après un changement d'API
cd frontend && npm run api:gen

# Build de prod — Quinoa build le front et l'embarque dans le jar
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar    # sert UI + API sur :8080
```

> Le front peut aussi tourner seul (`cd frontend && npm run dev` sur `:5173`, proxy `/api`,`/q` → `:8080`),
> utile pour le HMR pur ; mais avec Quinoa ce n'est plus nécessaire.

Tests backend (WireMock stube Odoo ; PostgreSQL via Dev Services — Docker requis, aucun appel Odoo réel) :

```shell
./mvnw test
```