package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import de.foorcee.keycloak.minecraft.util.UUIDUtil;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class XblTokenAuthenticationStep implements AuthenticationFlowStep<MsaAccessToken, XblToken> {

    private static final String AUTH_XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";

    @Override
    public XblToken execute(KeycloakSession session, AuthApplication application, MsaAccessToken msaAuthToken) throws Exception {
        boolean title = !UUIDUtil.isDashedUuid(application.clientId());

        ObjectNode props = MAPPER.createObjectNode();
        props.put("AuthMethod", "RPS");
        props.put("SiteName", "user.auth.xboxlive.com");
        props.put("RpsTicket", (title ? "t=" : "d=") + msaAuthToken.accessToken());

        ObjectNode obj = MAPPER.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", "http://auth.xboxlive.com");
        obj.put("TokenType", "JWT");

        try (SimpleHttpResponse response = SimpleHttp.create(session).doPost(AUTH_XBL_URL).json(obj).asResponse()) {

            JsonNode body = response.asJson();

            if (!body.has("Token")) {
                throw new IllegalArgumentException("Missing token from responseObj.has(\"Token\")");
            }
            String token = body.get("Token").asText();
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token is null or empty");
            }

            if (!body.has("DisplayClaims")) {
                throw new IllegalArgumentException("Missing token from responseObj.has(\"DisplayClaims\")");
            }

            JsonNode displayClaims = body.get("DisplayClaims");

            if (!displayClaims.has("xui")) {
                throw new IllegalArgumentException("Missing token from displayClaims.has(\"xui\")");
            }

            JsonNode xui = displayClaims.get("xui");
            if (!xui.isArray()) {
                throw new IllegalArgumentException("Invalid xui array");
            }

            if (xui.isEmpty())
                throw new IllegalArgumentException("xuiArray is empty");

            JsonNode xuiFirst = xui.get(0);

            if (!xuiFirst.has("uhs"))
                throw new IllegalArgumentException("Missing uhs in xui");

            String uhs = xuiFirst.get("uhs").asText();
            if (uhs == null || uhs.isEmpty()) {
                throw new IllegalArgumentException("uhs is null or empty");
            }

            return new XblToken(token, uhs);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain user profile from xbox auth", e);
        }
    }
}
