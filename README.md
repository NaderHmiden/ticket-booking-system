# 🎫 Ticket Platform (Smart Transport & Event Hub)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-Event_Driven-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-IaC-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)

Une plateforme backend centralisée, résiliente et à haute concurrence pour la vente de billets d'événements (matchs de football, théâtres, festivals) et la **réservation de tickets de bus interurbains**. 

Conçu spécifiquement pour absorber des pics massifs de trafic (ex: mise en vente de tickets pour un derby ou réservation de bus la veille de l'Aïd) **sans jamais faire de survente**, tout en luttant contre le marché noir.

---

## ✨ Fonctionnalités Principales

*   **🔒 Moteur de Réservation Haute Concurrence :** Verrouillage temporaire des billets (Hold de 5 minutes) géré en mémoire RAM via **Redis** (Distributed Locks) garantissant le "Zéro Survente".
*   **🚌 Hub de Transport Public :** Support natif pour les trajets de bus interurbains avec gares de départ/arrivée.
*   **🛡️ Lutte Anti-Marché Noir :** Obligation de fournir les Numéros de Cartes d'Identité (CIN) lors du checkout pour les billets de transport.
*   **💳 Paiements Asynchrones :** Traitement des flux de paiement via **Apache Kafka** pour ne pas bloquer les requêtes lors d'une forte affluence. Libération (Release) automatique des places si le paiement expire.
*   **🔐 Sécurité Avancée :** Authentification via tokens JWT, séparation stricte des rôles (`USER`, `ORGANIZER`, `ADMIN`).

---

## 🏗️ Architecture et Technologies

L'application repose sur une architecture de Monolithe Modulaire événementielle (Event-Driven) :
*   **Backend:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA.
*   **Bases de données:** PostgreSQL (Persistance robuste), Redis (Mise en cache & Verrous).
*   **Message Broker:** Apache Kafka (Communication asynchrone des confirmations de paiements).
*   **Observabilité:** Spring Actuator & Prometheus.

### ♾️ Pratiques DevOps (GitOps)
Ce projet intègre les meilleurs standards de l'industrie pour un cycle de vie 100% automatisé :
*   **CI/CD:** Pipeline GitHub Actions automatisant les tests, l'analyse SonarQube, et la création de l'image Docker.
*   **Infrastructure as Code (IaC):** Provisionnement des serveurs AWS via **Terraform** (avec Remote State sur Amazon S3).
*   **Gestion de configuration:** Installation automatisée des dépendances de production via **Ansible**.

---

## 🚀 Démarrage Rapide (En local)

### Prérequis
*   [Docker Desktop](https://www.docker.com/products/docker-desktop/) ou Docker Compose.
*   JDK 21 (Si vous souhaitez compiler manuellement sans Docker).

### Lancer l'environnement de développement
L'application dépend de PostgreSQL, Redis et Kafka. Vous pouvez démarrer ces services instantanément grâce au fichier `docker-compose.yml` :

```bash
# 1. Démarrer les bases de données et Kafka en arrière-plan
docker compose up -d

# 2. Lancer l'application Spring Boot
./mvnw spring-boot:run
```
L'API sera accessible sur `http://localhost:8080`.

---

## 📖 API Reference (Endpoints clés)

### 👤 Authentification
*   `POST /api/auth/register` : Créer un compte.
*   `POST /api/auth/login` : Obtenir le token JWT.

### 🚌 Recherche & Catalogue
*   `GET /api/catalog/events` : Lister tous les événements.
*   `GET /api/catalog/trips/search?departureVenueId={id}&arrivalVenueId={id}` : Rechercher un trajet de bus.

### 🎟️ Réservation (Protégé par JWT)
*   `POST /api/booking/holds` : Bloquer temporairement une place ou une quantité de tickets. Requiert les numéros de CIN pour les bus.
*   `POST /bookings/{holdId}/confirm` : Simuler le paiement (Kafka).
*   `GET /bookings/{holdId}` : Vérifier le statut de la réservation (`HELD`, `PAYMENT_PENDING`, `CONFIRMED`, `RELEASED`).

---

## 🧪 Qualité et Tests
Le projet impose une couverture de test stricte via **JaCoCo** et une analyse **SonarCloud** continue. 
Pour lancer les tests unitaires et d'intégration :
```bash
./mvnw clean verify
```

---
*Ce projet est une solution technique pour les organisateurs d'événements et le Ministère du Transport visant à fluidifier et sécuriser la vente de titres d'accès.*
