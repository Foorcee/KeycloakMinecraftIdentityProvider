package de.foorcee.keycloak.minecraft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

public class MinecraftIdentityProvider extends AbstractOAuth2IdentityProvider<MinecraftIdentityProviderConfig> implements SocialIdentityProvider<MinecraftIdentityProviderConfig> {

    private static final String AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    private static final String AUTH_XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String AUTH_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String AUTH_MINECRAFT_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private static final String DEFAULT_SCOPE = "XboxLive.signin offline_access";

    public MinecraftIdentityProvider(KeycloakSession session, MinecraftIdentityProviderConfig config) {
        super(session, config);

        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        XblToken xblToken = requestXboxToken(accessToken);
        XstsToken xstsToken = requestXstsToken(xblToken);
        MinecraftToken minecraftToken = requestMinecraftToken(xstsToken, xblToken);

        MinecraftProfile profile = requestMinecraftProfile(minecraftToken);

        BrokeredIdentityContext user = new BrokeredIdentityContext(profile.id(), getConfig());
        user.setUsername(profile.username());

        return user;
    }

    @Override
    public SimpleHttpRequest authenticateTokenRequest(SimpleHttpRequest tokenRequest) {
        SimpleHttpRequest request = tokenRequest.param("scope", "xboxlive.signin");
        request = super.authenticateTokenRequest(request);
//        if (request.getParams().containsKey("client_secret")) {
//            Map<String, String> params = new HashMap<>(request.getParams());
//            params.remove("client_secret");
//            request.params(params);
//        }

        return request;
    }

    private XblToken requestXboxToken(String accessToken) {
        ObjectNode props = mapper.createObjectNode();
        props.put("AuthMethod", "RPS");
        props.put("SiteName", "user.auth.xboxlive.com");
        props.put("RpsTicket", "d=" + accessToken);

        ObjectNode obj = mapper.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", "http://auth.xboxlive.com");
        obj.put("TokenType", "JWT");

        try {
            JsonNode response = SimpleHttp.create(session).doPost(AUTH_XBL_URL)
                    .json(obj).asJson();

            if (!response.has("Token")) {
                throw new IllegalArgumentException("Missing token from responseObj.has(\"Token\")");
            }
            String token = response.get("Token").asText();
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token is null or empty");
            }

            if (!response.has("DisplayClaims")) {
                throw new IllegalArgumentException("Missing token from responseObj.has(\"DisplayClaims\")");
            }

            JsonNode displayClaims = response.get("DisplayClaims");

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

    private XstsToken requestXstsToken(XblToken xblToken) {
        if (xblToken == null || xblToken.token() == null) {
            throw new IdentityBrokerException("xblToken or xblToken.token is null");
        }

        ObjectNode props = mapper.createObjectNode();
        props.put("SandboxId", "RETAIL");
        props.putArray("UserTokens").add(xblToken.token());

        ObjectNode obj = mapper.createObjectNode();
        obj.set("Properties", props);
        obj.put("RelyingParty", "rp://api.minecraftservices.com/");
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

    private MinecraftToken requestMinecraftToken(XstsToken xstsToken, XblToken xblToken) {
        if (xstsToken == null || xstsToken.token() == null) {
            throw new IdentityBrokerException("xstsToken or xstsToken.token is null");
        }
        if (xblToken == null || xblToken.ush() == null) {
            throw new IdentityBrokerException("xblToken or xblToken.ush is null");
        }

        ObjectNode obj = mapper.createObjectNode();
        obj.put("identityToken", "XBL3.0 x=" + xblToken.ush() + ";" + xstsToken.token());

        try {
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
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain Minecraft token", e);
        }
    }

    private MinecraftProfile requestMinecraftProfile(MinecraftToken minecraftToken) {
        if (minecraftToken == null || minecraftToken.accessToken() == null) {
            throw new IdentityBrokerException("minecraftToken or minecraftToken.accessToken is null");
        }

        try {
            JsonNode response = SimpleHttp.create(session).doGet(MINECRAFT_PROFILE_URL)
                    .header("Authorization", "Bearer " + minecraftToken.accessToken())
                    .asJson();

            if (response.has("error")) {
                throw new IdentityBrokerException("No Minecraft account found for this user");
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
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not obtain Minecraft profile", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    record XblToken(String token, String ush) {}

    record XstsToken(String token) {}

    record MinecraftToken(String accessToken) {}

    record MinecraftProfile(String username, String id) {}
}
