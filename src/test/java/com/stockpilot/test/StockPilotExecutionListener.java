package com.stockpilot.test;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom TestExecutionListener that collects test results and outputs
 * the strict JSON format required by the challenge platform on test completion.
 */
public class StockPilotExecutionListener implements TestExecutionListener {

    private static class BugResult {
        String status;
        long durationMs;
        String error;

        BugResult(String status, long durationMs, String error) {
            this.status = status;
            this.durationMs = durationMs;
            this.error = error;
        }
    }

    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private final Map<String, BugResult> bugResults = new ConcurrentHashMap<>();
    private long suiteStartTime;
    private int lastFailedCount = 0;
    private int lastPassedCount = 0;

    public int getFailedCount() {
        return lastFailedCount;
    }

    public int getPassedCount() {
        return lastPassedCount;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        suiteStartTime = System.currentTimeMillis();
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            startTimes.put(testIdentifier.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            long startTime = startTimes.getOrDefault(testIdentifier.getUniqueId(), System.currentTimeMillis());
            long duration = Math.max(1, System.currentTimeMillis() - startTime);

            String testName = testIdentifier.getDisplayName();
            // Clean up method name if it has parentheses
            if (testName.endsWith("()")) {
                testName = testName.substring(0, testName.length() - 2);
            }

            // Map method or display name to canonical Bug label if marked
            String bugKey = mapToBugKey(testName);
            String status = testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? "passed" : "failed";
            String errorMsg = null;
            if (testExecutionResult.getThrowable().isPresent()) {
                Throwable t = testExecutionResult.getThrowable().get();
                errorMsg = t.getMessage() != null ? t.getMessage() : t.toString();
            }

            bugResults.put(bugKey, new BugResult(status, duration, errorMsg));
        }
    }

    private String mapToBugKey(String name) {
        if (name.contains("Bug 1") || name.equalsIgnoreCase("testBug1_OversellConcurrentOrders")) {
            return "Bug 1: Stock is checked but not reserved atomically";
        }
        if (name.contains("Bug 2") || name.equalsIgnoreCase("testBug2_CancelledOrdersDoNotRestoreAvailableStockCorrectly")) {
            return "Bug 2: Incorrect stock adjustment during cancellation";
        }
        if (name.contains("Bug 3") || name.equalsIgnoreCase("testBug3_StockLookupReturnsWrongWarehouseInventory")) {
            return "Bug 3: Missing or incorrect query condition in stock lookup";
        }
        if (name.contains("Bug 4") || name.equalsIgnoreCase("testBug4_FailedRequestsReturnMisleadingResponses")) {
            return "Bug 4: Incorrect HTTP status or response mapping on order creation failure";
        }
        if (name.contains("Bug 5") || name.equalsIgnoreCase("testBug5_MissingTransactionBoundaryLeavesInventoryCommittedOnFailure")) {
            return "Bug 5: Transaction boundary is missing or incorrectly placed";
        }
        return name;
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        long totalDuration = Math.max(1, System.currentTimeMillis() - suiteStartTime);
        int totalBugs = bugResults.size();
        int passed = 0;
        int failed = 0;

        for (BugResult result : bugResults.values()) {
            if ("passed".equalsIgnoreCase(result.status)) {
                passed++;
            } else {
                failed++;
            }
        }
        this.lastFailedCount = failed;
        this.lastPassedCount = passed;

        // Output strictly the JSON format to stdout
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        for (Map.Entry<String, BugResult> entry : bugResults.entrySet()) {
            BugResult res = entry.getValue();
            json.append("  \"").append(escapeJson(entry.getKey())).append("\": {\n");
            json.append("    \"Status\": \"").append(res.status).append("\",\n");
            json.append("    \"Execution time\": \"").append(res.durationMs).append("ms\"");
            if ("failed".equalsIgnoreCase(res.status) && res.error != null) {
                json.append(",\n    \"Error\": \"").append(escapeJson(res.error.replace("\r", " ").replace("\n", " "))).append("\"");
            }
            json.append("\n  },\n");
        }

        json.append("  \"Passed\": ").append(passed).append(",\n");
        json.append("  \"Failed\": ").append(failed).append(",\n");
        json.append("  \"Total bugs\": ").append(totalBugs).append(",\n");
        json.append("  \"Total Execution time\": \"").append(totalDuration).append("ms\"\n");
        json.append("}\n");

        System.out.println(json.toString());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
