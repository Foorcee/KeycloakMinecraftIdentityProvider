package de.foorcee.keycloak.minecraft.auth.flow.steps;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import org.keycloak.models.KeycloakSession;

import java.util.function.BiFunction;

public class MergeAuthenticationFlowStep<TIn, TResult, TMerge> implements AuthenticationFlowStep<TIn, TMerge> {

    private final AuthenticationFlowStep<TIn, TResult> flow;
    private final BiFunction<TIn, TResult, TMerge> merge;

    public MergeAuthenticationFlowStep(AuthenticationFlowStep<TIn, TResult> flow, BiFunction<TIn, TResult, TMerge> merge) {
        this.flow = flow;
        this.merge = merge;
    }

    @Override
    public TMerge execute(KeycloakSession session, AuthApplication application, TIn tIn) throws Exception {
        TResult result = flow.execute(session, application, tIn);
        return merge.apply(tIn, result);
    }
}
