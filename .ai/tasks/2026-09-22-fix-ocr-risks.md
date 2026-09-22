# Исправление OCR-рисков — 2026-09-22

- **Цель:** устранить подтверждённые OCR-дефекты после merge PR #260–#270 и передеплоить только LOCAL.
- **База:** `0c1885abe4e677a3895a3d7b833956873f1e6132`.
- **Production:** не используется и не изменяется.

## Исправления

1. CandidateVacancyMatch: строки с `height=AUTO` больше не растягиваются по вертикали; wrap/gap применяются к корневым action/filter/quick rows; details ScrollBox оставляет вертикальный scroll на корневом scroll-контейнере. Все 7 тем синхронизированы.
2. MarkdownRenderer: продолжение таблицы требует совпадающего количества колонок и стиля внешних pipe; single-column таблицы сохранены; строка с другим стилем внешних pipe закрывает таблицу и остаётся обычным текстом.
3. Sidebar image normalization: JPEG EXIF scanner безопасно обрабатывает обрыв на fill-byte в конце входа.
4. Sidebar image upload: ошибки валидации изображения и внутренние ошибки storage/UI разделены; для внутренних ошибок не показывается сообщение о неверном формате, прежнее изображение сохраняется.
5. IteractionList timefield: error/focus border rules имеют приоритет над service-card базовым цветом во всех 7 темах.

## Проверки

- Targeted core/web tests: PASS.
- `git diff --check`: PASS.
- Полный `scripts/start-app.sh --branch` выполняется после коммита; миграций в этой ветке нет.
- Ручной UI smoke остаётся отдельным шагом; localhost browser policy может блокировать встроенный браузер.
