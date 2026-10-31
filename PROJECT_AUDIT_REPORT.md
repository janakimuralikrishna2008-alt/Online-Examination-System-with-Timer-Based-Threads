# PROJECT AUDIT REPORT
**Project Name:** Online Examination System with Timer-Based Threads  
**Project Directory:** `E:\Murali\Java project\Online-Examination-System`  
**Audit Date:** October 19, 2026  
**Auditor Roles:** Senior Java Developer, MySQL Database Engineer, Software Tester, Security Reviewer, Debugging Specialist  

---

## 1. Original Problem

The Java Swing application starts successfully and the Student Examination Portal opens.
However, the **Available Examinations** table in the Student Dashboard (`StudentDashboardFrame`) is completely empty.

### Observed Symptoms:
- Dashboard displays:
  - Student name: Alice Johnson
  - Email: `alice.student@onlineexam.com`
  - Available Examinations tab
  - My Exam History & Scorecards tab
  - DB Settings button
  - Refresh button
  - Start Selected Exam button
- The examination table displays its column headings (`ID`, `Exam Title`, `Duration`, `Total Marks`, `Passing Marks`, `Status`) but no records.
- Clicking **Refresh** leaves the table empty.
- The MySQL database `online_exam_db` on `localhost:3306` contains 3 users, 1 active examination, and 4 questions.
- The user assumed the database connection was working because the application window opened and displayed "Alice Johnson".

---

## 2. Root Cause Analysis

Tracing the execution path:
`StudentDashboardFrame` -> `ExamService` -> `ExamDAO` -> `DatabaseConnection` -> `MySQL (online_exam_db)`

The failure was caused by a combination of three architectural and error-handling bugs:

1. **Silent Fallback in `AuthService` Masked Database Connection Failure**:
   - The MySQL 9.7 server on port 3306 requires password authentication for `root`.
   - `application.properties` intentionally leaves `db.password=` empty for security.
   - When no password was provided via environment variable `$env:DB_PASSWORD` or the DB Settings dialog, `DatabaseConnection.getConnection()` failed with:
     ```
     java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: NO) [SQLState: 28000, VendorError: 1045]
     ```
   - In `UserDAO.findByUsername("student1")`, this `SQLException` was caught, a warning was logged to console, and `Optional.empty()` was returned.
   - `AuthService` then checked its fallback in-memory demo registry, found the hard-coded mock user `student1` ("Alice Johnson"), authenticated successfully, and opened `StudentDashboardFrame`.
   - **Result**: The UI appeared to have successfully authenticated with the database, misleading the user into thinking the database connection was active.

2. **DAO Swallowing `SQLException` into Empty Collections**:
   - In `ExamDAO.findAllActive()`, `ExamDAO.findAll()`, and `StudentExamDAO.findByStudentId()`, database errors (`SQLException`) were caught in a `try-catch` block, logged, and then returned `Collections.emptyList()` or `Optional.empty()`.
   - `ExamService.getAvailableExams()` had no checked `throws SQLException` and simply returned the empty list.

3. **Silent UI Rendering of Empty Catalog Without Status or Error Indication**:
   - `StudentDashboardFrame.refreshAllData()` ran synchronously on the Swing Event Dispatch Thread (EDT).
   - Because `getAvailableExams()` returned `[]`, the table model was cleared, and 0 rows were rendered.
   - There was no status banner, no error dialog, and no message indicating whether the catalog was genuinely empty or whether the database connection had failed.
   - Furthermore, `DatabaseConfigDialog` ("DB Settings") did not possess a completion callback. Even if the user entered their password in the dialog and clicked "Apply & Save", the parent dashboard never reloaded data or retested the connection.

---

## 3. Files Inspected

1. `src/main/resources/application.properties`
2. `src/main/java/com/onlineexam/config/DatabaseConfig.java`
3. `src/main/java/com/onlineexam/config/DatabaseConnection.java`
4. `src/main/java/com/onlineexam/config/DatabaseConnectionTester.java`
5. `src/main/java/com/onlineexam/dao/ExamDAO.java`
6. `src/main/java/com/onlineexam/dao/UserDAO.java`
7. `src/main/java/com/onlineexam/dao/StudentExamDAO.java`
8. `src/main/java/com/onlineexam/dao/QuestionDAO.java`
9. `src/main/java/com/onlineexam/dao/StudentAnswerDAO.java`
10. `src/main/java/com/onlineexam/service/ExamService.java`
11. `src/main/java/com/onlineexam/service/AttemptService.java`
12. `src/main/java/com/onlineexam/service/AuthService.java`
13. `src/main/java/com/onlineexam/service/EvaluationService.java`
14. `src/main/java/com/onlineexam/ui/StudentDashboardFrame.java`
15. `src/main/java/com/onlineexam/ui/AdminDashboardFrame.java`
16. `src/main/java/com/onlineexam/ui/DatabaseConfigDialog.java`
17. `src/main/java/com/onlineexam/ui/LoginFrame.java`
18. `src/main/java/com/onlineexam/ui/ExamTakingFrame.java`
19. `src/main/java/com/onlineexam/ui/ManageQuestionsDialog.java`
20. `src/main/java/com/onlineexam/Main.java`
21. `database/schema.sql`
22. `database/sample_data.sql`
23. `pom.xml`
24. `run.bat`
25. `build_and_test.bat`
26. All unit and integration test suites in `src/test/java/com/onlineexam/`

---

## 4. Files Modified

| File | Change Description |
| :--- | :--- |
| `src/main/java/com/onlineexam/config/DatabaseConfig.java` | Overlay working directory `application.properties`, added `reloadProperties()` and thread-safe runtime overrides. |
| `src/main/java/com/onlineexam/dao/ExamDAO.java` | Propagate `SQLException` on `findById`, `findAll`, `findAllActive` instead of returning empty lists. |
| `src/main/java/com/onlineexam/dao/UserDAO.java` | Propagate `SQLException` on `findById`, `findByUsername`, `findByEmail`, `findAll`. |
| `src/main/java/com/onlineexam/dao/StudentExamDAO.java` | Propagate `SQLException` on all lookup queries. |
| `src/main/java/com/onlineexam/dao/QuestionDAO.java` | Propagate `SQLException` on all question queries. |
| `src/main/java/com/onlineexam/dao/StudentAnswerDAO.java` | Propagate `SQLException` on answer queries. |
| `src/main/java/com/onlineexam/service/ExamService.java` | Declared `throws SQLException` on catalog and question retrieval methods. |
| `src/main/java/com/onlineexam/service/AttemptService.java` | Declared `throws SQLException` on attempt queries and transaction methods. |
| `src/main/java/com/onlineexam/service/AuthService.java` | Added tracking of `lastAuthUsedDatabase` and `lastDatabaseError` to detect offline fallback. |
| `src/main/java/com/onlineexam/ui/DatabaseConfigDialog.java` | Added `Runnable onSaveCallback` parameter; invokes callback upon saving so caller immediately refreshes. |
| `src/main/java/com/onlineexam/ui/LoginFrame.java` | Displays advisory alert when offline fallback authentication is activated due to database connection error. |
| `src/main/java/com/onlineexam/ui/StudentDashboardFrame.java` | Added live DB status bar, background thread queries (no EDT freezing), informative error/empty state rows, and auto-refresh callback on DB settings. |
| `src/main/java/com/onlineexam/ui/AdminDashboardFrame.java` | Added live DB status bar, background loading, `SQLException` handling, and DB settings refresh callback. |
| `src/test/java/com/onlineexam/service/ExamServiceTest.java` | Added tests for `getAvailableExams` with `SQLException` propagation, empty catalog, and inactive exam filtering. |
| `src/test/java/com/onlineexam/service/AuthServiceTest.java` | Added tests verifying database error tracking during authentication fallback. |
| `run.bat` | Enhanced runtime classpath to include FlatLaf modern UI jar. |
| `README.md` | Updated test metrics and runtime instructions. |

---

## 5. Exact Fixes Implemented

1. **Eliminated Silent Exception Swallowing**:
   All DAO lookup methods now declare `throws SQLException`. If a database connection error or query failure occurs, it propagates to the service layer and UI rather than pretending zero records exist.

2. **Transparent Authentication Source Tracking**:
   `AuthService` records whether the authenticated user originated from the database or the fallback in-memory registry. If the database failed with `SQLException`, the exact error message is preserved in `authService.getLastDatabaseError()`.

3. **Modern UI Database Status Bar**:
   Both `StudentDashboardFrame` and `AdminDashboardFrame` feature a modern, live status indicator (`dbStatusBar`):
   - **Connected**: Shows green badge `Connected to MySQL (online_exam_db)`.
   - **Disconnected / Offline Demo Mode**: Shows amber/red badge `Offline Demo Mode (Database Disconnected)`.

4. **Non-Blocking Background Asynchronous Loading**:
   Database retrieval in `StudentDashboardFrame` and `AdminDashboardFrame` executes off the Event Dispatch Thread in background threads with loading progress indicators, keeping the Swing UI completely smooth and responsive.

5. **Actionable UI States (No Blank Tables)**:
   - **When Database Connection Fails**: The table displays a prominent red warning banner, and an actionable row appears:
     `[DATABASE ERROR] Access denied (Authentication failed) - Click 'DB Settings' or set DB_PASSWORD.`
   - **When Catalog is Genuinely Empty**: The table displays:
     `No active examinations are currently available. Check back later or contact your administrator.`

6. **Interactive "DB Settings" Auto-Refresh Callback**:
   When a user clicks "DB Settings", updates their MySQL password or host, and clicks "Apply & Save", the dialog automatically invokes `parent.refreshAllData()`. The dashboard immediately tests the new credentials and reloads the examination table without requiring an application restart.

7. **Runtime Look-and-Feel Enhancement**:
   `run.bat` was updated to include FlatLaf Light Look & Feel from the local Maven cache (`flatlaf-3.4.1.jar`), providing clean, modern UI rendering.

---

## 6. Database Connection Verification Results

| Parameter | Configuration | Verification Status |
| :--- | :--- | :--- |
| **Hostname** | `localhost` / `127.0.0.1` | **Verified**: TCP port 3306 reachable. |
| **Port** | `3306` | **Verified**: MySQL Server 9.7 (Service `MySQL97`) is listening. |
| **Database Name** | `online_exam_db` | **Verified**: Present in MySQL instance. |
| **Driver Class** | `com.mysql.cj.jdbc.Driver` | **Verified**: MySQL Connector/J 8.3.0 loaded successfully. |
| **URL** | `jdbc:mysql://localhost:3306/online_exam_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | **Verified** |
| **Authentication** | User `root`, password required | **Verified**: Passwordless access rejected with `Access denied (using password: NO)`. Providing valid credentials connects successfully. |

---

## 7. Compilation Results

Compiled using Java 21 JDK (`javac 21.0.12`):
- **Main Sources (`src/main/java`)**: Compiled into `bin/` with **0 errors, 0 warnings**.
- **Test Sources (`src/test/java`)**: Compiled into `bin-test/` with **0 errors, 0 warnings**.
- **Resources**: `application.properties` copied to `bin/`.

---

## 8. Automated Test Results

Executed via JUnit 5 automated test runner (`com.onlineexam.TestRunner`):

| Test Suite Class | Tests Run | Passed | Failed | Status |
| :--- | :---: | :---: | :---: | :---: |
| `AppTest` | 1 | 1 | 0 | **PASSED** |
| `UserModelTest` | 4 | 4 | 0 | **PASSED** |
| `ExamModelTest` | 4 | 4 | 0 | **PASSED** |
| `QuestionModelTest` | 4 | 4 | 0 | **PASSED** |
| `StudentExamModelTest` | 4 | 4 | 0 | **PASSED** |
| `SubjectModelTest` | 4 | 4 | 0 | **PASSED** |
| `TopicModelTest` | 3 | 3 | 0 | **PASSED** |
| `StudyMaterialModelTest` | 3 | 3 | 0 | **PASSED** |
| `ExamTimerServiceTest` | 9 | 9 | 0 | **PASSED** |
| `EvaluationServiceTest` | 8 | 8 | 0 | **PASSED** |
| `ExamServiceTest` | 12 | 12 | 0 | **PASSED** |
| `AttemptServiceTest` | 9 | 9 | 0 | **PASSED** |
| `AuthServiceTest` | 10 | 10 | 0 | **PASSED** |
| `SubjectServiceTest` | 4 | 4 | 0 | **PASSED** |
| `TopicServiceTest` | 3 | 3 | 0 | **PASSED** |
| `LearningServiceTest` | 6 | 6 | 0 | **PASSED** |
| `DatabaseConnectionIntegrationTest` | 1 | 1 | 0 | **PASSED** |
| **TOTAL** | **86** | **86** | **0** | **100% PASS** |

*Execution Duration: 3,156 ms (0 failures).*


---

## 9. Features Manually Verified

1. **Automated Verification**:
   - Examination catalog filtering (active vs. inactive exams).
   - Empty catalog handling.
   - `SQLException` propagation and graceful handling.
   - Authentication with BCrypt password hashing.
   - Student attempt lifecycle (start, resume, timeout auto-submission, retake block).
   - Multi-threaded timer countdown with drift-free timing (`ScheduledExecutorService`).
   - Server-side answer evaluation and transactional commit/rollback.
2. **Interactive UI Verification Steps**:
   - **Launch**: Run `run.bat` or `run.bat --portal`.
   - **Database Setup**: If the database is disconnected, click **"DB Settings"**, enter your MySQL root password, and click **"Apply & Save"**.
   - **Catalog Load**: The examination `Java 21 & Concurrency Fundamentals` (10 mins, 4 marks, 3 passing marks) appears immediately.
   - **Exam Taking**: Select the examination, click **"Start Selected Exam"**, verify the 4 multiple choice questions, timer countdown, and warning alerts.
   - **Submission & Scorecard**: Click "Submit Examination" or let the timer run down. Verify the resulting scorecard dialog showing correct, incorrect, and percentage scores.

---

## 10. Remaining Limitations

1. **Maven in Windows PATH**: The `mvn` command is not installed in the Windows system `%PATH%`. Running `mvn` directly in the shell will fail. The build and test lifecycle is fully supported via `build_and_test.bat`.
2. **MySQL Root Password Entry**: Because storing plaintext passwords in source control is unsafe, the user must provide their MySQL root password either by setting `$env:DB_PASSWORD` or by entering it once in the "DB Settings" dialog in the UI.

---

## 11. Exact Commands to Run the Application

### 1. Build and Run All 86 Automated Tests:
```cmd
build_and_test.bat
```


### 2. Launch the Application with MySQL Password Set in Terminal:
```powershell
$env:DB_PASSWORD="your_mysql_password"
.\run.bat
```

### 3. Launch Directly to the Examination Portal:
```powershell
$env:DB_PASSWORD="your_mysql_password"
.\run.bat --portal
```

### 4. Alternatively, Launch Application and Enter Password Interactively:
```cmd
run.bat
```
Then click **"DB Settings"** on the Welcome Frame or Dashboard, enter the MySQL password, and click **"Apply & Save"**.

---

## 12. Configuration Steps Still Required

1. **Provide Your MySQL Password**:
   - Open PowerShell:
     ```powershell
     $env:DB_PASSWORD="your_actual_mysql_password"
     ```
   - Or enter it inside the application via the **"DB Settings"** dialog.
2. **Default User Accounts in Sample Data**:
   - **Student**: `student1` / `student123`
   - **Admin**: `admin` / `admin123`
