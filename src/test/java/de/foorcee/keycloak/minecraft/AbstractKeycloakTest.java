package de.foorcee.keycloak.minecraft;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;

import static org.mockito.Mockito.*;

public class AbstractKeycloakTest {

    public static AuthApplication MINECRAFT_LAUNCHER = new AuthApplication("00000000402b5328", null, "service::user.auth.xboxlive.com::MBI_SSL");

    protected final KeycloakSession session = mock(KeycloakSession.class);
    private final HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
    private final KeycloakTransactionManager transactionManager = mock(KeycloakTransactionManager.class);

    protected AbstractKeycloakTest() {
        when(session.getTransactionManager()).thenReturn(transactionManager);

        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.create()).thenReturn(session);

        when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
        when(httpClientProvider.getMaxConsumedResponseSize()).thenReturn(10_000_000L);
    }
}
