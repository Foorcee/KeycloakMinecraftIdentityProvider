package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AuthenticationFlowBuilder<TInput, TResult> {

    private final AuthenticationFlowBuilder<TInput, ?> previous;
    private final AuthenticationFlowStep<?, TResult> step;

    public AuthenticationFlowBuilder(AuthenticationFlowStep<TInput, TResult> step) {
        this.previous = null;
        this.step = step;
    }

    public AuthenticationFlowBuilder(AuthenticationFlowBuilder<TInput, ?> previous, AuthenticationFlowStep<?, TResult> step) {
        this.previous = previous;
        this.step = step;
    }

    public static <TIn, TRes> AuthenticationFlowBuilder<TIn, TRes> initialize(AuthenticationFlowStep<TIn, TRes> step) {
        return new AuthenticationFlowBuilder<>(step);
    }

    public <TNext> AuthenticationFlowBuilder<TInput, TNext> then(AuthenticationFlowStep<TResult, TNext> step) {
        return new AuthenticationFlowBuilder<>(this, step);
    }

    public <TNext, TOut> AuthenticationFlowBuilder<TInput, TNext> merge(AuthenticationFlowStep<TResult, TOut> step, BiFunction<TResult, TOut, TNext> merge) {
        return new AuthenticationFlowBuilder<>(this, new AuthenticationFlowStep<TResult, TNext>() {
            @Override
            public TNext execute(KeycloakSession session, AuthApplication application, TResult o) throws Exception {
                TOut result = step.execute(session, application, o);
                return merge.apply(o, result);
            }
        });
    }

    public AuthenticationFlowBuilder<TInput, TResult> accept(Consumer<TResult> consumer) {
        return new AuthenticationFlowBuilder<>(this, (AuthenticationFlowStep<TResult, TResult>)
                (session, application, tResult) -> {
                    consumer.accept(tResult);
                    return tResult;
                });
    }

    public <TNext> AuthenticationFlowBuilder<TInput, TNext> map(Function<TResult, TNext> mapper) {
        return new AuthenticationFlowBuilder<>(this, (AuthenticationFlowStep<TResult, TNext>)
                (session, application, tResult) -> mapper.apply(tResult));
    }

    public AuthenticationFlow<TInput, TResult> build() {
        List<AuthenticationFlowStep<?, ?>> steps = new ArrayList<>();
        AuthenticationFlowBuilder<TInput, ?> current = this;

        while (current != null) {
            steps.add(current.step);
            current = current.previous;
        }
        return new AuthenticationFlow<>(steps.reversed());
    }
}
