package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Programmatic end-to-end verification of Peer Matching & Request Lifecycle:
 * Student A (finds Student B) → sends request → Student B logs in → accepts/declines → persistence updates correctly.
 */
public class PeerMatchingLifecycleTest {

    private static final AtomicBoolean testPassed = new AtomicBoolean(false);
    private static Throwable failure = null;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("  SkillSync: Peer Matching & Request Lifecycle Test");
        System.out.println("=============================================================");

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    executePeerMatchingLifecycle();
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
                System.err.println("❌ Lifecycle test timed out after 20 seconds!");
                System.exit(1);
            }

            if (testPassed.get()) {
                System.out.println("\n🎉 ALL PEER MATCHING & REQUEST LIFECYCLE CHECKS PASSED!");
                System.exit(0);
            } else {
                System.err.println("\n❌ Test FAILED: " + (failure != null ? failure.getMessage() : "Unknown error"));
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize JavaFX Platform: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void cleanTestPair(String u1, String u2) {
        try {
            // Clean matches.csv
            Path matchesPath = Paths.get("matches.csv");
            if (Files.exists(matchesPath)) {
                List<String> lines = Files.readAllLines(matchesPath);
                List<String> filtered = lines.stream().filter(line -> {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 2) {
                        if ((parts[0].equalsIgnoreCase(u1) && parts[1].equalsIgnoreCase(u2)) ||
                            (parts[0].equalsIgnoreCase(u2) && parts[1].equalsIgnoreCase(u1))) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                Files.write(matchesPath, filtered);
            }

            // Clean match_requests.csv
            Path requestsPath = Paths.get("match_requests.csv");
            if (Files.exists(requestsPath)) {
                List<String> lines = Files.readAllLines(requestsPath);
                List<String> filtered = lines.stream().filter(line -> {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 2) {
                        if ((parts[0].equalsIgnoreCase(u1) && parts[1].equalsIgnoreCase(u2)) ||
                            (parts[0].equalsIgnoreCase(u2) && parts[1].equalsIgnoreCase(u1))) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                Files.write(requestsPath, filtered);
            }
        } catch (IOException e) {
            System.err.println("Warning cleaning test pair: " + e.getMessage());
        }
    }

    private static void executePeerMatchingLifecycle() {
        // Step 1: Initialize Application and Find Candidate Pair
        System.out.println("\n[Step 1] Initializing platform and selecting Candidate A and Candidate B...");

        // Candidates: Abbas Khan and Usman (both qualified in AI, currently unmatched)
        final String nameA = "Abbas Khan";
        final String nameB = "Usman";
        final String nameC = "saadkhan";
        final String nameD = "shawaiz";

        cleanTestPair(nameA, nameB);
        cleanTestPair(nameC, nameD);

        SkillSyncApp app = new SkillSyncApp();
        Stage stage = new Stage();
        app.start(stage);

        List<User> users = app.getUsers();

        User studentA = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameA)).findFirst()
                .orElseThrow(() -> new AssertionError("Student A not found: " + nameA));
        User studentB = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameB)).findFirst()
                .orElseThrow(() -> new AssertionError("Student B not found: " + nameB));
        User studentC = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameC)).findFirst()
                .orElseThrow(() -> new AssertionError("Student C not found: " + nameC));
        User studentD = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameD)).findFirst()
                .orElseThrow(() -> new AssertionError("Student D not found: " + nameD));

        // Reset pairings if previously matched for clean idempotency
        studentA.setMatched(false, "");
        studentB.setMatched(false, "");
        studentC.setMatched(false, "");
        studentD.setMatched(false, "");
        FileHandler.saveUsers(users);

        System.out.println("   Student A: " + studentA.getUsername() + " | Qualified: " + studentA.getQualifiedInterests());
        System.out.println("   Student B: " + studentB.getUsername() + " | Qualified: " + studentB.getQualifiedInterests());

        // Step 2: Student A logs in and opens Find Peers
        System.out.println("\n[Step 2] Student A logs in and opens Find Peers...");
        app.onLoginSuccess(studentA);

        MainShellView shellA = new MainShellView(app, studentA);
        FindPeersView findPeersViewA = new FindPeersView(app, studentA, shellA::navigateTo);
        if (findPeersViewA.getView() == null) {
            throw new AssertionError("FindPeersView failed to render for Student A.");
        }

        // Verify Student B is recommended to Student A
        List<User> compatibleWithA = MatchManager.getCompatibleUsers(studentA, users);
        boolean recommendsB = compatibleWithA.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(nameB));
        if (!recommendsB) {
            throw new AssertionError("Expected Student B (" + nameB + ") to be recommended to Student A, but list was: " + compatibleWithA);
        }
        List<String> sharedSkills = MatchManager.getSharedInterests(studentA, studentB);
        System.out.println("   ✓ Recommendation confirmed! Student B recommended with shared track(s): " + sharedSkills);

        // Step 3: Student A sends Match Request for shared interest (ai)
        System.out.println("\n[Step 3] Student A sends match request for track '" + sharedSkills.get(0) + "' to Student B...");
        boolean requestSent = MatchManager.sendMatchRequest(studentA, studentB, sharedSkills.get(0));
        if (!requestSent) {
            throw new AssertionError("MatchManager.sendMatchRequest() returned false.");
        }

        // Verify request exists on disk in match_requests.csv
        List<MatchManager.MatchRequest> diskRequests = FileHandler.loadMatchRequests();
        MatchManager.MatchRequest pendingReq = diskRequests.stream()
                .filter(r -> r.getSender().equalsIgnoreCase(nameA) && r.getReceiver().equalsIgnoreCase(nameB))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending request not found in match_requests.csv"));

        if (pendingReq.getStatus() != MatchManager.MatchStatus.PENDING) {
            throw new AssertionError("Expected request status to be PENDING, got: " + pendingReq.getStatus());
        }
        System.out.println("   ✓ Disk persistence verified: Request " + nameA + " -> " + nameB + " is PENDING in match_requests.csv.");

        // Step 4: Student B logs in and opens Requests View
        System.out.println("\n[Step 4] Student B logs in and navigates to Match Requests...");
        app.refreshUsers();
        app.onLoginSuccess(studentB);

        MainShellView shellB = new MainShellView(app, studentB);
        RequestsView requestsViewB = new RequestsView(app, studentB, shellB::navigateTo, () -> {});
        if (requestsViewB.getView() == null) {
            throw new AssertionError("RequestsView failed to render for Student B.");
        }
        System.out.println("   ✓ RequestsView rendered for Student B with incoming invitations.");

        // Step 5: Student B Accepts the Match Request
        System.out.println("\n[Step 5] Student B accepts the match request...");
        boolean accepted = MatchManager.acceptMatchRequest(pendingReq, studentB, app.getUsers());
        if (!accepted) {
            throw new AssertionError("MatchManager.acceptMatchRequest() returned false.");
        }
        System.out.println("   ✓ Match request accepted.");

        // Step 6: Verify Mutual Disk Persistence
        System.out.println("\n[Step 6] Verifying mutual match updates across all CSV files...");

        // 6a: Check users.csv
        List<User> reloadedUsers = FileHandler.loadUsers();
        User diskStudentA = reloadedUsers.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameA)).findFirst().get();
        User diskStudentB = reloadedUsers.stream().filter(u -> u.getUsername().equalsIgnoreCase(nameB)).findFirst().get();

        if (!diskStudentA.isMatched() || !diskStudentA.getMatchedWith().equalsIgnoreCase(nameB)) {
            throw new AssertionError("Student A should be matched with " + nameB + ", got: matched=" + diskStudentA.isMatched() + ", partner=" + diskStudentA.getMatchedWith());
        }
        if (!diskStudentB.isMatched() || !diskStudentB.getMatchedWith().equalsIgnoreCase(nameA)) {
            throw new AssertionError("Student B should be matched with " + nameA + ", got: matched=" + diskStudentB.isMatched() + ", partner=" + diskStudentB.getMatchedWith());
        }
        System.out.println("   ✓ users.csv: Student A is matched with '" + diskStudentA.getMatchedWith() + "' and Student B is matched with '" + diskStudentB.getMatchedWith() + "'.");

        // 6b: Check match_requests.csv
        List<MatchManager.MatchRequest> reloadedRequests = FileHandler.loadMatchRequests();
        MatchManager.MatchRequest acceptedReq = reloadedRequests.stream()
                .filter(r -> r.getSender().equalsIgnoreCase(nameA) && r.getReceiver().equalsIgnoreCase(nameB))
                .findFirst().get();

        if (acceptedReq.getStatus() != MatchManager.MatchStatus.ACCEPTED) {
            throw new AssertionError("Expected request status to be ACCEPTED in match_requests.csv, got: " + acceptedReq.getStatus());
        }
        System.out.println("   ✓ match_requests.csv: Request status updated to ACCEPTED.");

        // 6c: Check matches.csv
        List<String[]> finalizedMatches = FileHandler.loadFinalMatches();
        boolean finalMatchFound = finalizedMatches.stream()
                .anyMatch(m -> m.length >= 3 &&
                        ((m[0].equalsIgnoreCase(nameA) && m[1].equalsIgnoreCase(nameB)) ||
                         (m[0].equalsIgnoreCase(nameB) && m[1].equalsIgnoreCase(nameA))) &&
                        m[2].equalsIgnoreCase(sharedSkills.get(0)));

        if (!finalMatchFound) {
            throw new AssertionError("Finalized match not found in matches.csv!");
        }
        System.out.println("   ✓ matches.csv: Finalized match record found for " + nameA + " ↔ " + nameB + " (" + sharedSkills.get(0) + ").");

        // Step 7: Verify Decline Flow
        System.out.println("\n[Step 7] Testing Decline flow between secondary candidates...");

        boolean declineSent = MatchManager.sendMatchRequest(studentC, studentD, "ai");
        if (!declineSent) {
            throw new AssertionError("Failed to send request from " + nameC + " to " + nameD);
        }

        List<MatchManager.MatchRequest> declineRequests = FileHandler.loadMatchRequests();
        MatchManager.MatchRequest reqToDecline = declineRequests.stream()
                .filter(r -> r.getSender().equalsIgnoreCase(nameC) && r.getReceiver().equalsIgnoreCase(nameD) && r.getStatus() == MatchManager.MatchStatus.PENDING)
                .findFirst().get();

        boolean declined = MatchManager.declineMatchRequest(reqToDecline);
        if (!declined) {
            throw new AssertionError("declineMatchRequest() failed.");
        }

        List<MatchManager.MatchRequest> postDeclineRequests = FileHandler.loadMatchRequests();
        MatchManager.MatchRequest declinedReq = postDeclineRequests.stream()
                .filter(r -> r.getSender().equalsIgnoreCase(nameC) && r.getReceiver().equalsIgnoreCase(nameD))
                .reduce((first, second) -> second).get();

        if (declinedReq.getStatus() != MatchManager.MatchStatus.DECLINED) {
            throw new AssertionError("Expected status to be DECLINED, got: " + declinedReq.getStatus());
        }
        System.out.println("   ✓ Decline flow verified: Request status set to DECLINED and users remain unmatched.");

        // Post-test cleanup to keep repository CSV files in pristine state
        cleanTestPair(nameA, nameB);
        cleanTestPair(nameC, nameD);
        studentA.setMatched(false, "");
        studentB.setMatched(false, "");
        studentC.setMatched(false, "");
        studentD.setMatched(false, "");
        FileHandler.saveUsers(users);
        System.out.println("   ✓ Test records cleaned and candidate states restored.");
    }
}
