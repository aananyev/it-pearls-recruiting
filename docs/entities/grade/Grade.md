# Grade — грейд

> Справочник грейдов (Junior/Middle/…).
> Оптимизация: 2026-06-23.

## 4. Представления

| View | Назначение |
|------|------------|
| `grade-browse-view` | Browse |
| `grade-edit-view` | Edit |
| `grade-picker-view` | FK в OpenPosition и др. |

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Edit-форма (`GradeEdit`) приведена к контракту серии справочных Edit-форм: двухпанельная композиция (sidebar 270px + edit-card), полоса-заголовок навигации `dictionary-navigation-title`, штатная заглушка-логотип `OvaFallbackImage` 176×176, presentation-навигация `focusMainSection`; view и data bindings не изменялись (см. `docs/ui/DictionaryEditForms_Spec.md`) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | specialized views, GradeServiceTest |
