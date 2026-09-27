package com.skillsync;
import java.util.*;

public class User {
    private final String username;
    private final String email;
    private final String password;
    private final Map<String, Integer> interestScores;
    private boolean matched;
    private String matchedWith;

    // Supported interest names
    private static final List<String> INTERESTS = List.of("dsa", "ai", "robotics", "design");
    private static final int QUALIFICATION_THRESHOLD = 7;
    private static final int NOT_ATTEMPTED = -1;

    // Constructor for new user
    public User(String username, String email, String password) {
        validate(username, email, password);
        this.username = username.trim();
        this.email = email.trim().toLowerCase();
        this.password = password.trim();
        this.interestScores = initializeInterestScores();
        this.matched = false;
        this.matchedWith = "";
    }

    // Constructor from CSV
    public User(String username, String email, String password,
                int dsa, int ai, int robotics, int design,
                boolean matched, String matchedWith) {
        this(username, email, password);
        this.interestScores.put("dsa", dsa);
        this.interestScores.put("ai", ai);
        this.interestScores.put("robotics", robotics);
        this.interestScores.put("design", design);
        this.matched = matched;
        this.matchedWith = matchedWith != null ? matchedWith.trim() : "";
    }

    private Map<String, Integer> initializeInterestScores() {
        Map<String, Integer> scores = new HashMap<>();
        INTERESTS.forEach(interest -> scores.put(interest, NOT_ATTEMPTED));
        return scores;
    }

    private void validate(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    // Enhanced CSV serialization
    public String toCsvString() {
        return String.join(",",
                username,
                email,
                password,
                String.valueOf(interestScores.get("dsa")),
                String.valueOf(interestScores.get("ai")),
                String.valueOf(interestScores.get("robotics")),
                String.valueOf(interestScores.get("design")),
                String.valueOf(matched),
                matchedWith
        );
    }

    // Profile display method
    public String getProfileString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDC64 Username: ").append(username).append("\n");
        sb.append("\uD83D\uDCE7 Email: ").append(email).append("\n\n");
        sb.append("\uD83E\uDDEA Test Scores:\n");

        INTERESTS.forEach(interest -> {
            String prettyName = interest.substring(0, 1).toUpperCase() + interest.substring(1);
            int score = interestScores.get(interest);
            sb.append("- ").append(prettyName).append(": ");

            if (score == NOT_ATTEMPTED) {
                sb.append("Not Attempted\n");
            } else {
                sb.append(score).append("/10 ")
                        .append(isQualified(interest) ? "✅ Qualified" : "❌")
                        .append("\n");
            }
        });

        sb.append("\n\uD83D\uDCCA Average Score: ").append(String.format("%.1f", getAverageScore()));
        sb.append("\nQualified Interests: ").append(getQualifiedInterests().size());

        if (matched) {
            sb.append("\n\n\uD83E\uDD1D Matched with: ").append(matchedWith);
        }

        return sb.toString();
    }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isMatched() { return matched; }
    public String getMatchedWith() { return matchedWith; }
    public Map<String, Integer> getInterestScores() { return Collections.unmodifiableMap(interestScores); }

    public int getScore(String interest) {
        return interestScores.getOrDefault(interest, NOT_ATTEMPTED);
    }

    public static List<String> getAllInterests() {
        return Collections.unmodifiableList(INTERESTS);
    }

    // Setters with validation
    public void setScore(String interest, int score) {
        if (!INTERESTS.contains(interest)) {
            throw new IllegalArgumentException("Invalid interest: " + interest);
        }
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Score must be between 0-10");
        }
        interestScores.put(interest, score);
    }

    public void setMatched(boolean matched, String matchedWith) {
        this.matched = matched;
        this.matchedWith = matched ? matchedWith.trim() : "";
    }

    // Test status methods
    public boolean hasAttempted(String interest) {
        return interestScores.getOrDefault(interest, NOT_ATTEMPTED) != NOT_ATTEMPTED;
    }

    public boolean isQualified(String interest) {
        return interestScores.getOrDefault(interest, 0) >= QUALIFICATION_THRESHOLD;
    }

    public List<String> getQualifiedInterests() {
        return INTERESTS.stream()
                .filter(this::isQualified)
                .toList();
    }

    public List<String> getAttemptedInterests() {
        return INTERESTS.stream()
                .filter(this::hasAttempted)
                .toList();
    }

    public List<String> getUnattemptedInterests() {
        return INTERESTS.stream()
                .filter(interest -> !hasAttempted(interest))
                .toList();
    }

    // Score calculations
    public double getAverageScore() {
        List<Integer> scores = INTERESTS.stream()
                .map(interestScores::get)
                .filter(score -> score != NOT_ATTEMPTED)
                .toList();

        return scores.isEmpty() ? 0.0 :
                scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    // Matching utilities
    public int countSharedQualifiedInterests(User other) {
        return (int) INTERESTS.stream()
                .filter(interest -> this.isQualified(interest) && other.isQualified(interest))
                .count();
    }

    // Leaderboard comparison
    public static Comparator<User> getLeaderboardComparator() {
        return Comparator.comparingDouble(User::getAverageScore).reversed()
                .thenComparingInt(u -> u.getQualifiedInterests().size()).reversed()
                .thenComparing(User::getUsername);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equalsIgnoreCase(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }
}
