package de.foorcee.keycloak.minecraft.auth.flow.steps;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.flow.AuthenticationFlow;
import org.keycloak.models.KeycloakSession;

public class ChainAuthenticationFlowStep<TIn, TOut> implements AuthenticationFlowStep<TIn, TOut> {

    private final AuthenticationFlow<TIn, TOut> flow;

    public ChainAuthenticationFlowStep(AuthenticationFlow<TIn, TOut> flow) {
        this.flow = flow;
    }

    @Override
    public TOut execute(KeycloakSession session, AuthApplication application, TIn tIn) throws Exception {
        return flow.execute(session, application, tIn);
    }
}
