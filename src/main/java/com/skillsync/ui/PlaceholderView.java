package com.skillsync.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Reusable placeholder view for sections scheduled for upcoming stages.
 */
public class PlaceholderView {

    private final VBox container;

    public PlaceholderView(NavSection section, Consumer<NavSection> navigationHandler) {
        this.container = new VBox(20);
        this.container.setAlignment(Pos.CENTER);
        this.container.setPadding(new javafx.geometry.Insets(40, 20, 40, 20));

        VBox card = new VBox(16);
        card.getStyleClass().add("placeholder-card");
        card.setAlignment(Pos.CENTER);

        Label badge = new Label("COMING IN UPCOMING STAGE");
        badge.getStyleClass().add("placeholder-badge");

        Label iconLabel = new Label(section.getIcon());
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label titleLabel = new Label(section.getTitle());
        titleLabel.getStyleClass().add("placeholder-title");

        String descriptionText = switch (section) {
            case ASSESSMENTS -> "Interactive, randomized 10-question multiple-choice tests across DSA, AI, Robotics, and Design with instant scoring and qualification checks.";
            case FIND_PEERS -> "Intelligent compatibility recommendations matching you with qualified peers who share complementary academic interests.";
            case REQUESTS -> "Review incoming pairing invitations, accept or decline match requests, and monitor your outgoing invitations in real-time.";
            case LEADERBOARD -> "View global student academic rankings sorted by average quiz performance, qualified skills count, and username.";
            case PROFILE -> "Inspect your complete student profile, domain score breakdowns, qualification thresholds, and match status.";
            default -> "This section is currently under development and will be activated in an upcoming stage.";
        };

        Label descLabel = new Label(descriptionText);
        descLabel.getStyleClass().add("placeholder-desc");
        descLabel.setWrapText(true);

        Button backButton = new Button("← Back to Dashboard");
        backButton.getStyleClass().add("btn-secondary");
        backButton.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.DASHBOARD);
            }
        });

        card.getChildren().addAll(badge, iconLabel, titleLabel, descLabel, backButton);
        container.getChildren().add(card);
    }

    public Node getView() {
        return container;
    }
}
