package com.skillsync.ui;

import com.skillsync.FileHandler;
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
 * Admin Finalized Matches View:
 * Direct implementation of FileHandler.viewAllFinalizedMatches():
 * Inspection of confirmed collaborative student partnerships and shared interest tracks.
 */
public class AdminMatchesView {

    private final SkillSyncApp app;
    private final VBox root;

    private TextField searchField;
    private VBox tableContainer;
    private Label countBadge;

    public AdminMatchesView(SkillSyncApp app) {
        this.app = app;

        this.root = new VBox(22);
        this.root.getStyleClass().add("content-container");

        buildView();
    }

    public Parent getView() {
        return root;
    }

    private void buildView() {
        List<String[]> matches = FileHandler.loadFinalMatches();

        // 1. Header Box
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Finalized Student Partnerships");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Confirmed peer collaborative teams paired based on shared qualified domain assessments.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countBadge = new Label(matches.size() + " Confirmed Teams");
        countBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-padding: 6px 14px; -fx-background-radius: 9999px; -fx-border-color: #a7f3d0; -fx-border-radius: 9999px;");

        headerBox.getChildren().addAll(titleBox, spacer, countBadge);

        // 2. Search Field
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Filter by student name or track...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(340);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderTable());

        searchBar.getChildren().add(searchField);

        // 3. Table Container
        tableContainer = new VBox(10);
        renderTable();

        root.getChildren().addAll(headerBox, searchBar, tableContainer);
    }

    private void renderTable() {
        tableContainer.getChildren().clear();

        List<String[]> matches = FileHandler.loadFinalMatches();
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        List<String[]> filtered = matches.stream()
                .filter(m -> m.length >= 3)
                .filter(m -> query.isEmpty() || m[0].toLowerCase().contains(query) || m[1].toLowerCase().contains(query) || m[2].toLowerCase().contains(query))
                .collect(Collectors.toList());

        countBadge.setText(filtered.size() + " of " + matches.size() + " Confirmed Teams");

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 20, 40, 20));
            emptyBox.getStyleClass().add("card-section");

            Label emptyIcon = new Label("🤝");
            emptyIcon.setStyle("-fx-font-size: 32px;");
            Label emptyTitle = new Label("No Finalized Partnerships Found");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            Label emptyDesc = new Label("No finalized teams match the search query.");
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

        Label colPartnerA = createHeaderLabel("PARTNER 1", 220);
        Label colIcon = createHeaderLabel("", 40);
        Label colPartnerB = createHeaderLabel("PARTNER 2", 220);
        Label colDomain = createHeaderLabel("TECHNICAL DOMAIN TRACK", 200);
        Label colStatus = createHeaderLabel("STATUS", 140);

        headerRow.getChildren().addAll(colPartnerA, colIcon, colPartnerB, colDomain, colStatus);
        HBox.setHgrow(colDomain, Priority.ALWAYS);
        tableContainer.getChildren().add(headerRow);

        for (String[] match : filtered) {
            tableContainer.getChildren().add(createMatchRow(match[0], match[1], match[2]));
        }
    }

    private HBox createMatchRow(String user1, String user2, String domain) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("leaderboard-row");

        // 1. Partner 1
        HBox p1Box = new HBox(8);
        p1Box.setAlignment(Pos.CENTER_LEFT);
        p1Box.setPrefWidth(220);
        Label p1Avatar = new Label(user1.isEmpty() ? "?" : user1.substring(0, 1).toUpperCase());
        p1Avatar.getStyleClass().add("avatar-small");
        Label p1Name = new Label(user1);
        p1Name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        p1Box.getChildren().addAll(p1Avatar, p1Name);

        // 2. Link Icon
        Label linkIcon = new Label("↔");
        linkIcon.setPrefWidth(40);
        linkIcon.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #059669; -fx-alignment: center;");

        // 3. Partner 2
        HBox p2Box = new HBox(8);
        p2Box.setAlignment(Pos.CENTER_LEFT);
        p2Box.setPrefWidth(220);
        Label p2Avatar = new Label(user2.isEmpty() ? "?" : user2.substring(0, 1).toUpperCase());
        p2Avatar.getStyleClass().add("avatar-small");
        Label p2Name = new Label(user2);
        p2Name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        p2Box.getChildren().addAll(p2Avatar, p2Name);

        // 4. Domain Track Chip
        HBox domainBox = new HBox();
        domainBox.setAlignment(Pos.CENTER_LEFT);
        domainBox.setPrefWidth(200);
        HBox.setHgrow(domainBox, Priority.ALWAYS);

        Label domainChip = new Label(domain.toUpperCase());
        domainChip.getStyleClass().add("shared-skill-chip");
        domainBox.getChildren().add(domainChip);

        // 5. Active Partnership Status
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPrefWidth(140);

        Label statusBadge = new Label("🤝 Collaborating");
        statusBadge.getStyleClass().add("badge-accepted");
        statusBox.getChildren().add(statusBadge);

        row.getChildren().addAll(p1Box, linkIcon, p2Box, domainBox, statusBox);
        return row;
    }

    private Label createHeaderLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add("table-header-label");
        return lbl;
    }
}
