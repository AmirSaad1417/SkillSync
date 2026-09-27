package com.skillsync;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static List<User> users;
    private static User currentUser;

    public static void main(String[] args) {
        try {
            users = FileHandler.loadUsers();
            System.out.println("=== SkillSync Career Matching Portal ===");
            mainMenu();
        } catch (IOException e) {
            System.err.println("⚠️ Error initializing system: " + e.getMessage());
        }
    }

    private static void mainMenu() throws IOException {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. 📝 Sign Up");
            System.out.println("2. 🔑 Login");
            System.out.println("3. 👮 Admin Login");
            System.out.println("4. 🚪 Exit");
            System.out.print("Choose an option: ");

            switch (getIntInput(1, 4)) {
                case 1 -> signUp();
                case 2 -> loginUser();
                case 3 -> loginAdmin();
                case 4 -> {
                    FileHandler.saveUsers(users);
                    System.out.println("👋 Exiting... Goodbye!");
                    System.exit(0);
                }
            }
        }
    }

    private static void signUp() throws IOException {
        System.out.println("\n=== 📝 Registration ===");

        String username = getValidInput("Choose a username: ",
                input -> !input.isBlank() && findUserByUsername(input) == null,
                "❌ Invalid or already taken username.");

        String email = getValidInput("Enter your email: ",
                input -> input.contains("@") && findUserByEmail(input) == null,
                "❌ Invalid or already used email.");

        String password = getValidInput("Choose a password (min 6 chars): ",
                input -> input.length() >= 6,
                "❌ Password must be at least 6 characters.");

        User user = new User(username, email, password);
        users.add(user);
        FileHandler.saveUsers(users);

        System.out.println("✅ Registration successful. You can now log in!");
    }

    private static void loginUser() throws IOException {
        System.out.println("\n=== 🔐 Login ===");

        String email = getValidInput("Enter your email: ",
                input -> input.contains("@"),
                "❌ Invalid email format.");

        String password = getValidInput("Enter your password: ",
                input -> !input.isBlank(),
                "❌ Password cannot be empty.");

        currentUser = findUserByEmail(email);

        if (currentUser != null && currentUser.getPassword().equals(password)) {
            System.out.println("✅ Login successful. Welcome, " + currentUser.getUsername() + "!");
            dashboard();
        } else {
            System.out.println("❌ Incorrect email or password.");
        }
    }

    private static void loginAdmin() throws IOException {
        System.out.println("\n=== 👮 Admin Login ===");

        String username = getValidInput("Admin Username: ",
                input -> !input.isBlank(),
                "❌ Username cannot be empty.");

        String password = getValidInput("Admin Password: ",
                input -> !input.isBlank(),
                "❌ Password cannot be empty.");

        if (username.equals("admin") && password.equals("admin123")) {
            System.out.println("✅ Admin login successful.");
            AdminPanel.start(users);
        } else {
            System.out.println("❌ Invalid admin credentials.");
        }
    }

    private static void dashboard() throws IOException {
        if (currentUser.getAttemptedInterests().isEmpty()) {
            System.out.println("\n⚠️ You haven’t taken any tests yet.");
            TestEngine.startTestSession(currentUser);
            FileHandler.saveUsers(users);
        }

        while (true) {
            System.out.println("\n=== 📊 Dashboard ===");
            System.out.println("1. 🧪 Take Assessment Test");
            System.out.println("2. 📁 View Profile");
            System.out.println("3. 🏆 Leaderboard");
            System.out.println("4. 👥 Recommend Teammates");
            System.out.println("5. ✉️ Match Requests");
            System.out.println("6. 🔒 Logout");

            switch (getIntInput(1, 6)) {
                case 1 -> {
                    TestEngine.startTestSession(currentUser);
                    FileHandler.saveUsers(users);
                }
                case 2 -> showUserProfile();
                case 3 -> MatchManager.viewLeaderboard(users);
                case 4 -> MatchManager.findMatches(currentUser, users);
                case 5 -> handleMatchRequests();
                case 6 -> {
                    FileHandler.saveUsers(users);
                    currentUser = null;
                    System.out.println("🚪 Logged out.");
                    return;
                }
            }
        }
    }

    private static void showUserProfile() {
        System.out.println("\n=== 👤 Your Profile ===");
        System.out.println(currentUser.getProfileString());
    }

    private static void handleMatchRequests() throws IOException {
        MatchManager.manageMatchRequests(currentUser, users);
    }

    // Utility Methods
    private static User findUserByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    private static User findUserByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public static Scanner getScanner() {
        return scanner;
    }

    public  static int getIntInput(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) return input;
                System.out.printf("❌ Enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid number. Try again: ");
            }
        }
    }

    private static String getValidInput(String prompt, Validator validator, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (validator.validate(input)) return input;
            System.out.println(errorMsg);
        }
    }

    interface Validator {
        boolean validate(String input);
    }
}