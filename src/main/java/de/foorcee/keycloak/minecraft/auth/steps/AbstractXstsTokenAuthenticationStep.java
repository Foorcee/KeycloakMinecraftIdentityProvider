package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public abstract class AbstractXstsTokenAuthenticationStep<TRes> implements AuthenticationFlowStep<XblToken, TRes> {

    private static final String AUTH_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    @Override
    public TRes execute(KeycloakSession session, AuthApplication application, XblToken xblToken) throws Exception {
        ObjectNode payload = requestPayload(xblToken);
        try {
            JsonNode response = SimpleHttp.create(session).doPost(AUTH_XSTS_URL)
                    .json(payload).asJson();

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

            return fromResponse(response);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain XSTS token from Xbox", e);
        }
    }

    abstract ObjectNode requestPayload(XblToken xblToken);

    abstract TRes fromResponse(JsonNode response);
}
