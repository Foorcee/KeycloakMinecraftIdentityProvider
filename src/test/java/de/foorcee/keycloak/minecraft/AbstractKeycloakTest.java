package de.foorcee.keycloak.minecraft;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import static org.mockito.Mockito.*;

public class AbstractKeycloakTest {

    public static AuthApplication MINECRAFT_LAUNCHER = new AuthApplication("00000000402b5328", null, "service::user.auth.xboxlive.com::MBI_SSL");

    protected final KeycloakSession session = mock(KeycloakSession.class);
    private final HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    protected AbstractKeycloakTest() {
        when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
        when(httpClientProvider.getMaxConsumedResponseSize()).thenReturn(10_000_000L);
    }
}
