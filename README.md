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
⚠️ Dans `.env`, commentez avec `#` (pas `;` : `source` exécute les lignes `;`).

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

---

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/sqq-dlc-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
