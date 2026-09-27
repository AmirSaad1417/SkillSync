package com.skillsync.ui;

import com.skillsync.SkillSyncApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * Administrative Application Shell:
 * Provides the persistent sidebar navigation, administrative top header, security status, and swappable viewport.
 */
public class AdminShellView {

    private final SkillSyncApp app;
    private final BorderPane root;

    private AdminSection currentSection = AdminSection.DASHBOARD;
    private final Map<AdminSection, Button> navButtons = new EnumMap<>(AdminSection.class);

    private Label headerTitle;
    private Label headerSubtitle;
    private ScrollPane contentScrollPane;

    public AdminShellView(SkillSyncApp app) {
        this.app = app;
        this.root = new BorderPane();
        this.root.getStyleClass().add("app-shell");

        buildLayout();
        navigateTo(AdminSection.DASHBOARD);
    }

    public BorderPane getRoot() {
        return root;
    }

    private void buildLayout() {
        // 1. Left Sidebar
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // 2. Central Area (Header + Scrollable Content Viewport)
        VBox centralContainer = new VBox();
        centralContainer.getStyleClass().add("central-container");

        VBox topHeader = buildHeader();

        contentScrollPane = new ScrollPane();
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(true);
        contentScrollPane.getStyleClass().add("content-scroll-pane");
        VBox.setVgrow(contentScrollPane, Priority.ALWAYS);

        centralContainer.getChildren().addAll(topHeader, contentScrollPane);
        root.setCenter(centralContainer);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(250);
        sidebar.getStyleClass().add("admin-sidebar");

        // Top Brand Box
        VBox brandBox = new VBox(6);
        brandBox.setPadding(new Insets(24, 20, 20, 20));
        brandBox.setAlignment(Pos.CENTER_LEFT);

        HBox logoLine = new HBox(8);
        logoLine.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("⚡");
        logoIcon.setStyle("-fx-font-size: 20px;");

        Label logoText = new Label("SkillSync");
        logoText.getStyleClass().add("admin-brand-logo");

        logoLine.getChildren().addAll(logoIcon, logoText);

        Label adminConsoleBadge = new Label("ADMIN CONSOLE");
        adminConsoleBadge.getStyleClass().add("admin-badge");

        brandBox.getChildren().addAll(logoLine, adminConsoleBadge);

        // Navigation Menu
        VBox navBox = new VBox(4);
        navBox.setPadding(new Insets(12, 12, 12, 12));

        for (AdminSection section : AdminSection.values()) {
            Button navBtn = createNavButton(section);
            navButtons.put(section, navBtn);
            navBox.getChildren().add(navBtn);
        }

        // Vertical Spring Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Admin User Identity Area (Bottom)
        VBox adminUserCard = buildAdminUserCard();
        VBox.setMargin(adminUserCard, new Insets(12));

        sidebar.getChildren().addAll(brandBox, navBox, spacer, adminUserCard);
        return sidebar;
    }

    private Button createNavButton(AdminSection section) {
        Button btn = new Button(section.getIcon() + "  " + section.getTitle());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("admin-nav-item");
        btn.setOnAction(e -> navigateTo(section));
        return btn;
    }

    private VBox buildAdminUserCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("admin-user-card");

        HBox userRow = new HBox(10);
        userRow.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("A");
        avatar.getStyleClass().add("admin-avatar-small");

        VBox infoBox = new VBox(2);
        Label nameLabel = new Label("admin");
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #ffffff;");

        Label roleBadge = new Label("Administrator");
        roleBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #a5b4fc;");

        infoBox.getChildren().addAll(nameLabel, roleBadge);
        userRow.getChildren().addAll(avatar, infoBox);

        Button logoutBtn = new Button("Sign Out 🚪");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.getStyleClass().add("admin-btn-logout");
        logoutBtn.setOnAction(e -> {
            app.logout();
        });

        card.getChildren().addAll(userRow, logoutBtn);
        return card;
    }

    private VBox buildHeader() {
        VBox headerContainer = new VBox();
        headerContainer.getStyleClass().add("top-header-bar");
        headerContainer.setPadding(new Insets(18, 32, 18, 32));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        headerTitle = new Label();
        headerTitle.getStyleClass().add("header-title");

        headerSubtitle = new Label();
        headerSubtitle.getStyleClass().add("header-subtitle");

        titleBox.getChildren().addAll(headerTitle, headerSubtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            app.refreshUsers();
            navigateTo(currentSection);
        });

        Label securityPill = new Label("🛡️ Elevated Access");
        securityPill.getStyleClass().add("admin-security-pill");

        headerRow.getChildren().addAll(titleBox, spacer, refreshBtn, securityPill);
        headerContainer.getChildren().add(headerRow);
        return headerContainer;
    }

    public void navigateTo(AdminSection section) {
        this.currentSection = section;

        // Update nav active classes
        for (Map.Entry<AdminSection, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey() == section) {
                if (!btn.getStyleClass().contains("admin-nav-item-active")) {
                    btn.getStyleClass().add("admin-nav-item-active");
                }
            } else {
                btn.getStyleClass().remove("admin-nav-item-active");
            }
        }

        // Update Header
        headerTitle.setText(section.getTitle());
        headerSubtitle.setText(section.getSubtitle());

        // Swap Content
        if (section == AdminSection.DASHBOARD) {
            AdminDashboardView dashboardView = new AdminDashboardView(app, this::navigateTo);
            contentScrollPane.setContent(dashboardView.getView());
        } else if (section == AdminSection.USERS) {
            AdminUsersView usersView = new AdminUsersView(app);
            contentScrollPane.setContent(usersView.getView());
        } else if (section == AdminSection.PARTICIPATION) {
            AdminStatsView statsView = new AdminStatsView(app);
            contentScrollPane.setContent(statsView.getView());
        } else if (section == AdminSection.REQUESTS) {
            AdminRequestsView requestsView = new AdminRequestsView(app);
            contentScrollPane.setContent(requestsView.getView());
        } else if (section == AdminSection.MATCHES) {
            AdminMatchesView matchesView = new AdminMatchesView(app);
            contentScrollPane.setContent(matchesView.getView());
        } else if (section == AdminSection.LEADERBOARD) {
            LeaderboardView leaderboardView = new LeaderboardView(app, null, sec -> navigateTo(AdminSection.DASHBOARD));
            contentScrollPane.setContent(leaderboardView.getView());
        }

        // Reset scroll position to top
        contentScrollPane.setVvalue(0.0);
    }
}
