package com.onlineexam;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Lightweight test runner executing JUnit 5 test suites directly.
 * Features full @BeforeEach and @AfterEach lifecycle support.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("  Online Examination System with Timer-Based Threads");
        System.out.println("  JUnit 5 Automated Test Suite Execution");
        System.out.println("===================================================================");

        List<Class<?>> testClasses = List.of(
                com.onlineexam.AppTest.class,
                com.onlineexam.model.UserModelTest.class,
                com.onlineexam.model.ExamModelTest.class,
                com.onlineexam.model.QuestionModelTest.class,
                com.onlineexam.model.StudentExamModelTest.class,
                com.onlineexam.model.SubjectModelTest.class,
                com.onlineexam.model.TopicModelTest.class,
                com.onlineexam.model.StudyMaterialModelTest.class,
                com.onlineexam.timer.ExamTimerServiceTest.class,
                com.onlineexam.service.EvaluationServiceTest.class,
                com.onlineexam.service.ExamServiceTest.class,
                com.onlineexam.service.AttemptServiceTest.class,
                com.onlineexam.service.AuthServiceTest.class,
                com.onlineexam.service.SubjectServiceTest.class,
                com.onlineexam.service.TopicServiceTest.class,
                com.onlineexam.service.LearningServiceTest.class,
                com.onlineexam.integration.DatabaseConnectionIntegrationTest.class
        );


        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        long totalStartTime = System.currentTimeMillis();

        for (Class<?> clazz : testClasses) {
            System.out.println("\n[SUITE] " + clazz.getSimpleName());
            Method[] methods = clazz.getDeclaredMethods();
            Method beforeEachMethod = findLifecycleMethod(clazz, BeforeEach.class);
            Method afterEachMethod = findLifecycleMethod(clazz, AfterEach.class);

            for (Method method : methods) {
                if (method.isAnnotationPresent(Test.class)) {
                    totalTests++;
                    String testName = getTestDisplayName(method);
                    Object instance = null;
                    try {
                        instance = clazz.getDeclaredConstructor().newInstance();
                        invokeLifecycle(beforeEachMethod, instance);
                        method.setAccessible(true);
                        method.invoke(instance);
                        passedTests++;
                        System.out.printf("  PASSED: %-60s%n", testName);
                    } catch (Exception e) {
                        failedTests++;
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        System.err.printf("  FAILED: %-60s (Reason: %s)%n", testName, cause.getMessage());
                    } finally {
                        invokeLifecycle(afterEachMethod, instance);
                    }
                } else if (method.isAnnotationPresent(ParameterizedTest.class)) {
                    ValueSource vs = method.getAnnotation(ValueSource.class);
                    if (vs != null && vs.strings().length > 0) {
                        for (String arg : vs.strings()) {
                            totalTests++;
                            String testName = getTestDisplayName(method) + " [arg=\"" + arg + "\"]";
                            Object instance = null;
                            try {
                                instance = clazz.getDeclaredConstructor().newInstance();
                                invokeLifecycle(beforeEachMethod, instance);
                                method.setAccessible(true);
                                method.invoke(instance, arg);
                                passedTests++;
                                System.out.printf("  PASSED: %-60s%n", testName);
                            } catch (Exception e) {
                                failedTests++;
                                Throwable cause = e.getCause() != null ? e.getCause() : e;
                                System.err.printf("  FAILED: %-60s (Reason: %s)%n", testName, cause.getMessage());
                            } finally {
                                invokeLifecycle(afterEachMethod, instance);
                            }
                        }
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - totalStartTime;
        System.out.println("\n===================================================================");
        System.out.printf("TEST SUMMARY: Total: %d, Passed: %d, Failed: %d (Duration: %d ms)%n",
                totalTests, passedTests, failedTests, duration);
        System.out.println("===================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static Method findLifecycleMethod(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotation) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static void invokeLifecycle(Method method, Object instance) {
        if (method != null && instance != null) {
            try {
                method.invoke(instance);
            } catch (Exception ex) {
                System.err.println("Warning: Lifecycle method " + method.getName() + " failed: " + ex.getMessage());
            }
        }
    }

    private static String getTestDisplayName(Method method) {
        DisplayName displayName = method.getAnnotation(DisplayName.class);
        return (displayName != null) ? displayName.value() : method.getName();
    }
}
