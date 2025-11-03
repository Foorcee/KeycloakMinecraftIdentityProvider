package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationFlowExecutor<TIn, TRes> {

    private final List<AuthenticationFlowStep<?, ?>> steps;

    public AuthenticationFlowExecutor(AuthenticationFlow<TIn, TRes> builder) {
        List<AuthenticationFlowStep<?, ?>> steps = new ArrayList<>();
        AuthenticationFlow<TIn, ?> current = builder;

        while (current != null) {
            steps.add(current.step);
            current = current.previous;
        }

        this.steps = steps.reversed();
    }

    public List<AuthenticationFlowStep<?, ?>> getSteps() {
        return steps;
    }

    public TRes execute(KeycloakSession session, AuthApplication application, TIn input) throws Exception {
        AuthenticationFlowStep<Object, ?> first = (AuthenticationFlowStep<Object, Object>) steps.getFirst();
        Object result = first.execute(session, application, input);
        for (int i = 1; i < steps.size(); i++) {
            var step = (AuthenticationFlowStep<Object, Object>) steps.get(i);
            result = step.execute(session, application, result);
        }
        return (TRes) result;
    }
}
