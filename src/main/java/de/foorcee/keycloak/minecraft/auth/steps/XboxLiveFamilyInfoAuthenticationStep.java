package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.JsonNode;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.XboxProfile;
import de.foorcee.keycloak.minecraft.auth.result.XboxLiveXstsToken;
import de.foorcee.keycloak.minecraft.auth.result.XboxTokenPair;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class XboxLiveFamilyInfoAuthenticationStep implements AuthenticationFlowStep<XboxTokenPair<XboxLiveXstsToken>, XboxProfile> {

    private static final String XBOX_ACCOUNT_URL = "https://accounts.xboxlive.com/family/memberXuid(%d)";
    private static final String XBL_AUTH_HEADER = "XBL3.0 x=%s;%s";

    @Override
    public XboxProfile execute(KeycloakSession session, AuthApplication application, XboxTokenPair<XboxLiveXstsToken> token) throws Exception {
        String target = String.format(XBOX_ACCOUNT_URL, token.xstsToken().xid());
        String authHeader = String.format(XBL_AUTH_HEADER, token.xblToken().userHash(), token.xstsToken().token());
        try (SimpleHttpResponse response = SimpleHttp.create(session).doGet(target)
                .header("x-xbl-contract-version", "3")
                .header("Authorization", authHeader)
                .asResponse()) {
            if (response.getStatus() != 200) {
                throw new IllegalStateException("Unexpected response status: " + response.getStatus());
            }

            JsonNode body = response.asJson();
            JsonNode member = findTargetMember(body, token.xstsToken().xid());

            if (!member.has("firstName")){
                throw new IllegalArgumentException("Missing 'firstName' field");
            }

            String firstName = member.get("firstName").asText();

            if (!member.has("lastName")){
                throw new IllegalArgumentException("Missing 'lastName' field");
            }

            String lastName = member.get("lastName").asText();

            if (!member.has("email")){
                throw new IllegalArgumentException("Missing 'email' field");
            }

            String email = member.get("email").asText();

            return new XboxProfile(email, firstName, lastName);
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain user profile from xbox auth", e);
        }
    }

    private static JsonNode findTargetMember(JsonNode node, long xid) {
        if (!node.has("familyUsers")) {
            throw new IllegalStateException("familyUsers field has not been set");
        }

        JsonNode familyUsers = node.get("familyUsers");

        if (!familyUsers.isArray()) {
            throw new IllegalStateException("Invalid familyUsers field; not an array");
        }

        for (JsonNode user : familyUsers) {
            if (user.get("xuid").asLong() == xid) {
                return user;
            }
        }

        throw new IllegalArgumentException("User with id " + xid + " not found");
    }
}
