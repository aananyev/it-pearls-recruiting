# UI-спецификации HRM HuntTech

Каталог содержит канонические технические спецификации экранов и фрагментов CUBA Platform. Каждый документ начинается с Business & Context Intro, описывает data containers, loaders, lifecycle, actions, визуальную компоновку и историю изменений.

## Экраны

| Экран | Документ | Назначение |
|---|---|---|
| `JobCandidateEdit` | [JobCandidateEdit_Spec.md](JobCandidateEdit_Spec.md) | Карточка кандидата, вкладки, история вакансий, анализ навыков и защита от OOM |
| `ExtSettingsWindow` | [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md) | Персональные настройки пользователя, интерфейса, email и AI |
| `ExtSettingsWindowAvatar` | [ExtSettingsWindowAvatar_Spec.md](ExtSettingsWindowAvatar_Spec.md) | Круглая фотография пользователя через `OvaFallbackImage` без изменения модели данных |

## Правила актуализации

При изменении XML-дескриптора, Java-контроллера, loader, view, JPQL, actions или поведения экрана соответствующая спецификация обновляется в той же сессии. Новая запись истории изменений добавляется первой строкой таблицы с датой `YYYY-MM-DD`.

### Обязательная синхронизация тем

Если визуальная компоновка или локальный SCSS экрана изменяются хотя бы в одной теме, в той же задаче и в том же PR необходимо проверить и адаптировать все поддерживаемые темы приложения. Частичное обновление одной темы запрещено. Для каждой темы сохраняются одинаковые структура, component ID, размеры, отступы и состояния компонентов; theme-aware различия допускаются только для цветов, прозрачности, контраста и типографики. Hermes обязан собрать все SCSS-темы и выполнить визуальный smoke изменённого экрана в каждой теме.

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

**`{FormName}`** — `@UiController("…")` (приоритет); примеры: `itpearls_JobCandidate.browse`, `itpearls_OpenPosition.edit`. Для legacy-классов без `@UiController` используется имя Java-класса, например `ExtSettingsWindow_Spec.md`.

Шаблон: [templates/ui-template.md](../templates/ui-template.md)

---

## Обязательные разделы UI Spec (6)

**Перед техническими разделами** — [Business & Context Intro](../.cursor/rules/living-documentation.mdc) (3 подраздела: What & Why, UI Context, Behavior Summary). Затем:

1. Точка вызова и контекст (Invocation & Context)
2. Связь с моделью данных (Data & Entity Binding)
3. Иерархия и взаимосвязь форм (Form Hierarchy)
4. Модель поведения и интерактивность (Behavior Model)
5. Логика управляющих элементов (Actions & Buttons Logic)
6. Визуальная компоновка элементов (Visual Layout Schema)

+ **История изменений** (YYYY-MM-DD сверху).

---

## Каталог UI-документов

### ExtSettingsWindow

| Форма / компонент | Controller | Документ |
|---|---|---|
| Окно настроек | `ExtSettingsWindow` | [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md) |
| Аватар вкладки «Обо мне» | `ExtSettingsWindow` | [ExtSettingsWindowAvatar_Spec.md](ExtSettingsWindowAvatar_Spec.md) |

### JobCandidate

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_JobCandidate.browse` | [itpearls_JobCandidate.browse_Spec.md](itpearls_JobCandidate.browse_Spec.md) |
| Edit | `itpearls_JobCandidate.edit` | [itpearls_JobCandidate.edit_Spec.md](itpearls_JobCandidate.edit_Spec.md) · [JobCandidateEdit_Spec.md](JobCandidateEdit_Spec.md) |
| Detail fragment | `itpearls_JobCanidateDetailScreenFragment` | [itpearls_JobCanidateDetailScreenFragment_Spec.md](itpearls_JobCanidateDetailScreenFragment_Spec.md) |
| Image face | `itpearls_JobCandidateImageFace` | [itpearls_JobCandidateImageFace_Spec.md](itpearls_JobCandidateImageFace_Spec.md) |
| Select positions | `itpearls_SelectPersonPositions` | [itpearls_SelectPersonPositions_Spec.md](itpearls_SelectPersonPositions_Spec.md) |

### OpenPosition

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_OpenPosition.browse` | [itpearls_OpenPosition.browse_Spec.md](itpearls_OpenPosition.browse_Spec.md) |
| Edit | `itpearls_OpenPosition.edit` | [itpearls_OpenPosition.edit_Spec.md](itpearls_OpenPosition.edit_Spec.md) |
| Detail fragment | `itpearls_OpenPositionDetailScreenFragment` | [itpearls_OpenPositionDetailScreenFragment_Spec.md](itpearls_OpenPositionDetailScreenFragment_Spec.md) |

### IteractionList

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_IteractionList.browse` | [itpearls_IteractionList.browse_Spec.md](itpearls_IteractionList.browse_Spec.md) |
