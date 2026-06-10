# SQQ Rebut

## Comment ça s'utilise ?

# 1. 

##

## Configuration Odoo (`.env` à la racine)

```
ODOO_URL=...            ODOO_DATABASE=...    ODOO_LOGIN=...    ODOO_PASSWORD=...
ODOO_BASIC_AUTH_USERNAME=...   ODOO_BASIC_AUTH_PASSWORD=...   # staging uniquement (couche HTTP Basic Auth en plus du login)
```

⚠️ `odoo.rebut.dry-run` vaut **`true`** par défaut partout (aucune écriture Odoo, payload loggé) — garde-fou anti-écriture accidentelle. Pour tester le **vrai** rebut **sur staging** : `./mvnw quarkus:dev -Dodoo.rebut.dry-run=false`.

## Lancer en local

```shell
./mvnw quarkus:dev
```

Ensuite l'app est accessible sur http://localhost:8080