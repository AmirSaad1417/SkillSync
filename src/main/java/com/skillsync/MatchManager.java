package com.skillsync;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
public class MatchManager {
    private static final int MIN_SHARED_INTERESTS = 1;

    public static void findBestMatches(User currentUser, List<User> allUsers) throws IOException {
        if (currentUser.isMatched()) {
            System.out.println("\n⚠️ You are already matched with " + currentUser.getMatchedWith() + ".");
            return;
        }

        System.out.println("\n🔍 Finding Your Best Matches...");

        List<String> myInterests = currentUser.getQualifiedInterests();
        if (myInterests.isEmpty()) {
            System.out.println("❌ You have no qualified interests yet. Take some assessments first!");
            return;
        }

        List<User> compatibleUsers = getCompatibleUsers(currentUser, allUsers);

        if (compatibleUsers.isEmpty()) {
            System.out.println("❌ No compatible users found.");
            return;
        }

        displayMatchResults(currentUser, compatibleUsers);
        processMatchSelection(currentUser, compatibleUsers);
    }

    public static List<User> getCompatibleUsers(User currentUser, List<User> allUsers) {
        if (currentUser.isMatched()) {
            return Collections.emptyList();
        }

        List<String> myInterests = currentUser.getQualifiedInterests();
        if (myInterests.isEmpty()) {
            return Collections.emptyList();
        }

        return allUsers.stream()
                .filter(user -> !user.equals(currentUser))
                .filter(user -> !user.isMatched())
                .filter(user -> !FileHandler.isUserAlreadyMatched(currentUser.getUsername(), user.getUsername()))
                .map(user -> new AbstractMap.SimpleEntry<>(user, countSharedInterests(myInterests, user)))
                .filter(entry -> entry.getValue() >= MIN_SHARED_INTERESTS)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    public static void viewLeaderboard(List<User> users) {
        System.out.println("\n🏆 Leaderboard:");
        users.stream()
                .sorted((a, b) -> b.getQualifiedInterests().size() - a.getQualifiedInterests().size())
                .limit(10)
                .forEach(u -> System.out.printf("- %s (%d interests)%n", u.getUsername(), u.getQualifiedInterests().size()));
    }

    // Wrapper for match search
    public static void findMatches(User currentUser, List<User> allUsers) throws IOException {
        findBestMatches(currentUser, allUsers);
    }

    // View pending match requests
    public static void viewMatchRequests(User currentUser, List<User> allUsers) throws IOException {
        List<MatchRequest> allRequests = FileHandler.loadMatchRequests();
        List<MatchRequest> pending = allRequests.stream()
                .filter(r -> r.getReceiver().equalsIgnoreCase(currentUser.getUsername()))
                .filter(r -> r.getStatus() == MatchStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            System.out.println("📭 No match requests.");
        } else {
            System.out.println("✉️ Match Requests:");
            for (int i = 0; i < pending.size(); i++) {
                MatchRequest r = pending.get(i);
                System.out.printf("%d. From: %s for interest: %s%n", i + 1, r.getSender(), r.getInterest());
            }
        }
    }

    // Interactive review and respond (Accept / Decline) for match requests
    public static void manageMatchRequests(User currentUser, List<User> allUsers) throws IOException {
        List<MatchRequest> allRequests = FileHandler.loadMatchRequests();
        List<MatchRequest> pending = allRequests.stream()
                .filter(r -> r.getReceiver().equalsIgnoreCase(currentUser.getUsername()))
                .filter(r -> r.getStatus() == MatchStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            System.out.println("\n📭 You have no pending match requests.");
            return;
        }

        System.out.println("\n✉️ Pending Match Requests:");
        for (int i = 0; i < pending.size(); i++) {
            MatchRequest r = pending.get(i);
            System.out.printf("%d. From: %s | Interest: %s%n", i + 1, r.getSender(), r.getInterest());
        }

        System.out.print("\nSelect a request to respond to (0 to go back): ");
        int requestChoice = Main.getIntInput(0, pending.size());
        if (requestChoice == 0) return;

        MatchRequest selected = pending.get(requestChoice - 1);

        System.out.printf("%nResponding to request from '%s' for '%s':%n", selected.getSender(), selected.getInterest());
        System.out.println("1. ✅ Accept");
        System.out.println("2. ❌ Decline");
        System.out.println("3. 🔙 Back");
        System.out.print("Choose an option: ");
        int action = Main.getIntInput(1, 3);

        if (action == 1) {
            boolean accepted = acceptMatchRequest(selected, currentUser, allUsers);
            if (!accepted) {
                System.out.println("⚠️ Unable to accept request. Either you or the sender is already matched.");
            } else {
                System.out.printf("🤝 Match confirmed! You and %s are now matched for %s.%n",
                        selected.getSender(), selected.getInterest());
            }
        } else if (action == 2) {
            declineMatchRequest(selected);
            System.out.printf("❌ Declined match request from %s.%n", selected.getSender());
        }
    }

    public static boolean acceptMatchRequest(MatchRequest selected, User currentUser, List<User> allUsers) {
        if (currentUser.isMatched()) {
            return false;
        }

        User senderUser = allUsers.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(selected.getSender()))
                .findFirst().orElse(null);

        if (senderUser != null && senderUser.isMatched()) {
            return false;
        }

        List<MatchRequest> allRequests = FileHandler.loadMatchRequests();

        // Update request status in request log
        updateRequestStatus(allRequests, selected, MatchStatus.ACCEPTED);
        FileHandler.saveAllMatchRequests(allRequests);

        // Update match state for both users and persist
        currentUser.setMatched(true, selected.getSender());
        for (User u : allUsers) {
            if (u.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
                u.setMatched(true, selected.getSender());
            }
            if (u.getUsername().equalsIgnoreCase(selected.getSender())) {
                u.setMatched(true, currentUser.getUsername());
            }
        }
        FileHandler.saveUsers(allUsers);

        // Save final match record
        FileHandler.saveFinalMatch(selected.getSender(), currentUser.getUsername(), selected.getInterest());
        return true;
    }

    public static boolean declineMatchRequest(MatchRequest selected) {
        List<MatchRequest> allRequests = FileHandler.loadMatchRequests();
        updateRequestStatus(allRequests, selected, MatchStatus.DECLINED);
        return FileHandler.saveAllMatchRequests(allRequests);
    }

    public static void updateRequestStatus(List<MatchRequest> allRequests, MatchRequest target, MatchStatus newStatus) {
        for (int i = 0; i < allRequests.size(); i++) {
            MatchRequest r = allRequests.get(i);
            if (r.getSender().equalsIgnoreCase(target.getSender()) &&
                r.getReceiver().equalsIgnoreCase(target.getReceiver()) &&
                r.getInterest().equalsIgnoreCase(target.getInterest()) &&
                r.getStatus() == MatchStatus.PENDING) {
                allRequests.set(i, new MatchRequest(r.getSender(), r.getReceiver(), r.getInterest(), newStatus));
                break;
            }
        }
    }

    private static int countSharedInterests(List<String> myInterests, User otherUser) {
        return (int) myInterests.stream().filter(otherUser::isQualified).count();
    }

    private static void displayMatchResults(User currentUser, List<User> matches) {
        System.out.println("\n🧑‍🤝‍🧑 Top Match Suggestions:");
        for (int i = 0; i < matches.size(); i++) {
            User match = matches.get(i);
            List<String> shared = getSharedInterests(currentUser, match);
            System.out.printf("%d. %s (%d shared): %s%n", i + 1, match.getUsername(), shared.size(), String.join(", ", shared));
        }
    }

    public static List<String> getSharedInterests(User a, User b) {
        return a.getQualifiedInterests().stream().filter(b::isQualified).collect(Collectors.toList());
    }

    private static void processMatchSelection(User currentUser, List<User> matches) throws IOException {
        System.out.print("\nEnter number to send request (0 to cancel): ");
        int choice = Main.getIntInput(0, matches.size());

        if (choice > 0) {
            User selected = matches.get(choice - 1);
            List<String> sharedInterests = getSharedInterests(currentUser, selected);
            if (sharedInterests.isEmpty()) {
                System.out.println("❌ No shared qualified interests found.");
                return;
            }
            boolean sent = sendMatchRequest(currentUser, selected, sharedInterests.get(0));
            if (sent) {
                System.out.printf("📨 Match request sent to %s for interest: %s%n", selected.getUsername(), sharedInterests.get(0));
            } else {
                System.out.println("⚠️ Unable to send request (already matched or request already exists).");
            }
        }
    }

    public static boolean sendMatchRequest(User from, User to, String interest) {
        if (from.isMatched()) {
            return false;
        }
        if (to.isMatched()) {
            return false;
        }
        if (FileHandler.hasPendingRequest(from.getUsername(), to.getUsername())) {
            return false;
        }

        MatchRequest request = new MatchRequest(
                from.getUsername(),
                to.getUsername(),
                interest,
                MatchStatus.PENDING
        );

        return FileHandler.saveMatchRequest(request);
    }

    // === Inner Classes and Enums ===

    public enum MatchStatus {
        PENDING, ACCEPTED, DECLINED
    }

    public static class MatchRequest {
        private final String sender;
        private final String receiver;
        private final String interest;
        private final MatchStatus status;

        public MatchRequest(String sender, String receiver, String interest, MatchStatus status) {
            this.sender = sender;
            this.receiver = receiver;
            this.interest = interest;
            this.status = status;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public String getInterest() {
            return interest;
        }

        public MatchStatus getStatus() {
            return status;
        }

        public String toCsvString() {
            return String.join(",", sender, receiver, interest, status.name());
        }
    }

    public static class Match {
        private final String user1;
        private final String user2;
        private final String sharedInterests;

        public Match(String user1, String user2, String sharedInterests) {
            this.user1 = user1;
            this.user2 = user2;
            this.sharedInterests = sharedInterests;
        }

        public String getUser1() {
            return user1;
        }

        public String getUser2() {
            return user2;
        }

        public String getSharedInterests() {
            return sharedInterests;
        }

        public String toCsvString() {
            return String.join(",", user1, user2, sharedInterests);
        }
    }
}
