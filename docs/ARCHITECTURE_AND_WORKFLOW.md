# SkillSync — System Architecture & Workflow Specification
> **Document Status:** Complete & Verified Baseline  
> **Target Audience:** System Architects, Examiners, Technical Evaluators, Peer Collaborators  
> **Platform:** Java 17 LTS • JavaFX Desktop GUI • Apache Maven • Flat CSV Persistence  

---

## 1. Executive Summary

**SkillSync** is a dedicated university peer-learning and project partner matching desktop application. It evaluates students' technical competencies through objective, domain-specific multiple-choice assessments and algorithmically pairs students based on the intersection of their qualified technical domains.

The system is engineered as an **offline-first desktop software application** following a strict **Three-Tier Architecture**:
1. **Presentation Layer (JavaFX 17 GUI):** Declarative controllers, reactive scene routing, component-based view layouts, and customized CSS styling.
2. **Business Logic Layer (Core Domain & Algorithms):** Domain models, assessment scoring engines, threshold qualification checks, compatibility intersection ranking, and administrative analytics.
3. **Data / Persistence Layer (FileHandler & Flat CSV Storage):** Thread-safe file I/O with crash-resilient **atomic temporary-file replacement** (`.tmp` $\rightarrow$ `.csv`) for zero data loss.

---

## 2. High-Level System Architecture (Three-Tier Desktop Model)

```mermaid
flowchart TD
    subgraph TIER1 ["1. PRESENTATION LAYER (JavaFX 17 GUI)"]
        direction TB
        subgraph AUTH_VIEWS ["Authentication Subsystem"]
            LoginView["LoginView\n(Student & Admin Auth)"]
            RegisterView["RegisterView\n(Student Registration)"]
        end
        
        subgraph STUDENT_VIEWS ["Student Portal (MainShellView)"]
            DashboardView["DashboardView\n(KPI Summary & Quick Nav)"]
            ProfileView["ProfileView\n(Identity & Scorecard)"]
            AssessmentView["AssessmentView\n(Interactive 10-MCQ Quiz)"]
            ResultView["AssessmentResultView\n(Score Review & Status)"]
            FindPeersView["FindPeersView\n(Recommendation Cards)"]
            RequestsView["RequestsView\n(Incoming / Outgoing Teaming)"]
            LeaderboardView["LeaderboardView\n(Rankings & Domain Badges)"]
        end

        subgraph ADMIN_VIEWS ["Administrator Console (AdminShellView)"]
            AdminDashView["AdminDashboardView\n(System KPIs & Telemetry)"]
            AdminUsersView["AdminUsersView\n(Student Directory & Badges)"]
            AdminStatsView["AdminStatsView\n(Participation Analytics)"]
            AdminReqsView["AdminRequestsView\n(Match Requests Audit Trail)"]
            AdminMatchesView["AdminMatchesView\n(Confirmed Teaming Records)"]
        end
    end

    subgraph TIER2 ["2. BUSINESS LOGIC & DOMAIN LAYER"]
        direction TB
        User["User Model\n(Profile, Credentials, Scores, Matched Flag)"]
        Question["Question Model\n(Immutable MCQ, Options, Answer Key)"]
        TestEngine["TestEngine Engine\n(Shuffling, 10-Question Limit, Scoring)"]
        MatchManager["MatchManager Engine\n(Shared Skill Intersection, Ranking, Requests)"]
        AdminPanel["AdminPanel Analytics\n(Aggregation, Pass Rates, Roster Audits)"]
    end

    subgraph TIER3 ["3. DATA PERSISTENCE & FILE STORAGE LAYER"]
        direction TB
        FileHandler["FileHandler Gateway\n(Atomic Writes, CSV Parsing, In-Memory Caching)"]
        
        subgraph CSV_STORE ["Flat-File CSV Storage"]
            UsersCSV[("users.csv\n(Student Registry & Scores)")]
            RequestsCSV[("match_requests.csv\n(Teaming Invitations Audit)")]
            MatchesCSV[("matches.csv\n(Confirmed Partnerships)")]
            QuestionsCSV[("resources/questions/*.csv\n(dsa, ai, robotics, design)")]
        end
    end

    %% UI to Logic Connections
    AUTH_VIEWS --> User
    STUDENT_VIEWS --> User
    STUDENT_VIEWS --> TestEngine
    STUDENT_VIEWS --> MatchManager
    ADMIN_VIEWS --> AdminPanel
    ADMIN_VIEWS --> MatchManager

    %% Logic to Logic Connections
    TestEngine --> Question
    TestEngine --> User
    MatchManager --> User
    AdminPanel --> User

    %% Logic to Persistence Connections
    User --> FileHandler
    TestEngine --> FileHandler
    MatchManager --> FileHandler
    AdminPanel --> FileHandler

    %% FileHandler to Storage
    FileHandler --> UsersCSV
    FileHandler --> RequestsCSV
    FileHandler --> MatchesCSV
    FileHandler --> QuestionsCSV

    classDef uiBox fill:#eff6ff,stroke:#3b82f6,stroke-width:2px,color:#1e3a8a;
    classDef logicBox fill:#eef2ff,stroke:#6366f1,stroke-width:2px,color:#312e81;
    classDef dataBox fill:#ecfdf5,stroke:#10b981,stroke-width:2px,color:#065f46;
    classDef storageBox fill:#f0fdf4,stroke:#059669,stroke-width:2px,color:#064e3b;

    class LoginView,RegisterView,DashboardView,ProfileView,AssessmentView,ResultView,FindPeersView,RequestsView,LeaderboardView,AdminDashView,AdminUsersView,AdminStatsView,AdminReqsView,AdminMatchesView uiBox;
    class User,Question,TestEngine,MatchManager,AdminPanel logicBox;
    class FileHandler dataBox;
    class UsersCSV,RequestsCSV,MatchesCSV,QuestionsCSV storageBox;
```

---

## 2. End-to-End System Workflow Diagram

This master diagram details the exact path of execution from initial process launch through authentication, student actions, assessment scoring, peer recommendation, teaming lifecycle, and admin oversight.

```mermaid
flowchart TD
    %% 1. Application Entry
    StartNode(["🚀 Application Launch\n(AppLauncher.main)"]) --> InitApp["Initialize SkillSyncApp\n(Load CSV Data into Memory)"]
    InitApp --> LoginScreen["Mount LoginView\n(Credentials Form)"]

    %% Authentication Decision
    LoginScreen --> AuthBranch{"User Action / Role?"}
    
    %% Registration Branch
    AuthBranch -- "New Student Register" --> RegisterScreen["RegisterView\n(Name, Email, Password)"]
    RegisterScreen --> ValidateReg{"Input Valid &\nUnique Email/User?"}
    ValidateReg -- "Validation Error" --> ShowRegErr["Display Error Banner\n(Inline Feedback)"]
    ShowRegErr --> RegisterScreen
    ValidateReg -- "Valid" --> CreateAccount["Instantiate User Model\n(All Scores = -1)"]
    CreateAccount --> SaveNewUser["FileHandler.saveUsers()\n(users.csv Atomic Write)"]
    SaveNewUser --> LoginScreen

    %% Admin Authentication
    AuthBranch -- "Admin Login\n(admin / admin123)" --> AdminShell["AdminShellView\n(Executive Sidebar & Header)"]

    %% Student Authentication
    AuthBranch -- "Student Sign In" --> AuthCheck{"Credentials\nMatch?"}
    AuthCheck -- "Invalid Credentials" --> LoginErr["Show Login Error\n(Inline Alert Banner)"]
    LoginErr --> LoginScreen
    AuthCheck -- "Authenticated" --> StudentShell["MainShellView\n(Navigation Sidebar & Header)"]

    %% Central Navigation Hub
    StudentShell --> Dashboard["DashboardView\n(Central Navigation Hub)"]
    
    Dashboard --> NavSelect{"Student Navigation"}
    NavSelect -- "Profile" --> ProfileView["ProfileView\n(View Identity, Scores & Partner)"]
    NavSelect -- "Leaderboard" --> LeaderboardView["LeaderboardView\n(Rankings & Domain Badges)"]
    NavSelect -- "Sign Out" --> LogoutAction["Clear Current User Session\nReset Stage View"]
    LogoutAction --> LoginScreen

    %% Assessment Workflow
    NavSelect -- "Skill Assessment" --> AssessSelect["AssessmentView: Select Domain\n(DSA | AI | Robotics | Design)"]
    AssessSelect --> LoadQuiz["FileHandler.loadQuestions()\n(Load & Cache Domain CSV)"]
    LoadQuiz --> QuizEngine["TestEngine.conductTest()\n(Shuffle & Present 10 MCQs)"]
    QuizEngine --> StudentAnswers["Student Selects A / B / C / D\n(Options Evaluated)"]
    StudentAnswers --> CalcScore["Compute Correct Answers\n(Score out of 10)"]
    
    CalcScore --> ScoreEval{"Score >= 7?\n(Qualification Threshold)"}
    ScoreEval -- "YES (Score >= 7)" --> MarkQualified["Domain Status: QUALIFIED ⭐\nAdd to User.qualifiedInterests"]
    ScoreEval -- "NO (Score < 7)" --> MarkUnqualified["Domain Status: NOT QUALIFIED\nRecord score, permit future retake"]
    
    MarkQualified --> PersistScore["FileHandler.saveUsers()\n(Immediate Atomic CSV Save)"]
    MarkUnqualified --> PersistScore
    PersistScore --> ResultView["AssessmentResultView\n(Score Card & Review List)"]
    ResultView --> Dashboard

    %% Peer Discovery Workflow
    NavSelect -- "Find Peers" --> ExtractInterests["Extract Current User's\nQualified Domains"]
    ExtractInterests --> CheckQuals{"User has >= 1\nQualified Domain?"}
    CheckQuals -- "NO" --> PromptAssess["Display Guidance Card:\n'Take assessments to unlock matching'"]
    PromptAssess --> AssessSelect

    CheckQuals -- "YES" --> ScanRoster["MatchManager.findBestMatches()\n(Scan Active Student Roster)"]
    ScanRoster --> FilterCandidates["Filter Candidates:\nExclude self & already paired students"]
    FilterCandidates --> IntersectSkills["Compute Set Intersection:\ncurrentUser.skills ∩ peer.skills"]
    IntersectSkills --> CompatibilityCheck{"Shared Qualified\nDomains >= 1?"}
    CompatibilityCheck -- "NO" --> NoPeers["Display Empty State:\n'No Compatible Peers Currently Found'"]
    CompatibilityCheck -- "YES" --> RankPeers["Sort by Match Relevance Score\n(Descending by Shared Count)"]
    RankPeers --> RenderCards["Render Peer Recommendation Cards\n(Avatar, Badges, Shared Tracks, Action Button)"]
    
    %% Match Request Lifecycle
    RenderCards --> SendRequestBtn["Click 'Send Match Request'\n(Select Collaboration Track)"]
    SendRequestBtn --> CreateRequest["MatchManager.sendMatchRequest()\nCreate MatchRequest (PENDING)"]
    CreateRequest --> SaveReq["FileHandler.saveAllMatchRequests()\nPersist to match_requests.csv"]
    SaveReq --> NavSelect

    %% Teaming / Requests View
    NavSelect -- "Match Requests" --> RequestsView["RequestsView\n(Incoming & Outgoing Invitations)"]
    RequestsView --> RecipientAction{"Recipient Action\n(Incoming Invitation)"}
    
    RecipientAction -- "Decline Action" --> DeclineFlow["MatchManager.declineMatchRequest()\nSet Status: DECLINED"]
    DeclineFlow --> SaveDeclined["Update match_requests.csv\n(Both Students Remain Available)"]
    SaveDeclined --> RequestsView

    RecipientAction -- "Accept Action" --> AcceptFlow["MatchManager.acceptMatchRequest()\nSet Status: ACCEPTED"]
    AcceptFlow --> MutualPairing["Set isMatched = true for Both Users\nAssign Matched Partner Usernames"]
    MutualPairing --> SaveFinalMatch["FileHandler.saveFinalMatch()\nAppend Record to matches.csv"]
    SaveFinalMatch --> SaveUsersMatched["FileHandler.saveUsers()\nPersist Partner States to users.csv"]
    SaveUsersMatched --> TeamingSuccess["Display Confirmation Modal 🎉\nBoth Profiles Show Matched Teammate"]
    TeamingSuccess --> Dashboard

    %% Admin Portal Workflow
    AdminShell --> AdminNav{"Admin Navigation"}
    AdminNav -- "Dashboard" --> AdminDash["AdminDashboardView\n(6 System KPIs, Telemetry & Pass Rates)"]
    AdminNav -- "Student Directory" --> AdminUsers["AdminUsersView\n(All 19 Students & 4-Domain Score Badges)"]
    AdminNav -- "Participation" --> AdminStats["AdminStatsView\n(Domain Attempt Ratios & Rosters)"]
    AdminNav -- "Requests Log" --> AdminReqs["AdminRequestsView\n(Audit Trail with Status Filters)"]
    AdminNav -- "Finalized Teams" --> AdminMatches["AdminMatchesView\n(Confirmed Teaming Partnerships)"]
    AdminNav -- "Sign Out" --> AdminLogout["Clear Admin Session\nReturn to Gateway"]
    AdminLogout --> LoginScreen

    %% Class Styling
    classDef startClass fill:#1e293b,stroke:#0f172a,stroke-width:2px,color:#ffffff;
    classDef uiClass fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#1e40af;
    classDef decisionClass fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#92400e;
    classDef actionClass fill:#f1f5f9,stroke:#64748b,stroke-width:2px,color:#0f172a;
    classDef successClass fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#14532d;
    classDef alertClass fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#991b1b;
    classDef adminClass fill:#0f172a,stroke:#475569,stroke-width:2px,color:#f8fafc;

    class StartNode,InitApp startClass;
    class LoginScreen,RegisterScreen,StudentShell,Dashboard,ProfileView,AssessmentView,AssessSelect,ResultView,FindPeersView,RequestsView,LeaderboardView uiClass;
    class AuthBranch,ValidateReg,AuthCheck,NavSelect,ScoreEval,CheckQuals,CompatibilityCheck,RecipientAction,AdminNav decisionClass;
    class CreateAccount,SaveNewUser,QuizEngine,StudentAnswers,CalcScore,ExtractInterests,ScanRoster,FilterCandidates,IntersectSkills,RankPeers,RenderCards,SendRequestBtn,CreateRequest,SaveReq,DeclineFlow,SaveDeclined,AcceptFlow,MutualPairing,SaveFinalMatch,SaveUsersMatched actionClass;
    class MarkQualified,TeamingSuccess successClass;
    class ShowRegErr,LoginErr,MarkUnqualified,NoPeers,PromptAssess alertClass;
    class AdminShell,AdminDash,AdminUsers,AdminStats,AdminReqs,AdminMatches,AdminLogout adminClass;
```

---

## 3. Bi-Directional Core Data Flow Pipeline

The application features a strict, predictable data flow pipeline that separates presentation event handling from business logic calculations and disk serialization.

```mermaid
flowchart LR
    subgraph WRITE_PIPELINE ["WRITE PIPELINE (Mutation & Persistence)"]
        direction LR
        U_IN["1. User Interaction\n(Button click, MCQ option, Form submit)"] -->
        V_EVT["2. JavaFX View\n(Input sanitization & Event handler)"] -->
        B_OP["3. Business Logic\n(Scoring, Matching, Status mutation)"] -->
        V_VAL{"4. Validation\n(Rules, duplicates, delimiters)"} -->|Passed|
        F_WR["5. FileHandler Gateway\n(Atomic file serialization)"] -->
        C_DISK[("6. CSV File Storage\n(.tmp -> atomic rename)")]
    end

    subgraph READ_PIPELINE ["READ PIPELINE (Hydration & Rendering)"]
        direction RL
        V_DISP["11. Rendered JavaFX View\n(Cards, Badges, Tables, Charts)"] <--
        O_LIST["10. Observable Java Collections\n(ObservableList, Binding properties)"] <--
        D_OBJ["9. Domain Object Models\n(User, Question, MatchRequest)"] <--
        P_PARSE["8. FileHandler CSV Parser\n(Header-aware, delimiter splitting)"] <--
        C_SRC[("7. CSV File on Disk\n(users, questions, requests, matches)")]
    end

    classDef writeNode fill:#eff6ff,stroke:#2563eb,stroke-width:1.5px,color:#1e3a8a;
    classDef readNode fill:#f0fdf4,stroke:#16a34a,stroke-width:1.5px,color:#14532d;
    classDef diskNode fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#78350f;

    class U_IN,V_EVT,B_OP,V_VAL,F_WR writeNode;
    class V_DISP,O_LIST,D_OBJ,P_PARSE readNode;
    class C_DISK,C_SRC diskNode;
```

### Pipeline Guarantees
1. **Delimiter Injection Defense:** All user inputs (names, credentials, messages) reject commas (`,`), preventing CSV row corruption.
2. **Crash-Resilient Atomic Writes:** Saves write to a temporary file (`.tmp`) first, followed by `Files.move(..., StandardCopyOption.REPLACE_EXISTING)`. If the JVM terminates mid-write, original data files remain uncorrupted.
3. **In-Memory Question Caching:** Question banks are parsed from CSV on first access and cached in memory, eliminating redundant disk reads during quizzes.

---

## 4. Match Request Lifecycle State Machine

The interaction between two students follows a formal deterministic finite state machine:

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : Student Registers or Unpaired
    
    state AVAILABLE {
        [*] --> Unqualified : No assessments passed
        Unqualified --> Qualified : Score >= 7 in >= 1 Domain
        Qualified --> Searching : Open 'Find Peers'
    }

    Searching --> PENDING : Student A sends Match Request to Student B
    
    state PENDING {
        note right of PENDING
            Stored in match_requests.csv
            Sender: Student A
            Recipient: Student B
            Track: Shared qualified interest
            Status: PENDING
        end note
    }

    PENDING --> DECLINED : Student B declines invitation
    DECLINED --> AVAILABLE : Request archived, both students free to pair
    
    PENDING --> ACCEPTED : Student B accepts invitation
    
    state ACCEPTED {
        note right of ACCEPTED
            1. Status updated in match_requests.csv
            2. Match pair written to matches.csv
            3. Both users flagged isMatched = true
            4. Partner names linked in users.csv
        end note
    }

    ACCEPTED --> FINALIZED_PARTNERSHIP : Mutual pairing committed
    
    state FINALIZED_PARTNERSHIP {
        [*] --> ActiveCollaboration
        ActiveCollaboration --> ExcludedFromSearch : Filtered from Find Peers
        ActiveCollaboration --> DisplayPartnerBadge : Visible in Dashboard & Profile
    }
```

---

## 5. Architectural Decision Matrix (Evaluation & Defense Guide)

This section provides technical justifications for why specific design patterns were chosen, ideal for project defense and technical viva:

| Architectural Component | Technical Choice | Alternatives Considered | Engineering Rationale |
| :--- | :--- | :--- | :--- |
| **GUI Framework** | **JavaFX 17 LTS** | Swing, AWT, Web Electron | Modern hardware-accelerated rendering pipeline, clean separation of CSS styling from layout nodes, native scene graphs, and zero web engine RAM overhead. |
| **Application Architecture** | **Three-Tier Desktop** | MVC with Spring Boot, Client-Server | Keeps the university project fully self-contained with zero server infrastructure, instant zero-latency startup, and predictable offline behavior. |
| **Data Persistence** | **Flat CSV + Atomic `.tmp` Replace** | SQLite, H2, JSON, XML | Human-readable flat storage meeting DSA Lab curriculum requirements, zero external native binaries, with enterprise-grade crash resilience via atomic renaming. |
| **Matching Engine** | **Set-Theoretic Domain Intersection** | Machine Learning, Cosine Vector Distance | Completely deterministic, transparent, and explainable to academic evaluators; precisely targets shared competency rather than opaque probabilistic guesses. |
| **Non-Modular Launcher** | **`AppLauncher` Bootstrap** | Modular `module-info.java` | Avoids complex modular reflection barriers when loading JavaFX controls and CSS across different IDE configurations and operating systems. |

---

## 6. Component & File Reference Map

```text
SkillSync Project Structure
├── src/main/java/com/skillsync/
│   ├── AppLauncher.java          # JVM Bootstrap entrypoint (bypasses module flags)
│   ├── SkillSyncApp.java         # Core JavaFX Application, stage manager & router
│   ├── User.java                 # Domain entity: credentials, 4-domain scores, matched flag
│   ├── Question.java             # Immutable domain entity: question text, options, answer key
│   ├── TestEngine.java           # Shuffles question pool, administers 10-MCQ tests, calculates scores
│   ├── MatchManager.java         # Candidate filtering, domain intersection, request state machine
│   ├── AdminPanel.java           # Telemetry computation, pass rates, and roster aggregation
│   ├── FileHandler.java          # Thread-safe CSV read/write operations with atomic replacement
│   └── ui/
│       ├── LoginView.java        # Modern split authentication card with student/admin switching
│       ├── RegisterView.java     # Student onboarding with duplicate & delimiter validation
│       ├── MainShellView.java    # Student workspace shell (sidebar navigation + dynamic header)
│       ├── DashboardView.java    # Student portal home: KPI cards, competency summary, shortcuts
│       ├── ProfileView.java      # Student scorecard, academic standings, and active partner card
│       ├── AssessmentView.java   # Domain selector, question cards, option pills, progress indicator
│       ├── AssessmentResultView.java # Celebratory score breakdown with detailed answer review
│       ├── FindPeersView.java    # Dynamic recommendation cards with shared qualified domain chips
│       ├── RequestsView.java     # Teaming management: incoming/outgoing review with Accept/Decline
│       ├── LeaderboardView.java  # Podium rankings, domain chips, live search, and sorting
│       ├── AdminSection.java     # Navigation route enum for administrator portal
│       ├── AdminShellView.java   # Executive dark slate shell with security badges and refresh action
│       ├── AdminDashboardView.java # 6 live system KPIs and 4-domain telemetry charts
│       ├── AdminUsersView.java   # Student directory table with 4-domain score badges
│       ├── AdminStatsView.java   # Domain attempt metrics, pass percentages, and roster tables
│       ├── AdminRequestsView.java# Peer invitation audit trail with status filters (Pending/Accepted/Declined)
│       └── AdminMatchesView.java # Confirmed student partnerships showcase
├── src/main/resources/
│   ├── styles/skillsync.css      # Desktop stylesheet (custom scrollbars, badges, buttons, cards)
│   └── questions/                # Assessment question banks
│       ├── dsa_questions.csv
│       ├── ai_questions.csv
│       ├── robotics_questions.csv
│       └── design_questions.csv
├── users.csv                     # Primary database: student credentials, scores, partner names
├── match_requests.csv            # Teaming request audit log
├── matches.csv                   # Confirmed finalized partnerships log
└── run-gui.bat                   # 1-Click local desktop launcher
```
