package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern peer discovery view that recommends compatible students based on
 * shared qualified skill tracks (MIN_SHARED_INTERESTS = 1).
 */
public class FindPeersView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User currentUser;
    private final Consumer<NavSection> navigationHandler;

    public FindPeersView(SkillSyncApp app, User currentUser, Consumer<NavSection> navigationHandler) {
        this.app = app;
        this.currentUser = currentUser;
        this.navigationHandler = navigationHandler;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        buildView();
    }

    private void buildView() {
        container.getChildren().clear();

        // 1. Heading Bar with Refresh Action
        HBox headerRow = buildHeaderRow();
        container.getChildren().add(headerRow);

        // 2. State Evaluations
        if (currentUser.isMatched()) {
            container.getChildren().add(buildAlreadyMatchedBanner());
            return;
        }

        if (currentUser.getQualifiedInterests().isEmpty()) {
            container.getChildren().add(buildUnqualifiedNotice());
            return;
        }

        // 3. Compatible Candidates List
        List<User> compatiblePeers = MatchManager.getCompatibleUsers(currentUser, app.getUsers());

        if (compatiblePeers.isEmpty()) {
            container.getChildren().add(buildEmptyStateNotice());
        } else {
            VBox peersGrid = buildPeersGrid(compatiblePeers);
            container.getChildren().add(peersGrid);
        }
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Compatible Peer Recommendations");
        title.getStyleClass().add("card-section-title");
        title.setStyle("-fx-font-size: 20px;");

        Label sub = new Label("Students with matching qualified competencies ready for team pairing.");
        sub.getStyleClass().add("card-section-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄  Refresh Recommendations");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            app.refreshUsers();
            buildView();
        });

        row.getChildren().addAll(titleBox, spacer, refreshBtn);
        return row;
    }

    private VBox buildAlreadyMatchedBanner() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-section");
        card.setStyle("-fx-border-color: #a7f3d0; -fx-background-color: #f0fdf4;");

        Label badge = new Label("TEAM LOCKED");
        badge.getStyleClass().add("badge-qualified");

        Label title = new Label("You Are Already Matched!");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #065f46;");

        Label desc = new Label("You are currently paired with " + currentUser.getMatchedWith() + ". You and your partner are locked in for collaboration.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");

        HBox btnRow = new HBox(10);
        Button profileBtn = new Button("👤  View Team Profile");
        profileBtn.getStyleClass().add("btn-primary");
        profileBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.PROFILE);
            }
        });

        Button requestsBtn = new Button("✉️  View Requests History");
        requestsBtn.getStyleClass().add("btn-secondary");
        requestsBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.REQUESTS);
            }
        });

        btnRow.getChildren().addAll(profileBtn, requestsBtn);
        card.getChildren().addAll(badge, title, desc, btnRow);
        return card;
    }

    private VBox buildUnqualifiedNotice() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card-section");
        card.setStyle("-fx-border-color: #fde68a; -fx-background-color: #fffbeb;");

        Label badge = new Label("QUALIFICATION REQUIRED");
        badge.getStyleClass().add("placeholder-badge");

        Label title = new Label("Complete a Skill Assessment to Unlock Matching");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #92400e;");

        Label desc = new Label("Intelligent peer compatibility requires at least 1 qualified skill track (score ≥ 7/10 in DSA, AI, Robotics, or Design). Once you pass an assessment, compatible peers will appear here automatically.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #b45309;");
        desc.setWrapText(true);

        Button startTestBtn = new Button("📝  Take Skill Assessment Now →");
        startTestBtn.getStyleClass().add("btn-primary");
        startTestBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.ASSESSMENTS);
            }
        });

        card.getChildren().addAll(badge, title, desc, startTestBtn);
        return card;
    }

    private VBox buildEmptyStateNotice() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-section");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(40, 20, 40, 20));

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size: 36px;");

        Label title = new Label("No Compatible Peers Available Right Now");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        Label desc = new Label("All currently available students are either already matched or do not yet share your qualified tracks (" + String.join(", ", currentUser.getQualifiedInterests()).toUpperCase() + "). Check back soon or qualify in additional skills!");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        desc.setWrapText(true);

        card.getChildren().addAll(icon, title, desc);
        return card;
    }

    private VBox buildPeersGrid(List<User> peers) {
        VBox list = new VBox(14);

        for (User peer : peers) {
            VBox card = new VBox(14);
            card.getStyleClass().add("peer-card");

            // Top Header: Avatar + Name + Compatibility Score
            HBox topRow = new HBox(14);
            topRow.setAlignment(Pos.CENTER_LEFT);

            String initial = peer.getUsername().isEmpty() ? "P" : peer.getUsername().substring(0, 1).toUpperCase();
            Label avatar = new Label(initial);
            avatar.getStyleClass().add("peer-avatar");

            VBox metaBox = new VBox(2);
            Label nameLbl = new Label(peer.getUsername());
            nameLbl.getStyleClass().add("peer-name");

            Label emailLbl = new Label(peer.getEmail());
            emailLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            metaBox.getChildren().addAll(nameLbl, emailLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            List<String> sharedInterests = MatchManager.getSharedInterests(currentUser, peer);
            Label matchPill = new Label("⭐ " + sharedInterests.size() + " Shared Skill" + (sharedInterests.size() > 1 ? "s" : ""));
            matchPill.getStyleClass().add("compatibility-pill");

            topRow.getChildren().addAll(avatar, metaBox, spacer, matchPill);

            // Shared Skills Chips Row
            VBox sharedBox = new VBox(6);
            Label sharedTitle = new Label("Shared Qualified Competencies:");
            sharedTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

            HBox chipsRow = new HBox(8);
            chipsRow.setAlignment(Pos.CENTER_LEFT);
            for (String interest : sharedInterests) {
                Label chip = new Label(interest.toUpperCase());
                chip.getStyleClass().add("shared-skill-chip");
                chipsRow.getChildren().add(chip);
            }
            sharedBox.getChildren().addAll(sharedTitle, chipsRow);

            // Additional Candidate Metrics Row
            HBox metricsRow = new HBox(16);
            metricsRow.setAlignment(Pos.CENTER_LEFT);

            String avgStr = peer.getAverageScore() > 0 ? String.format("%.1f/10", peer.getAverageScore()) : "N/A";
            Label avgLbl = new Label("📊 Average Score: " + avgStr);
            avgLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Label totalQualLbl = new Label("⭐ Total Qualified: " + peer.getQualifiedInterests().size() + "/4");
            totalQualLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Region bottomSpacer = new Region();
            HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

            // Action Button
            boolean pending = FileHandler.hasPendingRequest(currentUser.getUsername(), peer.getUsername());
            Button requestBtn = new Button();

            if (pending) {
                requestBtn.setText("⏳  Request Pending");
                requestBtn.getStyleClass().add("badge-pending");
                requestBtn.setDisable(true);
            } else {
                requestBtn.setText("Send Match Request ✉️");
                requestBtn.getStyleClass().add("btn-primary");
                requestBtn.setOnAction(e -> handleSendRequest(peer, sharedInterests));
            }

            metricsRow.getChildren().addAll(avgLbl, totalQualLbl, bottomSpacer, requestBtn);

            card.getChildren().addAll(topRow, sharedBox, metricsRow);
            list.getChildren().add(card);
        }

        return list;
    }

    private void handleSendRequest(User targetUser, List<String> sharedInterests) {
        String chosenInterest;

        if (sharedInterests.size() == 1) {
            chosenInterest = sharedInterests.get(0);
        } else {
            ChoiceDialog<String> dialog = new ChoiceDialog<>(sharedInterests.get(0), sharedInterests);
            dialog.setTitle("Select Collaboration Interest");
            dialog.setHeaderText("Choose the primary skill track for teaming up with " + targetUser.getUsername() + ":");
            dialog.setContentText("Skill Track:");
            attachDialogStyle(dialog);
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            chosenInterest = result.get();
        }

        boolean sent = MatchManager.sendMatchRequest(currentUser, targetUser, chosenInterest);

        if (sent) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Request Sent");
            alert.setHeaderText("Match Request Sent Successfully!");
            alert.setContentText("Your partnership request for " + chosenInterest.toUpperCase() + " has been sent to " + targetUser.getUsername() + ".");
            attachDialogStyle(alert);
            alert.showAndWait();
            buildView();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Unable to Send Request");
            alert.setHeaderText(null);
            alert.setContentText("Could not send match request. A request may already exist, or one of you is already paired.");
            attachDialogStyle(alert);
            alert.showAndWait();
            buildView();
        }
    }

    private void attachDialogStyle(javafx.scene.control.Dialog<?> dialog) {
        if (getClass().getResource("/styles/skillsync.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/skillsync.css").toExternalForm());
        }
    }

    public Node getView() {
        return container;
    }
}
