# Recruiter Dashboards — checkpoint

Дата: 2026-09-19  
Ветка: `agent/recruiter-dashboards-v1`  
Base: `master@e4241e8afa98436f5ceba58053dac6dbfddb42f0`

## Реализовано

- Dashboard «Кандидаты в работе — Kanban».
- Dashboard «Воронка найма».
- Dashboard «Кадровый резерв».
- Три applet-класса с `@DashboardWidget` и `RefreshableWidget`.
- Три JSON-модели Dashboard Add-on через `jsonPath`.
- Навигация «Подбор → Дашборды рекрутера».
- Единый SCSS-контракт во всех семи темах HuntTech.
- UI-spec и контрактный тест.

## Проверено в connector-среде

- JSON parse трёх dashboard-моделей: PASS.
- frameId JSON ↔ UiController: PASS.
- семь копий `recruiter-dashboard-shared-styles.scss`: IDENTICAL.
- branch base: master, branch ahead, behind=0.

## Проверки, требующие локального build-окружения Hermes

```bash
./gradlew :app-core:test --tests 'com.company.hunttech.core.RecruiterDashboardsContractTest' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

После сборки: local deploy и visual smoke на 1366×768, 1920×1080, 1920×1200 во всех поддерживаемых темах.

## Production

Не изменён.
