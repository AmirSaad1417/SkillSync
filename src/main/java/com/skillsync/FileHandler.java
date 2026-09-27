package com.skillsync;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import com.skillsync.MatchManager.MatchRequest;
public class FileHandler {
    private static final Path USERS_PATH = Paths.get("users.csv");
    private static final Path REQUESTS_PATH = Paths.get("match_requests.csv");
    private static final Path MATCHES_PATH = Paths.get("matches.csv");
    private static final Path QUESTIONS_DIR = Paths.get("src/main/resources/questions");

    static {
        try {
            Files.createDirectories(QUESTIONS_DIR);
            if (!Files.exists(USERS_PATH))
                Files.writeString(USERS_PATH, "username,email,password,dsa,ai,robotics,design,matched,matchedWith\n");

            if (!Files.exists(REQUESTS_PATH))
                Files.writeString(REQUESTS_PATH, "sender,receiver,interest,status\n");

            if (!Files.exists(MATCHES_PATH))
                Files.writeString(MATCHES_PATH, "user1,user2,sharedInterests\n");

        } catch (IOException e) {
            System.err.println("❌ File initialization error: " + e.getMessage());
        }
    }

    // ✅ Load users from CSV
    public static List<User> loadUsers() {
        try {
            return Files.lines(USERS_PATH)
                    .skip(1)
                    .map(FileHandler::parseUser)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("❌ Failed to load users: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static User parseUser(String line) {
        try {
            String[] parts = line.split(",", -1);
            return new User(
                    parts[0], parts[1], parts[2],
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Integer.parseInt(parts[6]),
                    Boolean.parseBoolean(parts[7]),
                    parts.length > 8 ? parts[8] : ""
            );
        } catch (Exception e) {
            System.err.println("⚠️ Malformed user line: " + line);
            return null;
        }
    }

    // ✅ Save users to CSV
    public static boolean saveUsers(Collection<User> users) {
        Path temp = USERS_PATH.resolveSibling("users.tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
            writer.write("username,email,password,dsa,ai,robotics,design,matched,matchedWith\n");
            for (User user : users) {
                writer.write(user.toCsvString());
                writer.newLine();
            }
            Files.move(temp, USERS_PATH, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Failed to save users: " + e.getMessage());
            return false;
        }
    }

    // ✅ Load MCQs (cached by interest)
    private static final Map<String, List<Question>> QUESTION_CACHE = new HashMap<>();

    public static List<Question> loadQuestions(String interest) {
        return QUESTION_CACHE.computeIfAbsent(interest, key -> {
            Path qPath = QUESTIONS_DIR.resolve(interest + "_questions.csv");
            try {
                return Files.lines(qPath)
                        .map(FileHandler::parseQuestion)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("❌ Error reading questions for " + interest);
                return Collections.emptyList();
            }
        });
    }

    private static Question parseQuestion(String line) {
        String[] parts = line.split(",");  // ✅ Use comma as delimiter
        if (parts.length != 6) return null;
        return new Question(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }


    // ✅ Match Request Functions
    public static List<MatchRequest> loadMatchRequests() {
        try {
            return Files.lines(REQUESTS_PATH)
                    .skip(1)
                    .map(FileHandler::parseRequest)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("❌ Failed to load match requests");
            return Collections.emptyList();
        }
    }

    private static MatchRequest parseRequest(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 4) return null;
        try {
            return new MatchRequest(
                    parts[0],
                    parts[1],
                    parts[2],
                    MatchManager.MatchStatus.valueOf(parts[3].trim())
            );
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean saveMatchRequest(MatchRequest req) {
        try (BufferedWriter writer = Files.newBufferedWriter(REQUESTS_PATH, StandardOpenOption.APPEND)) {
            writer.write(req.toCsvString());
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("❌ Failed to write request: " + e.getMessage());
            return false;
        }
    }

    // ✅ Save All Match Requests (overwrites with updated statuses)
    public static boolean saveAllMatchRequests(Collection<MatchRequest> requests) {
        Path temp = REQUESTS_PATH.resolveSibling("match_requests.tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
            writer.write("sender,receiver,interest,status\n");
            for (MatchRequest req : requests) {
                writer.write(req.toCsvString());
                writer.newLine();
            }
            Files.move(temp, REQUESTS_PATH, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Failed to save match requests: " + e.getMessage());
            return false;
        }
    }

    // ✅ Save Final Match
    public static boolean saveFinalMatch(String user1, String user2, String shared) {
        try (BufferedWriter writer = Files.newBufferedWriter(MATCHES_PATH, StandardOpenOption.APPEND)) {
            writer.write(String.join(",", user1, user2, shared));
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("❌ Failed to save match: " + e.getMessage());
            return false;
        }
    }

    public static List<String[]> loadFinalMatches() {
        try {
            return Files.lines(MATCHES_PATH)
                    .skip(1)
                    .map(line -> line.split(",", -1))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    // ✅ Helper: Check for existing pending request (either direction)
    public static boolean hasPendingRequest(String sender, String receiver) {
        try {
            List<MatchRequest> requests = loadMatchRequests();
            return requests.stream().anyMatch(req ->
                    ((req.getSender().equalsIgnoreCase(sender) && req.getReceiver().equalsIgnoreCase(receiver)) ||
                     (req.getSender().equalsIgnoreCase(receiver) && req.getReceiver().equalsIgnoreCase(sender))) &&
                    req.getStatus() == MatchManager.MatchStatus.PENDING);

        } catch (Exception e) {
            System.err.println("❌ Failed checking pending requests: " + e.getMessage());
            return false;
        }
    }

    // ✅ Helper: Check if already matched
    public static boolean isUserAlreadyMatched(String user1, String user2) {
        try {
            List<String[]> matches = loadFinalMatches();
            return matches.stream().anyMatch(parts ->
                    (parts[0].equalsIgnoreCase(user1) && parts[1].equalsIgnoreCase(user2)) ||
                    (parts[0].equalsIgnoreCase(user2) && parts[1].equalsIgnoreCase(user1)));
        } catch (Exception e) {
            System.err.println("❌ Failed checking existing match: " + e.getMessage());
            return false;
        }
    }
    public static List<String> getAvailableInterests() {
        List<String> interests = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(QUESTIONS_DIR, "*_questions.csv")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith("_questions.csv")) {
                    interests.add(fileName.replace("_questions.csv", ""));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to load interests: " + e.getMessage());
        }
        return interests;
    }
    public static void viewAllMatchRequests() throws IOException {
        List<MatchRequest> requests = loadMatchRequests();
        if (requests.isEmpty()) {
            System.out.println("📭 No match requests found.");
            return;
        }

        System.out.println("\n✉️ Match Requests:");
        for (MatchRequest req : requests) {
            System.out.printf("- From: %s → %s (%s) [%s]%n",
                    req.getSender(), req.getReceiver(), req.getInterest(), req.getStatus());
        }
    }

    // 🔍 View All Finalized Matches (Admin)
    public static void viewAllFinalizedMatches() throws IOException {
        List<String[]> matches = loadFinalMatches();
        if (matches.isEmpty()) {
            System.out.println("📭 No finalized matches.");
            return;
        }

        System.out.println("\n🤝 Finalized Matches:");
        for (String[] match : matches) {
            if (match.length >= 3) {
                System.out.printf("- %s ↔ %s | Shared Interests: %s%n",
                        match[0], match[1], match[2]);
            }
        }
    }
}