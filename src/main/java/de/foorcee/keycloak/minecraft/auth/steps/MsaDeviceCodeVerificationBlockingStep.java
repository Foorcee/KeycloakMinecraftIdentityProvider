package de.foorcee.keycloak.minecraft.auth.steps;

import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.exception.MsaAuthenticationException;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.result.MsaDeviceCode;
import org.keycloak.models.KeycloakSession;

import java.time.Duration;

public class MsaDeviceCodeVerificationBlockingStep extends MsaDeviceCodeVerificationStep {

    private final Duration timeout;

    public MsaDeviceCodeVerificationBlockingStep(Duration waitDuration) {
        this.timeout = waitDuration;
    }

    @Override
    public MsaAccessToken execute(KeycloakSession session, AuthApplication application, MsaDeviceCode msaDeviceCode) throws Exception {
        long endTime = System.currentTimeMillis() + this.timeout.toMillis();
        while (endTime > System.currentTimeMillis()) {
            try {
                return super.execute(session, application, msaDeviceCode);
            } catch (MsaAuthenticationException e) {
                if (e.isAuthorizationPending()) {
                    Thread.sleep(msaDeviceCode.interval());
                    continue;
                }
                throw e;
            }
        }

        throw new IllegalStateException("Timed out waiting for authentication");
    }
}
