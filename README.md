# Online Examination System with Timer-Based Threads

A high-performance, multi-threaded desktop examination platform built with **Java 21**, **Java Swing**, and **MySQL 8**. The application features precise, thread-safe timer countdowns using `ScheduledExecutorService`, secure authentication, and transactional JDBC persistence.

---

## Technology Stack

- **Language:** Java 21 (LTS)
- **User Interface:** Java Swing (with FlatLaf modern UI styling)
- **Concurrency & Timing:** `java.util.concurrent.ScheduledExecutorService`
- **Database:** MySQL 8.0+
- **Database Connectivity:** JDBC (MySQL Connector/J 8.3.0)
- **Build & Dependency Management:** Apache Maven
- **Testing:** JUnit 5 (Jupiter)

---

## Project Structure

```text
Online-Examination-System/
├── pom.xml
├── README.md
├── PROJECT_AUDIT_REPORT.md
├── build_and_test.bat
├── run.bat
├── database/
│   ├── schema.sql
│   ├── sample_data.sql
│   └── migration_v2.sql
└── src/
    ├── main/
    │   ├── java/com/onlineexam/
    │   │   ├── Main.java
    │   │   ├── config/
    │   │   │   ├── BCrypt.java
    │   │   │   ├── DatabaseConfig.java
    │   │   │   ├── DatabaseConnection.java
    │   │   │   ├── DatabaseConnectionTester.java
    │   │   │   └── DatabaseMigration.java
    │   │   ├── model/
    │   │   │   ├── User.java
    │   │   │   ├── Subject.java
    │   │   │   ├── Topic.java
    │   │   │   ├── StudyMaterial.java
    │   │   │   ├── StudentMaterialProgress.java
    │   │   │   ├── Exam.java
    │   │   │   ├── Question.java
    │   │   │   ├── StudentExam.java
    │   │   │   ├── StudentAnswer.java
    │   │   │   └── ExamResult.java
    │   │   ├── dao/
    │   │   │   ├── UserDAO.java
    │   │   │   ├── SubjectDAO.java
    │   │   │   ├── TopicDAO.java
    │   │   │   ├── StudyMaterialDAO.java
    │   │   │   ├── ExamDAO.java
    │   │   │   ├── QuestionDAO.java
    │   │   │   ├── StudentExamDAO.java
    │   │   │   └── StudentAnswerDAO.java
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   ├── SubjectService.java
    │   │   │   ├── TopicService.java
    │   │   │   ├── LearningService.java
    │   │   │   ├── ExamService.java
    │   │   │   ├── AttemptService.java
    │   │   │   └── EvaluationService.java
    │   │   ├── timer/
    │   │   │   ├── ExamTimerListener.java
    │   │   │   └── ExamTimerService.java
    │   │   └── ui/
    │   │       ├── UITheme.java
    │   │       ├── WelcomeFrame.java
    │   │       ├── LoginFrame.java
    │   │       ├── StudentDashboardFrame.java
    │   │       ├── ExamTakingFrame.java
    │   │       ├── ExamResultDialog.java
    │   │       ├── AdminDashboardFrame.java
    │   │       ├── ManageQuestionsDialog.java
    │   │       ├── DatabaseConfigDialog.java
    │   │       └── TimerDemoDialog.java
    │   └── resources/
    │       └── application.properties
    └── test/java/com/onlineexam/
        ├── AppTest.java
        ├── TestRunner.java
        ├── model/
        │   ├── UserModelTest.java
        │   ├── SubjectModelTest.java
        │   ├── TopicModelTest.java
        │   ├── StudyMaterialModelTest.java
        │   ├── ExamModelTest.java
        │   ├── QuestionModelTest.java
        │   └── StudentExamModelTest.java
        ├── timer/
        │   └── ExamTimerServiceTest.java
        ├── service/
        │   ├── AuthServiceTest.java
        │   ├── SubjectServiceTest.java
        │   ├── TopicServiceTest.java
        │   ├── LearningServiceTest.java
        │   ├── ExamServiceTest.java
        │   ├── AttemptServiceTest.java
        │   └── EvaluationServiceTest.java
        └── integration/
            └── DatabaseConnectionIntegrationTest.java
```

---

## Windows Setup Instructions

### 1. Verify Java 21 Installation

Open PowerShell or Command Prompt and run:

```powershell
java -version
javac -version
```

Ensure the output indicates **Java 21** (e.g., `openjdk version "21.0.12"`).

---

### 2. Install and Configure MySQL 8 on Windows

If MySQL is not yet installed:
1. Download the **MySQL Installer for Windows** from [dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/).
2. Select **Server Only** or **Developer Default** (includes MySQL Server 8 and MySQL Workbench).
3. Set your root password during installation and note it down.
4. Ensure the MySQL Windows service is running:
   ```powershell
   Get-Service *mysql*
   ```
   If stopped, start it via:
   ```powershell
   Start-Service -Name MySQL80   # (or MySQL97 / your service name)
   ```

---

### 3. Create Database & Execute Schema and Seed Scripts

Open PowerShell or Command Prompt in the project folder:

```powershell
cd "E:\Murali\Java project\Online-Examination-System"

# Log in to MySQL and run schema DDL
mysql -u root -p < "database/schema.sql"

# Seed initial admin, student, sample exam & questions
mysql -u root -p < "database/sample_data.sql"
```

*Alternatively, open **MySQL Workbench**, load `database/schema.sql`, execute it (⚡), then open and execute `database/sample_data.sql`.*

The schema provisions 5 core tables:
* `users` — Student and Admin accounts, hashed credentials, roles
* `exams` — Exam titles, duration in minutes, total marks, passing marks, active status
* `questions` — Four-choice multiple-choice questions with answer key and explanations
* `student_exams` — Active exam sessions with countdown timers, scores, and status lifecycle
* `student_answers` — Student responses with correctness flags and timestamps

---

### 4. Configure Database Credentials Safely

**Security Notice:** Real database passwords are never hardcoded into source control.

Configure your credentials using Windows Environment Variables:

#### PowerShell:
```powershell
$env:DB_USER="root"
$env:DB_PASSWORD="your_actual_mysql_password"
$env:DB_URL="jdbc:mysql://localhost:3306/online_exam_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

#### Command Prompt (CMD):
```cmd
set DB_USER=root
set DB_PASSWORD=your_actual_mysql_password
```

#### Or via local `application.properties`:
You may also set `db.password=your_password` inside `src/main/resources/application.properties` for local development.

---

### 5. Test Database Connectivity

You can test connectivity to MySQL at any time using 3 different ways:

#### Option A: Using the Standalone CLI Tester
```powershell
$mysqlJar = "C:\Users\nande\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar"
java -cp "bin;$mysqlJar" com.onlineexam.config.DatabaseConnectionTester
```
*Outputs real-time server version, catalog, latency, and comprehensive error diagnostics if credentials or schema need attention.*

#### Option B: Using the Welcome GUI Window
1. Run the application (`java -cp bin com.onlineexam.Main` or `mvn exec:java`).
2. Click the green **"Test DB Connection"** button.
3. A popup will report connection success, server metadata, and latency, or provide troubleshooting guidance.

#### Option C: Running the Integration Test Suite
```powershell
# Run the automated test suite covering all units and integration tests
$m2 = "C:\Users\nande\.m2\repository"
$cp = "bin;bin-test;$m2\org\junit\jupiter\junit-jupiter-api\5.10.2\junit-jupiter-api-5.10.2.jar;$m2\org\junit\jupiter\junit-jupiter-params\5.10.2\junit-jupiter-params-5.10.2.jar;$m2\org\junit\platform\junit-platform-commons\1.10.2\junit-platform-commons-1.10.2.jar;$m2\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;$m2\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;$m2\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar"
java -cp $cp com.onlineexam.TestRunner
```

---

---

### 6. Multi-Threaded Countdown Timer & Live Demonstration

Stage 3 implements an enterprise timer engine powered by `ScheduledExecutorService`:
* **Drift-Free Timing:** Calculates remaining time using authoritative `System.nanoTime()` against a calculated deadline, preventing timer drift caused by thread scheduling delays or garbage collection pauses.
* **Double-Submission Guard:** An `AtomicBoolean` guard and `AtomicReference<TimerState>` state machine guarantee that manual submission and timeout auto-submission can never submit the same exam twice.
* **Thread-Safe UI Updates:** All timer event dispatches (`onTick`, `onWarning`, `onTimeExpired`, `onSubmitted`) are routed to the **Swing Event Dispatch Thread (EDT)**.
* **Database Isolation:** Periodic persistence updates and final submission queries run asynchronously in background thread pools, never blocking user interactions.

#### How to Launch the Timer Demonstration:
1. Run the application:
   ```powershell
   java -cp bin com.onlineexam.Main
   ```
2. Click the indigo **"Launch Timer Demo"** button on the welcome screen.
3. In the diagnostic dialog:
   * Click **"Start Countdown (15s)"** to initiate the multi-threaded timer.
   * Observe real-time seconds, dynamic progress bar, thread execution logs, and warning threshold alerts (at 5 seconds).
   * Test **"Manual Submit"** to verify the double-submission guard.
   * Or let the timer expire to observe automatic submission triggered by the background worker thread.

---

---

### 7. Examination Business Services & Auto-Submission Engine

Stage 4 introduces enterprise business services coordinating DAOs, timers, and scoring:
* **ExamService (`com.onlineexam.service.ExamService`):**
  * Retrieves active examinations, validates configuration parameters, and asserts eligibility before permitting student attempts.
* **AttemptService (`com.onlineexam.service.AttemptService`):**
  * Manages student attempt sessions (`IN_PROGRESS`), enforces single-attempt rules, records answers, and strictly prevents changes after submission.
  * Connects attempts to `ExamTimerService` for automated deadline execution.
* **EvaluationService (`com.onlineexam.service.EvaluationService`):**
  * Strictly scores multiple-choice responses against canonical question answer keys from the database, ignoring any client-supplied scores.
  * Calculates earned marks, percentages, pass/fail status, and question counts (correct, incorrect, unanswered) into an immutable `ExamResult` report.
* **Transaction-Backed Safe Submission:**
  * Uses fine-grained attempt locks (`synchronized (lock)`) and state verification to guarantee that manual submission and timeout auto-submission can never double-submit.
  * Rolls back all database updates atomically if transaction commit fails.

---

## How to Build and Run the Application

### Option 1: One-Click Build & Test (Recommended on Windows)

Use the automated batch script in the project root:

```cmd
build_and_test.bat
```
This script automatically:
1. Compiles all main sources into `bin/` using `javac` (Java 21)
2. Copies resource files into `bin/`
3. Compiles all test sources into `bin-test/`
4. Executes the full JUnit 5 test suite (66 tests) via `TestRunner`

To launch the application:
```cmd
run.bat
```
Or launch directly into the student/admin portal login:
```cmd
run.bat --portal
```

---

### Option 2: Running with Maven (When Maven is installed in PATH)

```powershell
# Clean, compile, and run tests
mvn clean test

# Launch the Application
mvn exec:java

# Package Standalone Executable JAR
mvn clean package
java -jar target/online-examination-system-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```
*(Note: If Maven is not installed in your Windows PATH, use Option 1 or Option 3.)*

---

### Option 3: Compiling & Running Directly with `javac` and `java`

```powershell
cd "E:\Murali\Java project\Online-Examination-System"

# Create output folders
if not exist bin mkdir bin
if not exist bin-test mkdir bin-test

# Compile all source files
powershell -NoProfile -Command "(Get-ChildItem -Path src\main\java -Filter *.java -Recurse | Resolve-Path -Relative) | Out-File -FilePath sources.txt -Encoding ascii"
javac -d bin @sources.txt
del sources.txt

# Copy resources
copy /Y src\main\resources\* bin\

# Launch the Application
$mysqlJar = "C:\Users\nande\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar"
java -cp "bin;$mysqlJar" com.onlineexam.Main
```

---

## Project Roadmap & Verification Status

- [x] **Stage 1: Project Setup & Architecture**
  - Workspace inspection & verification
  - Maven POM creation with Java 21, MySQL Connector/J 8.3.0, JUnit 5.10.2, FlatLaf 3.4.1
  - Clean modular package layout (`com.onlineexam.*`)
  - Initial `Main.java` entry point and Swing `WelcomeFrame`
  - MySQL 8 `schema.sql` and `sample_data.sql` with verified BCrypt password hashes
  - Secure credential management via `DatabaseConfig` (Environment variable precedence, blank default password)

- [x] **Stage 2: Database Connectivity & Entity Models**
  - Resilient JDBC connection manager (`DatabaseConnection.java`) with diagnostics
  - Standalone diagnostic CLI utility (`DatabaseConnectionTester.java`)
  - Interactive "Test DB Connection" action in Swing `WelcomeFrame` and `DatabaseConfigDialog`
  - Java entity models: `User`, `Exam`, `Question`, `StudentExam`, `StudentAnswer` with strict validation
  - DAO Layer: `UserDAO`, `ExamDAO`, `QuestionDAO`, `StudentExamDAO`, `StudentAnswerDAO`
  - Transaction-safe exam submission workflow (`submitExamSessionTransactional`)

- [x] **Stage 3: Multi-Threaded Countdown Timer (`ScheduledExecutorService`)**
  - Drift-free `ExamTimerService` using `System.nanoTime()` deadline calculations
  - Listener interface `ExamTimerListener` with safe Swing EDT dispatching
  - Atomic guard preventing duplicate manual/timeout submissions
  - Global active timer registry preventing concurrent timers for the same attempt ID
  - Background database persistence throttling via `StudentExamDAO`
  - Interactive `TimerDemoDialog` launched from `WelcomeFrame`

- [x] **Stage 4: Examination Business Services & Auto-Submission Engine**
  - `ExamService` for catalog filtering, question retrieval, and eligibility verification
  - `AttemptService` for student attempt lifecycle and answer persistence
  - Authoritative elapsed deadline verification preventing deadline resets when resuming sessions
  - `EvaluationService` for server-side scoring, percentage computation, and pass/fail rules
  - Immutable `ExamResult` report holding detailed metrics
  - Transactional auto-submission and manual submission with rollback handling

- [x] **Stage 5: Student & Admin Swing UI Portals & Live Exam Taking Engine**
  - Modern design tokens and component styling helpers (`UITheme.java`)
  - `LoginFrame`: candidate & administrator portal authentication, one-click demo credentials filling
  - Security fix: Public registration strictly creates `STUDENT` accounts to prevent privilege escalation
  - `StudentDashboardFrame`: examination catalog exploration, attempt history, instructions, and scorecard launch with historical candidate answers
  - `ExamTakingFrame`: live examination experience with real-time multi-threaded timer countdown, question navigation palette (Answered / Flagged / Unanswered), asynchronous background answer persistence, and double-submission protected manual/auto-submission
  - `ExamResultDialog`: comprehensive performance scorecard displaying score, percentage, pass/fail status, and question-by-question breakdown with explanations
  - `AdminDashboardFrame`: exam catalog management (create, edit, delete, toggle active status), question bank authoring (`ManageQuestionsDialog`), candidate results analytics (pass rate %, average score, per-exam filter), and user administration
  - `DatabaseConfigDialog`: interactive JDBC settings manager and live connection tester

- [x] **Stage 6: Comprehensive Security & Quality Audit**
  - Full automated test suite passing (66 unit/integration tests)
  - Fixed seed password hashes in `database/sample_data.sql`
  - Eliminated public privilege escalation in `LoginFrame.java`
  - Added historical answer loading to student scorecard review
  - Added authoritative deadline resume enforcement to prevent cheating via disconnection
  - Generated detailed `PROJECT_AUDIT_REPORT.md`

- [x] **Stage 7: Dynamic Student Learning Hub, Practice Quizzes & Topic Assessment**
  - Schema extension (`migration_v2.sql`) adding `subjects`, `topics`, `study_materials`, `student_material_progress`
  - Zero-downtime idempotent runtime migration runner (`DatabaseMigration.java`)
  - Full CRUD and role-based authorization for Subjects, Topics, and Study Materials (`SubjectService`, `TopicService`, `LearningService`)
  - Safe file attachment handling with path traversal prevention, allowed extension whitelist, and 10MB size limits
  - Student learning progress tracking with visual progress bars and completion percentages
  - Practice Quiz generation directly from topic learning materials vs Formal Timed Examinations
  - Question Bank authoring with subject/topic alignment, answer validation, and interactive question previews
  - Admin Dashboard expanded to 7 management tabs; Student Dashboard expanded to 4 rich tabs
  - Automated test suite expanded to **86 total tests with 100% pass rate**


- [x] **Stage 8: Modern AI-Enhanced Web Application (Spring Boot 3.3.4 + React 19 + AI Tutor)**
  - Full-stack web application located in `web/` (`web/backend` and `web/frontend`)
  - **Spring Boot 3.3.4 REST Backend:**
    - High-performance Spring Security with Stateless JWT authentication (`/api/auth/**`)
    - Role-based authorization (`STUDENT` vs `ADMIN`) with CORS support
    - Reuses production JDBC DAOs and business domain services directly
    - RESTful APIs for Subjects, Topics, Study Materials, Exams, Question Bank, Attempts, and Analytics
    - Modular AI Service supporting Google Gemini API, OpenAI API, and an intelligent offline educational fallback engine
    - Verified with automated backend test suite (`JwtTokenProviderTest`, `AiServiceTest`)
  - **React 19 + TypeScript + Vite 8 Responsive Frontend:**
    - Deep Navy & Electric Indigo design system with accessible contrast and micro-animations
    - Dual Portal Sign In (Student Portal / Admin Console) with 1-click demo credential filling
    - Server-authoritative countdown timer with visual color pulses (< 5m warning, < 1m danger) and automatic deadline submission
    - Question navigation palette with real-time Answered, Flagged, and Unanswered statuses
    - Rich Study Notes viewer with code snippets, copy button, and "Mark as Studied" progress tracking
    - Embedded interactive **AI Study Assistant Drawer** providing explanations, summaries, and exam tips
    - **AI Question Generator** inside Admin Console for 1-click multiple-choice question drafting with distractors and explanations
    - Verifiable Scorecard with celebratory confetti, earned/total score, pass/fail status, and question-by-question review with explanations
    - Zero build errors (`npm run build` passes 100% cleanly)

---

## Launching the Web Application

The project includes one-click launcher scripts for running the web platform:

### Method 1: One-Click Launch All (Recommended)
Double-click or run:
```cmd
run_web.bat
```
This opens two dedicated command windows:
1. **Web Backend (Spring Boot 3.3.4):** Runs on `http://localhost:8080/api`
2. **Web Frontend (React Vite):** Runs on `http://localhost:5173`

### Method 2: Launch Separately
To run backend only:
```cmd
run_web_backend.bat
```
To run frontend only:
```cmd
run_web_frontend.bat
```

### AI Configuration (Optional)
The system works out-of-the-box with an embedded offline knowledge engine. To connect live Gemini AI:
```cmd
set AI_API_KEY=your_gemini_api_key_here
set AI_PROVIDER=gemini
run_web_backend.bat
```

---

## Launching the Desktop Application (Java Swing)

The original Java Swing desktop application remains 100% operational:
```cmd
run.bat
```
To run the automated desktop test suite (86 tests):
```cmd
build_and_test.bat
```

---

### Demo Credentials for Immediate Testing:
- **Student Portal:** `student1` / `student123` (or click **"Student (student1)"** quick-fill button)
- **Admin Portal:** `admin` / `admin123` (or click **"Admin (admin)"** quick-fill button)
- **New Account Registration:** Click **"Register"** to register a new student account.



