package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import de.foorcee.keycloak.minecraft.auth.result.XstsToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class XstsTokenAuthenticationStep implements AuthenticationFlowStep<XblToken, XstsToken> {
    private static final String AUTH_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final String relyingParty;

    public XstsTokenAuthenticationStep(String relyingParty) {
        this.relyingParty = relyingParty;
    }

    @Override
    public XstsToken execute(KeycloakSession session, AuthApplication application, XblToken xblToken) throws Exception {
        ObjectNode props = MAPPER.createObjectNode();
        props.put("SandboxId", "RETAIL");
        props.putArray("UserTokens").add(xblToken.token());

        ObjectNode obj = MAPPER.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", relyingParty);
        obj.put("TokenType", "JWT");

        try {
            JsonNode response = SimpleHttp.create(session).doPost(AUTH_XSTS_URL)
                    .json(obj).asJson();

            if (response.has("XErr")) {
                long xErr = response.get("XErr").asLong();
                if (xErr == 2148916233L) {
                    throw new IllegalStateException("No Xbox account associated");
                } else if (xErr == 2148916235L) {
                    throw new IllegalStateException("Xbox Live is not available in your country");
                } else if (xErr == 2148916238L) {
                    throw new IllegalStateException("Account is underage and requires adult verification");
                } else {
                    throw new IllegalStateException("Xbox authentication error: " + xErr);
                }
            }

            if (!response.has("Token")) {
                throw new IllegalArgumentException("Missing token from response.has(\"Token\")");
            }

            String token = response.get("Token").asText();
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token is null or empty");
            }

            return new XstsToken(token);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain XSTS token from Xbox", e);
        }
    }
}
