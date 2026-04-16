package com.mycompany.keycloak.authenticator.address;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class AddressCaptureAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "address-capture-authenticator";
    private static final AddressCaptureAuthenticator SINGLETON = new AddressCaptureAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Address Capture";
    }

    @Override
    public String getHelpText() {
        return "Captures user address information.";
    }

    @Override
    public String getReferenceCategory() {
        return "address";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * FINAL FIX: Keycloak interfaces expect an Array [].
     */
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}
}