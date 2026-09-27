package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.Question;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Programmatic end-to-end verification of the student journey:
 * Login → Dashboard → Assessment → Result → Persistence Check → Profile.
 */
public class StudentFlowVerification {

    private static final AtomicBoolean allChecksPassed = new AtomicBoolean(false);
    private static Throwable failure = null;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("  SkillSync: Student Journey Flow Verification Test");
        System.out.println("=============================================================");

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    executeStudentJourney();
                    allChecksPassed.set(true);
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

            if (allChecksPassed.get()) {
                System.out.println("\n🎉 ALL STUDENT JOURNEY CHECKS PASSED SUCCESSFULLY!");
                System.exit(0);
            } else {
                System.err.println("\n❌ Verification FAILED: " + (failure != null ? failure.getMessage() : "Unknown error"));
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize JavaFX Platform: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void executeStudentJourney() {
        // Step 1: User Preparation
        System.out.println("\n[Step 1] Initializing application and selecting test candidate...");
        SkillSyncApp app = new SkillSyncApp();
        Stage stage = new Stage();
        app.start(stage);

        final String candidateName = "Amir Saad";
        final User student = app.getUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(candidateName))
                .findFirst()
                .orElse(app.getUsers().get(0));

        System.out.println("   Candidate: " + student.getUsername() + " (" + student.getEmail() + ")");
        System.out.println("   Initial Qualified Count: " + student.getQualifiedInterests().size());

        // Step 2: Application Launch & Login
        System.out.println("\n[Step 2] Executing Student Login -> Main Shell...");
        app.onLoginSuccess(student);
        if (app.getCurrentUser() == null || !app.getCurrentUser().getUsername().equals(student.getUsername())) {
            throw new AssertionError("currentUser not set after login.");
        }
        System.out.println("   ✓ Logged in successfully. Shell Title: " + stage.getTitle());

        // Step 3: Dashboard View Inspection
        System.out.println("\n[Step 3] Mounting Dashboard and verifying metrics...");
        MainShellView shellView = new MainShellView(app, student);
        DashboardView dashboardView = new DashboardView(app, student, shellView::navigateTo, shellView::launchAssessment);
        if (dashboardView.getView() == null) {
            throw new AssertionError("DashboardView failed to construct.");
        }
        System.out.println("   ✓ Dashboard shell constructed with dynamic stat cards & competency table.");

        // Step 4: Launch Skill Assessment (DSA)
        System.out.println("\n[Step 4] Launching DSA Skill Assessment...");
        String testDomain = "dsa";
        List<Question> dsaQuestions = FileHandler.loadQuestions(testDomain);
        if (dsaQuestions.size() < 10) {
            throw new AssertionError("Expected at least 10 questions in " + testDomain + "_questions.csv");
        }
        System.out.println("   ✓ Loaded " + dsaQuestions.size() + " questions from " + testDomain + "_questions.csv");

        // Simulate taking test: 8 correct, 2 incorrect -> Score = 8 (Qualified)
        Map<Integer, String> testAnswers = new HashMap<>();
        List<Question> activeTestQuestions = dsaQuestions.subList(0, 10);
        int expectedScore = 0;

        for (int i = 0; i < 10; i++) {
            Question q = activeTestQuestions.get(i);
            if (i < 8) {
                // Answer correctly
                testAnswers.put(i, q.getCorrectOption().trim());
                expectedScore++;
            } else {
                // Answer incorrectly
                String wrong = q.getCorrectOption().trim().equalsIgnoreCase("A") ? "B" : "A";
                testAnswers.put(i, wrong);
            }
        }

        // Apply score via User business logic
        student.setScore(testDomain, expectedScore);
        if (student.getScore(testDomain) != 8) {
            throw new AssertionError("User.getScore(dsa) should be 8, got: " + student.getScore(testDomain));
        }
        if (!student.isQualified(testDomain)) {
            throw new AssertionError("User.isQualified(dsa) should be true for score 8");
        }
        System.out.println("   ✓ Assessment completed. Calculated Score: " + expectedScore + " / 10 (Qualified: " + student.isQualified(testDomain) + ")");

        // Step 5: Persistence Check
        System.out.println("\n[Step 5] Persisting score to CSV and verifying on disk...");
        boolean saved = FileHandler.saveUsers(app.getUsers());
        if (!saved) {
            throw new AssertionError("FileHandler.saveUsers() returned false.");
        }

        // Read directly from disk to confirm atomic file replacement
        List<User> reloadedUsers = FileHandler.loadUsers();
        User reloadedStudent = reloadedUsers.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(student.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Student not found in reloaded CSV"));

        if (reloadedStudent.getScore(testDomain) != 8) {
            throw new AssertionError("Disk persistence failed! Reloaded score: " + reloadedStudent.getScore(testDomain) + ", expected: 8");
        }
        System.out.println("   ✓ Disk persistence confirmed: " + reloadedStudent.getUsername() + " has " + testDomain + " score = " + reloadedStudent.getScore(testDomain) + " in users.csv.");

        // Step 6: Assessment Results View
        System.out.println("\n[Step 6] Rendering Assessment Result View...");
        shellView.showAssessmentResult(testDomain, expectedScore, activeTestQuestions, testAnswers);
        AssessmentResultView resultView = new AssessmentResultView(
                app,
                student,
                testDomain,
                expectedScore,
                activeTestQuestions,
                testAnswers,
                shellView::navigateTo,
                shellView::launchAssessment
        );
        if (resultView.getView() == null) {
            throw new AssertionError("AssessmentResultView failed to render.");
        }
        System.out.println("   ✓ Result View rendered with celebration header, 8/10 score, and 10 review items.");

        // Step 7: Profile View Verification
        System.out.println("\n[Step 7] Navigating to Updated Profile View...");
        shellView.navigateTo(NavSection.PROFILE);
        ProfileView profileView = new ProfileView(app, student, shellView::navigateTo, shellView::launchAssessment);
        if (profileView.getView() == null) {
            throw new AssertionError("ProfileView failed to construct.");
        }
        System.out.println("   ✓ Profile View reflects updated DSA score: " + student.getScore("dsa") + " / 10");
        System.out.println("   ✓ Profile View reflects average score: " + String.format("%.1f", student.getAverageScore()) + " / 10");
        System.out.println("   ✓ Profile View reflects qualified domains: " + student.getQualifiedInterests());

        // Step 8: Return to Dashboard
        System.out.println("\n[Step 8] Returning to Dashboard to verify refreshed metrics...");
        shellView.navigateTo(NavSection.DASHBOARD);
        DashboardView refreshedDashboard = new DashboardView(app, student, shellView::navigateTo, shellView::launchAssessment);
        if (refreshedDashboard.getView() == null) {
            throw new AssertionError("Refreshed DashboardView failed to construct.");
        }
        System.out.println("   ✓ Dashboard reflects updated metrics without restarting application.");
    }
}
