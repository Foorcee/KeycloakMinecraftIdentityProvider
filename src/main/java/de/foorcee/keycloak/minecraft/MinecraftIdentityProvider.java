package de.foorcee.keycloak.minecraft;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.flow.AuthenticationFlow;
import de.foorcee.keycloak.minecraft.auth.flow.AuthenticationsFlows;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftProfile;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;

public class MinecraftIdentityProvider extends AbstractOAuth2IdentityProvider<MinecraftIdentityProviderConfig> implements SocialIdentityProvider<MinecraftIdentityProviderConfig> {

    private static final String AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    private static final String DEFAULT_SCOPE = "XboxLive.signin offline_access openid profile"; //

    private final AuthApplication application;

    public MinecraftIdentityProvider(KeycloakSession session, MinecraftIdentityProviderConfig config) {
        super(session, config);

        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);

        this.application = new AuthApplication(config.getClientId(), config.getClientSecret(), config.getDefaultScope());
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        MsaAccessToken msaAccessToken = new MsaAccessToken(accessToken);

        AuthenticationFlow<MsaAccessToken, MinecraftProfile> flow = AuthenticationsFlows.JAVA_MINECRAFT_PROFILE;

        MinecraftProfile profile;
        try {
            profile = flow.execute(session, application, msaAccessToken);
        } catch (Exception e) {
            throw new IdentityBrokerException("Authentication flow error", e);
        }

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

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

}
