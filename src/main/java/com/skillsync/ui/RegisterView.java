package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Modern Desktop Registration View for SkillSync.
 */
public class RegisterView {

    private final SkillSyncApp app;
    private final VBox root;
    private final TextField usernameField;
    private final TextField emailField;
    private final PasswordField passwordField;
    private final Label usernameError;
    private final Label emailError;
    private final Label passwordError;
    private final Label alertBanner;

    public RegisterView(SkillSyncApp app) {
        this.app = app;
        this.root = new VBox();
        this.root.getStyleClass().add("auth-container");
        this.root.setAlignment(Pos.CENTER);
        this.root.setPadding(new Insets(30));

        VBox card = new VBox(18);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Header section
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label badge = new Label("STUDENT REGISTRATION");
        badge.getStyleClass().add("brand-badge");

        Label title = new Label("Create Account");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("Join SkillSync to take assessments and connect with compatible peers.");
        subtitle.getStyleClass().add("brand-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        headerBox.getChildren().addAll(badge, title, subtitle);

        // Top alert banner
        alertBanner = new Label();
        alertBanner.getStyleClass().addAll("alert-banner", "alert-error");
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
        alertBanner.setMaxWidth(Double.MAX_VALUE);

        // Form Fields
        VBox form = new VBox(14);

        // 1. Username
        VBox userGroup = new VBox(4);
        userGroup.getStyleClass().add("form-group");
        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("form-label");
        usernameField = new TextField();
        usernameField.getStyleClass().add("form-input");
        usernameField.setPromptText("e.g. AmirSaad");
        usernameError = new Label();
        usernameError.getStyleClass().add("field-error");
        usernameError.setVisible(false);
        usernameError.setManaged(false);
        userGroup.getChildren().addAll(userLabel, usernameField, usernameError);

        // 2. Email
        VBox emailGroup = new VBox(4);
        emailGroup.getStyleClass().add("form-group");
        Label emailLabel = new Label("University Email Address");
        emailLabel.getStyleClass().add("form-label");
        emailField = new TextField();
        emailField.getStyleClass().add("form-input");
        emailField.setPromptText("e.g. saad123@gmail.com");
        emailError = new Label();
        emailError.getStyleClass().add("field-error");
        emailError.setVisible(false);
        emailError.setManaged(false);
        emailGroup.getChildren().addAll(emailLabel, emailField, emailError);

        // 3. Password
        VBox passGroup = new VBox(4);
        passGroup.getStyleClass().add("form-group");
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("form-input");
        passwordField.setPromptText("Minimum 6 characters");
        passwordError = new Label();
        passwordError.getStyleClass().add("field-error");
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        passGroup.getChildren().addAll(passLabel, passwordField, passwordError);

        // Submit Button
        Button createAccountBtn = new Button("Create Account");
        createAccountBtn.getStyleClass().add("btn-primary");
        createAccountBtn.setMaxWidth(Double.MAX_VALUE);
        createAccountBtn.setOnAction(e -> handleRegistration());

        // Keyboard ENTER support
        usernameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleRegistration());

        form.getChildren().addAll(userGroup, emailGroup, passGroup, createAccountBtn);

        // Footer / Back to Login
        HBox footerBox = new HBox(6);
        footerBox.setAlignment(Pos.CENTER);
        Label loginPrompt = new Label("Already registered?");
        loginPrompt.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Button loginLink = new Button("Sign In");
        loginLink.getStyleClass().add("btn-link");
        loginLink.setOnAction(e -> app.showLoginView(""));
        footerBox.getChildren().addAll(loginPrompt, loginLink);

        card.getChildren().addAll(headerBox, alertBanner, form, footerBox);
        root.getChildren().add(card);
    }

    private void handleRegistration() {
        resetErrors();

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        boolean hasError = false;

        // Username validation
        if (username.isEmpty()) {
            showFieldError(usernameError, "Username cannot be empty.");
            hasError = true;
        } else if (username.contains(",")) {
            showFieldError(usernameError, "Username cannot contain commas.");
            hasError = true;
        } else if (app.getUsers().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username))) {
            showFieldError(usernameError, "Username is already taken.");
            hasError = true;
        }

        // Email validation
        if (email.isEmpty()) {
            showFieldError(emailError, "Email cannot be empty.");
            hasError = true;
        } else if (!email.contains("@")) {
            showFieldError(emailError, "Please enter a valid email format (e.g. user@domain.com).");
            hasError = true;
        } else if (email.contains(",")) {
            showFieldError(emailError, "Email cannot contain commas.");
            hasError = true;
        } else if (app.getUsers().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
            showFieldError(emailError, "Email is already registered.");
            hasError = true;
        }

        // Password validation
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password cannot be empty.");
            hasError = true;
        } else if (password.length() < 6) {
            showFieldError(passwordError, "Password must be at least 6 characters.");
            hasError = true;
        } else if (password.contains(",")) {
            showFieldError(passwordError, "Password cannot contain commas.");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        try {
            // Instantiate new domain User
            User newUser = new User(username, email, password);
            app.getUsers().add(newUser);

            // Persist to CSV atomically
            boolean saved = FileHandler.saveUsers(app.getUsers());
            if (!saved) {
                showAlert("Failed to save account to database. Please try again.");
                return;
            }

            // Successfully created -> navigate to login with pre-filled email
            app.showLoginView(email);

        } catch (IllegalArgumentException ex) {
            showAlert("Registration error: " + ex.getMessage());
        }
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void resetErrors() {
        usernameError.setVisible(false);
        usernameError.setManaged(false);
        emailError.setVisible(false);
        emailError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }

    private void showAlert(String message) {
        alertBanner.setText(message);
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);
    }

    public VBox getView() {
        return root;
    }
}
