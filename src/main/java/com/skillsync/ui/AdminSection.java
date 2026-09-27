package com.skillsync.ui;

/**
 * Enumerates all administrative portal navigation sections.
 */
public enum AdminSection {
    DASHBOARD("System Dashboard", "Platform telemetry, key performance indicators & overview", "📊"),
    USERS("Student Directory", "Roster of registered students, domain scores & team pairings", "👥"),
    PARTICIPATION("Participation Analytics", "Assessment attempts, pass rates & domain participation stats", "📈"),
    REQUESTS("Match Requests Log", "Complete audit trail of all peer match invitations and statuses", "✉️"),
    MATCHES("Finalized Partnerships", "Confirmed student collaboration partnerships and technical tracks", "🤝"),
    LEADERBOARD("Academic Leaderboard", "Live student merit rankings and competency standings", "🏆");

    private final String title;
    private final String subtitle;
    private final String icon;

    AdminSection(String title, String subtitle, String icon) {
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
