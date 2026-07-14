package com.company.hunttech.web.screens.jobcandidate;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JobCandidateEditPerformanceProbeTest {

    @Test
    public void disabledProbeDoesNotWriteLog() {
        AtomicLong clock = new AtomicLong();
        List<String> lines = new ArrayList<>();
        JobCandidateEditPerformanceProbe probe = new JobCandidateEditPerformanceProbe(
                false, clock::get, lines::add);

        probe.start("candidate-1");
        probe.measure("phase", () -> clock.set(10_000_000L));
        probe.finish();

        assertTrue(lines.isEmpty());
    }

    @Test
    public void measureWritesStructuredDuration() {
        AtomicLong clock = new AtomicLong();
        List<String> lines = new ArrayList<>();
        JobCandidateEditPerformanceProbe probe = new JobCandidateEditPerformanceProbe(
                true, clock::get, lines::add);

        probe.start("candidate-1");
        clock.set(5_000_000L);
        probe.measure("onBeforeShow.rating", () -> clock.set(42_000_000L));

        assertEquals(2, lines.size());
        String line = lines.get(1);
        assertTrue(line.startsWith(JobCandidateEditPerformanceProbe.LOG_PREFIX + "|"));
        assertTrue(line.contains("candidateId=candidate-1"));
        assertTrue(line.contains("phase=onBeforeShow.rating"));
        assertTrue(line.contains("elapsedMs=37.000"));
        assertTrue(line.contains("status=OK"));
    }

    @Test
    public void checkpointAndFinishUseExpectedIntervals() {
        AtomicLong clock = new AtomicLong();
        List<String> lines = new ArrayList<>();
        JobCandidateEditPerformanceProbe probe = new JobCandidateEditPerformanceProbe(
                true, clock::get, lines::add);

        probe.start("candidate-2");
        clock.set(15_000_000L);
        probe.checkpoint("framework.autoLoadGap");
        clock.set(65_000_000L);
        probe.finish();

        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("phase=framework.autoLoadGap"));
        assertTrue(lines.get(1).contains("elapsedMs=15.000"));
        assertTrue(lines.get(2).contains("phase=screen.visible.total"));
        assertTrue(lines.get(2).contains("elapsedMs=65.000"));
    }

    @Test
    public void errorIsLoggedAndRethrown() {
        AtomicLong clock = new AtomicLong();
        List<String> lines = new ArrayList<>();
        JobCandidateEditPerformanceProbe probe = new JobCandidateEditPerformanceProbe(
                true, clock::get, lines::add);

        probe.start("candidate-3");
        clock.set(10_000_000L);
        try {
            probe.measure("onBeforeShow.image", () -> {
                clock.set(25_000_000L);
                throw new IllegalStateException("test");
            });
            fail("Ожидалось исключение");
        } catch (IllegalStateException expected) {
            assertEquals("test", expected.getMessage());
        }

        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("phase=onBeforeShow.image"));
        assertTrue(lines.get(1).contains("elapsedMs=15.000"));
        assertTrue(lines.get(1).contains("status=ERROR"));
        assertTrue(lines.get(1).contains("error=java.lang.IllegalStateException"));
    }

    @Test
    public void candidateIdCanBeUpdatedAfterEntityLoad() {
        AtomicLong clock = new AtomicLong();
        List<String> lines = new ArrayList<>();
        JobCandidateEditPerformanceProbe probe = new JobCandidateEditPerformanceProbe(
                true, clock::get, lines::add);

        probe.start("not-loaded");
        probe.setCandidateId("candidate-loaded");
        clock.set(1_000_000L);
        probe.measure("phase", () -> clock.set(2_000_000L));

        assertTrue(lines.get(1).contains("candidateId=candidate-loaded"));
    }
}
