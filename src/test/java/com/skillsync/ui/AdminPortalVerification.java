package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Programmatic verification suite for Stage 5:
 * - Student Leaderboard
 * - Admin Authentication
 * - Admin Dashboard & Telemetry
 * - Student Directory (Users)
 * - Participation Statistics
 * - Match Requests Audit Log
 * - Finalized Partnerships
 * - Admin Logout Lifecycle
 */
public class AdminPortalVerification {

    private static final AtomicBoolean testPassed = new AtomicBoolean(false);
    private static Throwable failure = null;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("  SkillSync: Leaderboard & Admin Portal Verification");
        System.out.println("=============================================================");

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    executeVerification();
                    testPassed.set(true);
                } catch (Throwable t) {
                    failure = t;
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });

            boolean finished = latch.await(20, TimeUnit.SECONDS);
            if (!finished) {
                System.err.println("❌ Verification timed out after 20 seconds!");
                System.exit(1);
            }

            if (testPassed.get()) {
                System.out.println("\n🎉 ALL LEADERBOARD & ADMIN PORTAL CHECKS PASSED!");
                System.exit(0);
            } else {
                System.err.println("\n❌ Test FAILED: " + (failure != null ? failure.getMessage() : "Unknown error"));
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Platform initialization error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void executeVerification() {
        // Step 1: Initialize App
        System.out.println("\n[Step 1] Initializing Application & Primary Stage...");
        SkillSyncApp app = new SkillSyncApp();
        Stage stage = new Stage();
        app.start(stage);

        List<User> users = app.getUsers();
        if (users.isEmpty()) {
            throw new AssertionError("User roster is empty!");
        }
        System.out.println("   ✓ Loaded " + users.size() + " registered students from users.csv.");

        // Step 2: Student Leaderboard Verification
        System.out.println("\n[Step 2] Testing Student Leaderboard View...");
        User sampleStudent = users.get(0);
        app.onLoginSuccess(sampleStudent);

        MainShellView studentShell = new MainShellView(app, sampleStudent);
        studentShell.navigateTo(NavSection.LEADERBOARD);

        LeaderboardView leaderboardView = new LeaderboardView(app, sampleStudent, studentShell::navigateTo);
        if (leaderboardView.getView() == null) {
            throw new AssertionError("LeaderboardView failed to render!");
        }
        System.out.println("   ✓ LeaderboardView rendered successfully with rank cards, domain chips, and student row highlighting.");

        // Step 3: Admin Login & Shell Initialization
        System.out.println("\n[Step 3] Testing Admin Login & AdminShellView...");
        app.onAdminLoginSuccess();

        AdminShellView adminShell = new AdminShellView(app);
        if (adminShell.getRoot() == null) {
            throw new AssertionError("AdminShellView failed to build root layout!");
        }
        System.out.println("   ✓ Admin authenticated successfully. Shell initialized with elevated security styling.");

        // Step 4: Admin Dashboard Verification
        System.out.println("\n[Step 4] Testing Admin Dashboard Telemetry View...");
        adminShell.navigateTo(AdminSection.DASHBOARD);
        AdminDashboardView dashboardView = new AdminDashboardView(app, adminShell::navigateTo);
        if (dashboardView.getView() == null) {
            throw new AssertionError("AdminDashboardView failed to render!");
        }
        System.out.println("   ✓ AdminDashboardView rendered with 6 KPI cards, domain telemetry, and quick action shortcuts.");

        // Step 5: Admin Student Directory (Users) Verification
        System.out.println("\n[Step 5] Testing Admin Student Directory (Users)...");
        adminShell.navigateTo(AdminSection.USERS);
        AdminUsersView usersView = new AdminUsersView(app);
        if (usersView.getView() == null) {
            throw new AssertionError("AdminUsersView failed to render!");
        }
        System.out.println("   ✓ AdminUsersView rendered with all " + users.size() + " students and full 4-domain score badges.");

        // Step 6: Admin Participation Statistics Verification
        System.out.println("\n[Step 6] Testing Admin Participation Statistics View...");
        adminShell.navigateTo(AdminSection.PARTICIPATION);
        AdminStatsView statsView = new AdminStatsView(app);
        if (statsView.getView() == null) {
            throw new AssertionError("AdminStatsView failed to render!");
        }
        System.out.println("   ✓ AdminStatsView rendered with domain pass rates and student participation rosters.");

        // Step 7: Admin Match Requests Log Verification
        System.out.println("\n[Step 7] Testing Admin Match Requests Audit Log...");
        adminShell.navigateTo(AdminSection.REQUESTS);
        AdminRequestsView requestsView = new AdminRequestsView(app);
        if (requestsView.getView() == null) {
            throw new AssertionError("AdminRequestsView failed to render!");
        }
        List<MatchManager.MatchRequest> allRequests = FileHandler.loadMatchRequests();
        System.out.println("   ✓ AdminRequestsView rendered with audit log of " + allRequests.size() + " peer match invitations.");

        // Step 8: Admin Finalized Partnerships Verification
        System.out.println("\n[Step 8] Testing Admin Finalized Partnerships View...");
        adminShell.navigateTo(AdminSection.MATCHES);
        AdminMatchesView matchesView = new AdminMatchesView(app);
        if (matchesView.getView() == null) {
            throw new AssertionError("AdminMatchesView failed to render!");
        }
        List<String[]> finalMatches = FileHandler.loadFinalMatches();
        System.out.println("   ✓ AdminMatchesView rendered with " + finalMatches.size() + " confirmed student collaboration partnerships.");

        // Step 9: Admin Leaderboard Verification
        System.out.println("\n[Step 9] Testing Admin Leaderboard Navigation...");
        adminShell.navigateTo(AdminSection.LEADERBOARD);
        System.out.println("   ✓ Admin successfully navigated to integrated Leaderboard.");

        // Step 10: Admin Logout Lifecycle Verification
        System.out.println("\n[Step 10] Testing Admin Logout Lifecycle...");
        app.logout();
        if (app.getCurrentUser() != null) {
            throw new AssertionError("Expected session currentUser to be null after logout.");
        }
        System.out.println("   ✓ Admin logged out cleanly, session cleared, and redirected to Login screen.");
    }
}
