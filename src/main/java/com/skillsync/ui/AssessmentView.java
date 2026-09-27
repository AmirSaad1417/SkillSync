package com.skillsync.ui;

import com.skillsync.FileHandler;
import com.skillsync.Question;
import com.skillsync.SkillSyncApp;
import com.skillsync.User;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern interactive skill assessment view with domain selection, randomized MCQs,
 * visual option cards, progress tracking, and score persistence.
 */
public class AssessmentView {

    private final VBox container;
    private final SkillSyncApp app;
    private final User user;
    private final Consumer<NavSection> navigationHandler;
    private final AssessmentCompletionHandler completionHandler;

    // Active test session state
    private String activeDomain = null;
    private List<Question> testQuestions = null;
    private int currentQuestionIndex = 0;
    private final Map<Integer, String> selectedAnswers = new HashMap<>();

    public interface AssessmentCompletionHandler {
        void onTestComplete(String domain, int score, List<Question> questions, Map<Integer, String> answers);
    }

    public AssessmentView(SkillSyncApp app,
                          User user,
                          String initialDomain,
                          Consumer<NavSection> navigationHandler,
                          AssessmentCompletionHandler completionHandler) {
        this.app = app;
        this.user = user;
        this.navigationHandler = navigationHandler;
        this.completionHandler = completionHandler;

        this.container = new VBox(22);
        this.container.getStyleClass().add("content-container");

        if (initialDomain != null && !initialDomain.isEmpty()) {
            startTest(initialDomain);
        } else {
            showDomainSelection();
        }
    }

    public void showDomainSelection() {
        this.activeDomain = null;
        this.testQuestions = null;
        this.currentQuestionIndex = 0;
        this.selectedAnswers.clear();

        container.getChildren().clear();

        // 1. Heading Card
        VBox headingCard = new VBox(6);
        headingCard.getStyleClass().add("card-section");

        Label title = new Label("Skill Qualification Assessments");
        title.getStyleClass().add("card-section-title");
        title.setStyle("-fx-font-size: 20px;");

        Label sub = new Label("Select a domain to begin a 10-question evaluation. Score 7 or higher to qualify for peer matching.");
        sub.getStyleClass().add("card-section-subtitle");
        headingCard.getChildren().addAll(title, sub);

        // 2. Domain Cards Grid
        VBox domainList = new VBox(12);

        List<String[]> domains = List.of(
                new String[]{"dsa", "Data Structures & Algorithms", "🧠", "Binary trees, graphs, dynamic programming, time complexity, and sorting algorithms."},
                new String[]{"ai", "Artificial Intelligence & ML", "🤖", "Machine learning fundamentals, neural networks, supervised models, and heuristic search."},
                new String[]{"robotics", "Robotics & Embedded Systems", "⚡", "Kinematics, sensors, microcontrollers, motor drivers, and automated control loops."},
                new String[]{"design", "System Design & UI/UX", "🎨", "Architectural patterns, modular design, ergonomics, interface principles, and REST conventions."}
        );

        for (String[] d : domains) {
            String domainKey = d[0];
            String domainTitle = d[1];
            String domainIcon = d[2];
            String domainDesc = d[3];

            HBox card = new HBox(16);
            card.getStyleClass().add("skill-row");
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new javafx.geometry.Insets(16, 20, 16, 20));

            Label iconLbl = new Label(domainIcon);
            iconLbl.setStyle("-fx-font-size: 28px;");

            VBox infoBox = new VBox(3);
            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label(domainTitle);
            nameLbl.getStyleClass().add("skill-name");
            nameLbl.setStyle("-fx-font-size: 15px;");

            titleRow.getChildren().add(nameLbl);

            if (user.hasAttempted(domainKey)) {
                int score = user.getScore(domainKey);
                Label scorePill = new Label("Score: " + score + "/10");
                scorePill.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

                Label statusBadge = new Label(user.isQualified(domainKey) ? "✅ Qualified" : "❌ Not Qualified");
                statusBadge.getStyleClass().add(user.isQualified(domainKey) ? "badge-qualified" : "badge-attempted-unqualified");

                titleRow.getChildren().addAll(scorePill, statusBadge);
            } else {
                Label pendingBadge = new Label("⏳ Not Attempted");
                pendingBadge.getStyleClass().add("badge-not-attempted");
                titleRow.getChildren().add(pendingBadge);
            }

            Label descLbl = new Label(domainDesc);
            descLbl.getStyleClass().add("stat-note");
            descLbl.setWrapText(true);

            infoBox.getChildren().addAll(titleRow, descLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button startBtn = new Button(user.hasAttempted(domainKey) ? "Retake Assessment" : "Start Assessment →");
            startBtn.getStyleClass().add(user.hasAttempted(domainKey) ? "btn-secondary" : "btn-primary");
            startBtn.setOnAction(e -> startTest(domainKey));

            card.getChildren().addAll(iconLbl, infoBox, spacer, startBtn);
            domainList.getChildren().add(card);
        }

        container.getChildren().addAll(headingCard, domainList);
    }

    public void startTest(String domain) {
        this.activeDomain = domain;
        List<Question> rawQuestions = FileHandler.loadQuestions(domain);

        if (rawQuestions.isEmpty()) {
            showError("No questions found for domain: " + domain.toUpperCase());
            showDomainSelection();
            return;
        }

        List<Question> shuffled = new ArrayList<>(rawQuestions);
        Collections.shuffle(shuffled);
        this.testQuestions = shuffled.subList(0, Math.min(10, shuffled.size()));
        this.currentQuestionIndex = 0;
        this.selectedAnswers.clear();

        renderQuestionView();
    }

    private void renderQuestionView() {
        container.getChildren().clear();

        VBox testBox = new VBox(18);
        testBox.getStyleClass().add("test-container");

        // 1. Top Header Bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(780);

        Label domainPill = new Label(activeDomain.toUpperCase() + " ASSESSMENT");
        domainPill.getStyleClass().add("brand-badge");

        Label counterLbl = new Label("Question " + (currentQuestionIndex + 1) + " of " + testQuestions.size());
        counterLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #334155; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exitBtn = new Button("✕ Exit Test");
        exitBtn.getStyleClass().add("btn-secondary");
        exitBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
        exitBtn.setOnAction(e -> confirmExit());

        topBar.getChildren().addAll(domainPill, counterLbl, spacer, exitBtn);

        // 2. Progress Bar
        ProgressBar progressBar = new ProgressBar((double) (currentQuestionIndex + 1) / testQuestions.size());
        progressBar.getStyleClass().add("test-progress-bar");
        progressBar.setMaxWidth(780);

        // 3. Question Card
        Question currentQuestion = testQuestions.get(currentQuestionIndex);
        VBox card = new VBox(20);
        card.getStyleClass().add("test-card");

        Label qText = new Label((currentQuestionIndex + 1) + ". " + currentQuestion.getQuestion());
        qText.getStyleClass().add("question-text");
        qText.setWrapText(true);

        // Options List
        VBox optionsBox = new VBox(10);
        optionsBox.getStyleClass().add("options-container");

        String currentAnswer = selectedAnswers.get(currentQuestionIndex);

        optionsBox.getChildren().addAll(
                createOptionCard("A", currentQuestion.getOptionA(), currentAnswer),
                createOptionCard("B", currentQuestion.getOptionB(), currentAnswer),
                createOptionCard("C", currentQuestion.getOptionC(), currentAnswer),
                createOptionCard("D", currentQuestion.getOptionD(), currentAnswer)
        );

        // 4. Navigation Buttons (Prev, Next, Submit)
        HBox navBar = new HBox(12);
        navBar.getStyleClass().add("test-nav-bar");

        Button prevBtn = new Button("← Previous");
        prevBtn.getStyleClass().add("btn-secondary");
        prevBtn.setDisable(currentQuestionIndex == 0);
        prevBtn.setOnAction(e -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                renderQuestionView();
            }
        });

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        boolean isLast = (currentQuestionIndex == testQuestions.size() - 1);

        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("btn-secondary");
        nextBtn.setOnAction(e -> {
            if (currentQuestionIndex < testQuestions.size() - 1) {
                currentQuestionIndex++;
                renderQuestionView();
            }
        });

        Button submitBtn = new Button("Submit Assessment ✓");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> submitAssessment());

        navBar.getChildren().add(prevBtn);
        navBar.getChildren().add(navSpacer);

        if (!isLast) {
            navBar.getChildren().add(nextBtn);
        } else {
            navBar.getChildren().add(submitBtn);
        }

        card.getChildren().addAll(qText, optionsBox, navBar);
        testBox.getChildren().addAll(topBar, progressBar, card);

        container.getChildren().add(testBox);
    }

    private HBox createOptionCard(String letter, String text, String currentSelection) {
        HBox card = new HBox(14);
        card.getStyleClass().add("option-card");
        boolean isSelected = letter.equalsIgnoreCase(currentSelection);

        if (isSelected) {
            card.getStyleClass().add("option-card-selected");
        }

        Label letterBox = new Label(letter);
        letterBox.getStyleClass().add("option-letter-box");
        if (isSelected) {
            letterBox.getStyleClass().add("option-letter-box-selected");
        }

        Label textLbl = new Label(text);
        textLbl.getStyleClass().add("option-text");
        textLbl.setWrapText(true);

        card.getChildren().addAll(letterBox, textLbl);

        card.setOnMouseClicked(e -> {
            selectedAnswers.put(currentQuestionIndex, letter);
            renderQuestionView();
        });

        return card;
    }

    private void submitAssessment() {
        int answeredCount = selectedAnswers.size();
        if (answeredCount < testQuestions.size()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unanswered Questions");
            alert.setHeaderText("You have answered " + answeredCount + " of " + testQuestions.size() + " questions.");
            alert.setContentText("Do you want to submit your assessment anyway? Unanswered questions will count as incorrect.");
            attachDialogStyle(alert);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        // Calculate score
        int correct = 0;
        for (int i = 0; i < testQuestions.size(); i++) {
            Question q = testQuestions.get(i);
            String ans = selectedAnswers.get(i);
            if (ans != null && ans.equalsIgnoreCase(q.getCorrectOption().trim())) {
                correct++;
            }
        }

        // Update score in User model
        user.setScore(activeDomain, correct);
        for (User u : app.getUsers()) {
            if (u.getUsername().equalsIgnoreCase(user.getUsername())) {
                u.setScore(activeDomain, correct);
            }
        }

        // Immediate atomic persistence to users.csv
        FileHandler.saveUsers(app.getUsers());

        // Hand over to completion handler
        if (completionHandler != null) {
            completionHandler.onTestComplete(activeDomain, correct, testQuestions, selectedAnswers);
        }
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Assessment");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Your current test progress will be lost.");
        attachDialogStyle(alert);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showDomainSelection();
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Assessment Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        attachDialogStyle(alert);
        alert.showAndWait();
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
