package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Participation Statistics View:
 * Direct implementation of AdminPanel.viewTestStats():
 * Breakdown of assessment attempts and domain qualification rates per domain and per student.
 */
public class AdminStatsView {

    private final SkillSyncApp app;
    private final VBox root;

    private TextField searchField;
    private VBox rosterContainer;
    private Label studentCountLabel;

    public AdminStatsView(SkillSyncApp app) {
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

        // 1. Header Banner
        VBox titleBox = new VBox(4);
        Label title = new Label("Assessment Participation Statistics");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Evaluation telemetry across technical domains and student participation rosters.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        // 2. Domain Participation Cards (4 Columns)
        HBox domainCardsGrid = new HBox(16);
        domainCardsGrid.getStyleClass().add("stats-grid");

        for (String domain : User.getAllInterests()) {
            VBox card = createDomainAnalyticsCard(domain, users);
            domainCardsGrid.getChildren().add(card);
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        // 3. Student Participation Roster Section
        VBox rosterSection = new VBox(16);
        rosterSection.getStyleClass().add("card-section");

        HBox rosterHeader = new HBox(12);
        rosterHeader.setAlignment(Pos.CENTER_LEFT);

        VBox rosterTitleBox = new VBox(2);
        Label rosterTitle = new Label("Student Participation Telemetry");
        rosterTitle.getStyleClass().add("card-section-title");
        Label rosterSubtitle = new Label("Individual student attempted tracks, qualified certifications, and success ratios.");
        rosterSubtitle.getStyleClass().add("card-section-subtitle");
        rosterTitleBox.getChildren().addAll(rosterTitle, rosterSubtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍 Filter student...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderRoster());

        studentCountLabel = new Label(users.size() + " Students");
        studentCountLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #4338ca; -fx-background-color: #eef2ff; -fx-padding: 4px 10px; -fx-background-radius: 9999px;");

        rosterHeader.getChildren().addAll(rosterTitleBox, spacer, searchField, studentCountLabel);

        rosterContainer = new VBox(8);
        renderRoster();

        rosterSection.getChildren().addAll(rosterHeader, rosterContainer);

        root.getChildren().addAll(titleBox, domainCardsGrid, rosterSection);
    }

    private VBox createDomainAnalyticsCard(String domain, List<User> users) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");

        long attempted = users.stream().filter(u -> u.hasAttempted(domain)).count();
        long qualified = users.stream().filter(u -> u.isQualified(domain)).count();
        double passRate = attempted > 0 ? ((double) qualified / attempted) : 0.0;
        double avgScore = users.stream()
                .filter(u -> u.hasAttempted(domain))
                .mapToInt(u -> u.getScore(domain))
                .average()
                .orElse(0.0);

        HBox topBox = new HBox(6);
        topBox.setAlignment(Pos.CENTER_LEFT);
        Label domainName = new Label(domain.toUpperCase());
        domainName.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label passPill = new Label(Math.round(passRate * 100) + "% Pass");
        passPill.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        topBox.getChildren().addAll(domainName, sp, passPill);

        Label attemptsVal = new Label(attempted + " Attempted");
        attemptsVal.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        ProgressBar pb = new ProgressBar(passRate);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.getStyleClass().add("domain-progress-bar");

        Label detailLabel = new Label(qualified + " Qualified (Avg: " + (attempted > 0 ? String.format("%.1f", avgScore) : "—") + ")");
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(topBox, attemptsVal, pb, detailLabel);
        return card;
    }

    private void renderRoster() {
        rosterContainer.getChildren().clear();

        List<User> users = app.getUsers();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        List<User> filtered = users.stream()
                .filter(u -> query.isEmpty() || u.getUsername().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query))
                .collect(Collectors.toList());

        if (studentCountLabel != null) {
            studentCountLabel.setText(filtered.size() + " of " + users.size() + " Students");
        }

        // Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 14, 8, 14));
        headerRow.getStyleClass().add("table-header-row");

        Label colStudent = createHeaderLabel("STUDENT", 200);
        Label colAttempted = createHeaderLabel("ATTEMPTED TRACKS", 220);
        Label colQualified = createHeaderLabel("QUALIFIED TRACKS", 220);
        Label colRatio = createHeaderLabel("PASS RATIO", 100);

        headerRow.getChildren().addAll(colStudent, colAttempted, colQualified, colRatio);
        HBox.setHgrow(colAttempted, Priority.ALWAYS);
        HBox.setHgrow(colQualified, Priority.ALWAYS);
        rosterContainer.getChildren().add(headerRow);

        for (User user : filtered) {
            rosterContainer.getChildren().add(createRosterRow(user));
        }
    }

    private HBox createRosterRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.getStyleClass().add("leaderboard-row");

        // 1. Student Identity
        VBox studentBox = new VBox(2);
        studentBox.setPrefWidth(200);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        studentBox.getChildren().addAll(nameLabel, emailLabel);

        // 2. Attempted Tracks
        HBox attemptedBox = new HBox(6);
        attemptedBox.setAlignment(Pos.CENTER_LEFT);
        attemptedBox.setPrefWidth(220);
        HBox.setHgrow(attemptedBox, Priority.ALWAYS);

        List<String> attempted = user.getAttemptedInterests();
        if (attempted.isEmpty()) {
            Label none = new Label("None attempted");
            none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            attemptedBox.getChildren().add(none);
        } else {
            for (String domain : attempted) {
                Label chip = new Label(domain.toUpperCase());
                chip.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-background-color: #f1f5f9; -fx-padding: 3px 6px; -fx-background-radius: 4px;");
                attemptedBox.getChildren().add(chip);
            }
        }

        // 3. Qualified Tracks
        HBox qualifiedBox = new HBox(6);
        qualifiedBox.setAlignment(Pos.CENTER_LEFT);
        qualifiedBox.setPrefWidth(220);
        HBox.setHgrow(qualifiedBox, Priority.ALWAYS);

        List<String> qualified = user.getQualifiedInterests();
        if (qualified.isEmpty()) {
            Label none = new Label("None qualified");
            none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            qualifiedBox.getChildren().add(none);
        } else {
            for (String domain : qualified) {
                Label chip = new Label(domain.toUpperCase());
                chip.getStyleClass().add("shared-skill-chip");
                qualifiedBox.getChildren().add(chip);
            }
        }

        // 4. Pass Ratio
        HBox ratioBox = new HBox();
        ratioBox.setAlignment(Pos.CENTER_LEFT);
        ratioBox.setPrefWidth(100);

        String ratioText = qualified.size() + " / " + attempted.size();
        Label ratioLabel = new Label(ratioText);
        ratioLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (qualified.size() > 0 ? "#059669" : "#94a3b8") + ";");
        ratioBox.getChildren().add(ratioLabel);

        row.getChildren().addAll(studentBox, attemptedBox, qualifiedBox, ratioBox);
        return row;
    }

    private Label createHeaderLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add("table-header-label");
        return lbl;
    }
}
