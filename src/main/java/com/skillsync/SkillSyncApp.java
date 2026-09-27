package com.skillsync;

import com.skillsync.ui.AdminShellView;
import com.skillsync.ui.LoginView;
import com.skillsync.ui.RegisterView;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Main JavaFX Application for SkillSync Modern Desktop Platform.
 */
public class SkillSyncApp extends Application {

    private Stage primaryStage;
    private List<User> users;
    private User currentUser;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.users = FileHandler.loadUsers();

        primaryStage.setTitle("SkillSync — Student Career Matching Platform");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(680);

        showLoginView("");
        primaryStage.show();
    }

    public void showLoginView(String initialEmail) {
        LoginView loginView = new LoginView(this, initialEmail);
        Scene scene = new Scene(loginView.getView(), 950, 680);
        attachStylesheet(scene);
        primaryStage.setScene(scene);
    }

    public void showRegisterView() {
        RegisterView registerView = new RegisterView(this);
        Scene scene = new Scene(registerView.getView(), 950, 680);
        attachStylesheet(scene);
        primaryStage.setScene(scene);
    }

    public void onLoginSuccess(User user) {
        this.currentUser = user;
        showMainShell(user);
    }

    public void showMainShell(User user) {
        com.skillsync.ui.MainShellView shellView = new com.skillsync.ui.MainShellView(this, user);
        Scene scene = new Scene((javafx.scene.Parent) shellView.getView(), 1050, 720);
        attachStylesheet(scene);
        primaryStage.setTitle("SkillSync — " + user.getUsername() + " (Student Dashboard)");
        primaryStage.setScene(scene);
    }

    public void logout() {
        String lastEmail = (currentUser != null) ? currentUser.getEmail() : "";
        this.currentUser = null;
        primaryStage.setTitle("SkillSync — Student Career Matching Platform");
        showLoginView(lastEmail);
    }

    public void onAdminLoginSuccess() {
        this.currentUser = null;
        primaryStage.setTitle("SkillSync — Administrator Portal");
        AdminShellView adminShell = new AdminShellView(this);
        Scene scene = new Scene(adminShell.getRoot(), 1050, 720);
        attachStylesheet(scene);
        primaryStage.setScene(scene);
    }

    private void attachStylesheet(Scene scene) {
        String css = getClass().getResource("/styles/skillsync.css") != null
                ? getClass().getResource("/styles/skillsync.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public void refreshUsers() {
        this.users = FileHandler.loadUsers();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
