package com.stockpilot;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import com.stockpilot.test.StockPilotExecutionListener;

import java.io.File;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 * CLI TestRunner executed directly via:
 * java -cp ... com.stockpilot.TestRunner
 * 
 * Executes JUnit tests and outputs the strict JSON test summary to stdout.
 */
public class TestRunner {

    public static void main(String[] args) {
        // Run tests via JUnit platform launcher
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try {
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass("com.stockpilot.test.StockPilotChallengeTests"))
                    .build();

            Launcher launcher = LauncherFactory.create();
            StockPilotExecutionListener listener = new StockPilotExecutionListener();
            launcher.registerTestExecutionListeners(listener);

            // Execute the tests
            launcher.execute(request);

            // If listener reported failures, exit non-zero
            if (listener.getFailedCount() > 0) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        } catch (Throwable t) {
            System.err.println("Failed to run tests: " + t.getMessage());
            System.exit(1);
        }
    }
}
