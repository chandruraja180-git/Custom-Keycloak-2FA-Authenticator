package com.mycompany.keycloak.authenticator;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.http.HttpRequest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class Custom2FAAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        //  Check if OTP already exists in session (Prevents new OTP on language change)
        String existingOtp = context.getAuthenticationSession().getAuthNote("OTP");
        String otpToShow;

        if (existingOtp == null) {
            otpToShow = String.valueOf((int)(Math.random() * 900000) + 100000);
            context.getAuthenticationSession().setAuthNote("OTP", otpToShow);
            context.getAuthenticationSession().setAuthNote("OTP_TIME", String.valueOf(System.currentTimeMillis()));
            System.out.println("Generated NEW OTP: " + otpToShow);
        } else {
            otpToShow = existingOtp;
            System.out.println("Language changed or refresh. Reusing OTP: " + otpToShow);
        }

        // Pass the OTP to the FreeMarker template
        Response challenge = context.form()
                .setAttribute("otp", otpToShow)
                .createForm("2fa-form.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        try {
            HttpRequest request = context.getHttpRequest();
            MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
            String enteredOtp = formData.getFirst("otp");

            String storedOtp = context.getAuthenticationSession().getAuthNote("OTP");
            String otpTimeStr = context.getAuthenticationSession().getAuthNote("OTP_TIME");

            // 1. Validate Input
            if (enteredOtp == null || enteredOtp.isEmpty()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("otp.empty").createForm("2fa-form.ftl"));
                return;
            }

            // 2. Validate Expiry (60 Seconds)
            long otpTime = Long.parseLong(otpTimeStr);
            if ((System.currentTimeMillis() - otpTime) > 60000) {
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setError("otp.expired").createForm("2fa-form.ftl"));
                return;
            }

            // 3. Validate Match
            if (!enteredOtp.equals(storedOtp)) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("otp.failed").createForm("2fa-form.ftl"));
                return;
            }

            // 4. Call Mock API (Beeceptor)
            String username = context.getUser().getUsername();
            String apiUrl = "https://keycloak2fa.free.beeceptor.com/verify?username=" + username;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                String responseBody = new BufferedReader(new InputStreamReader(is))
                        .lines().collect(Collectors.joining());

                // Simple JSON Parsing for cardNumber
                String cardNumber = responseBody.split("\"cardNumber\"\\s*:\\s*\"")[1].split("\"")[0];

                // 5. Store in User Attribute (Required for Token Mapper)
                context.getUser().setSingleAttribute("cardNumber", cardNumber);

                System.out.println("Success! Card Number " + cardNumber + " stored for user " + username);

                // Cleanup session notes
                context.getAuthenticationSession().removeAuthNote("OTP");
                context.getAuthenticationSession().removeAuthNote("OTP_TIME");

                context.success();
            } else {
                context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                        context.form().setError("otp.error").createForm("2fa-form.ftl"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("otp.error").createForm("2fa-form.ftl"));
        }
    }

    @Override public boolean requiresUser() { return true; }
    @Override public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) { return true; }
    @Override public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {}
    @Override public void close() {}
}