package com.skillsync;
import java.io.IOException;
import java.util.List;
public class AdminPanel {

    public static void start(List<User> users) throws IOException {
        while (true) {
            System.out.println("\n=== 🛠️ Admin Panel ===");
            System.out.println("1. 👤 View All Users");
            System.out.println("2. 📊 View Test Participation Stats");
            System.out.println("3. ✉️ View Match Requests");
            System.out.println("4. 🤝 View Finalized Matches");
            System.out.println("5. 🏆 View Leaderboard");
            System.out.println("6. 🔒 Logout");
            System.out.print("Choose an option: ");

            int choice = Main.getIntInput(1, 6);

            switch (choice) {
                case 1 -> viewAllUsers(users);
                case 2 -> viewTestStats(users);
                case 3 -> FileHandler.viewAllMatchRequests();
                case 4 -> FileHandler.viewAllFinalizedMatches();
                case 5 -> MatchManager.viewLeaderboard(users);
                case 6 -> {
                    System.out.println("🔓 Logging out from Admin Panel...");
                    return;
                }
            }
        }
    }

    private static void viewAllUsers(List<User> users) {
        System.out.println("\n📋 Registered Users:");
        if (users.isEmpty()) {
            System.out.println("⚠️ No users found.");
        } else {
            for (User user : users) {
                System.out.println("- " + user.getUsername() + " | " + user.getEmail());
            }
        }
    }

    private static void viewTestStats(List<User> users) {
        System.out.println("\n📊 Test Participation Stats:");
        for (User user : users) {
            List<String> attempted = user.getAttemptedInterests();
            List<String> qualified = user.getQualifiedInterests();
            System.out.printf(
                    "%s: %d interests attempted %s, %d qualified %s\n",
                    user.getUsername(),
                    attempted.size(),
                    attempted.isEmpty() ? "" : attempted.toString(),
                    qualified.size(),
                    qualified.isEmpty() ? "" : qualified.toString()
            );
        }
    }
}
