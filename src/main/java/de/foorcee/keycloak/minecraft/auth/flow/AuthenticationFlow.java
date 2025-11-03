package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.flow.steps.ChainAuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.flow.steps.MergeAuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.flow.steps.ParallelizeAuthenticationFlowStep;
import org.keycloak.models.KeycloakSession;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AuthenticationFlow<TInput, TResult> {

    final AuthenticationFlow<TInput, ?> previous;
    final AuthenticationFlowStep<?, TResult> step;

    public AuthenticationFlow(AuthenticationFlowStep<TInput, TResult> step) {
        this.previous = null;
        this.step = step;
    }

    public AuthenticationFlow(AuthenticationFlow<TInput, ?> previous, AuthenticationFlowStep<?, TResult> step) {
        this.previous = previous;
        this.step = step;
    }

    public static <TIn, TRes> AuthenticationFlow<TIn, TRes> initialize(AuthenticationFlowStep<TIn, TRes> step) {
        return new AuthenticationFlow<>(step);
    }

    public static <TIn, TOut, TRes> AuthenticationFlow<TIn, TRes> initialize(AuthenticationFlowStep<TIn, TOut> step, BiFunction<TIn, TOut, TRes> merge) {
        return new AuthenticationFlow<>(new MergeAuthenticationFlowStep<>(step, merge));
    }

    public <TNext> AuthenticationFlow<TInput, TNext> then(AuthenticationFlowStep<TResult, TNext> step) {
        return new AuthenticationFlow<>(this, step);
    }

    public <TNext, TOut> AuthenticationFlow<TInput, TNext> merge(AuthenticationFlowStep<TResult, TOut> step, BiFunction<TResult, TOut, TNext> merge) {
        return new AuthenticationFlow<>(this, new MergeAuthenticationFlowStep<>(step, merge));
    }

    public <TNext, TOut1, TOut2> AuthenticationFlow<TInput, TNext> parallelize(
            AuthenticationFlow<TResult, TOut1> flow1,
            AuthenticationFlow<TResult, TOut2> flow2,
            BiFunction<TOut1, TOut2, TNext> combine
    ) {
        return new AuthenticationFlow<>(this, new ParallelizeAuthenticationFlowStep<>(flow1, flow2, combine));
    }

    public <TNext> AuthenticationFlow<TInput, TNext> chain(AuthenticationFlow<TResult, TNext> flow) {
        return new AuthenticationFlow<>(this, new ChainAuthenticationFlowStep<>(flow));
    }

    public AuthenticationFlow<TInput, TResult> accept(Consumer<TResult> consumer) {
        return new AuthenticationFlow<>(this, (AuthenticationFlowStep<TResult, TResult>)
                (session, application, tResult) -> {
                    consumer.accept(tResult);
                    return tResult;
                });
    }

    public <TNext> AuthenticationFlow<TInput, TNext> map(Function<TResult, TNext> mapper) {
        return new AuthenticationFlow<>(this, (AuthenticationFlowStep<TResult, TNext>)
                (session, application, tResult) -> mapper.apply(tResult));
    }

    public TResult execute(KeycloakSession session, AuthApplication application, TInput input) throws Exception {
        return new AuthenticationFlowExecutor<>(this).execute(session, application, input);
    }
}
