package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modern student profile view displaying student credentials, overall academic statistics,
 * visual proficiency progress bars for each domain, and team status.
 */
public class ProfileView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User user;
    private final Consumer<NavSection> navigationHandler;
    private final Consumer<String> testLaunchHandler;

    public ProfileView(SkillSyncApp app,
                       User user,
                       Consumer<NavSection> navigationHandler,
                       Consumer<String> testLaunchHandler) {
        this.app = app;
        this.user = user;
        this.navigationHandler = navigationHandler;
        this.testLaunchHandler = testLaunchHandler;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        buildProfileView();
    }

    private void buildProfileView() {
        // 1. Header Card (Avatar + Identity + Match Status)
        VBox headerCard = buildHeaderCard();

        // 2. Academic Stats Grid
        HBox statsGrid = buildStatsGrid();

        // 3. Domain Competencies with Progress Bars
        VBox domainSection = buildDomainProficiencySection();

        // 4. Team & Collaboration Details
        VBox teamSection = buildTeamCollaborationSection();

        container.getChildren().addAll(headerCard, statsGrid, domainSection, teamSection);
    }

    private VBox buildHeaderCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("profile-header-card");

        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        String initial = user.getUsername().isEmpty()
                ? "S"
                : user.getUsername().substring(0, 1).toUpperCase();

        Label avatar = new Label(initial);
        avatar.getStyleClass().add("profile-avatar-large");

        VBox metaBox = new VBox(4);

        HBox titleBadgeRow = new HBox(10);
        titleBadgeRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(user.getUsername());
        nameLbl.getStyleClass().add("profile-user-name");

        Label roleBadge = new Label("Student Account");
        roleBadge.getStyleClass().add("user-badge-student");

        titleBadgeRow.getChildren().addAll(nameLbl, roleBadge);

        Label emailLbl = new Label(user.getEmail());
        emailLbl.getStyleClass().add("profile-user-email");

        metaBox.getChildren().addAll(titleBadgeRow, emailLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label matchPill = new Label(user.isMatched()
                ? "🤝 Matched with " + user.getMatchedWith()
                : "🔍 Seeking Teammates");
        matchPill.getStyleClass().add(user.isMatched() ? "status-pill-matched" : "status-pill-unmatched");

        mainRow.getChildren().addAll(avatar, metaBox, spacer, matchPill);
        card.getChildren().add(mainRow);
        return card;
    }

    private HBox buildStatsGrid() {
        HBox grid = new HBox(16);
        grid.getStyleClass().add("stats-grid");

        int attemptedCount = user.getAttemptedInterests().size();
        int qualifiedCount = user.getQualifiedInterests().size();

        // 1. Overall GPA / Average
        String avgStr = attemptedCount > 0
                ? String.format("%.1f / 10", user.getAverageScore())
                : "N/A";
        VBox avgCard = createMetricCard("📊", "ACADEMIC AVERAGE", avgStr, attemptedCount + " / 4 assessments taken");

        // 2. Qualified Domains
        String qualStr = qualifiedCount + " / 4";
        VBox qualCard = createMetricCard("⭐", "QUALIFIED DOMAINS", qualStr, "Pass standard: score ≥ 7");

        // 3. Qualification Rate
        String rateStr = attemptedCount > 0
                ? Math.round((double) qualifiedCount / attemptedCount * 100) + "%"
                : "N/A";
        VBox rateCard = createMetricCard("🎯", "PASS SUCCESS RATE", rateStr, "Based on attempted tests");

        // 4. Team Status
        String teamStr = user.isMatched() ? "Paired" : "Single";
        VBox teamCard = createMetricCard("🤝", "COLLABORATION", teamStr, user.isMatched() ? "Partner: " + user.getMatchedWith() : "Ready for requests");

        HBox.setHgrow(avgCard, Priority.ALWAYS);
        HBox.setHgrow(qualCard, Priority.ALWAYS);
        HBox.setHgrow(rateCard, Priority.ALWAYS);
        HBox.setHgrow(teamCard, Priority.ALWAYS);

        grid.getChildren().addAll(avgCard, qualCard, rateCard, teamCard);
        return grid;
    }

    private VBox createMetricCard(String icon, String title, String value, String sub) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-title");

        header.getChildren().addAll(iconLbl, titleLbl);

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-value");

        Label subLbl = new Label(sub);
        subLbl.getStyleClass().add("stat-note");
        subLbl.setWrapText(true);

        card.getChildren().addAll(header, valLbl, subLbl);
        return card;
    }

    private VBox buildDomainProficiencySection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        VBox headingBox = new VBox(2);
        Label title = new Label("Skill Domain Proficiency Breakdown");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Visual evaluation of your skills across all 4 core curriculum tracks.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox domainsList = new VBox(12);

        List<String[]> domains = List.of(
                new String[]{"dsa", "Data Structures & Algorithms", "🧠"},
                new String[]{"ai", "Artificial Intelligence & ML", "🤖"},
                new String[]{"robotics", "Robotics & Systems", "⚡"},
                new String[]{"design", "System Design & UI/UX", "🎨"}
        );

        for (String[] d : domains) {
            String domainKey = d[0];
            String domainTitle = d[1];
            String domainIcon = d[2];

            VBox row = new VBox(8);
            row.getStyleClass().add("skill-row");
            row.setPadding(new javafx.geometry.Insets(14, 18, 14, 18));

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(domainIcon);
            iconLbl.setStyle("-fx-font-size: 16px;");

            Label nameLbl = new Label(domainTitle);
            nameLbl.getStyleClass().add("skill-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            boolean attempted = user.hasAttempted(domainKey);
            boolean qualified = user.isQualified(domainKey);
            int score = user.getScore(domainKey);

            Label scoreLbl = new Label(attempted ? (score + " / 10") : "Not Attempted");
            scoreLbl.getStyleClass().add("skill-score-label");

            Label badge;
            if (qualified) {
                badge = new Label("✅ Qualified (≥ 7)");
                badge.getStyleClass().add("badge-qualified");
            } else if (attempted) {
                badge = new Label("❌ Below Standard (< 7)");
                badge.getStyleClass().add("badge-attempted-unqualified");
            } else {
                badge = new Label("⏳ Not Attempted");
                badge.getStyleClass().add("badge-not-attempted");
            }

            Button actionBtn = new Button(attempted ? "Retake" : "Take Test");
            actionBtn.getStyleClass().add("btn-secondary");
            actionBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
            actionBtn.setOnAction(e -> {
                if (testLaunchHandler != null) {
                    testLaunchHandler.accept(domainKey);
                }
            });

            topRow.getChildren().addAll(iconLbl, nameLbl, spacer, scoreLbl, badge, actionBtn);

            // Progress bar
            double progress = attempted ? Math.max(0, score) / 10.0 : 0.0;
            ProgressBar progressBar = new ProgressBar(progress);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.getStyleClass().add("domain-score-bar");
            if (qualified) {
                progressBar.getStyleClass().add("domain-score-bar-qualified");
            }

            row.getChildren().addAll(topRow, progressBar);
            domainsList.getChildren().add(row);
        }

        section.getChildren().addAll(headingBox, domainsList);
        return section;
    }

    private VBox buildTeamCollaborationSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card-section");

        Label title = new Label("Peer Collaboration Status");
        title.getStyleClass().add("card-section-title");

        String infoText;
        if (user.isMatched()) {
            infoText = "🎉 You are paired with " + user.getMatchedWith() + ". You and your teammate are locked in for project collaboration. Review your shared qualified interests or view match requests for history.";
        } else {
            int qualifiedCount = user.getQualifiedInterests().size();
            if (qualifiedCount == 0) {
                infoText = "You currently have 0 qualified skill tracks. Complete at least one assessment with a score of 7 or higher to unlock intelligent peer compatibility matching.";
            } else {
                infoText = "You are currently qualified in " + String.join(", ", user.getQualifiedInterests()).toUpperCase() + ". You have " + qualifiedCount + " domain(s) eligible for peer pairing! Navigate to 'Find Peers' to discover compatible students.";
            }
        }

        Label infoLbl = new Label(infoText);
        infoLbl.getStyleClass().add("card-section-subtitle");
        infoLbl.setWrapText(true);

        HBox btnRow = new HBox(12);
        if (!user.isMatched()) {
            Button findPeersBtn = new Button("🔍  Explore Compatible Peers");
            findPeersBtn.getStyleClass().add("btn-primary");
            findPeersBtn.setOnAction(e -> {
                if (navigationHandler != null) {
                    navigationHandler.accept(NavSection.FIND_PEERS);
                }
            });
            btnRow.getChildren().add(findPeersBtn);
        }

        Button requestsBtn = new Button("✉️  View Match Requests");
        requestsBtn.getStyleClass().add("btn-secondary");
        requestsBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.REQUESTS);
            }
        });
        btnRow.getChildren().add(requestsBtn);

        section.getChildren().addAll(title, infoLbl, btnRow);
        return section;
    }

    public Node getView() {
        return container;
    }
}
