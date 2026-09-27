package com.skillsync.ui;

import com.skillsync.Question;
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
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern assessment results view presenting score evaluation, qualification status,
 * and a full question-by-question review.
 */
public class AssessmentResultView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User user;
    private final String domain;
    private final int score;
    private final List<Question> questions;
    private final Map<Integer, String> userAnswers;
    private final Consumer<NavSection> navigationHandler;
    private final Consumer<String> testLaunchHandler;

    public AssessmentResultView(SkillSyncApp app,
                                User user,
                                String domain,
                                int score,
                                List<Question> questions,
                                Map<Integer, String> userAnswers,
                                Consumer<NavSection> navigationHandler,
                                Consumer<String> testLaunchHandler) {
        this.app = app;
        this.user = user;
        this.domain = domain;
        this.score = score;
        this.questions = questions;
        this.userAnswers = userAnswers;
        this.navigationHandler = navigationHandler;
        this.testLaunchHandler = testLaunchHandler;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        buildView();
    }

    private void buildView() {
        // 1. Result Summary Header Card
        VBox summaryCard = buildSummaryCard();

        // 2. Action Buttons Row
        HBox actionsRow = buildActionsRow();

        // 3. Question-by-Question Review Section
        VBox reviewSection = buildReviewSection();

        container.getChildren().addAll(summaryCard, actionsRow, reviewSection);
    }

    private VBox buildSummaryCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("result-card");
        card.setAlignment(Pos.CENTER);

        boolean qualified = user.isQualified(domain);

        Label badge = new Label(qualified ? "✅ QUALIFIED FOR PEER MATCHING" : "❌ SCORE BELOW THRESHOLD");
        badge.getStyleClass().add(qualified ? "result-badge-qualified" : "result-badge-unqualified");

        Label title = new Label(qualified ? "🎉 Outstanding Achievement!" : "Assessment Completed");
        title.getStyleClass().add("card-section-title");
        title.setStyle("-fx-font-size: 22px;");

        String feedbackText = qualified
                ? "You scored " + score + " / 10 in " + domain.toUpperCase() + ". You have qualified and this domain will now boost your peer recommendations!"
                : "You scored " + score + " / 10 in " + domain.toUpperCase() + ". A score of 7 or higher is required to qualify for peer matching. You can practice and retake this test at any time.";

        Label feedbackLbl = new Label(feedbackText);
        feedbackLbl.getStyleClass().add("card-section-subtitle");
        feedbackLbl.setStyle("-fx-font-size: 13px; -fx-alignment: center;");
        feedbackLbl.setWrapText(true);

        // Score display
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER);

        Label scoreLbl = new Label(score + " / 10");
        scoreLbl.getStyleClass().add("result-score-number");

        Label scoreSub = new Label((score * 10) + "% Accuracy • Passing Standard: 7 / 10");
        scoreSub.getStyleClass().add("result-score-sub");

        scoreBox.getChildren().addAll(scoreLbl, scoreSub);

        card.getChildren().addAll(badge, title, feedbackLbl, scoreBox);
        return card;
    }

    private HBox buildActionsRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);

        Button profileBtn = new Button("👤  View My Profile");
        profileBtn.getStyleClass().add("btn-primary");
        profileBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.PROFILE);
            }
        });

        Button dashboardBtn = new Button("📊  Back to Dashboard");
        dashboardBtn.getStyleClass().add("btn-secondary");
        dashboardBtn.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(NavSection.DASHBOARD);
            }
        });

        Button anotherTestBtn = new Button("📝  Take Another Assessment");
        anotherTestBtn.getStyleClass().add("btn-secondary");
        anotherTestBtn.setOnAction(e -> {
            if (testLaunchHandler != null) {
                testLaunchHandler.accept(null);
            }
        });

        row.getChildren().addAll(profileBtn, dashboardBtn, anotherTestBtn);
        return row;
    }

    private VBox buildReviewSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card-section");

        VBox headingBox = new VBox(2);
        Label title = new Label("Question Review & Answer Key");
        title.getStyleClass().add("card-section-title");

        Label sub = new Label("Inspect your submitted answers compared against the verified answer key.");
        sub.getStyleClass().add("card-section-subtitle");
        headingBox.getChildren().addAll(title, sub);

        VBox list = new VBox(10);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String userAns = userAnswers.get(i);
            boolean isCorrect = userAns != null && userAns.equalsIgnoreCase(q.getCorrectOption().trim());

            VBox item = new VBox(8);
            item.getStyleClass().addAll("review-item", isCorrect ? "review-item-correct" : "review-item-incorrect");

            HBox headerRow = new HBox(10);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            Label qNum = new Label("Question " + (i + 1));
            qNum.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badge = new Label(isCorrect ? "✓ Correct (+1)" : "✗ Incorrect (0)");
            badge.getStyleClass().add(isCorrect ? "review-badge-correct" : "review-badge-incorrect");

            headerRow.getChildren().addAll(qNum, spacer, badge);

            Label qText = new Label(q.getQuestion());
            qText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
            qText.setWrapText(true);

            VBox answersBox = new VBox(4);
            answersBox.setStyle("-fx-padding: 4px 0 0 0;");

            String chosenText = userAns != null ? userAns + " — " + getOptionContent(q, userAns) : "None selected";
            Label yourAnsLbl = new Label("Your answer: " + chosenText);
            yourAnsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isCorrect ? "#166534;" : "#991b1b;"));

            if (!isCorrect) {
                String correctKey = q.getCorrectOption().trim();
                String correctText = correctKey + " — " + getOptionContent(q, correctKey);
                Label correctAnsLbl = new Label("Correct answer: " + correctText);
                correctAnsLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #166534;");
                answersBox.getChildren().addAll(yourAnsLbl, correctAnsLbl);
            } else {
                answersBox.getChildren().add(yourAnsLbl);
            }

            item.getChildren().addAll(headerRow, qText, answersBox);
            list.getChildren().add(item);
        }

        section.getChildren().addAll(headingBox, list);
        return section;
    }

    private String getOptionContent(Question q, String optionLetter) {
        if (optionLetter == null) return "";
        return switch (optionLetter.toUpperCase().trim()) {
            case "A" -> q.getOptionA();
            case "B" -> q.getOptionB();
            case "C" -> q.getOptionC();
            case "D" -> q.getOptionD();
            default -> "";
        };
    }

    public Node getView() {
        return container;
    }
}
