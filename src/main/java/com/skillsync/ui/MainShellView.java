package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window shell featuring a modern responsive sidebar,
 * top header, user identity area, and swappable content viewport.
 */
public class MainShellView {

    private final SkillSyncApp app;
    private final User currentUser;
    private final BorderPane root;

    private final Label headerTitle;
    private final Label headerSubtitle;
    private final Label headerStatusPill;
    private final ScrollPane contentScrollPane;

    private final Map<NavSection, Button> navButtons = new EnumMap<>(NavSection.class);
    private NavSection currentSection = NavSection.DASHBOARD;

    public MainShellView(SkillSyncApp app, User user) {
        this.app = app;
        this.currentUser = user;
        this.root = new BorderPane();
        this.root.getStyleClass().add("main-shell");

        // 1. Sidebar on the left
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // 2. Central Area (Header on top, Scrollable Content in center)
        BorderPane centerArea = new BorderPane();

        // Header
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        this.headerTitle = new Label();
        headerTitle.getStyleClass().add("header-title");

        this.headerSubtitle = new Label();
        headerSubtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(headerTitle, headerSubtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        this.headerStatusPill = new Label();
        updateHeaderStatusPill();

        header.getChildren().addAll(titleBox, headerSpacer, headerStatusPill);
        centerArea.setTop(header);

        // Content ScrollPane
        this.contentScrollPane = new ScrollPane();
        contentScrollPane.getStyleClass().add("content-scroll");
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(false);
        centerArea.setCenter(contentScrollPane);

        root.setCenter(centerArea);

        // Initialize at Dashboard
        navigateTo(NavSection.DASHBOARD);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Brand box
        HBox brandBox = new HBox(10);
        brandBox.getStyleClass().add("sidebar-brand-box");
        brandBox.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("SS");
        logoIcon.getStyleClass().add("sidebar-logo-icon");

        VBox brandMeta = new VBox(1);
        Label brandTitle = new Label("SkillSync");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSub = new Label("Student Platform");
        brandSub.getStyleClass().add("sidebar-brand-sub");

        brandMeta.getChildren().addAll(brandTitle, brandSub);
        brandBox.getChildren().addAll(logoIcon, brandMeta);

        // Section label
        Label navLabel = new Label("NAVIGATION");
        navLabel.getStyleClass().add("sidebar-section-label");

        // Nav items container
        VBox navContainer = new VBox(4);
        navContainer.getStyleClass().add("sidebar-nav-container");

        for (NavSection section : NavSection.values()) {
            Button btn = new Button(section.getIcon() + "   " + section.getTitle());
            btn.getStyleClass().add("nav-item");
            btn.setOnAction(e -> navigateTo(section));
            navButtons.put(section, btn);
            navContainer.getChildren().add(btn);
        }

        // Vertical spacer pushing user identity card to bottom
        Region verticalSpacer = new Region();
        VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

        // User Identity Box
        VBox userCard = buildUserIdentityCard();

        sidebar.getChildren().addAll(brandBox, navLabel, navContainer, verticalSpacer, userCard);
        return sidebar;
    }

    private VBox buildUserIdentityCard() {
        VBox userBox = new VBox(10);
        userBox.getStyleClass().add("user-identity-box");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        String initial = currentUser.getUsername().isEmpty()
                ? "S"
                : currentUser.getUsername().substring(0, 1).toUpperCase();

        Label avatar = new Label(initial);
        avatar.getStyleClass().add("user-avatar-circle");

        VBox userText = new VBox(2);
        HBox nameBadgeRow = new HBox(6);
        nameBadgeRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(currentUser.getUsername());
        nameLbl.getStyleClass().add("user-name-label");

        Label roleBadge = new Label("Student");
        roleBadge.getStyleClass().add("user-badge-student");

        nameBadgeRow.getChildren().addAll(nameLbl, roleBadge);

        Label emailLbl = new Label(currentUser.getEmail());
        emailLbl.getStyleClass().add("user-email-label");

        userText.getChildren().addAll(nameBadgeRow, emailLbl);
        metaRow.getChildren().addAll(avatar, userText);

        Button logoutBtn = new Button("🚪  Sign Out");
        logoutBtn.getStyleClass().add("btn-logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> app.logout());

        userBox.getChildren().addAll(metaRow, logoutBtn);
        return userBox;
    }

    private String pendingAssessmentDomain = null;

    public void launchAssessment(String domain) {
        this.pendingAssessmentDomain = domain;
        navigateTo(NavSection.ASSESSMENTS);
    }

    public void showAssessmentResult(String domain, int score, List<com.skillsync.Question> questions, Map<Integer, String> answers) {
        headerTitle.setText("Assessment Result — " + domain.toUpperCase());
        headerSubtitle.setText("Score evaluation and detailed answer key review");
        updateHeaderStatusPill();

        AssessmentResultView resultView = new AssessmentResultView(
                app,
                currentUser,
                domain,
                score,
                questions,
                answers,
                this::navigateTo,
                this::launchAssessment
        );
        contentScrollPane.setContent(resultView.getView());
        contentScrollPane.setVvalue(0.0);
    }

    public void navigateTo(NavSection section) {
        this.currentSection = section;

        // Update nav active classes
        for (Map.Entry<NavSection, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey() == section) {
                if (!btn.getStyleClass().contains("nav-item-active")) {
                    btn.getStyleClass().add("nav-item-active");
                }
            } else {
                btn.getStyleClass().remove("nav-item-active");
            }
        }

        // Update Header
        headerTitle.setText(section.getTitle());
        headerSubtitle.setText(section.getSubtitle());
        updateHeaderStatusPill();

        // Swap Content
        if (section == NavSection.DASHBOARD) {
            DashboardView dashboardView = new DashboardView(app, currentUser, this::navigateTo, this::launchAssessment);
            contentScrollPane.setContent(dashboardView.getView());
        } else if (section == NavSection.PROFILE) {
            ProfileView profileView = new ProfileView(app, currentUser, this::navigateTo, this::launchAssessment);
            contentScrollPane.setContent(profileView.getView());
        } else if (section == NavSection.ASSESSMENTS) {
            String domainToStart = pendingAssessmentDomain;
            pendingAssessmentDomain = null; // consumed
            AssessmentView assessmentView = new AssessmentView(app, currentUser, domainToStart, this::navigateTo, this::showAssessmentResult);
            contentScrollPane.setContent(assessmentView.getView());
        } else if (section == NavSection.FIND_PEERS) {
            FindPeersView findPeersView = new FindPeersView(app, currentUser, this::navigateTo);
            contentScrollPane.setContent(findPeersView.getView());
        } else if (section == NavSection.REQUESTS) {
            RequestsView requestsView = new RequestsView(app, currentUser, this::navigateTo, this::updateHeaderStatusPill);
            contentScrollPane.setContent(requestsView.getView());
        } else if (section == NavSection.LEADERBOARD) {
            LeaderboardView leaderboardView = new LeaderboardView(app, currentUser, this::navigateTo);
            contentScrollPane.setContent(leaderboardView.getView());
        } else {
            PlaceholderView placeholderView = new PlaceholderView(section, this::navigateTo);
            contentScrollPane.setContent(placeholderView.getView());
        }

        // Reset scroll position to top
        contentScrollPane.setVvalue(0.0);
    }

    private void updateHeaderStatusPill() {
        if (currentUser.isMatched()) {
            headerStatusPill.setText("🤝 Matched with " + currentUser.getMatchedWith());
            headerStatusPill.getStyleClass().removeAll("status-pill-unmatched", "status-pill-matched");
            headerStatusPill.getStyleClass().add("status-pill-matched");
        } else {
            headerStatusPill.setText("🔍 Looking for Teammates");
            headerStatusPill.getStyleClass().removeAll("status-pill-unmatched", "status-pill-matched");
            headerStatusPill.getStyleClass().add("status-pill-unmatched");
        }
    }

    public Node getView() {
        return root;
    }
}
