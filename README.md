# Keycloak Minecraft Identity Provider

An Identity Provider (IdP) plugin for Keycloak that lets users authenticate with their Minecraft/Microsoft account. This provider uses the Microsoft OAuth 2.0 consumer tenant for login and exchanges the resulting access token for Xbox Live (XBL), XSTS, and Minecraft tokens to identify the user.

Status: Experimental/Work-in-progress.

## Overview
- Stack: Java 21, Maven, Keycloak SPI (server-spi/services/core), Docker (optional)
- Provider ID: `minecraft`
- Default OAuth scope used against Microsoft: `XboxLive.signin offline_access`

What it does at a high level:
1. Redirects users to Microsoft OAuth authorize endpoint (consumer tenant).
2. Exchanges code for a Microsoft access token.
3. Uses the access token to obtain:
   - Xbox Live token (XBL)
   - XSTS token
   - Minecraft token
4. Fetches the Minecraft profile and creates a Keycloak brokered identity.


## Requirements
- Java 21 (JDK)
- Maven 3.9+
- Keycloak 26.4.x (tested with 26.4.2 libraries; container base image 26.4)
- Docker and Docker Compose (optional, for the included example)
- A Microsoft Entra ID (consumer) application registration with OAuth 2.0 client credentials for Sign-in using the consumer tenant
  - You will need the Client ID and Client Secret configured in Keycloak


## Quick start (Docker Compose)
This repository includes a compose setup that builds Keycloak with the provider pre-installed and starts a Postgres database.

1. Build and start
   - docker compose up --build

2. Access Keycloak
   - URL: http://localhost:8999/
   - Admin user/password (from compose): admin / admin

3. Add the Minecraft Identity Provider in the Keycloak Admin Console
   - Go to: your realm → Identity Providers → Select provider "Minecraft"
   - Configure the following:
     - Client ID: your Microsoft app’s client ID
     - Client Secret: your Microsoft app’s secret
     - Scopes: leave default (`XboxLive.signin offline_access`) unless you know you need to change it
   - Set the Redirect URI in your Microsoft app registration to:
     - http://localhost:8999/realms/<your-realm>/broker/minecraft/endpoint

4. Test the login
   - From your realm’s login page, use the “Minecraft” identity provider button.


## Manual installation into an existing Keycloak
If you already run Keycloak, you can build the provider JAR and drop it into your Keycloak’s providers directory.

1. Build the JAR
   - mvn clean package -DskipTests
   - Output: `target/minecraft-identity-provider-1.0-SNAPSHOT.jar`

2. Copy the JAR to your Keycloak installation
   - Place the JAR into: `$KEYCLOAK_HOME/providers/`

3. Start Keycloak
   - For distribution images: `$KEYCLOAK_HOME/bin/kc.sh start-dev`
   - For containers, mount the JAR into `/opt/keycloak/providers/` and start as usual

4. Configure the IdP in the Admin Console as described in the Quick start


## Build locally (without Docker)
- mvn clean package -DskipTests

This will also compile against Keycloak SPI dependencies defined in the POM (core, server-spi, services).


## Docker
Two options:

- Build image directly
  - docker build -t kc-minecraft-provider .
  - docker run --rm -p 8999:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin kc-minecraft-provider start-dev

- Use docker compose (recommended for local dev)
  - docker compose up --build

The Dockerfile uses a multi-stage build to compile with Maven and copy the resulting provider JAR into the Keycloak image at `/opt/keycloak/providers/`.

## Limitations and notes
- This provider targets the Microsoft consumer tenant endpoints (`login.microsoftonline.com/consumers`).
- The provider uses fixed endpoints for XBL/XSTS/Minecraft services per current public APIs. These may change.
- New created Azure Apps must apply for the Permission to use the Minecraft API.


## License
- TODO: Add a license file and update this section accordingly.
