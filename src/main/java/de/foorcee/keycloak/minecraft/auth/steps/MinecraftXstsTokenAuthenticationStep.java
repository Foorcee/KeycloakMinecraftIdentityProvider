package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.RelyingParty;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftXstsToken;

public class MinecraftXstsTokenAuthenticationStep extends AbstractXstsTokenAuthenticationStep<MinecraftXstsToken> {

    @Override
    ObjectNode requestPayload(XblToken xblToken) {
        ObjectNode props = MAPPER.createObjectNode();
        props.put("SandboxId", "RETAIL");
        props.putArray("UserTokens").add(xblToken.token());

        ObjectNode obj = MAPPER.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", RelyingParty.MINECRAFT_SERVICES);
        obj.put("TokenType", "JWT");
        return obj;
    }

    @Override
    MinecraftXstsToken fromResponse(JsonNode response) {
        if (!response.has("Token")) {
            throw new IllegalArgumentException("Missing token from response.has(\"Token\")");
        }

        String token = response.get("Token").asText();
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        return new MinecraftXstsToken(token);
    }
}
