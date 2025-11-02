package de.foorcee.keycloak.minecraft.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.KeycloakSession;

public interface AuthenticationFlowStep<TPrev, TRes> {

    ObjectMapper MAPPER = new ObjectMapper();

    TRes execute(KeycloakSession session, AuthApplication application, TPrev tPrev) throws Exception;
}
