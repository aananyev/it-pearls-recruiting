#!/usr/bin/env python3
"""Строит CSV и Markdown-отчёт по структурированным замерам JobCandidateEdit."""

from __future__ import annotations

import argparse
import csv
import math
import statistics
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

PREFIX = "JOB_CANDIDATE_EDIT_PERF|"
TOTAL_PHASE = "screen.visible.total"


@dataclass(frozen=True)
class Record:
    line_no: int
    open_id: str
    candidate_id: str
    phase: str
    elapsed_ms: float
    status: str
    thread: str


def parse_record(line: str, line_no: int) -> Record | None:
    marker = line.find(PREFIX)
    if marker < 0:
        return None

    fields: Dict[str, str] = {}
    for part in line[marker + len(PREFIX):].strip().split("|"):
        if "=" not in part:
            continue
        key, value = part.split("=", 1)
        fields[key] = value

    required = ("openId", "candidateId", "phase", "elapsedMs", "status", "thread")
    if any(key not in fields for key in required):
        raise ValueError(f"Строка {line_no}: отсутствуют обязательные поля: {line.strip()}")

    return Record(
        line_no=line_no,
        open_id=fields["openId"],
        candidate_id=fields["candidateId"],
        phase=fields["phase"],
        elapsed_ms=float(fields["elapsedMs"]),
        status=fields["status"],
        thread=fields["thread"],
    )


def read_records(path: Path) -> List[Record]:
    records: List[Record] = []
    with path.open("r", encoding="utf-8", errors="replace") as source:
        for line_no, line in enumerate(source, start=1):
            record = parse_record(line, line_no)
            if record is not None:
                records.append(record)
    return records


def remove_warmup(records: Sequence[Record], warmup_opens: int) -> List[Record]:
    if warmup_opens <= 0:
        return list(records)

    opens_by_candidate: Dict[str, List[str]] = defaultdict(list)
    seen: Dict[str, set[str]] = defaultdict(set)
    for record in records:
        if record.open_id not in seen[record.candidate_id]:
            seen[record.candidate_id].add(record.open_id)
            opens_by_candidate[record.candidate_id].append(record.open_id)

    excluded = {
        open_id
        for candidate_id, open_ids in opens_by_candidate.items()
        for open_id in open_ids[:warmup_opens]
    }
    return [record for record in records if record.open_id not in excluded]


def percentile(values: Sequence[float], percentile_value: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    rank = max(1, math.ceil(percentile_value * len(ordered)))
    return ordered[rank - 1]


def severity(p95_ms: float) -> str:
    if p95_ms >= 500.0:
        return "КРИТИЧНО"
    if p95_ms >= 200.0:
        return "МЕДЛЕННО"
    if p95_ms >= 50.0:
        return "ВНИМАНИЕ"
    return "НОРМА"


def phase_statistics(records: Sequence[Record]) -> List[dict]:
    values_by_phase: Dict[str, List[float]] = defaultdict(list)
    errors_by_phase: Dict[str, int] = defaultdict(int)
    for record in records:
        values_by_phase[record.phase].append(record.elapsed_ms)
        if record.status != "OK":
            errors_by_phase[record.phase] += 1

    visible_values = values_by_phase.get(TOTAL_PHASE, [])
    visible_average = statistics.fmean(visible_values) if visible_values else 0.0

    rows: List[dict] = []
    for phase, values in values_by_phase.items():
        avg = statistics.fmean(values)
        p50 = statistics.median(values)
        p95 = percentile(values, 0.95)
        maximum = max(values)
        share = (avg / visible_average * 100.0) if visible_average > 0.0 else 0.0
        rows.append({
            "phase": phase,
            "runs": len(values),
            "avg_ms": avg,
            "p50_ms": p50,
            "p95_ms": p95,
            "max_ms": maximum,
            "share_percent": share,
            "errors": errors_by_phase[phase],
            "severity": severity(p95),
        })

    rows.sort(key=lambda row: (row["phase"] == TOTAL_PHASE, row["p95_ms"]), reverse=True)
    return rows


def opening_statistics(records: Sequence[Record]) -> List[dict]:
    by_open: Dict[Tuple[str, str], List[Record]] = defaultdict(list)
    first_line: Dict[Tuple[str, str], int] = {}
    for record in records:
        key = (record.candidate_id, record.open_id)
        by_open[key].append(record)
        first_line.setdefault(key, record.line_no)

    result: List[dict] = []
    for key, open_records in by_open.items():
        candidate_id, open_id = key
        total = next((r.elapsed_ms for r in open_records if r.phase == TOTAL_PHASE), None)
        result.append({
            "candidate_id": candidate_id,
            "open_id": open_id,
            "visible_ms": total,
            "phase_count": len(open_records),
            "errors": sum(1 for r in open_records if r.status != "OK"),
            "first_line": first_line[key],
        })
    result.sort(key=lambda row: row["first_line"])
    return result


def write_csv(path: Path, rows: Sequence[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = [
        "phase", "runs", "avg_ms", "p50_ms", "p95_ms", "max_ms",
        "share_percent", "errors", "severity",
    ]
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            output = dict(row)
            for field in ("avg_ms", "p50_ms", "p95_ms", "max_ms", "share_percent"):
                output[field] = f"{output[field]:.3f}"
            writer.writerow(output)


def format_optional_ms(value: float | None) -> str:
    return "нет данных" if value is None else f"{value:.3f}"


def write_markdown(path: Path,
                   source: Path,
                   warmup_opens: int,
                   records: Sequence[Record],
                   phase_rows: Sequence[dict],
                   opening_rows: Sequence[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    measured_candidates = sorted({record.candidate_id for record in records})
    total_values = [
        record.elapsed_ms for record in records
        if record.phase == TOTAL_PHASE and record.status == "OK"
    ]

    bottlenecks = [
        row for row in phase_rows
        if row["phase"] not in {TOTAL_PHASE, "screen.open.start"}
    ][:15]

    lines: List[str] = [
        "# Отчёт производительности открытия JobCandidateEdit",
        "",
        f"- Источник: `{source}`",
        f"- Исключено прогревочных открытий на кандидата: `{warmup_opens}`",
        f"- Измеренных кандидатов: `{len(measured_candidates)}`",
        f"- Измеренных открытий: `{len(opening_rows)}`",
        f"- Структурированных событий: `{len(records)}`",
        "",
        "## Итоговое время до отображения формы",
        "",
    ]

    if total_values:
        lines.extend([
            f"- Среднее: **{statistics.fmean(total_values):.3f} мс**",
            f"- P50: **{statistics.median(total_values):.3f} мс**",
            f"- P95: **{percentile(total_values, 0.95):.3f} мс**",
            f"- Максимум: **{max(total_values):.3f} мс**",
        ])
    else:
        lines.append("События `screen.visible.total` не найдены.")

    lines.extend([
        "",
        "## Основные задержки",
        "",
        "| Фаза | Запусков | Среднее, мс | P50, мс | P95, мс | Макс., мс | Доля от открытия | Ошибки | Оценка |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---|",
    ])
    for row in bottlenecks:
        lines.append(
            "| `{phase}` | {runs} | {avg_ms:.3f} | {p50_ms:.3f} | {p95_ms:.3f} | "
            "{max_ms:.3f} | {share_percent:.1f}% | {errors} | {severity} |".format(**row)
        )

    lines.extend([
        "",
        "## Все измеренные фазы",
        "",
        "| Фаза | Запусков | Среднее, мс | P50, мс | P95, мс | Макс., мс | Ошибки |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ])
    for row in phase_rows:
        lines.append(
            "| `{phase}` | {runs} | {avg_ms:.3f} | {p50_ms:.3f} | {p95_ms:.3f} | "
            "{max_ms:.3f} | {errors} |".format(**row)
        )

    lines.extend([
        "",
        "## Отдельные открытия",
        "",
        "| № | Candidate ID | Open ID | До отображения, мс | Фаз | Ошибки |",
        "|---:|---|---|---:|---:|---:|",
    ])
    for index, row in enumerate(opening_rows, start=1):
        lines.append(
            f"| {index} | `{row['candidate_id']}` | `{row['open_id']}` | "
            f"{format_optional_ms(row['visible_ms'])} | {row['phase_count']} | {row['errors']} |"
        )

    lines.extend([
        "",
        "## Интерпретация ключевых фаз",
        "",
        "- `framework.autoLoadGap` — время между завершением `onInit` и входом в `onBeforeShow`; сюда попадают `@LoadDataBeforeShow`, загрузка `jobCandidateDl`, entity view и автоматические loaders.",
        "- `onBeforeShow.rating` — агрегатный SQL-запрос средней оценки кандидата.",
        "- `onBeforeShow.candidateImage` — проверка файла фотографии и подготовка ресурса изображения.",
        "- `onBeforeShow.lastInteraction` — синхронный вызов `InteractionService.getLastIteraction()`.",
        "- `tab.initTabCandidate` — инициализация основной вкладки, включая загрузку городов и должностей.",
        "- `onAfterShow.startSkillsBackgroundLoading` — только постановка фоновой задачи; её последующая CPU/SQL-нагрузка анализируется по JFR.",
        "",
    ])

    path.write_text("\n".join(lines), encoding="utf-8")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", required=True, type=Path, help="Лог Tomcat/CUBA")
    parser.add_argument("--markdown", required=True, type=Path, help="Итоговый Markdown")
    parser.add_argument("--csv", required=True, type=Path, help="Итоговый CSV")
    parser.add_argument("--warmup-opens", type=int, default=2,
                        help="Сколько первых открытий каждого кандидата исключить")
    args = parser.parse_args(argv)

    try:
        records = read_records(args.log)
    except (OSError, ValueError) as exception:
        print(f"Ошибка чтения замеров: {exception}", file=sys.stderr)
        return 2

    if not records:
        print(f"В {args.log} не найдены строки {PREFIX}", file=sys.stderr)
        return 3

    measured_records = remove_warmup(records, args.warmup_opens)
    if not measured_records:
        print("После исключения прогрева не осталось измерений", file=sys.stderr)
        return 4

    phase_rows = phase_statistics(measured_records)
    opening_rows = opening_statistics(measured_records)
    write_csv(args.csv, phase_rows)
    write_markdown(
        args.markdown,
        args.log,
        args.warmup_opens,
        measured_records,
        phase_rows,
        opening_rows,
    )
    print(f"Markdown: {args.markdown}")
    print(f"CSV: {args.csv}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
