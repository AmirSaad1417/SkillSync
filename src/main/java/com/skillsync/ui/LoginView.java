package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Modern Desktop Login View for SkillSync.
 */
public class LoginView {

    private final SkillSyncApp app;
    private final VBox root;
    private final TextField identifierField;
    private final PasswordField passwordField;
    private final Label alertBanner;

    public LoginView(SkillSyncApp app, String initialEmail) {
        this.app = app;
        this.root = new VBox();
        this.root.getStyleClass().add("auth-container");
        this.root.setAlignment(Pos.CENTER);
        this.root.setPadding(new Insets(30));

        // Center card
        VBox card = new VBox(20);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Header section
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label badge = new Label("SKILLSYNC • PEER MATCHING");
        badge.getStyleClass().add("brand-badge");

        Label title = new Label("Welcome Back");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("Sign in to access your skills, assessments, and peer matches.");
        subtitle.getStyleClass().add("brand-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        headerBox.getChildren().addAll(badge, title, subtitle);

        // Alert message banner
        alertBanner = new Label();
        alertBanner.getStyleClass().addAll("alert-banner", "alert-error");
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
        alertBanner.setMaxWidth(Double.MAX_VALUE);

        // Form Fields
        VBox form = new VBox(16);

        // Identifier (Email or Username)
        VBox idGroup = new VBox(6);
        idGroup.getStyleClass().add("form-group");
        Label idLabel = new Label("Email Address or Username");
        idLabel.getStyleClass().add("form-label");
        identifierField = new TextField(initialEmail != null ? initialEmail : "");
        identifierField.getStyleClass().add("form-input");
        identifierField.setPromptText("e.g. saad1122@gmail.com or admin");
        idGroup.getChildren().addAll(idLabel, identifierField);

        // Password
        VBox passGroup = new VBox(6);
        passGroup.getStyleClass().add("form-group");
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("form-input");
        passwordField.setPromptText("Enter your account password");
        passGroup.getChildren().addAll(passLabel, passwordField);

        // Sign In Button
        Button signInBtn = new Button("Sign In");
        signInBtn.getStyleClass().add("btn-primary");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setOnAction(e -> handleLogin());

        // Keyboard ENTER support
        identifierField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());

        form.getChildren().addAll(idGroup, passGroup, signInBtn);

        // Footer / Register switch
        HBox footerBox = new HBox(6);
        footerBox.setAlignment(Pos.CENTER);
        Label registerPrompt = new Label("Don't have an account?");
        registerPrompt.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Button registerLink = new Button("Sign Up");
        registerLink.getStyleClass().add("btn-link");
        registerLink.setOnAction(e -> app.showRegisterView());
        footerBox.getChildren().addAll(registerPrompt, registerLink);

        // Admin info note
        Label adminNote = new Label("Tip: Admin login using admin / admin123");
        adminNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        adminNote.setMaxWidth(Double.MAX_VALUE);
        adminNote.setAlignment(Pos.CENTER);

        card.getChildren().addAll(headerBox, alertBanner, form, footerBox, adminNote);
        root.getChildren().add(card);
    }

    private void handleLogin() {
        String identifier = identifierField.getText().trim();
        String password = passwordField.getText().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both your email/username and password.");
            return;
        }

        // 1. Admin Authentication Check
        if (identifier.equalsIgnoreCase("admin") && password.equals("admin123")) {
            hideAlert();
            app.onAdminLoginSuccess();
            return;
        }

        // 2. Student Authentication Check
        User authenticatedUser = app.getUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(identifier) || u.getUsername().equalsIgnoreCase(identifier))
                .filter(u -> u.getPassword().equals(password))
                .findFirst()
                .orElse(null);

        if (authenticatedUser != null) {
            hideAlert();
            app.onLoginSuccess(authenticatedUser);
        } else {
            showAlert("Invalid email or password. Please verify your credentials.");
        }
    }

    private void showAlert(String message) {
        alertBanner.setText(message);
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);
    }

    private void hideAlert() {
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }

    public VBox getView() {
        return root;
    }
}
