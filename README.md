# Rebut — SuperQuinquin

Application de relevé des DLC (dates limites de consommation) et des pertes, puis
d'envoi automatique au rebut dans Odoo.

Chaque jour, les coopérateur-ices scannent en rayon les produits proches de leur date limite ou
abîmés ; le ou la salarié-e consulte le relevé du jour depuis son poste, puis envoie les
produits concernés au rebut dans Odoo.

---

## 1. Pas à pas

### Côté rayon — la scannette

1. **Choisir le type de relevé.** À l'ouverture, la scannette demande quoi relever :
   `DLC` ou `Pertes`.

   <img src="docs/img/scannette-menu.png" alt="Menu de la scannette" width="250">

2. **Scanner le produit.** L'écran « prêt à scanner » attend une lecture. Visez le
   code-barres : un bip confirme la reconnaissance du produit.

   <img src="docs/img/scannette-scan.png" alt="Écran de scan" width="250">

3a. **Si c'est une DLC** : choisissez l'urgence (J-0 / J-1 / J-2) ou une date exacte,
   ajustez la quantité (ou le poids), puis **Valider la ligne**.

   <img src="docs/img/scannette-dlc.png" alt="Saisie d'une DLC" width="250">

3b. **Si c'est une perte** : choisissez le **motif**, ajustez la quantité, puis
   **Valider la ligne**.

   <img src="docs/img/scannette-perte.png" alt="Saisie d'une perte" width="250">

4. **Vérifier le relevé.** L'onglet *Le relevé* liste tout ce qui a été saisi.
   Touchez une ligne pour modifier sa quantité/son motif ou la supprimer.

   <img src="docs/img/scannette-liste.png" alt="Onglet Le relevé" width="250">

### Côté poste — le responsable

5. **Consulter le relevé du jour.** Vue *Par urgence* (groupes J-0 / J-1 / J-2 puis
   Pertes). Recherchez un produit, filtrez par rayon, ajustez les quantités.

   ![Poste — par urgence](docs/img/poste-urgence.png)

6**Envoyer au rebut.** Cliquez sur *Envoyer les pertes au rebut* ou *Envoyer les J-0
   au rebut*. Un récapitulatif s'affiche ; confirmez pour créer les ordres de rebut dans
   Odoo.

7**Retrouver les relevés passés** depuis l'*Historique*.

### Le rapport quotidien par e-mail

8. **Recevoir la liste des grosses quantités.** Chaque jour à l'heure choisie, un e-mail
   liste les produits **DLC** du relevé du jour dont la quantité dépasse les seuils
   (par défaut plus de 5 pièces, ou plus d'1 kg pour les produits au poids), groupés par
   J-0 / J-1 / J-2. Le détail complet est joint au format Excel (`.xlsx`). S'il n'y a rien
   à signaler, l'e-mail part quand même — c'est le signe que l'envoi fonctionne toujours.

9. **Régler l'envoi** depuis *Réglages* (lien dans l'en-tête du poste) : activation,
   heure d'envoi, destinataires et seuils. Le bouton **Envoyer maintenant** déclenche un
   envoi immédiat avec les réglages **enregistrés** — utile pour vérifier la configuration
   sans attendre l'heure planifiée ; il ne remplace pas l'envoi automatique du jour.

   > Les seuils sont **stricts** : un produit à exactement 5 pièces n'est pas listé.

---

## 2. Développement local

### Pré-requis

- **JDK 21**
- **Node.js 22** (le front est buildé par le Maven via Quinoa, et tourne en dev sur Vite)
- **Docker** — Quarkus Dev Services démarre automatiquement une base PostgreSQL le temps
  du `dev` ; aucune base à installer à la main.

### Configuration Odoo et Brevo (`.env` à la racine)

```
ODOO_URL=...            ODOO_DATABASE=...    ODOO_LOGIN=...    ODOO_PASSWORD=...
ODOO_BASIC_AUTH_USERNAME=...   ODOO_BASIC_AUTH_PASSWORD=...   # staging uniquement (couche HTTP Basic Auth en plus du login)
BREVO_API_KEY=...              BREVO_SENDER_EMAIL=...         # rapport quotidien par e-mail
```

> Sans `BREVO_API_KEY`, l'application démarre normalement : seul l'envoi du rapport
> échoue (502 sur *Envoyer maintenant*, erreur tracée dans les réglages).

> ⚠️ `odoo.rebut.dry-run` vaut **`true`** par défaut partout (aucune écriture Odoo, le
> payload est seulement loggé) — garde-fou anti-écriture accidentelle. Pour tester le
> **vrai** rebut **sur staging** : `./mvnw quarkus:dev -Dodoo.rebut.dry-run=false`.

### Lancer en local

```shell
./mvnw quarkus:dev
```

L'app est alors accessible sur http://localhost:8080 (le serveur Vite du front tourne en
arrière-plan sur le port 5173 et est servi via Quarkus).

---

## 3. Déploiement

### Lancer le conteneur

L'application a besoin d'une **base PostgreSQL** (les migrations Flyway sont jouées au
démarrage) et de l'accès à **Odoo**. À configurer via variables d'environnement :

```shell
docker run --rm -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://<host>:5432/<db>" \
  -e QUARKUS_DATASOURCE_USERNAME="<user>" \
  -e QUARKUS_DATASOURCE_PASSWORD="<password>" \
  -e ODOO_URL="https://odoo.example.com" \
  -e ODOO_DATABASE="<db_odoo>" \
  -e ODOO_LOGIN="<login>" \
  -e ODOO_PASSWORD="<password>" \
  -e ODOO_REBUT_DRY_RUN="false" \
  -e TZ="Europe/Paris" \
  -e BREVO_API_KEY="<clé Brevo>" \
  -e BREVO_SENDER_EMAIL="rebut@superquinquin.fr" \
  ghcr.io/<org>/<repo>:latest
```

> En production, pensez à passer `ODOO_REBUT_DRY_RUN=false` (sinon les rebuts ne sont que
> simulés) et `APP_STAGING=false` (pour masquer le bandeau « Staging »).

> ⚠️ `TZ=Europe/Paris` est nécessaire : l'image tourne en UTC par défaut, et le relevé
> « du jour » basculerait alors à 2 h du matin heure française. Le rapport quotidien, lui,
> raisonne toujours en heure de Paris (`REPORT_TIMEZONE`, `Europe/Paris` par défaut).

> ⚠️ L'adresse `BREVO_SENDER_EMAIL` doit appartenir à un domaine **vérifié dans Brevo**
> (SPF/DKIM), sans quoi les rapports partiront en indésirables.

Healthcheck disponible sur `GET /q/health`

---

## 5. Installer sur le terminal Zebra (PWA)

L'application est une **PWA** : elle s'installe sur l'écran d'accueil du terminal

### Pré-requis

- Le terminal Zebra doit accéder à l'URL de l'application **en HTTPS** (les PWA ne
  s'installent qu'en contexte sécurisé) et disposer d'un **navigateur Chrome** récent.
- Le scanner intégré doit envoyer les codes en **émulation clavier (keystroke)** : via
  **DataWedge**, créez/activez un profil pour Chrome avec la sortie *Keystroke* activée.
  L'app gère le scan comme une saisie clavier et masque le clavier virtuel pour ne pas
  gêner.

### Installation

1. Ouvrez **Chrome** sur le terminal et rendez-vous sur l'URL de l'application.
2. Ouvrez le menu **⋮** de Chrome → **Installer l'application** (ou *Ajouter à l'écran
   d'accueil*).
3. Confirmez : l'icône **Rebut** apparaît sur l'écran d'accueil.
4. Lancez l'app depuis cette icône : elle s'ouvre en plein écran, en portrait.
