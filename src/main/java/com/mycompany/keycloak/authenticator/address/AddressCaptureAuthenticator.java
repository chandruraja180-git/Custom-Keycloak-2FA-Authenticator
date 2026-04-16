package com.mycompany.keycloak.authenticator.address;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.UserModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import jakarta.ws.rs.core.MultivaluedMap;

import com.mycompany.keycloak.authenticator.db.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddressCaptureAuthenticator implements Authenticator {

    // 🔹 Show form
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(context.form().createForm("address-form.ftl"));
    }

    // 🔹 Handle form submit
    @Override
    public void action(AuthenticationFlowContext context) {

        System.out.println("=== ADDRESS VALIDATION START ===");

        MultivaluedMap<String, String> formData =
                context.getHttpRequest().getDecodedFormParameters();

        String address = safe(formData.getFirst("address_line1"));
        String city = safe(formData.getFirst("city"));
        String postalCode = safe(formData.getFirst("postal_code"));

        System.out.println("INPUT -> " + address + ", " + city + ", " + postalCode);

        //  1. Missing field validation
        if (address.isEmpty()) {
            returnError(context, "Address is required");
            return;
        }

        if (city.isEmpty()) {
            returnError(context, "City is required");
            return;
        }

        if (postalCode.isEmpty()) {
            returnError(context, "Postal Code is required");
            return;
        }

        //  2. Address validation
        if (!address.matches("^[a-zA-Z0-9 ,.-]{3,255}$")) {
            returnError(context, "Invalid address format");
            return;
        }

        //  3. City validation
        if (!city.matches("^[a-zA-Z ]{2,100}$")) {
            returnError(context, "City must contain only letters");
            return;
        }

        //  4. Postal code validation
        if (!postalCode.matches("^\\d{5,6}$")) {
            returnError(context, "Postal code must be 5 or 6 digits");
            return;
        }

        UserModel user = context.getUser();

        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        //  Save only valid data
        try {
            saveAddress(user.getId(), address, city, postalCode);
            System.out.println(" VALID DATA SAVED");
        } catch (Exception e) {
            System.err.println("DB ERROR: " + e.getMessage());
        }

        context.success();
    }

    //  Save to DB
    private void saveAddress(String userId, String address, String city, String postalCode) {

        String sql = "INSERT INTO user_address (user_id, address_line1, city, postal_code) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setString(4, postalCode);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //  Sanitization
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    //  Error handler
    private void returnError(AuthenticationFlowContext context, String message) {
        context.challenge(
                context.form()
                        .setError(message)
                        .createForm("address-form.ftl")
        );
    }

    //  Required methods
    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public void close() {}
}