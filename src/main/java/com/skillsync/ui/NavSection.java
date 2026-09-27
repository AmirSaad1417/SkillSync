package com.skillsync.ui;

/**
 * Navigation sections for the SkillSync Desktop Application.
 */
public enum NavSection {
    DASHBOARD("Dashboard", "Overview & Academic Activity", "📊"),
    ASSESSMENTS("Assessments", "Skill Qualification Tests", "📝"),
    FIND_PEERS("Find Peers", "Compatible Student Matching", "🔍"),
    REQUESTS("Match Requests", "Incoming & Outgoing Pairing Requests", "✉️"),
    LEADERBOARD("Leaderboard", "Student Academic Rankings", "🏆"),
    PROFILE("My Profile", "Student Identity & Domain Scores", "👤");

    private final String title;
    private final String subtitle;
    private final String icon;

    NavSection(String title, String subtitle, String icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getIcon() {
        return icon;
    }
}
