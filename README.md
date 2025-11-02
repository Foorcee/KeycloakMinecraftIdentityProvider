# Keycloak Minecraft Identity Provider

An Identity Provider (IdP) plugin for Keycloak that lets users authenticate with their Minecraft/Microsoft account. It performs the Microsoft → Xbox Live (XBL) → XSTS → Minecraft token exchange and maps the resulting profile into a Keycloak user via the Identity Brokering SPI.

Status: Experimental/Work‑in‑progress.

## Overview
- Stack: Java 21, Maven, Keycloak SPI (core/server-spi/services), Docker (optional)
- Provider ID: `minecraft`
- Default Microsoft OAuth scopes: `XboxLive.signin offline_access`
- Entry points:
  - Factory: `de.foorcee.keycloak.minecraft.MinecraftIdentityProviderFactory` (registered via `META-INF/services/org.keycloak.broker.social.SocialIdentityProviderFactory`)
  - Provider: `de.foorcee.keycloak.minecraft.MinecraftIdentityProvider`
  - Themes/templates for UI: `src/main/resources/theme/...`

High‑level flow:
1. Redirect user to Microsoft OAuth 2.0 authorize endpoint (consumer tenant).
2. Exchange code for a Microsoft access token.
3. Use the token to obtain:
   - Xbox Live token (XBL)
   - XSTS token
   - Minecraft token
4. Fetch the Minecraft profile and create/update a brokered identity in Keycloak.

## Requirements
- Java 21 (JDK)
- Maven 3.9+
- Keycloak 26.4.x (built against 26.4.2; Docker base image 26.4)
- Docker and Docker Compose (optional for local dev)
- A Microsoft Entra ID (consumer) application registration for OAuth 2.0
  - You will need the Client ID and Client Secret (configured inside Keycloak Admin Console for this IdP)

## Setup and run
You can either run with Docker Compose (recommended for local testing) or build and install the provider into an existing Keycloak.

### Option A: Quick start with Docker Compose
This repository includes a Compose setup that builds Keycloak with the provider pre-installed and starts a Postgres database.

1) Build and start
- docker compose up --build

2) Access Keycloak
- URL: http://localhost:8999/
- Admin user/password: admin / admin

3) Configure the Minecraft Identity Provider in the Admin Console
- Go to: your realm → Identity Providers → Select "Minecraft"
- Set Client ID and Client Secret from your Microsoft app registration
- Scopes: keep default `XboxLive.signin offline_access` unless you have specific needs
- Ensure Redirect URI is registered in your Microsoft app:
  - http://localhost:8999/realms/<your-realm>/broker/minecraft/endpoint

4) Test the login
- On your realm’s login page, click the “Minecraft” identity provider button.

### Option B: Manual install into an existing Keycloak
1) Build the JAR
- mvn clean package -DskipTests
- Output: target/minecraft-identity-provider-1.0-SNAPSHOT.jar

2) Install the JAR
- Copy into $KEYCLOAK_HOME/providers/

3) Start Keycloak
- $KEYCLOAK_HOME/bin/kc.sh start-dev
- Or for containers: mount into /opt/keycloak/providers/ and start as usual

4) Configure the IdP as in Option A.

## Scripts and useful commands
- Build (skip tests):
  - mvn clean package -DskipTests
- Run tests:
  - mvn test
- Docker image build and run directly:
  - docker build -t kc-minecraft-provider .
  - docker run --rm -p 8999:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin kc-minecraft-provider start-dev
- Docker Compose (recommended):
  - docker compose up --build

## Environment variables
Used by docker-compose.yml for the Keycloak container:
- KEYCLOAK_ADMIN: default admin username (example: admin)
- KEYCLOAK_ADMIN_PASSWORD: default admin password (example: admin)
- DEBUG: enable remote debugging ("true" to enable)
- DEBUG_PORT: debug port (8787)
- KC_DB: database vendor (postgres)
- KC_DB_URL: JDBC URL to Postgres (jdbc:postgresql://db:5432/postgres)
- KC_DB_USERNAME: database username (postgres)
- KC_DB_PASSWORD: database password (postgres)

Note: Microsoft OAuth Client ID and Client Secret are configured inside Keycloak Admin Console for the Minecraft IdP, not via these environment variables.

## Tests
This project uses JUnit 5 and Mockito.
- Run all tests:
  - mvn test
- Example test classes:
  - src/test/java/de/foorcee/keycloak/minecraft/auth/flow/AuthenticationFlowBuilderTests.java
  - src/test/java/de/foorcee/keycloak/minecraft/auth/steps/MinecraftAuthenticationStepTests.java

## Project structure
Key paths to be aware of:
- src/main/java/de/foorcee/keycloak/minecraft/
  - MinecraftIdentityProviderFactory.java — registers provider (ID: "minecraft")
  - MinecraftIdentityProvider.java — provider implementation
  - .../auth/... — authentication flow steps (XBL/XSTS/Minecraft)
- src/main/resources/META-INF/services/
  - org.keycloak.broker.social.SocialIdentityProviderFactory — service registration pointing to the factory
- src/main/resources/theme/ and src/main/resources/theme-resources/
  - FreeMarker templates used by the provider (e.g., signup-with-minecraft.ftl)
- Dockerfile — multi-stage build; copies provider JAR to /opt/keycloak/providers/
- docker-compose.yml — local development stack (Keycloak + Postgres)
- pom.xml — Maven configuration (Keycloak 26.4.2)

## Limitations and notes
- Targets Microsoft consumer tenant endpoints (login.microsoftonline.com/consumers).
- Uses public Xbox/Minecraft endpoints as of now; these can change.
- Newly created Microsoft apps may require approval/permissions to access Minecraft APIs.
- Error handling covers common XSTS/Xbox errors; behavior may vary if upstream APIs change.

## License
- TODO: Add a LICENSE file (e.g., Apache-2.0, MIT, etc.) and update this section accordingly.
