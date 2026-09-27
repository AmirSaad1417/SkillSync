package com.skillsync;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
public class TestEngine {

    private static final int PASSING_SCORE = 7;
    private static final int QUESTION_LIMIT = 10;

    public static int conductTest(User user, String interest) throws IOException {
        if (user.hasAttempted(interest)) {
            int score = user.getScore(interest);
            System.out.println("\n⚠️ You have already attempted the " + interest.toUpperCase() + " test.");
            System.out.println("Your score: " + score + "/10 " + (user.isQualified(interest) ? "✅ Qualified" : "❌ Not Qualified"));
            return score;
        }

        // ✅ Use the correct return type
        List<Question> questions = FileHandler.loadQuestions(interest);
        if (questions.size() < QUESTION_LIMIT) {
            System.out.println("❌ Not enough questions available for " + interest);
            return -1;
        }

        Collections.shuffle(questions);
        List<Question> testQuestions = questions.subList(0, QUESTION_LIMIT);
        int correct = 0;

        for (int i = 0; i < QUESTION_LIMIT; i++) {
            Question q = testQuestions.get(i);
            System.out.println("\nQ" + (i + 1) + ": " + q.getQuestion());
            System.out.println("A. " + q.getOptionA());
            System.out.println("B. " + q.getOptionB());
            System.out.println("C. " + q.getOptionC());
            System.out.println("D. " + q.getOptionD());
            System.out.print("Your Answer (A/B/C/D): ");
            String answer = Main.getScanner().nextLine().trim().toUpperCase();
            if (answer.length() == 1 && answer.charAt(0) == q.getCorrectOption().toUpperCase().charAt(0)) {
                correct++;
            }
        }

        System.out.println("\n🧾 Test Completed.");
        System.out.println("You scored " + correct + "/10 " + (correct >= PASSING_SCORE ? "✅ Qualified" : "❌ Not Qualified"));

        user.setScore(interest, correct);
        return correct;
    }

    public static void startTestSession(User user) throws IOException {
        System.out.println("\n=== 🎯 Available Interests ===");
        List<String> interests = FileHandler.getAvailableInterests();
        for (int i = 0; i < interests.size(); i++) {
            System.out.println((i + 1) + ". " + interests.get(i));
        }

        System.out.print("Choose an interest to take test (or 0 to go back): ");
        int choice = Main.getIntInput(0, interests.size());

        if (choice == 0) return;

        String selectedInterest = interests.get(choice - 1);
        conductTest(user, selectedInterest);
    }
}
