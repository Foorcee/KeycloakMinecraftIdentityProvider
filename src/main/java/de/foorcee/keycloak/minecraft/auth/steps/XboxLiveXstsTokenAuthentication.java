package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.RelyingParty;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import de.foorcee.keycloak.minecraft.auth.result.XboxLiveXstsToken;

public class XboxLiveXstsTokenAuthentication extends AbstractXstsTokenAuthenticationStep<XboxLiveXstsToken> {

    @Override
    ObjectNode requestPayload(XblToken xblToken) {
        ObjectNode props = MAPPER.createObjectNode();
        props.put("SandboxId", "RETAIL");
        props.putArray("UserTokens").add(xblToken.token());
        // Ask for the modern gamertag values
        props.putArray("OptionalDisplayClaims").add("mgt").add("umg").add("mgs");

        ObjectNode obj = MAPPER.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", RelyingParty.XBOX_LIVE);
        obj.put("TokenType", "JWT");
        return obj;
    }

    @Override
    XboxLiveXstsToken fromResponse(JsonNode response) {
        if (!response.has("Token")) {
            throw new IllegalArgumentException("Missing token from response.has(\"Token\")");
        }

        String token = response.get("Token").asText();
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        JsonNode xui = XblTokenAuthenticationStep.extractXuiClaims(response);

        if (!xui.has("gtg")) {
            throw new IllegalArgumentException("Missing gamertag in response");
        }

        String gamertag = xui.get("gtg").asText();

        if (!xui.has("xid")) {
            throw new IllegalArgumentException("Missing xid in response");
        }

        long xid = xui.get("xid").asLong();
        return new XboxLiveXstsToken(token, gamertag, xid);
    }

}
