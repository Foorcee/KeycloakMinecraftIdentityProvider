package de.foorcee.keycloak.minecraft.auth.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractAuthenticationStep<TPrev, TRes> {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public abstract TRes execute(KeycloakSession session, AuthApplication application, TPrev tPrev) throws Exception;
}
