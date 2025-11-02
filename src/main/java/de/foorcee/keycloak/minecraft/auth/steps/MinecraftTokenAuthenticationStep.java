package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftToken;
import de.foorcee.keycloak.minecraft.auth.result.XboxTokenPair;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

public class MinecraftTokenAuthenticationStep implements AuthenticationFlowStep<XboxTokenPair, MinecraftToken> {

    private static final String AUTH_MINECRAFT_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    @Override
    public MinecraftToken execute(KeycloakSession session, AuthApplication application, XboxTokenPair pair) throws Exception {
        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("identityToken", "XBL3.0 x=" + pair.xblToken().userHash() + ";" + pair.xstsToken().token());
        try (SimpleHttpResponse response = SimpleHttp.create(session).doPost(AUTH_MINECRAFT_URL).json(obj).asResponse()) {
            if (response.getStatus() != 200) {
                throw new IdentityBrokerException("Could not obtain Minecraft token. Response code: " + response.getStatus());
            }

            JsonNode body = response.asJson();
            if (!body.has("access_token")) {
                throw new IllegalStateException("Missing access_token from response.has(\"access_token\")");
            }

            String accessToken = body.get("access_token").asText();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalStateException("access_token is null or empty");
            }

            return new MinecraftToken(accessToken);
        }
    }
}
