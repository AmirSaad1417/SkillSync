package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Automated programmatic verification test for MainShellView, DashboardView,
 * navigation transitions, and logout lifecycle.
 */
public class MainShellVerification {

    private static final AtomicBoolean testPassed = new AtomicBoolean(false);
    private static Throwable failureReason = null;

    public static void main(String[] args) {
        System.out.println("=== Starting SkillSync GUI Stage 2 Verification ===");

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    runVerificationSuite();
                    testPassed.set(true);
                } catch (Throwable t) {
                    failureReason = t;
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });

            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("❌ Verification timed out after 15 seconds!");
                System.exit(1);
            }

            if (testPassed.get()) {
                System.out.println("✅ ALL GUI STAGE 2 VERIFICATION CHECKS PASSED SUCCESSFULLY!");
                System.exit(0);
            } else {
                System.err.println("❌ Verification FAILED: " + (failureReason != null ? failureReason.getMessage() : "Unknown error"));
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize JavaFX Platform: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runVerificationSuite() {
        System.out.println("1. Loading users from CSV...");
        List<User> users = FileHandler.loadUsers();
        if (users.isEmpty()) {
            throw new AssertionError("Expected users.csv to contain users, but list was empty.");
        }
        System.out.println("   -> Loaded " + users.size() + " users.");

        // Find test users: 'saad' (matched) and 'ali' (unmatched, high score)
        User matchedUser = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase("saad"))
                .findFirst()
                .orElse(users.get(0));

        User activeStudent = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase("ali"))
                .findFirst()
                .orElse(users.get(0));

        System.out.println("2. Initializing SkillSyncApp and Primary Stage...");
        SkillSyncApp app = new SkillSyncApp();
        Stage stage = new Stage();
        app.start(stage);

        System.out.println("3. Testing Student Login Flow for Matched User ('" + matchedUser.getUsername() + "')...");
        app.onLoginSuccess(matchedUser);

        if (app.getCurrentUser() == null || !app.getCurrentUser().getUsername().equals(matchedUser.getUsername())) {
            throw new AssertionError("currentUser was not set properly on login.");
        }

        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            throw new AssertionError("Primary stage scene should not be null after onLoginSuccess.");
        }
        System.out.println("   -> Shell window created with title: '" + stage.getTitle() + "'");

        System.out.println("4. Verifying MainShellView Components for Matched User...");
        MainShellView shellView = new MainShellView(app, matchedUser);
        if (shellView.getView() == null) {
            throw new AssertionError("MainShellView.getView() returned null.");
        }
        System.out.println("   -> Shell layout constructed successfully.");

        System.out.println("5. Verifying DashboardView for Active Student ('" + activeStudent.getUsername() + "')...");
        DashboardView dashboardView = new DashboardView(app, activeStudent, shellView::navigateTo);
        if (dashboardView.getView() == null) {
            throw new AssertionError("DashboardView.getView() returned null.");
        }
        System.out.println("   -> Dashboard metrics and quick actions constructed successfully.");

        System.out.println("6. Testing Navigation through all NavSection values...");
        for (NavSection section : NavSection.values()) {
            shellView.navigateTo(section);
            System.out.println("   -> Navigated to: " + section.name() + " (" + section.getTitle() + ") OK");
        }

        // Return back to dashboard
        shellView.navigateTo(NavSection.DASHBOARD);
        System.out.println("   -> Navigated back to DASHBOARD OK");

        System.out.println("7. Testing Logout Lifecycle...");
        app.logout();
        if (app.getCurrentUser() != null) {
            throw new AssertionError("currentUser should be null after logout.");
        }
        System.out.println("   -> Logout cleared session and reset stage title: '" + stage.getTitle() + "'");

        System.out.println("8. Verifying Login Screen rendered after Logout...");
        if (stage.getScene() == null) {
            throw new AssertionError("Stage should have Login scene mounted after logout.");
        }
        System.out.println("   -> Login scene successfully mounted after logout.");
    }
}
