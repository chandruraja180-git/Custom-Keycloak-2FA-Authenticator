package com.mycompany.keycloak.authenticator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Custom2FAAuthenticatorTest {

    @InjectMocks
    private Custom2FAAuthenticator authenticator;

    @Mock
    private AuthenticationFlowContext context;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private MultivaluedMap<String, String> formData;

    @Mock
    private LoginFormsProvider formProvider;

    @Mock
    private Response response;

    @Mock
    private UserModel user;


    @Test
    void testAuthenticate_ShouldChallengeUser() {
        when(context.form()).thenReturn(formProvider);
        when(formProvider.createForm("2fa-form.ftl")).thenReturn(response);

        authenticator.authenticate(context);

        verify(context).challenge(response);
    }

    //  OTP empty
    @Test
    void testAction_ShouldFail_WhenOtpEmpty() {
        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(formData.getFirst("otp")).thenReturn("");

        when(context.form()).thenReturn(formProvider);
        when(formProvider.setError(anyString())).thenReturn(formProvider);
        when(formProvider.createForm("2fa-form.ftl")).thenReturn(response);

        authenticator.action(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), eq(response));
    }

    //   OTP null
    @Test
    void testAction_ShouldFail_WhenOtpNull() {
        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(formData.getFirst("otp")).thenReturn(null);

        when(context.form()).thenReturn(formProvider);
        when(formProvider.setError(anyString())).thenReturn(formProvider);
        when(formProvider.createForm("2fa-form.ftl")).thenReturn(response);

        authenticator.action(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), eq(response));
    }

    //  API exception case
    @Test
    void testAction_ShouldHandleException() {
        when(context.getHttpRequest()).thenThrow(new RuntimeException("API error"));

        when(context.form()).thenReturn(formProvider);
        when(formProvider.setError(anyString())).thenReturn(formProvider);
        when(formProvider.createForm("2fa-form.ftl")).thenReturn(response);

        authenticator.action(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INTERNAL_ERROR), eq(response));
    }

    //  5. requiresUser()
    @Test
    void testRequiresUser_ShouldReturnTrue() {
        assertTrue(authenticator.requiresUser());
    }

    //  6. configuredFor()
    @Test
    void testConfiguredFor_ShouldReturnTrue() {
        assertTrue(authenticator.configuredFor(null, null, user));
    }
}