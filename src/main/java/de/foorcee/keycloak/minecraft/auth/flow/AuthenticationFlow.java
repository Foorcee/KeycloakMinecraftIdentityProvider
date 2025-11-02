package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import org.keycloak.models.KeycloakSession;

import java.util.List;

public class AuthenticationFlow<TIn, TRes> {

    private final List<AuthenticationFlowStep<?, ?>> steps;

    public AuthenticationFlow(List<AuthenticationFlowStep<?, ?>> steps) {
        this.steps = steps;
    }

    public TRes execute(KeycloakSession session, AuthApplication authApp, TIn input) throws Exception {
        AuthenticationFlowStep<Object, ?> first = (AuthenticationFlowStep<Object, Object>) steps.getFirst();
        Object result = first.execute(session, authApp, input);
        for (int i = 1; i < steps.size(); i++) {
            var step = (AuthenticationFlowStep<Object, Object>) steps.get(i);
            result = step.execute(session, authApp, result);
        }
        return (TRes) result;
    }

    public List<AuthenticationFlowStep<?, ?>> getSteps() {
        return steps;
    }
}
