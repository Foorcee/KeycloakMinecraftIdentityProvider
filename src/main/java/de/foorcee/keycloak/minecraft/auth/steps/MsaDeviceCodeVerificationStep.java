package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.exception.MsaAuthenticationException;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.result.MsaDeviceCode;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

public class MsaDeviceCodeVerificationStep implements AuthenticationFlowStep<MsaDeviceCode, MsaAccessToken> {

    private static final String MSA_TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    @Override
    public MsaAccessToken execute(KeycloakSession session, AuthApplication application, MsaDeviceCode msaDeviceCode) throws Exception {
        try (SimpleHttpResponse response = SimpleHttp.create(session).doPost(MSA_TOKEN_URL)
                .param("client_id", application.clientId())
                .param("grant_type", "device_code")
                .param("device_code", msaDeviceCode.deviceCode())
                .asResponse()) {
            JsonNode result = response.asJson();
            JsonNode errorNode = result.get("error");
            if (errorNode != null && !errorNode.isNull()) {
                String error = errorNode.asText("");

                //TODO parse message
                throw new MsaAuthenticationException(error, "");
            }

            String accessToken = result.get("access_token").asText();

            return new MsaAccessToken(accessToken);
        }
    }
}
