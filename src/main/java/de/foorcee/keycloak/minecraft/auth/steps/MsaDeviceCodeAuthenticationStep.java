package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.MsaDeviceCode;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

public class MsaDeviceCodeAuthenticationStep implements AuthenticationFlowStep<Void, MsaDeviceCode> {

    private static final String MSA_DEVICE_TOKEN_URL = "https://login.live.com/oauth20_connect.srf";

    @Override
    public MsaDeviceCode execute(KeycloakSession session, AuthApplication application, Void unused) throws Exception {
        try (SimpleHttpResponse response = SimpleHttp.create(session).doPost(MSA_DEVICE_TOKEN_URL)
                .param("client_id", application.clientId())
                .param("scope", application.scope())
                .param("response_type", "device_code")
                .asResponse()) {

            JsonNode result = response.asJson();
            long expiresIn = result.get("expires_in").asLong();
            long interval = result.get("interval").asLong();

            String userCode = result.get("user_code").asText();
            String deviceCode = result.get("device_code").asText();
            String verificationUrl = result.get("verification_uri").asText();

            long now = System.currentTimeMillis();
            long expiresAt = now + (expiresIn * 1000L);

            return new MsaDeviceCode(expiresAt, interval, deviceCode, userCode, verificationUrl);
        }
    }
}
