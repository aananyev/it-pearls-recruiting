# UI-спецификации HRM HuntTech (`docs/ui/`)

Living-документация экранов, окон, фрагментов и составных UI-потоков **без замены** entity living-doc (`docs/entities/`) и architecture spec (`docs/architecture/`).

Правила агента: [living-ui-documentation.mdc](../../.cursor/rules/living-ui-documentation.mdc) · [`.cursorrules`](../../.cursorrules)

---

## Соглашения имён

| Тип | Путь | Когда |
|-----|------|-------|
| **UI Spec (канон)** | `docs/ui/{FormName}_Spec.md` | create / modify / fix UI — GLOBAL UI TRIGGER |
| **Legacy** | `docs/ui/{kebab-name}.md` | документы до введения `_Spec`; при крупных правках — cross-link или миграция |
| **Архив** | `docs/ui/archive/{FormName}_Spec.md` | полное удаление UI из кода |

**`{FormName}`** — `@UiController("…")` (приоритет) или имя класса; примеры: `loginBranded`, `jobCandidateBrowse`, `extMainScreen`.

Шаблон: [templates/ui-template.md](../templates/ui-template.md)

---

## Обязательные разделы UI Spec (6)

1. Точка вызова и контекст (Invocation & Context)
2. Связь с моделью данных (Data & Entity Binding)
3. Иерархия и взаимосвязь форм (Form Hierarchy)
4. Модель поведения и интерактивность (Behavior Model)
5. Логика управляющих элементов (Actions & Buttons Logic)
6. Визуальная компоновка элементов (Visual Layout Schema)

+ **История изменений** (YYYY-MM-DD сверху).

Завершение задачи с правкой UI — Diff-log:

```
Синхронизация UI-документации [Имя_Формы]: ...
```

---

## Каталог UI-документов

| Форма / экран | Controller / id | Тип | Документ | Статус |
|---------------|-----------------|-----|----------|--------|
| Login (брендированный) | `loginBranded` | login screen | [login-screen.md](login-screen.md) | ✅ legacy (kebab); канон при миграции: `loginBranded_Spec.md` |

*Добавляйте строку при создании каждого нового `{FormName}_Spec.md`.*

---

## Связь с документацией сущностей

| Сценарий | Entity doc | UI Spec |
|----------|------------|---------|
| Browse/Edit сущности | `docs/entities/{EntityName}.md` §2 | `docs/ui/{FormName}_Spec.md` — детализация формы |
| Экран без одной entity (login, dashboard) | — | только `docs/ui/{FormName}_Spec.md` |
| Триггер «Сделай документацию сущности …» | `docs/architecture/{EntityName}_Spec.md` | §3–5 architecture могут ссылаться на UI Spec |

Cross-links в шапке Spec ↔ entity/architecture при наличии привязки.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Введён каталог UI Spec, соглашение `{FormName}_Spec.md`, archive/, связь с living-ui-documentation |
