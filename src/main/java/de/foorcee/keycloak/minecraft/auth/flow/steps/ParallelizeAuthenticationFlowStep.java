package de.foorcee.keycloak.minecraft.auth.flow.steps;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.flow.AuthenticationFlow;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class ParallelizeAuthenticationFlowStep<TIn, TOut1, TOut2, TComb> implements AuthenticationFlowStep<TIn, TComb> {

    private final AuthenticationFlow<TIn, TOut1> flow1;
    private final AuthenticationFlow<TIn, TOut2> flow2;

    private final BiFunction<TOut1, TOut2, TComb> combine;

    public ParallelizeAuthenticationFlowStep(AuthenticationFlow<TIn, TOut1> flow1, AuthenticationFlow<TIn, TOut2> flow2, BiFunction<TOut1, TOut2, TComb> combine) {
        this.flow1 = flow1;
        this.flow2 = flow2;
        this.combine = combine;
    }

    @Override
    public TComb execute(KeycloakSession session, AuthApplication application, TIn prev) throws Exception {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        CompletableFuture<TOut1> flow1Result = executeFlowAsync(flow1, sessionFactory, application, prev);
        CompletableFuture<TOut2> flow2Result = executeFlowAsync(flow2, sessionFactory, application, prev);

        //Wait
        CompletableFuture.allOf(flow1Result, flow2Result).orTimeout(1, TimeUnit.MINUTES).join();

        return combine.apply(flow1Result.get(), flow2Result.get());
    }

    @ActivateRequestContext
    private <T> CompletableFuture<T> executeFlowAsync(AuthenticationFlow<TIn, T> flow, KeycloakSessionFactory sessionFactory, AuthApplication application, TIn prev) {
        return CompletableFuture.supplyAsync(() -> KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory,
                innerSession -> {
                    try {
                        return flow.execute(innerSession, application, prev);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
}
