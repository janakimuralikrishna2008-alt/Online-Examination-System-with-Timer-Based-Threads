# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Primary:** Students, pupils, and learners in General Academic & K-12 educational environments preparing for and undertaking multi-subject timed examinations.
- **Secondary:** Teachers, exam proctors, and academic administrators designing curricula, managing subject question banks, supervising live exam sessions, and analyzing student scorecards.

## Product Purpose

"Online Examination System with Timer-Based Threads" delivers a dependable, distraction-free examination platform. It guarantees fair, drift-free, and tamper-resistant student evaluations through high-precision background timer threads, strict double-submission prevention, transactional persistence, and instantaneous pedagogical feedback.

## Positioning

Unlike fragile web quiz widgets vulnerable to browser tab sleep, client clock tampering, or network drops, this system couples an authoritative background thread timer (`ScheduledExecutorService`) with atomic state guards. When the exam deadline expires, the system automatically and transactionally persists the candidate's answers, preventing duplicate submissions or lost progress.

## Operating Context

- School computer labs, institutional test centers, and home study desks running on Windows desktop workstations.
- High-stakes, time-sensitive examination environments requiring reliable uptime and offline fallback resilience.
- Educational workflows: reading curriculum study materials -> attempting self-paced practice quizzes -> taking formal timed examinations -> reviewing instant scorecards with question-level explanations.

## Capabilities and Constraints

- **Core UI Surface:** Desktop Application engineered in Java 21 Swing with modern FlatLaf styling, Event Dispatch Thread (EDT) safety, and specialized frames (WelcomeFrame, LoginFrame, StudentDashboardFrame, ExamTakingFrame, AdminDashboardFrame, ManageQuestionsDialog, TimerDemoDialog).
- **Companion Web UI:** Modern React 18 + TypeScript + Vite frontend communicating with a Spring Boot 3.3.4 REST backend and JWT authentication for flexible web-based access.
- **Timer & Concurrency Engine:** Nanosecond drift-free countdown via `java.util.concurrent.ScheduledExecutorService` with automatic threshold warnings and background database sync.
- **Submission Guard:** `AtomicBoolean` mutex preventing race conditions between candidate manual submission and worker-thread auto-submission.
- **Database Architecture:** MySQL 8.0+ relational schema (`online_exam_db`) with transactional JDBC DAO persistence and an in-memory fallback registry for offline demonstration.
- **Assessment Model:** Multi-subject catalog, topic progression, and multiple-choice questions (4 options, point weights, canonical answer keys, and rationales).

## Brand Commitments

- Exact Project Name: "Online Examination System with Timer-Based Threads".
- Clean, focused, academically rigorous visual identity designed to reduce exam anxiety and maximize clarity.
- Unambiguous timer prominence with distinct amber/red warning states as deadlines approach.

## Evidence on Hand

- Verified, fully passing test suites:
  - 86 JUnit 5 desktop tests (`.\mvnw.cmd test` / `build_and_test.bat`).
  - 10 Spring Boot web backend tests (`..\..\mvnw.cmd clean test package`).
  - Clean TypeScript/Vite frontend production bundle (`npm run build`).
- Relational schema and seed scripts: `database/schema.sql`, `database/sample_data.sql`, `database/migration_v2.sql`.

## Product Principles

- **Authoritative Integrity:** Exam timers and scoring logic are strictly server/thread-authoritative; no client manipulation can alter elapsed duration or earned points.
- **Atomic Safety:** Exam start, answer saves, and final evaluations are transactionally isolated with zero risk of duplicate submissions.
- **Cognitive Calm:** The interface recedes to eliminate distractions, presenting clean typography, obvious navigation, and unmistakable exam status indicators.
- **Educational Transparency:** Scorecards provide instant, comprehensive breakdowns of correct options, candidate selections, and explanatory rationales.

## Accessibility & Inclusion

- Full keyboard accessibility for navigating between questions, selecting answer options (A, B, C, D), and submitting.
- High-contrast visual hierarchy meeting WCAG standards for questions, answer choices, and critical countdown clocks.
- Clear visual differentiation between answered, flagged, and pending questions.
