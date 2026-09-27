package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.MatchManager;
import com.skillsync.SkillSyncApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Match Requests View:
 * Direct implementation of FileHandler.viewAllMatchRequests():
 * Complete audit trail of all peer match invitations, technical domains, and status transitions.
 */
public class AdminRequestsView {

    private final SkillSyncApp app;
    private final VBox root;

    private TextField searchField;
    private MatchManager.MatchStatus filterStatus = null; // null means ALL
    private VBox tableContainer;
    private Label countBadge;

    private Button btnAll;
    private Button btnPending;
    private Button btnAccepted;
    private Button btnDeclined;

    public AdminRequestsView(SkillSyncApp app) {
        this.app = app;

        this.root = new VBox(22);
        this.root.getStyleClass().add("content-container");

        buildView();
    }

    public Parent getView() {
        return root;
    }

    private void buildView() {
        List<MatchManager.MatchRequest> requests = FileHandler.loadMatchRequests();

        // 1. Header Box
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Match Requests Audit Log");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Complete audit trail of all student-to-student peer match invitations and status outcomes.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countBadge = new Label(requests.size() + " Total Invitations");
        countBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #4338ca; -fx-background-color: #eef2ff; -fx-padding: 6px 14px; -fx-background-radius: 9999px; -fx-border-color: #c7d2fe; -fx-border-radius: 9999px;");

        headerBox.getChildren().addAll(titleBox, spacer, countBadge);

        // 2. Filter & Search Controls
        HBox controlsBox = new HBox(12);
        controlsBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Filter by sender or receiver...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTable());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        HBox filterButtonGroup = new HBox(6);
        long pendingCount = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.PENDING).count();
        long acceptedCount = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.ACCEPTED).count();
        long declinedCount = requests.stream().filter(r -> r.getStatus() == MatchManager.MatchStatus.DECLINED).count();

        btnAll = createFilterButton("All (" + requests.size() + ")", null);
        btnPending = createFilterButton("Pending (" + pendingCount + ")", MatchManager.MatchStatus.PENDING);
        btnAccepted = createFilterButton("Accepted (" + acceptedCount + ")", MatchManager.MatchStatus.ACCEPTED);
        btnDeclined = createFilterButton("Declined (" + declinedCount + ")", MatchManager.MatchStatus.DECLINED);

        updateFilterButtonStyles();

        filterButtonGroup.getChildren().addAll(btnAll, btnPending, btnAccepted, btnDeclined);
        controlsBox.getChildren().addAll(searchField, filterSpacer, filterButtonGroup);

        // 3. Table Container
        tableContainer = new VBox(8);
        renderTable();

        root.getChildren().addAll(headerBox, controlsBox, tableContainer);
    }

    private Button createFilterButton(String text, MatchManager.MatchStatus status) {
        Button btn = new Button(text);
        btn.setOnAction(e -> {
            this.filterStatus = status;
            updateFilterButtonStyles();
            renderTable();
        });
        return btn;
    }

    private void updateFilterButtonStyles() {
        setBtnActive(btnAll, filterStatus == null);
        setBtnActive(btnPending, filterStatus == MatchManager.MatchStatus.PENDING);
        setBtnActive(btnAccepted, filterStatus == MatchManager.MatchStatus.ACCEPTED);
        setBtnActive(btnDeclined, filterStatus == MatchManager.MatchStatus.DECLINED);
    }

    private void setBtnActive(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 6px 12px; -fx-font-size: 12px;");
        } else {
            btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-background-radius: 6px; -fx-padding: 6px 12px; -fx-font-size: 12px;");
        }
    }

    private void renderTable() {
        tableContainer.getChildren().clear();

        List<MatchManager.MatchRequest> requests = FileHandler.loadMatchRequests();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        List<MatchManager.MatchRequest> filtered = requests.stream()
                .filter(r -> filterStatus == null || r.getStatus() == filterStatus)
                .filter(r -> query.isEmpty() || r.getSender().toLowerCase().contains(query) || r.getReceiver().toLowerCase().contains(query))
                .collect(Collectors.toList());

        countBadge.setText(filtered.size() + " of " + requests.size() + " Invitations");

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            emptyBox.getStyleClass().add("card-section");

            Label emptyIcon = new Label("✉️");
            emptyIcon.setStyle("-fx-font-size: 32px;");
            Label emptyTitle = new Label("No Match Requests Found");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            Label emptyDesc = new Label("No requests match the selected status or query filter.");
            emptyDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
            tableContainer.getChildren().add(emptyBox);
            return;
        }

        // Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(8, 16, 8, 16));
        headerRow.getStyleClass().add("table-header-row");

        Label colSender = createHeaderLabel("SENDER", 200);
        Label colArrow = createHeaderLabel("", 30);
        Label colReceiver = createHeaderLabel("RECIPIENT", 200);
        Label colTrack = createHeaderLabel("INTEREST TRACK", 180);
        Label colStatus = createHeaderLabel("STATUS", 120);

        headerRow.getChildren().addAll(colSender, colArrow, colReceiver, colTrack, colStatus);
        HBox.setHgrow(colTrack, Priority.ALWAYS);
        tableContainer.getChildren().add(headerRow);

        for (MatchManager.MatchRequest req : filtered) {
            tableContainer.getChildren().add(createRequestRow(req));
        }
    }

    private HBox createRequestRow(MatchManager.MatchRequest req) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("leaderboard-row");

        // 1. Sender
        HBox senderBox = new HBox(8);
        senderBox.setAlignment(Pos.CENTER_LEFT);
        senderBox.setPrefWidth(200);
        Label senderAvatar = new Label(req.getSender().isEmpty() ? "?" : req.getSender().substring(0, 1).toUpperCase());
        senderAvatar.getStyleClass().add("avatar-small");
        Label senderLabel = new Label(req.getSender());
        senderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        senderBox.getChildren().addAll(senderAvatar, senderLabel);

        // 2. Arrow
        Label arrowLabel = new Label("→");
        arrowLabel.setPrefWidth(30);
        arrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-alignment: center;");

        // 3. Receiver
        HBox receiverBox = new HBox(8);
        receiverBox.setAlignment(Pos.CENTER_LEFT);
        receiverBox.setPrefWidth(200);
        Label receiverAvatar = new Label(req.getReceiver().isEmpty() ? "?" : req.getReceiver().substring(0, 1).toUpperCase());
        receiverAvatar.getStyleClass().add("avatar-small");
        Label receiverLabel = new Label(req.getReceiver());
        receiverLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        receiverBox.getChildren().addAll(receiverAvatar, receiverLabel);

        // 4. Domain Track Chip
        HBox trackBox = new HBox();
        trackBox.setAlignment(Pos.CENTER_LEFT);
        trackBox.setPrefWidth(180);
        HBox.setHgrow(trackBox, Priority.ALWAYS);

        Label trackChip = new Label(req.getInterest().toUpperCase());
        trackChip.getStyleClass().add("shared-skill-chip");
        trackBox.getChildren().add(trackChip);

        // 5. Status Badge
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPrefWidth(120);

        Label statusBadge = new Label(req.getStatus().name());
        if (req.getStatus() == MatchManager.MatchStatus.PENDING) {
            statusBadge.getStyleClass().add("badge-pending");
        } else if (req.getStatus() == MatchManager.MatchStatus.ACCEPTED) {
            statusBadge.getStyleClass().add("badge-accepted");
        } else {
            statusBadge.getStyleClass().add("badge-declined");
        }
        statusBox.getChildren().add(statusBadge);

        row.getChildren().addAll(senderBox, arrowLabel, receiverBox, trackBox, statusBox);
        return row;
    }

    private Label createHeaderLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add("table-header-label");
        return lbl;
    }
}
