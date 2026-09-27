package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Modern Leaderboard View:
 * Displays student rankings based on qualified technical skills and assessment scores.
 */
public class LeaderboardView {

    private final SkillSyncApp app;
    private final User currentUser;
    private final Consumer<NavSection> navigationHandler;
    private final VBox root;

    private TextField searchField;
    private ComboBox<String> sortSelector;
    private VBox tableContainer;

    public LeaderboardView(SkillSyncApp app, User currentUser, Consumer<NavSection> navigationHandler) {
        this.app = app;
        this.currentUser = currentUser;
        this.navigationHandler = navigationHandler;

        this.root = new VBox(22);
        this.root.getStyleClass().add("content-container");

        buildView();
    }

    public Parent getView() {
        return root;
    }

    private void buildView() {
        List<User> allUsers = new ArrayList<>(app.getUsers());

        // 1. Header Banner
        VBox headerBanner = new VBox(8);
        headerBanner.getStyleClass().add("dashboard-banner");

        Label badge = new Label("ACADEMIC MERIT & COMPETENCY");
        badge.getStyleClass().add("banner-badge");

        Label title = new Label("Student Competency Leaderboard");
        title.getStyleClass().add("banner-title");

        Label subtitle = new Label("Official rankings based on validated technical assessments and domain qualifications.");
        subtitle.getStyleClass().add("banner-subtitle");

        headerBanner.getChildren().addAll(badge, title, subtitle);

        // 2. Summary KPI Cards
        HBox statsGrid = new HBox(16);
        statsGrid.getStyleClass().add("stats-grid");

        // Total Students
        VBox totalCard = createStatCard("TOTAL STUDENTS", String.valueOf(allUsers.size()), "Registered on platform");

        // Top Performer
        User topUser = allUsers.stream()
                .max((a, b) -> {
                    int c = Integer.compare(a.getQualifiedInterests().size(), b.getQualifiedInterests().size());
                    return c != 0 ? c : Double.compare(a.getAverageScore(), b.getAverageScore());
                }).orElse(null);

        String topPerformerName = topUser != null ? topUser.getUsername() : "—";
        String topPerformerNote = topUser != null
                ? topUser.getQualifiedInterests().size() + " tracks (" + String.format("%.1f", topUser.getAverageScore()) + " avg)"
                : "No assessments taken";
        VBox topCard = createStatCard("TOP PERFORMER", topPerformerName, topPerformerNote);

        // Current User Standing
        String myStandingText = "—";
        String myStandingNote;
        if (currentUser != null) {
            List<User> rankedUsers = getSortedUsers(allUsers, true);
            int myRank = -1;
            for (int i = 0; i < rankedUsers.size(); i++) {
                if (rankedUsers.get(i).getUsername().equalsIgnoreCase(currentUser.getUsername())) {
                    myRank = i + 1;
                    break;
                }
            }
            if (myRank != -1) {
                myStandingText = "#" + myRank;
                myStandingNote = currentUser.getQualifiedInterests().size() + " qualified (" + String.format("%.1f", currentUser.getAverageScore()) + " avg)";
            } else {
                myStandingNote = "Take an assessment to rank";
            }
        } else {
            myStandingNote = "Admin View (Global Standings)";
        }
        VBox myRankCard = createStatCard("YOUR RANK", myStandingText, myStandingNote);

        // Average Platform Score
        double platformAvg = allUsers.stream()
                .mapToDouble(User::getAverageScore)
                .filter(score -> score > 0)
                .average()
                .orElse(0.0);
        VBox avgCard = createStatCard("PLATFORM AVG", String.format("%.1f", platformAvg), "Out of 10.0 scale");

        statsGrid.getChildren().addAll(totalCard, topCard, myRankCard, avgCard);
        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(topCard, Priority.ALWAYS);
        HBox.setHgrow(myRankCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);

        // 3. Search & Sort Controls
        HBox controlsBox = new HBox(12);
        controlsBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search student by username or email...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(340);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTable(allUsers));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label sortLabel = new Label("Sort by:");
        sortLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        sortSelector = new ComboBox<>();
        sortSelector.getItems().addAll("Qualified Skills (Descending)", "Average Score (Descending)", "Username (A-Z)");
        sortSelector.setValue("Qualified Skills (Descending)");
        sortSelector.getStyleClass().add("form-input");
        sortSelector.setOnAction(e -> renderTable(allUsers));

        controlsBox.getChildren().addAll(searchField, spacer, sortLabel, sortSelector);

        // 4. Leaderboard Table / Cards Container
        tableContainer = new VBox(10);

        renderTable(allUsers);

        root.getChildren().addAll(headerBanner, statsGrid, controlsBox, tableContainer);
    }

    private void renderTable(List<User> allUsers) {
        tableContainer.getChildren().clear();

        String query = searchField.getText().trim().toLowerCase();
        String sortOption = sortSelector.getValue();

        List<User> sortedUsers = new ArrayList<>(allUsers);

        // Apply Sorting
        if ("Average Score (Descending)".equals(sortOption)) {
            sortedUsers.sort((a, b) -> {
                int c = Double.compare(b.getAverageScore(), a.getAverageScore());
                return c != 0 ? c : Integer.compare(b.getQualifiedInterests().size(), a.getQualifiedInterests().size());
            });
        } else if ("Username (A-Z)".equals(sortOption)) {
            sortedUsers.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        } else {
            // Default: Qualified Skills (Descending)
            sortedUsers.sort((a, b) -> {
                int c = Integer.compare(b.getQualifiedInterests().size(), a.getQualifiedInterests().size());
                return c != 0 ? c : Double.compare(b.getAverageScore(), a.getAverageScore());
            });
        }

        // Apply Search Filter
        if (!query.isEmpty()) {
            sortedUsers = sortedUsers.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        if (sortedUsers.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            emptyBox.getStyleClass().add("card-section");

            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size: 32px;");
            Label emptyTitle = new Label("No Students Found");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            Label emptyDesc = new Label("No students match the search term '" + query + "'.");
            emptyDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
            tableContainer.getChildren().add(emptyBox);
            return;
        }

        // Table Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 16, 8, 16));
        headerRow.getStyleClass().add("table-header-row");

        Label colRank = createHeaderLabel("RANK", 50);
        Label colStudent = createHeaderLabel("STUDENT", 220);
        Label colTracks = createHeaderLabel("QUALIFIED DOMAINS", 260);
        Label colCount = createHeaderLabel("QUALIFIED", 90);
        Label colAvg = createHeaderLabel("AVG SCORE", 90);
        Label colStatus = createHeaderLabel("PARTNER STATUS", 140);

        headerRow.getChildren().addAll(colRank, colStudent, colTracks, colCount, colAvg, colStatus);
        HBox.setHgrow(colTracks, Priority.ALWAYS);
        tableContainer.getChildren().add(headerRow);

        // Table Rows
        int rank = 1;
        for (User user : sortedUsers) {
            boolean isSelf = currentUser != null && user.getUsername().equalsIgnoreCase(currentUser.getUsername());
            HBox row = createLeaderboardRow(rank, user, isSelf);
            tableContainer.getChildren().add(row);
            rank++;
        }
    }

    private HBox createLeaderboardRow(int rank, User user, boolean isSelf) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("leaderboard-row");

        if (isSelf) {
            row.getStyleClass().add("leaderboard-row-self");
        }

        // 1. Rank Badge
        StackPane rankPane = new StackPane();
        rankPane.setPrefWidth(50);
        rankPane.setAlignment(Pos.CENTER_LEFT);

        Label rankLabel = new Label();
        if (rank == 1) {
            rankLabel.setText("🥇 1");
            rankLabel.getStyleClass().add("rank-gold");
        } else if (rank == 2) {
            rankLabel.setText("🥈 2");
            rankLabel.getStyleClass().add("rank-silver");
        } else if (rank == 3) {
            rankLabel.setText("🥉 3");
            rankLabel.getStyleClass().add("rank-bronze");
        } else {
            rankLabel.setText("#" + rank);
            rankLabel.getStyleClass().add("rank-number");
        }
        rankPane.getChildren().add(rankLabel);

        // 2. Student Info (Avatar + Username + Email)
        HBox studentBox = new HBox(10);
        studentBox.setAlignment(Pos.CENTER_LEFT);
        studentBox.setPrefWidth(220);

        Label avatar = new Label(user.getUsername().isEmpty() ? "?" : user.getUsername().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("avatar-small");

        VBox nameBox = new VBox(2);
        HBox titleLine = new HBox(6);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        titleLine.getChildren().add(nameLabel);

        if (isSelf) {
            Label youBadge = new Label("YOU");
            youBadge.getStyleClass().add("badge-you");
            titleLine.getChildren().add(youBadge);
        }

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        nameBox.getChildren().addAll(titleLine, emailLabel);
        studentBox.getChildren().addAll(avatar, nameBox);

        // 3. Qualified Domain Chips
        HBox domainsBox = new HBox(6);
        domainsBox.setAlignment(Pos.CENTER_LEFT);
        domainsBox.setPrefWidth(260);
        HBox.setHgrow(domainsBox, Priority.ALWAYS);

        List<String> qualified = user.getQualifiedInterests();
        if (qualified.isEmpty()) {
            Label noneLabel = new Label("None qualified yet");
            noneLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            domainsBox.getChildren().add(noneLabel);
        } else {
            for (String domain : qualified) {
                Label chip = new Label(domain.toUpperCase());
                chip.getStyleClass().add("shared-skill-chip");
                domainsBox.getChildren().add(chip);
            }
        }

        // 4. Qualified Count Pill
        HBox countBox = new HBox();
        countBox.setAlignment(Pos.CENTER_LEFT);
        countBox.setPrefWidth(90);

        Label countPill = new Label(qualified.size() + " / " + User.getAllInterests().size());
        countPill.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (qualified.isEmpty() ? "#94a3b8" : "#2563eb") + ";");
        countBox.getChildren().add(countPill);

        // 5. Average Score Pill
        HBox avgBox = new HBox();
        avgBox.setAlignment(Pos.CENTER_LEFT);
        avgBox.setPrefWidth(90);

        double avg = user.getAverageScore();
        Label avgPill = new Label(avg > 0 ? String.format("%.1f", avg) + " / 10" : "—");
        avgPill.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (avg >= 7 ? "#059669" : (avg > 0 ? "#d97706" : "#94a3b8")) + ";");
        avgBox.getChildren().add(avgPill);

        // 6. Partner Status Pill
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPrefWidth(140);

        Label statusPill = new Label();
        if (user.isMatched()) {
            statusPill.setText("🤝 " + user.getMatchedWith());
            statusPill.getStyleClass().add("badge-accepted");
        } else {
            statusPill.setText("🔍 Available");
            statusPill.getStyleClass().add("badge-available");
        }
        statusBox.getChildren().add(statusPill);

        row.getChildren().addAll(rankPane, studentBox, domainsBox, countBox, avgBox, statusBox);
        return row;
    }

    private List<User> getSortedUsers(List<User> users, boolean byQualified) {
        List<User> list = new ArrayList<>(users);
        if (byQualified) {
            list.sort((a, b) -> {
                int c = Integer.compare(b.getQualifiedInterests().size(), a.getQualifiedInterests().size());
                return c != 0 ? c : Double.compare(b.getAverageScore(), a.getAverageScore());
            });
        }
        return list;
    }

    private Label createHeaderLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add("table-header-label");
        return lbl;
    }

    private VBox createStatCard(String title, String value, String note) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        Label noteLabel = new Label(note);
        noteLabel.getStyleClass().add("stat-note");

        card.getChildren().addAll(titleLabel, valueLabel, noteLabel);
        return card;
    }
}
