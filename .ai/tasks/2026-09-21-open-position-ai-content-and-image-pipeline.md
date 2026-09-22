# Задача: AI-материалы вакансии и единый image pipeline sidebar

**Дата:** 2026-09-21
**Статус:** `WAITING_FOR_HERMES`
**Проект:** HRM HuntTech
**Репозиторий:** `aananyev/it-pearls-recruiting`
**BASE_SHA:** `9d89522112010a41845e941c3da0b864fc5ab3ad`
**Ветка:** `agent/open-position-ai-content-and-image-pipeline`
**PR:** Draft #261 — https://github.com/aananyev/it-pearls-recruiting/pull/261

## Цель

Добавить стандартную AI-генерацию чеклиста, карты поиска и плана собеседования в форме вакансии и Smart Vacancy Creation через единый business-service, а также унифицировать безопасную нормализацию пользовательских изображений sidebar в PNG без изменения дизайна и модели хранения.

## Scope

- три действия «Генерировать» в существующих вкладках вакансии;
- три системные AI-функции/промпта в штатном AI Control Plane;
- единый сервис генерации материалов вакансии для Edit-формы и Smart Vacancy Creation;
- защита непустого текста, фоновые AI-вызовы и стандартные уведомления;
- общий image pipeline: реальная декодируемость, лимиты, PNG, `512×512`, aspect ratio, no upscale, alpha, первый кадр GIF;
- Project sidebar и другие подтверждённые пользовательские image-upload точки;
- TDD, Data View Integrity, документация и инструкция Hermes.

## Ограничения

- production не изменять;
- не создавать новые entity fields и не менять модель FileDescriptor/BLOB;
- не менять layout/SCSS/sidebar design;
- не читать локальные prompt-файлы в runtime;
- не дублировать AI и image-processing логику;
- merge запрещён без прямой команды пользователя.

## Чекпоинты

- [x] Проверены GitHub HEAD и локальный `master`: `9d89522112010a41845e941c3da0b864fc5ab3ad`.
- [x] Проверены открытые PR: `0`.
- [x] Изучены последние отчёты Hermes от 2026-09-21.
- [x] Выполнен системный анализ требований.
- [x] Создан отдельный worktree и ветка от актуального `master`.
- [x] Выполнить code inventory AI, Smart Vacancy Creation, views и image uploads.
- [x] Зафиксировать UI/UX-контракт кнопок и состояний.
- [x] Реализовать вертикальные TDD-срезы backend и frontend.
- [x] Синхронизировать `docs/` и историю изменений.
- [x] Выполнить локальные проверки, automated QA и code review; OCR не запускался, поскольку доступный OCR CLI отправляет кодовый diff внешней модели.
- [x] Commit и push в `agent/open-position-ai-content-and-image-pipeline`.
- [x] Создать Draft PR #261 в `master`.
- [x] Подготовить Hermes-инструкцию с точным HEAD; ждать явного согласия перед запуском внешней проверки.

## Текущий шаг

Draft PR #261 открыт. Локальные проверки: core 43/43, web 6/6, `:app-web:compileJava`, `AllXmlScreensIntegrityTest`, XML lint и `git diff --check` — PASS; локальный code review P1=0/P2=0/P3=1; QA P1=0/P2=0. `ScreenViewIntegrityTest` локально блокируется отсутствующим `modules/core/test/com/company/hunttech/context.xml` и включён в Hermes-инструкцию.

## Следующий шаг

После явного согласия передать Hermes полный PR HEAD для `ScreenViewIntegrityTest` (ожидание 8/8), `clean assemble`, local deploy, HTTP 200, runtime logs, AI/Smart/image smoke и performance-проверок. Production и merge запрещены.
