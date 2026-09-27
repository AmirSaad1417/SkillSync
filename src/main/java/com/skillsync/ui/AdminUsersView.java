package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Student Directory View:
 * Full inspection of registered students, domain scores, qualification status, and match pairings.
 */
public class AdminUsersView {

    private final SkillSyncApp app;
    private final VBox root;

    private TextField searchField;
    private ComboBox<String> filterSelector;
    private VBox tableContainer;
    private Label countLabel;

    public AdminUsersView(SkillSyncApp app) {
        this.app = app;

        this.root = new VBox(22);
        this.root.getStyleClass().add("content-container");

        buildView();
    }

    public Parent getView() {
        return root;
    }

    private void buildView() {
        List<User> users = app.getUsers();

        // 1. Header Area
        HBox headerArea = new HBox(12);
        headerArea.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Student Directory");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Inspect registered student profiles, credentials, domain scores, and active team assignments.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel = new Label(users.size() + " Total Registered Students");
        countLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #4338ca; -fx-background-color: #eef2ff; -fx-padding: 6px 14px; -fx-background-radius: 9999px; -fx-border-color: #c7d2fe; -fx-border-radius: 9999px;");

        headerArea.getChildren().addAll(titleBox, spacer, countLabel);

        // 2. Search & Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search student by username or email...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(340);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTable());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        Label filterLabel = new Label("Filter:");
        filterLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        filterSelector = new ComboBox<>();
        filterSelector.getItems().addAll("All Students", "Matched Only", "Unmatched Only", "Qualified in ≥ 1 Domain");
        filterSelector.setValue("All Students");
        filterSelector.getStyleClass().add("form-input");
        filterSelector.setOnAction(e -> renderTable());

        filterBar.getChildren().addAll(searchField, filterSpacer, filterLabel, filterSelector);

        // 3. Table Container
        tableContainer = new VBox(8);

        renderTable();

        root.getChildren().addAll(headerArea, filterBar, tableContainer);
    }

    private void renderTable() {
        tableContainer.getChildren().clear();

        List<User> users = app.getUsers();
        String query = searchField.getText().trim().toLowerCase();
        String filter = filterSelector.getValue();

        List<User> filtered = users.stream()
                .filter(u -> {
                    if (query.isEmpty()) return true;
                    return u.getUsername().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query);
                })
                .filter(u -> {
                    if ("Matched Only".equals(filter)) return u.isMatched();
                    if ("Unmatched Only".equals(filter)) return !u.isMatched();
                    if ("Qualified in ≥ 1 Domain".equals(filter)) return !u.getQualifiedInterests().isEmpty();
                    return true;
                })
                .collect(Collectors.toList());

        countLabel.setText(filtered.size() + " of " + users.size() + " Students Shown");

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            emptyBox.getStyleClass().add("card-section");

            Label emptyIcon = new Label("👥");
            emptyIcon.setStyle("-fx-font-size: 32px;");
            Label emptyTitle = new Label("No Matching Students");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            Label emptyDesc = new Label("No registered students match the active search and filter criteria.");
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

        Label colStudent = createHeaderLabel("STUDENT", 220);
        Label colScores = createHeaderLabel("SCORES (DSA / AI / ROBOTICS / DESIGN)", 260);
        Label colQualified = createHeaderLabel("QUALIFIED", 90);
        Label colAvg = createHeaderLabel("AVG SCORE", 90);
        Label colStatus = createHeaderLabel("TEAM STATUS", 160);

        headerRow.getChildren().addAll(colStudent, colScores, colQualified, colAvg, colStatus);
        HBox.setHgrow(colScores, Priority.ALWAYS);
        tableContainer.getChildren().add(headerRow);

        for (User user : filtered) {
            tableContainer.getChildren().add(createUserRow(user));
        }
    }

    private HBox createUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("leaderboard-row");

        // 1. Student Identity (Avatar + Name + Email)
        HBox studentBox = new HBox(10);
        studentBox.setAlignment(Pos.CENTER_LEFT);
        studentBox.setPrefWidth(220);

        Label avatar = new Label(user.getUsername().isEmpty() ? "?" : user.getUsername().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("avatar-small");

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        nameBox.getChildren().addAll(nameLabel, emailLabel);

        studentBox.getChildren().addAll(avatar, nameBox);

        // 2. Scores Grid across 4 Domains
        HBox scoresBox = new HBox(8);
        scoresBox.setAlignment(Pos.CENTER_LEFT);
        scoresBox.setPrefWidth(260);
        HBox.setHgrow(scoresBox, Priority.ALWAYS);

        for (String domain : User.getAllInterests()) {
            scoresBox.getChildren().add(createDomainScoreBadge(domain, user.getScore(domain)));
        }

        // 3. Qualified Count
        HBox qualBox = new HBox();
        qualBox.setAlignment(Pos.CENTER_LEFT);
        qualBox.setPrefWidth(90);
        int qualCount = user.getQualifiedInterests().size();
        Label qualLabel = new Label(qualCount + " / " + User.getAllInterests().size());
        qualLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (qualCount > 0 ? "#2563eb" : "#94a3b8") + ";");
        qualBox.getChildren().add(qualLabel);

        // 4. Average Score
        HBox avgBox = new HBox();
        avgBox.setAlignment(Pos.CENTER_LEFT);
        avgBox.setPrefWidth(90);
        double avg = user.getAverageScore();
        Label avgLabel = new Label(avg > 0 ? String.format("%.1f", avg) + " / 10" : "—");
        avgLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (avg >= 7 ? "#059669" : (avg > 0 ? "#d97706" : "#94a3b8")) + ";");
        avgBox.getChildren().add(avgLabel);

        // 5. Team Status
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPrefWidth(160);

        Label statusBadge = new Label();
        if (user.isMatched()) {
            statusBadge.setText("🤝 " + user.getMatchedWith());
            statusBadge.getStyleClass().add("badge-accepted");
        } else {
            statusBadge.setText("Available");
            statusBadge.getStyleClass().add("badge-available");
        }
        statusBox.getChildren().add(statusBadge);

        row.getChildren().addAll(studentBox, scoresBox, qualBox, avgBox, statusBox);
        return row;
    }

    private Label createDomainScoreBadge(String domain, int score) {
        Label badge = new Label();
        if (score == -1) {
            badge.setText(domain.toUpperCase() + ": —");
            badge.getStyleClass().add("badge-domain-unattempted");
        } else if (score >= 7) {
            badge.setText(domain.toUpperCase() + ": " + score);
            badge.getStyleClass().add("badge-domain-qualified");
        } else {
            badge.setText(domain.toUpperCase() + ": " + score);
            badge.getStyleClass().add("badge-domain-unqualified");
        }
        return badge;
    }

    private Label createHeaderLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add("table-header-label");
        return lbl;
    }
}
