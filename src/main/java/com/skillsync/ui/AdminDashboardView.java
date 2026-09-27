package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Admin Dashboard View:
 * Comprehensive overview of users, assessment metrics, match operations, and domain analytics.
 */
public class AdminDashboardView {

    private final SkillSyncApp app;
    private final Consumer<AdminSection> navigationHandler;
    private final VBox root;

    public AdminDashboardView(SkillSyncApp app, Consumer<AdminSection> navigationHandler) {
        this.app = app;
        this.navigationHandler = navigationHandler;

        this.root = new VBox(22);
        this.root.getStyleClass().add("content-container");

        buildView();
    }

    public Parent getView() {
        return root;
    }

    private void buildView() {
        List<User> users = app.getUsers();
        List<MatchManager.MatchRequest> requests = FileHandler.loadMatchRequests();
        List<String[]> matches = FileHandler.loadFinalMatches();

        // 1. Admin Welcome Banner
        VBox banner = new VBox(8);
        banner.getStyleClass().add("admin-banner");

        Label badge = new Label("ADMINISTRATIVE CONSOLE");
        badge.getStyleClass().add("admin-banner-badge");

        Label title = new Label("SkillSync System Overview");
        title.getStyleClass().add("banner-title");

        Label subtitle = new Label("Real-time telemetry across registered students, domain assessments, and peer collaborative matching.");
        subtitle.getStyleClass().add("banner-subtitle");

        banner.getChildren().addAll(badge, title, subtitle);

        // 2. High-Level KPI Stat Cards
        int totalStudents = users.size();
        int totalAttempts = users.stream().mapToInt(u -> u.getAttemptedInterests().size()).sum();
        int totalQualified = users.stream().mapToInt(u -> u.getQualifiedInterests().size()).sum();
        int passRate = totalAttempts > 0 ? (int) Math.round((double) totalQualified * 100.0 / totalAttempts) : 0;

        long pendingReqs = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.PENDING).count();
        long acceptedReqs = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.ACCEPTED).count();
        long declinedReqs = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.DECLINED).count();

        int totalTeams = matches.size();
        long pairedUsersCount = users.stream().filter(User::isMatched).count();

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(16);

        VBox card1 = createStatCard("REGISTERED STUDENTS", String.valueOf(totalStudents), pairedUsersCount + " currently paired");
        VBox card2 = createStatCard("ASSESSMENTS ATTEMPTED", String.valueOf(totalAttempts), "Across all 4 domains");
        VBox card3 = createStatCard("DOMAIN QUALIFICATIONS", String.valueOf(totalQualified), "Scores ≥ 7/10 standard");
        VBox card4 = createStatCard("PLATFORM PASS RATE", passRate + "%", totalQualified + " passed of " + totalAttempts);
        VBox card5 = createStatCard("MATCH REQUESTS", String.valueOf(requests.size()), pendingReqs + " pending • " + acceptedReqs + " accepted");
        VBox card6 = createStatCard("FINALIZED TEAMS", String.valueOf(totalTeams), pairedUsersCount + " students collaborating");

        statsGrid.add(card1, 0, 0);
        statsGrid.add(card2, 1, 0);
        statsGrid.add(card3, 2, 0);
        statsGrid.add(card4, 0, 1);
        statsGrid.add(card5, 1, 1);
        statsGrid.add(card6, 2, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33.33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33.33);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33.34);
        statsGrid.getColumnConstraints().addAll(col1, col2, col3);

        // 3. Domain Telemetry Section
        VBox domainSection = new VBox(16);
        domainSection.getStyleClass().add("card-section");

        HBox domainHeader = new HBox(8);
        domainHeader.setAlignment(Pos.CENTER_LEFT);
        Label domainTitle = new Label("Skill Domain Competency Telemetry");
        domainTitle.getStyleClass().add("card-section-title");
        Region domainSpacer = new Region();
        HBox.setHgrow(domainSpacer, Priority.ALWAYS);
        Button viewStatsBtn = new Button("Detailed Analytics →");
        viewStatsBtn.getStyleClass().add("btn-link");
        viewStatsBtn.setOnAction(e -> navigationHandler.accept(AdminSection.PARTICIPATION));
        domainHeader.getChildren().addAll(domainTitle, domainSpacer, viewStatsBtn);

        VBox domainList = new VBox(12);
        for (String domain : User.getAllInterests()) {
            domainList.getChildren().add(createDomainTelemetryRow(domain, users));
        }
        domainSection.getChildren().addAll(domainHeader, domainList);

        // 4. Quick Action Shortcuts Row
        VBox quickActionsSection = new VBox(12);
        quickActionsSection.getStyleClass().add("card-section");

        Label actionsTitle = new Label("Administrative Quick Actions");
        actionsTitle.getStyleClass().add("card-section-title");

        HBox actionsRow = new HBox(12);
        Button btnUsers = createQuickActionButton("👥 Student Directory", "Inspect profiles, credentials & domain scores", () -> navigationHandler.accept(AdminSection.USERS));
        Button btnStats = createQuickActionButton("📈 Participation Stats", "Analyze attempts, rates & student roster", () -> navigationHandler.accept(AdminSection.PARTICIPATION));
        Button btnReqs = createQuickActionButton("✉️ Match Requests", "Audit log of all student matching requests", () -> navigationHandler.accept(AdminSection.REQUESTS));
        Button btnMatches = createQuickActionButton("🤝 Finalized Matches", "Confirmed student partnerships & tracks", () -> navigationHandler.accept(AdminSection.MATCHES));

        actionsRow.getChildren().addAll(btnUsers, btnStats, btnReqs, btnMatches);
        HBox.setHgrow(btnUsers, Priority.ALWAYS);
        HBox.setHgrow(btnStats, Priority.ALWAYS);
        HBox.setHgrow(btnReqs, Priority.ALWAYS);
        HBox.setHgrow(btnMatches, Priority.ALWAYS);

        quickActionsSection.getChildren().addAll(actionsTitle, actionsRow);

        root.getChildren().addAll(banner, statsGrid, domainSection, quickActionsSection);
    }

    private HBox createDomainTelemetryRow(String domain, List<User> users) {
        long attemptedCount = users.stream().filter(u -> u.hasAttempted(domain)).count();
        long qualifiedCount = users.stream().filter(u -> u.isQualified(domain)).count();
        double domainAvg = users.stream()
                .filter(u -> u.hasAttempted(domain))
                .mapToInt(u -> u.getScore(domain))
                .average()
                .orElse(0.0);
        double rate = attemptedCount > 0 ? ((double) qualifiedCount / attemptedCount) : 0.0;

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("telemetry-row");

        // Domain Name Badge
        Label nameLabel = new Label(domain.toUpperCase());
        nameLabel.setPrefWidth(90);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        // Progress Bar
        ProgressBar progressBar = new ProgressBar(rate);
        progressBar.setPrefWidth(180);
        progressBar.getStyleClass().add("domain-progress-bar");
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        // Stats summary
        Label statsSummary = new Label(qualifiedCount + " / " + attemptedCount + " qualified (" + Math.round(rate * 100) + "%)");
        statsSummary.setPrefWidth(170);
        statsSummary.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        // Average score
        Label avgLabel = new Label("Avg: " + (attemptedCount > 0 ? String.format("%.1f", domainAvg) + "/10" : "—"));
        avgLabel.setPrefWidth(100);
        avgLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #2563eb;");

        row.getChildren().addAll(nameLabel, progressBar, statsSummary, avgLabel);
        return row;
    }

    private Button createQuickActionButton(String title, String desc, Runnable action) {
        VBox content = new VBox(4);
        content.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);

        content.getChildren().addAll(titleLabel, descLabel);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("quick-action-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());
        return btn;
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
