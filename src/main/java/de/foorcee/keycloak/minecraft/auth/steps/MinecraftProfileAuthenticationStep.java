package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftProfile;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftToken;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

public class MinecraftProfileAuthenticationStep implements AuthenticationFlowStep<MinecraftToken, MinecraftProfile> {

    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    @Override
    public MinecraftProfile execute(KeycloakSession session, AuthApplication application, MinecraftToken minecraftToken) throws Exception {
        JsonNode response = SimpleHttp.create(session).doGet(MINECRAFT_PROFILE_URL)
                .header("Authorization", "Bearer " + minecraftToken.accessToken())
                .asJson();

        if (response.has("error")) {
            throw new IllegalStateException("No Minecraft account found for this user");
        }

        if (!response.has("name")) {
            throw new IllegalStateException("Missing name from response.has(\"name\")");
        }
        String name = response.get("name").asText();
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("name is null or empty");
        }

        if (!response.has("id")) {
            throw new IllegalStateException("Missing id from response.has(\"id\")");
        }
        String id = response.get("id").asText();
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("id is null or empty");
        }

        return new MinecraftProfile(name, id);
    }
}
