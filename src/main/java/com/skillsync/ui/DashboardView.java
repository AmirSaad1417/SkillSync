package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modern student dashboard shell for SkillSync Desktop.
 */
public class DashboardView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User user;
    private final Consumer<NavSection> navigationHandler;
    private final Consumer<String> testLaunchHandler;

    public DashboardView(SkillSyncApp app, User user, Consumer<NavSection> navigationHandler) {
        this(app, user, navigationHandler, null);
    }

    public DashboardView(SkillSyncApp app, User user, Consumer<NavSection> navigationHandler, Consumer<String> testLaunchHandler) {
        this.app = app;
        this.user = user;
        this.navigationHandler = navigationHandler;
        this.testLaunchHandler = testLaunchHandler;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        buildDashboard();
    }

    private void buildDashboard() {
        // Calculate dynamic user metrics
        int attemptedCount = user.getAttemptedInterests().size();
        int qualifiedCount = user.getQualifiedInterests().size();
        long pendingRequestsCount = countPendingRequests();

        // 1. Welcome Banner
        VBox banner = buildWelcomeBanner(pendingRequestsCount);

        // 2. Stat Cards Grid
        HBox statsGrid = buildStatsGrid(attemptedCount, qualifiedCount, pendingRequestsCount);

        // 3. Quick Actions Section
        VBox quickActionsSection = buildQuickActionsSection();

        // 4. Skills Overview Section
        VBox skillsSection = buildSkillsOverviewSection();

        container.getChildren().addAll(banner, statsGrid, quickActionsSection, skillsSection);
    }

    private VBox buildWelcomeBanner(long pendingRequestsCount) {
        VBox banner = new VBox(6);
        banner.getStyleClass().add("dashboard-banner");

        Label title = new Label("Welcome back, " + user.getUsername() + "! 👋");
        title.getStyleClass().add("banner-title");

        String subtitleText;
        if (user.isMatched()) {
            subtitleText = "🤝 You are paired with " + user.getMatchedWith() + ". Connect and start building your collaborative project!";
        } else if (pendingRequestsCount > 0) {
            subtitleText = "✉️ You have " + pendingRequestsCount + " pending peer request(s) awaiting your review in Match Requests.";
        } else if (user.getAttemptedInterests().isEmpty()) {
            subtitleText = "🚀 Get started by taking a skill test in DSA, AI, Robotics, or Design to qualify for peer matching.";
        } else {
            subtitleText = "💡 Complete more skill tests with a score of 7 or higher to expand your teammate compatibility.";
        }

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("banner-subtitle");
        subtitle.setWrapText(true);

        banner.getChildren().addAll(title, subtitle);
        return banner;
    }

    private HBox buildStatsGrid(int attemptedCount, int qualifiedCount, long pendingRequestsCount) {
        HBox grid = new HBox(16);
        grid.getStyleClass().add("stats-grid");

        // Card 1: Average Score
        String avgScoreStr = attemptedCount > 0
                ? String.format("%.1f / 10", user.getAverageScore())
                : "N/A";
        String avgNote = attemptedCount > 0
                ? attemptedCount + " of 4 tests taken"
                : "No tests attempted yet";
        VBox scoreCard = createStatCard("📊", "AVERAGE SCORE", avgScoreStr, avgNote);

        // Card 2: Qualified Skills
        String qualStr = qualifiedCount + " / 4";
        String qualNote = qualifiedCount > 0
                ? String.join(", ", user.getQualifiedInterests()).toUpperCase()
                : "Threshold: 7/10 score";
        VBox qualCard = createStatCard("⭐", "QUALIFIED SKILLS", qualStr, qualNote);

        // Card 3: Pending Requests
        String pendingStr = String.valueOf(pendingRequestsCount);
        String pendingNote = pendingRequestsCount == 1
                ? "1 peer awaiting response"
                : pendingRequestsCount + " peers awaiting response";
        VBox reqCard = createStatCard("✉️", "PENDING REQUESTS", pendingStr, pendingNote);

        // Card 4: Match Status
        String matchStr = user.isMatched() ? "Matched" : "Available";
        String matchNote = user.isMatched()
                ? "Partner: " + user.getMatchedWith()
                : "Seeking project teammates";
        VBox matchCard = createStatCard("🤝", "TEAM STATUS", matchStr, matchNote);

        HBox.setHgrow(scoreCard, Priority.ALWAYS);
        HBox.setHgrow(qualCard, Priority.ALWAYS);
        HBox.setHgrow(reqCard, Priority.ALWAYS);
        HBox.setHgrow(matchCard, Priority.ALWAYS);

        grid.getChildren().addAll(scoreCard, qualCard, reqCard, matchCard);
        return grid;
    }

    private VBox createStatCard(String icon, String titleText, String valueText, String noteText) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");

        Label titleLbl = new Label(titleText);
        titleLbl.getStyleClass().add("stat-title");

        header.getChildren().addAll(iconLbl, titleLbl);

        Label valueLbl = new Label(valueText);
        valueLbl.getStyleClass().add("stat-value");

        Label noteLbl = new Label(noteText);
        noteLbl.getStyleClass().add("stat-note");
        noteLbl.setWrapText(true);

        card.getChildren().addAll(header, valueLbl, noteLbl);
        return card;
    }

    private VBox buildQuickActionsSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        VBox headingBox = new VBox(2);
        Label title = new Label("Quick Navigation");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Jump straight into key student platform features.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        HBox actionsRow = new HBox(12);

        Button testBtn = new Button("📝  Take Skill Assessment");
        testBtn.getStyleClass().add("quick-action-btn");
        testBtn.setOnAction(e -> navigate(NavSection.ASSESSMENTS));

        Button peerBtn = new Button("🔍  Discover Compatible Peers");
        peerBtn.getStyleClass().add("quick-action-btn");
        peerBtn.setOnAction(e -> navigate(NavSection.FIND_PEERS));

        Button reqBtn = new Button("✉️  View Match Requests");
        reqBtn.getStyleClass().add("quick-action-btn");
        reqBtn.setOnAction(e -> navigate(NavSection.REQUESTS));

        Button rankBtn = new Button("🏆  Explore Leaderboard");
        rankBtn.getStyleClass().add("quick-action-btn");
        rankBtn.setOnAction(e -> navigate(NavSection.LEADERBOARD));

        HBox.setHgrow(testBtn, Priority.ALWAYS);
        HBox.setHgrow(peerBtn, Priority.ALWAYS);
        HBox.setHgrow(reqBtn, Priority.ALWAYS);
        HBox.setHgrow(rankBtn, Priority.ALWAYS);

        testBtn.setMaxWidth(Double.MAX_VALUE);
        peerBtn.setMaxWidth(Double.MAX_VALUE);
        reqBtn.setMaxWidth(Double.MAX_VALUE);
        rankBtn.setMaxWidth(Double.MAX_VALUE);

        actionsRow.getChildren().addAll(testBtn, peerBtn, reqBtn, rankBtn);
        section.getChildren().addAll(headingBox, actionsRow);
        return section;
    }

    private VBox buildSkillsOverviewSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        VBox headingBox = new VBox(2);
        Label title = new Label("Skill Competency Overview");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Score 7 or above out of 10 to qualify for peer matching in that domain.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox skillsList = new VBox(8);

        List<String[]> domainMeta = List.of(
                new String[]{"dsa", "Data Structures & Algorithms", "Core problem solving, trees, graphs, sorting"},
                new String[]{"ai", "Artificial Intelligence & ML", "Machine learning concepts, neural networks, data analysis"},
                new String[]{"robotics", "Robotics & Embedded Systems", "Hardware controllers, kinematics, sensor integration"},
                new String[]{"design", "System Design & UI/UX", "Architecture patterns, modularity, user experience"}
        );

        for (String[] meta : domainMeta) {
            String domainKey = meta[0];
            String domainName = meta[1];
            String domainDesc = meta[2];

            HBox row = new HBox(12);
            row.getStyleClass().add("skill-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox infoBox = new VBox(2);
            Label nameLbl = new Label(domainName);
            nameLbl.getStyleClass().add("skill-name");

            Label descLbl = new Label(domainDesc);
            descLbl.getStyleClass().add("stat-note");
            infoBox.getChildren().addAll(nameLbl, descLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            int score = user.getScore(domainKey);
            Label scoreLbl;
            Label statusBadge;

            if (user.hasAttempted(domainKey)) {
                scoreLbl = new Label(score + " / 10");
                scoreLbl.getStyleClass().add("skill-score-label");

                if (user.isQualified(domainKey)) {
                    statusBadge = new Label("✅ Qualified");
                    statusBadge.getStyleClass().add("badge-qualified");
                } else {
                    statusBadge = new Label("❌ Score < 7");
                    statusBadge.getStyleClass().add("badge-attempted-unqualified");
                }
            } else {
                scoreLbl = new Label("Not Attempted");
                scoreLbl.getStyleClass().add("skill-score-label");

                statusBadge = new Label("⏳ Pending");
                statusBadge.getStyleClass().add("badge-not-attempted");
            }

            Button actionBtn = new Button(user.hasAttempted(domainKey) ? "Retake / Review" : "Take Test →");
            actionBtn.getStyleClass().add("btn-secondary");
            actionBtn.setOnAction(e -> {
                if (testLaunchHandler != null) {
                    testLaunchHandler.accept(domainKey);
                } else {
                    navigate(NavSection.ASSESSMENTS);
                }
            });

            row.getChildren().addAll(infoBox, spacer, scoreLbl, statusBadge, actionBtn);
            skillsList.getChildren().add(row);
        }

        section.getChildren().addAll(headingBox, skillsList);
        return section;
    }

    private long countPendingRequests() {
        try {
            List<MatchManager.MatchRequest> requests = FileHandler.loadMatchRequests();
            return requests.stream()
                    .filter(r -> r.getReceiver().equalsIgnoreCase(user.getUsername()))
                    .filter(r -> r.getStatus() == MatchManager.MatchStatus.PENDING)
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private void navigate(NavSection target) {
        if (navigationHandler != null) {
            navigationHandler.accept(target);
        }
    }

    public Node getView() {
        return container;
    }
}
