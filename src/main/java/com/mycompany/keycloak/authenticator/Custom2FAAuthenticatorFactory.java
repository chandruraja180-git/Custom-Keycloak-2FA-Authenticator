package com.mycompany.keycloak.authenticator;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

public class Custom2FAAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "custom-2fa-authenticator";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new Custom2FAAuthenticator();
    }

    @Override public void init(org.keycloak.Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public void close() {}
    @Override public String getId()          { return PROVIDER_ID; }
    @Override public String getDisplayType() { return "Custom 2FA Authenticator"; }
    @Override public String getHelpText()    { return "Custom two factor authentication"; }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }


    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override public boolean isConfigurable()     { return false; }
    @Override public boolean isUserSetupAllowed() { return false; }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}