# Analyse du projet — Ticket Booking System

## Vue d'ensemble

Ce dépôt contient le backend d'une plateforme de billetterie à forte concurrence, développée en Java 21 avec Spring Boot. L'application adopte une architecture de monolithe modulaire, séparée en domaines : catalogue, réservation, paiement, utilisateurs et notifications.

Il n'y a pas de frontend implémenté dans ce dépôt. Le fichier `FRONTEND_AI_PROMPT.md` contient le cahier des charges d'un futur frontend.

## Technologies et infrastructure

- Spring Boot 3.3.4 et Spring Web
- Spring Data JPA + PostgreSQL
- Flyway pour les migrations de schéma
- Redis pour les réservations temporaires avec expiration
- Kafka pour les événements asynchrones inter-domaines
- Spring Security + JWT pour l'authentification
- Spring Actuator + Prometheus pour l'observabilité
- Docker Compose pour PostgreSQL, Redis et Kafka en local

L'application écoute par défaut sur le port `8080`.

## Fonctionnalités métier

- Inscription, connexion et consultation du profil utilisateur via JWT.
- Catalogue public d'événements.
- Création de lieux et d'événements.
- Deux catégories d'inventaire :
  - `SEATED` : réservation d'un siège identifié.
  - `GENERAL_ADMISSION` : réservation d'une quantité de billets par type.
- Génération de sièges par lots : section, rangée, numéro initial, quantité, prix et devise.
- Création de types de billets, par exemple Standard ou VIP.
- Création, affichage et suppression de sections de salle ; les sections définissent une géométrie et peuvent générer les sièges associés.
- Hold temporaire d'un siège ou d'un quota de billets, avec durée de vie configurée à cinq minutes.
- Prévention des doubles réservations et du dépassement de stock.
- Libération d'un hold de siège.
- Création d'une réservation après l'acquisition d'un hold.
- Confirmation de réservation et lancement d'un paiement asynchrone simulé.
- Consultation de l'état de réservation et de paiement.
- Projection de la disponibilité des sièges dans Redis à partir des événements Kafka.
- Notification interne après confirmation d'une réservation.
- Idempotence du traitement de paiement.

## API REST

Base URL locale : `http://localhost:8080`

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Inscrit un utilisateur et renvoie un JWT. |
| POST | `/api/auth/login` | Public | Authentifie un utilisateur et renvoie un JWT. |
| GET | `/api/auth/me` | JWT | Renvoie le profil de l'utilisateur connecté. |
| GET | `/api/catalog/events` | Public | Liste les événements. |
| GET | `/api/catalog/events/{eventId}/seat-map` | Public | Renvoie le plan de salle et l'état des sièges. |
| GET | `/api/catalog/events/{eventId}/ticket-types` | Public | Renvoie les types de billets et leur disponibilité. |
| POST | `/api/catalog/venues` | Public actuellement | Crée un lieu. |
| POST | `/api/catalog/events` | Public actuellement | Crée un événement. |
| POST | `/api/catalog/events/{eventId}/seats` | Public actuellement | Génère des sièges pour un événement assis. |
| POST | `/api/catalog/events/{eventId}/ticket-types` | Public actuellement | Crée un type de billet pour un événement général. |
| POST | `/api/catalog/events/{eventId}/sections` | Public actuellement | Crée une section de salle et ses sièges. |
| GET | `/api/catalog/events/{eventId}/sections` | Public | Liste les sections d'un événement. |
| DELETE | `/api/catalog/events/{eventId}/sections/{sectionId}` | JWT | Supprime une section et ses sièges. |
| POST | `/api/booking/holds` | JWT, USER ou ADMIN | Place un hold sur un siège ou une quantité de billets. |
| POST | `/api/booking/holds/seat` | JWT, USER ou ADMIN | API historique de hold d'un siège. |
| DELETE | `/api/booking/holds/{holdId}?seatId={seatId}` | JWT, USER ou ADMIN | Libère un hold de siège. |
| GET | `/bookings/{holdId}` | Public | Renvoie l'état d'une réservation. |
| POST | `/bookings/{holdId}/confirm` | JWT, USER ou ADMIN | Déclenche le paiement asynchrone. |
| GET | `/api/payment/{holdId}` | Public | Renvoie l'état du paiement simulé. |

## Formats de requêtes importants

### Inscription

```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "Alice"
}
```

### Hold pour un siège numéroté

```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "seatId": "uuid"
}
```

### Hold pour un événement général

```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2
}
```

## Événements Kafka

- `booking.seat-held` : un siège est temporairement bloqué.
- `booking.seat-released` : un siège est libéré.
- `booking.confirmed` : une réservation est confirmée.
- `catalog.ticket-type-created` : un type de billet est créé.
- `booking.ticket-availability-changed` : le stock de billets est modifié.
- `payment.requests` : demande de paiement émise par Booking.
- `payment.results` : résultat du paiement émis par Payment.
- `payment.anomalies` : paiement réussi alors que le hold avait expiré ou été libéré.

Flux principal : `hold → réservation → confirmation → payment.requests → payment.results → confirmé ou libéré`.

## Observabilité

Les endpoints Actuator exposés sont :

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

## Points d'attention

- Les écritures de catalogue (`POST /api/catalog/**`) sont ouvertes sans JWT dans la configuration actuelle. Elles devraient être limitées aux administrateurs en production.
- Le `userId` est fourni dans le corps des requêtes de réservation et n'est pas comparé explicitement à l'utilisateur porté par le JWT. Cela doit être corrigé pour éviter les réservations au nom d'un autre utilisateur.
- Le secret JWT est stocké en clair dans `application.yaml`. Une variable d'environnement ou un gestionnaire de secrets est requis en production.
- Le paiement est un stub asynchrone, avec un taux d'échec configuré à 10 %. Il n'y a pas de PSP réel.
- Le `pom.xml` contient des références historiques à `com.ticketbooking...` alors que le code source utilise `com.ticketapp.demo...`. Cette incohérence doit être vérifiée avant déploiement.
- Les tests Maven n'ont pas pu être exécutés dans l'environnement d'analyse, car le wrapper Maven ne trouvait pas la commande `powershell`.
