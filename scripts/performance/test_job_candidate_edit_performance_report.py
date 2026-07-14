#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("job_candidate_edit_performance_report.py")
SPEC = importlib.util.spec_from_file_location("job_candidate_edit_performance_report", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class JobCandidateEditPerformanceReportTest(unittest.TestCase):

    def test_parse_record_from_prefixed_log_line(self):
        line = (
            "2026-07-14 12:00:00 INFO x - "
            "JOB_CANDIDATE_EDIT_PERF|openId=o1|candidateId=c1|"
            "phase=framework.autoLoadGap|elapsedMs=125.500|status=OK|thread=http-nio-8080-exec-1"
        )
        record = MODULE.parse_record(line, 1)
        self.assertIsNotNone(record)
        self.assertEqual("o1", record.open_id)
        self.assertEqual("c1", record.candidate_id)
        self.assertEqual("framework.autoLoadGap", record.phase)
        self.assertEqual(125.5, record.elapsed_ms)

    def test_warmup_is_removed_per_candidate(self):
        records = [
            MODULE.Record(1, "a1", "a", MODULE.TOTAL_PHASE, 1000.0, "OK", "t"),
            MODULE.Record(2, "a2", "a", MODULE.TOTAL_PHASE, 800.0, "OK", "t"),
            MODULE.Record(3, "b1", "b", MODULE.TOTAL_PHASE, 900.0, "OK", "t"),
            MODULE.Record(4, "b2", "b", MODULE.TOTAL_PHASE, 700.0, "OK", "t"),
        ]
        measured = MODULE.remove_warmup(records, 1)
        self.assertEqual(["a2", "b2"], [record.open_id for record in measured])

    def test_phase_statistics_contains_expected_percentile(self):
        records = [
            MODULE.Record(1, "o1", "c", "phase", 10.0, "OK", "t"),
            MODULE.Record(2, "o2", "c", "phase", 20.0, "OK", "t"),
            MODULE.Record(3, "o3", "c", "phase", 30.0, "OK", "t"),
            MODULE.Record(4, "o1", "c", MODULE.TOTAL_PHASE, 100.0, "OK", "t"),
            MODULE.Record(5, "o2", "c", MODULE.TOTAL_PHASE, 100.0, "OK", "t"),
            MODULE.Record(6, "o3", "c", MODULE.TOTAL_PHASE, 100.0, "OK", "t"),
        ]
        rows = {row["phase"]: row for row in MODULE.phase_statistics(records)}
        self.assertEqual(20.0, rows["phase"]["avg_ms"])
        self.assertEqual(20.0, rows["phase"]["p50_ms"])
        self.assertEqual(30.0, rows["phase"]["p95_ms"])
        self.assertEqual(20.0, rows["phase"]["share_percent"])

    def test_main_writes_markdown_and_csv(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            log_path = root / "app.log"
            markdown_path = root / "report.md"
            csv_path = root / "report.csv"
            log_path.write_text(
                "\n".join([
                    "JOB_CANDIDATE_EDIT_PERF|openId=o1|candidateId=c1|phase=screen.open.start|elapsedMs=0.000|status=OK|thread=t",
                    "JOB_CANDIDATE_EDIT_PERF|openId=o1|candidateId=c1|phase=framework.autoLoadGap|elapsedMs=400.000|status=OK|thread=t",
                    "JOB_CANDIDATE_EDIT_PERF|openId=o1|candidateId=c1|phase=screen.visible.total|elapsedMs=900.000|status=OK|thread=t",
                ]),
                encoding="utf-8",
            )

            exit_code = MODULE.main([
                "--log", str(log_path),
                "--markdown", str(markdown_path),
                "--csv", str(csv_path),
                "--warmup-opens", "0",
            ])

            self.assertEqual(0, exit_code)
            self.assertIn("framework.autoLoadGap", markdown_path.read_text(encoding="utf-8"))
            self.assertIn("screen.visible.total", csv_path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
