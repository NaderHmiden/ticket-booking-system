# Prompt Frontend — Ticket Booking Platform

Copie ce fichier entier et donne-le à une IA pour générer le frontend complet.

---

Tu es un senior frontend engineer. Construis une application web COMPLÈTE (pas de mock backend) pour une plateforme de billetterie haute concurrence.

# Backend (source de vérité — NE PAS inventer d’autres endpoints)

- Base URL: `http://localhost:8080` (configurable via `VITE_API_BASE_URL`)
- JSON, JWT Bearer: `Authorization: Bearer <token>`
- CORS déjà autorisé pour `http://localhost:5173` et `http://localhost:3000`
- Stack backend: Spring Boot, Postgres, Redis, Kafka (paiement async stub)

## Auth

- `POST /api/auth/register`  
  body: `{ email, password (>=8), displayName }`  
  → `201 { token, userId, email, displayName, role }`

- `POST /api/auth/login`  
  body: `{ email, password }`  
  → `200 { token, userId, email, displayName, role }`

- `GET /api/auth/me`  
  header Authorization  
  → `{ userId, email, displayName, role }`

**Note rôles :** aujourd’hui `register` crée toujours `role=USER`. Pour l’UI « Organisateur » vs « Acheteur », implémente un mode d’interface choisi par l’utilisateur après login (toggle Organizer / Buyer stocké en `localStorage`), OU traite `role=ADMIN` comme organisateur si présent. Ne bloque pas l’accès admin API si le backend accepte `USER` sur `POST /api/catalog/**` (c’est le cas actuel : `USER` ou `ADMIN`).

## Catalogue (lecture — public, sans token)

- `GET /api/catalog/events`  
  → `[{ id, name, category, venueName, city, startsAt, status, eventType }]`  
  `eventType`: `"SEATED"` | `"GENERAL_ADMISSION"`

- `GET /api/catalog/events/{eventId}/seat-map`  
  → `{ eventId, eventName, seats: [{ id, section, rowLabel, seatNumber, priceCents, currency, status }] }`  
  status siège: `AVAILABLE` | `HELD` | `BOOKED`

- `GET /api/catalog/events/{eventId}/ticket-types`  
  → `[{ id, name, price, totalQuantity, availableQuantity }]`  
  `availableQuantity` peut être `null` si Redis pas initialisé

## Catalogue admin / organisateur (token requis)

- `POST /api/catalog/venues`  
  body: `{ name, city, timezone }` → `201 { venueId }`

- `POST /api/catalog/events`  
  body: `{ venueId, name, category, startsAt, eventType? }`  
  `eventType`: `"SEATED"` (défaut) | `"GENERAL_ADMISSION"`  
  → `201 { eventId }`

- `POST /api/catalog/events/{eventId}/seats` (SEATED only)  
  body: `{ section, rowLabel, startingSeatNumber, count, priceCents, currency? }`  
  → `201 { seatIds, count }`

- `POST /api/catalog/events/{eventId}/ticket-types` (GENERAL_ADMISSION only)  
  body: `{ name, price, totalQuantity }`  
  → `201 { ticketTypeId }`

## Booking acheteur (token requis pour writes)

- `POST /api/booking/holds`  
  SEATED body: `{ eventId, userId, seatId }`  
  GA body: `{ eventId, userId, ticketTypeId, quantity }`  
  → `201 { holdId, seatId, eventId, expiresAt }`  
  (pour GA, `seatId` dans HoldResult = `ticketTypeId`)  
  Erreurs: `400`, `409 { error, message }` (`SEAT_ALREADY_HELD` / `INSUFFICIENT_INVENTORY`)

- `DELETE /api/booking/holds/{holdId}?seatId={seatId}` (libération siège SEATED)  
  → `204` / `404`

- `POST /bookings/{holdId}/confirm` (token requis)  
  → `202 { id, holdId, eventId, userId, status, amount, paymentRequested, createdAt, updatedAt }`  
  status: `HELD` | `PAYMENT_PENDING` | `CONFIRMED` | `RELEASED`

- `GET /bookings/{holdId}` (public lecture)  
  → même shape — utiliser pour polling après confirm

- `GET /api/payment/{holdId}` (public lecture)  
  → `{ holdId, status, createdAt }` status: `PROCESSING` | `SUCCEEDED` | `FAILED`  
  → `404` si pas encore de paiement

## Comportement paiement (IMPORTANT UX)

Le paiement est ASYNCHRONE (stub Kafka). Après `POST confirm` :

1. status passe souvent à `PAYMENT_PENDING`
2. Poll `GET /bookings/{holdId}` toutes les 1–2s (max ~30–60s)
3. Optionnel: poll aussi `GET /api/payment/{holdId}`
4. Succès UI = booking status `CONFIRMED`
5. Échec UI = booking status `RELEASED` (ou payment `FAILED`)
6. Hold TTL ~5 minutes (`expiresAt`) — countdown obligatoire avant confirm
7. `userId` dans les holds: utiliser le `userId` du JWT (pas un UUID inventé)

---

# Produit à construire — DEUX EXPERIENCES

## A) Espace ORGANISATEUR (créer / configurer des événements)

Objectif: un organisateur crée un lieu, un événement, puis configure l’inventaire.

Parcours wizard:

1. Créer Venue (`name`, `city`, `timezone` — ex `Europe/Paris`)
2. Créer Event (lier venue, `name`, `category`, `startsAt`, choisir `SEATED` ou `GENERAL_ADMISSION`)
3a. Si **SEATED**: générer des sièges par lots (`section`, `row`, starting number, `count`, `priceCents`). Permettre plusieurs lots (Orchestre rangée A, Balcon, etc.). Prévisualiser un résumé du plan.
3b. Si **GA**: créer un ou plusieurs ticket types (Standard, VIP…) avec `price` + `totalQuantity`.
4. Dashboard organisateur: liste des events créés (filtrer ceux qu’il a créés côté client via `localStorage` des ids créés, car le backend n’a pas encore « my events ») + lien vers page publique de l’event.

UI organisateur: claire, formulaires validés, feedback succès/erreur, pas de faux dashboards stats inventés.

## B) Espace ACHETEUR (acheter siège ou ticket)

Parcours:

1. Home: grille d’événements (nom, lieu, ville, date, badge SEATED/GA, statut)
2. Détail event:
   - **SEATED**: plan de salle interactif groupé section/row ; couleurs `AVAILABLE` / `HELD` / `BOOKED` ; clic siège `AVAILABLE` → résumé prix → « Réserver »
   - **GA**: cartes ticket types avec `price`, `availableQuantity` (si `null`, afficher `totalQuantity` + « stock live indisponible »), quantité, « Réserver »
3. Après hold: page « Réservation temporaire »
   - countdown jusqu’à `expiresAt`
   - montant
   - `holdId`
   - CTA « Payer / Confirmer »
   - CTA « Annuler » (SEATED: `DELETE` hold ; GA: message « expire automatiquement » si pas d’API release claire)
4. Après confirm: écran statut avec polling jusqu’à `CONFIRMED` ou `RELEASED` + message compréhensible
5. Page « Confirmation » succès (récap event, `holdId`, `amount`) — pas de PDF requis

Auth gated: register/login avant hold/confirm. Pages catalogue lisibles sans compte.

---

# Stack & qualité front

- React + TypeScript + Vite (recommandé)
- React Router
- TanStack Query pour fetch/polling
- État auth: token + user en mémoire + `localStorage`
- Design soigné, mobile-first, une direction visuelle claire (éviter purple-gradient générique / dashboard SaaS générique)
- Une composition par viewport (pas 12 widgets)
- Accessibilité plan de salle (clavier, `aria-pressed`, contrastes)
- Gestion d’erreurs `401` / `403` / `404` / `409` avec toasts/bannières
- Aucun endpoint inventé ; si une feature manque côté backend, afficher un état « non supporté » honnête

# Pages minimales à livrer

**Public**

- `/login`, `/register`
- `/` events list
- `/events/:eventId`

**Acheteur**

- `/checkout/:holdId` (hold + countdown + pay)
- `/orders/:holdId` (statut / confirmation)

**Organisateur**

- `/organizer` (hub)
- `/organizer/venues/new`
- `/organizer/events/new`
- `/organizer/events/:eventId/inventory` (seats ou ticket-types)

# Non-objectifs

- Pas de vrai PSP (Stripe)
- Pas de carte bancaire réelle
- Pas de notifications email UI
- Pas de modification backend Java sauf si bloquant absolu (préférer documenter)

# Livrables

1. App runnable (`npm install && npm run dev`)
2. README: setup, env, mapping écrans↔API, parcours Organizer et Buyer, limitations backend
3. `.env.example` avec `VITE_API_BASE_URL=http://localhost:8080`
4. Gestion token expirée → redirect login

Commence par le shell app + auth + liste events, puis détail SEATED, puis GA, puis checkout/polling, puis wizard organisateur.
