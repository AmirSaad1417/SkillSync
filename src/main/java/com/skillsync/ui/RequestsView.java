package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modern requests management view displaying incoming invitations (with Accept/Decline),
 * outgoing invitations tracking, and confirmed finalized team pairings.
 */
public class RequestsView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User currentUser;
    private final Consumer<NavSection> navigationHandler;
    private final Runnable onMatchChangedCallback;

    public RequestsView(SkillSyncApp app,
                        User currentUser,
                        Consumer<NavSection> navigationHandler,
                        Runnable onMatchChangedCallback) {
        this.app = app;
        this.currentUser = currentUser;
        this.navigationHandler = navigationHandler;
        this.onMatchChangedCallback = onMatchChangedCallback;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        buildView();
    }

    private void buildView() {
        container.getChildren().clear();

        // 1. Header Bar with Refresh Action
        HBox headerRow = buildHeaderRow();
        container.getChildren().add(headerRow);

        // 2. Active Match Banner (if paired)
        if (currentUser.isMatched()) {
            container.getChildren().add(buildActiveMatchBanner());
        }

        List<MatchManager.MatchRequest> allRequests = FileHandler.loadMatchRequests();

        // 3. Section: Incoming Requests
        VBox incomingSection = buildIncomingRequestsSection(allRequests);

        // 4. Section: Outgoing Requests
        VBox outgoingSection = buildOutgoingRequestsSection(allRequests);

        // 5. Section: Finalized Matches
        VBox finalizedSection = buildFinalizedMatchesSection();

        container.getChildren().addAll(incomingSection, outgoingSection, finalizedSection);
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Match Requests & Team Partnerships");
        title.getStyleClass().add("card-section-title");
        title.setStyle("-fx-font-size: 20px;");

        Label sub = new Label("Review received pairing invitations, monitor your sent requests, and view confirmed teams.");
        sub.getStyleClass().add("card-section-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄  Refresh Requests");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> {
            app.refreshUsers();
            buildView();
        });

        row.getChildren().addAll(titleBox, spacer, refreshBtn);
        return row;
    }

    private VBox buildActiveMatchBanner() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card-section");
        card.setStyle("-fx-border-color: #a7f3d0; -fx-background-color: #f0fdf4;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("CONFIRMED PARTNERSHIP");
        badge.getStyleClass().add("badge-qualified");

        Label partnerLbl = new Label("You are currently paired with " + currentUser.getMatchedWith() + "!");
        partnerLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #065f46;");

        topRow.getChildren().addAll(badge, partnerLbl);

        Label note = new Label("You and your teammate are locked in. You cannot accept or send new partnership invitations while matched.");
        note.setStyle("-fx-font-size: 12px; -fx-text-fill: #047857;");

        card.getChildren().addAll(topRow, note);
        return card;
    }

    private VBox buildIncomingRequestsSection(List<MatchManager.MatchRequest> allRequests) {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        List<MatchManager.MatchRequest> incoming = allRequests.stream()
                .filter(r -> r.getReceiver().equalsIgnoreCase(currentUser.getUsername()))
                .toList();

        long pendingCount = incoming.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.PENDING).count();

        VBox headingBox = new VBox(2);
        Label title = new Label("Incoming Pairing Invitations (" + pendingCount + " Pending)");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Requests sent to you by peers wanting to partner on projects.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox list = new VBox(10);

        if (incoming.isEmpty()) {
            Label emptyLbl = new Label("📭 No incoming match requests received yet.");
            emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 8px 0;");
            list.getChildren().add(emptyLbl);
        } else {
            for (MatchManager.MatchRequest req : incoming) {
                HBox card = new HBox(12);
                card.getStyleClass().add("request-card");
                card.setAlignment(Pos.CENTER_LEFT);

                String initial = req.getSender().isEmpty() ? "S" : req.getSender().substring(0, 1).toUpperCase();
                Label avatar = new Label(initial);
                avatar.getStyleClass().add("peer-avatar");
                avatar.setStyle("-fx-min-width: 38px; -fx-min-height: 38px; -fx-max-width: 38px; -fx-max-height: 38px; -fx-font-size: 14px;");

                VBox metaBox = new VBox(2);
                Label nameLbl = new Label("From: " + req.getSender());
                nameLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

                Label trackLbl = new Label("Requested Collaboration Track: " + req.getInterest().toUpperCase());
                trackLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
                metaBox.getChildren().addAll(nameLbl, trackLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                card.getChildren().addAll(avatar, metaBox, spacer);

                if (req.getStatus() == MatchManager.MatchStatus.PENDING) {
                    if (currentUser.isMatched()) {
                        Label disabledMsg = new Label("⚠️ Already Matched");
                        disabledMsg.getStyleClass().add("badge-pending");
                        card.getChildren().add(disabledMsg);
                    } else {
                        Button acceptBtn = new Button("✅ Accept");
                        acceptBtn.getStyleClass().add("btn-primary");
                        acceptBtn.setOnAction(e -> handleAccept(req));

                        Button declineBtn = new Button("❌ Decline");
                        declineBtn.getStyleClass().add("btn-danger");
                        declineBtn.setOnAction(e -> handleDecline(req));

                        card.getChildren().addAll(acceptBtn, declineBtn);
                    }
                } else if (req.getStatus() == MatchManager.MatchStatus.ACCEPTED) {
                    Label acceptedBadge = new Label("🤝 Accepted");
                    acceptedBadge.getStyleClass().add("badge-accepted");
                    card.getChildren().add(acceptedBadge);
                } else {
                    Label declinedBadge = new Label("❌ Declined");
                    declinedBadge.getStyleClass().add("badge-declined");
                    card.getChildren().add(declinedBadge);
                }

                list.getChildren().add(card);
            }
        }

        section.getChildren().addAll(headingBox, list);
        return section;
    }

    private VBox buildOutgoingRequestsSection(List<MatchManager.MatchRequest> allRequests) {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        List<MatchManager.MatchRequest> outgoing = allRequests.stream()
                .filter(r -> r.getSender().equalsIgnoreCase(currentUser.getUsername()))
                .toList();

        VBox headingBox = new VBox(2);
        Label title = new Label("Sent Invitations (" + outgoing.size() + ")");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Track the status of partnership invitations you have sent to peers.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox list = new VBox(10);

        if (outgoing.isEmpty()) {
            Label emptyLbl = new Label("📭 You haven't sent any partnership invitations yet.");
            emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 8px 0;");
            list.getChildren().add(emptyLbl);
        } else {
            for (MatchManager.MatchRequest req : outgoing) {
                HBox card = new HBox(12);
                card.getStyleClass().add("request-card");
                card.setAlignment(Pos.CENTER_LEFT);

                VBox metaBox = new VBox(2);
                Label nameLbl = new Label("Sent to: " + req.getReceiver());
                nameLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #0f172a;");

                Label trackLbl = new Label("Track: " + req.getInterest().toUpperCase());
                trackLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                metaBox.getChildren().addAll(nameLbl, trackLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label statusBadge = new Label();
                if (req.getStatus() == MatchManager.MatchStatus.PENDING) {
                    statusBadge.setText("⏳ Awaiting Response");
                    statusBadge.getStyleClass().add("badge-pending");
                } else if (req.getStatus() == MatchManager.MatchStatus.ACCEPTED) {
                    statusBadge.setText("🤝 Accepted & Paired");
                    statusBadge.getStyleClass().add("badge-accepted");
                } else {
                    statusBadge.setText("❌ Declined by Partner");
                    statusBadge.getStyleClass().add("badge-declined");
                }

                card.getChildren().addAll(metaBox, spacer, statusBadge);
                list.getChildren().add(card);
            }
        }

        section.getChildren().addAll(headingBox, list);
        return section;
    }

    private VBox buildFinalizedMatchesSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        List<String[]> allFinalMatches = FileHandler.loadFinalMatches();
        List<String[]> myFinalMatches = allFinalMatches.stream()
                .filter(m -> m.length >= 3 &&
                        (m[0].equalsIgnoreCase(currentUser.getUsername()) || m[1].equalsIgnoreCase(currentUser.getUsername())))
                .toList();

        VBox headingBox = new VBox(2);
        Label title = new Label("Confirmed Team Partnerships");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Officially confirmed project partnerships persisted to matches.csv.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox list = new VBox(10);

        if (myFinalMatches.isEmpty()) {
            Label emptyLbl = new Label("No confirmed team partnerships recorded yet.");
            emptyLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 8px 0;");
            list.getChildren().add(emptyLbl);
        } else {
            for (String[] m : myFinalMatches) {
                HBox card = new HBox(12);
                card.getStyleClass().add("final-match-card");
                card.setAlignment(Pos.CENTER_LEFT);

                String partner = m[0].equalsIgnoreCase(currentUser.getUsername()) ? m[1] : m[0];
                String interest = m[2];

                VBox metaBox = new VBox(2);
                Label teamLbl = new Label("🤝 " + currentUser.getUsername() + " ↔ " + partner);
                teamLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #065f46;");

                Label trackLbl = new Label("Confirmed Skill Domain: " + interest.toUpperCase());
                trackLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #047857;");
                metaBox.getChildren().addAll(teamLbl, trackLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label verifiedPill = new Label("VERIFIED TEAM");
                verifiedPill.getStyleClass().add("badge-accepted");

                card.getChildren().addAll(metaBox, spacer, verifiedPill);
                list.getChildren().add(card);
            }
        }

        section.getChildren().addAll(headingBox, list);
        return section;
    }

    private void handleAccept(MatchManager.MatchRequest req) {
        boolean ok = MatchManager.acceptMatchRequest(req, currentUser, app.getUsers());

        if (ok) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Match Confirmed!");
            alert.setHeaderText("Team Partnership Confirmed! 🎉");
            alert.setContentText("You and " + req.getSender() + " are now paired for " + req.getInterest().toUpperCase() + ".\nThis match has been recorded in matches.csv and your profile has been updated.");
            attachDialogStyle(alert);
            alert.showAndWait();

            app.refreshUsers();
            if (onMatchChangedCallback != null) {
                onMatchChangedCallback.run();
            }
            buildView();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Unable to Accept");
            alert.setHeaderText(null);
            alert.setContentText("Could not accept match request. Either you or the sender is already paired with someone else.");
            attachDialogStyle(alert);
            alert.showAndWait();
            buildView();
        }
    }

    private void handleDecline(MatchManager.MatchRequest req) {
        boolean ok = MatchManager.declineMatchRequest(req);
        if (ok) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Request Declined");
            alert.setHeaderText(null);
            alert.setContentText("The match request from " + req.getSender() + " has been declined.");
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
