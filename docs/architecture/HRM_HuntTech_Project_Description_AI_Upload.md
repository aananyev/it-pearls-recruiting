# Архитектура AI upload описания проекта HRM HuntTech

## Назначение и бизнес-смысл (What & Why)

Документ фиксирует подключение upload-сценария Project к новой стратегии AI Control Plane. Цель — отделить пользовательский workflow загрузки проектного описания от system prompt, модели, provider и credential. Администратор управляет AI-функцией централизованно, а ProjectEdit остаётся стабильным бизнес-потребителем.

## UI Context & Navigation

Путь пользователя: `ProjectBrowse → ProjectEdit → Описание проекта → Загрузить описание`. Путь администратора: `Управление AI → Функции AI → PROJECT_DESCRIPTION_GENERATE`.

## Behavior Summary

- пользователь загружает документ → локальный extractor получает текст → raw text помещается в существующий `Project.projectDescription`;
- raw text готов → `ProjectAiService` передаёт context в `AiExecutionService` → admin-controlled prompt выполняется через эффективное подключение;
- AI success → описание заменяется результатом;
- AI failure → raw text сохраняется как fallback;
- администратор меняет prompt/provider/model/policy → код ProjectEdit не меняется.

## 1. Компоненты

```text
ProjectBrowse
  → ProjectEdit
      → ProjectDescriptionTextExtractor
      → ProjectAiService
          → AiExecutionService
              → AiFunctionConfiguration(PROJECT_DESCRIPTION_GENERATE)
              → UserAiFunctionOverride / AdminAiConfiguration
              → AIProviderRegistry
```

## 2. Function contract

Code: `PROJECT_DESCRIPTION_GENERATE`.

Capability: `DOCUMENT_ANALYSIS`.

Template variables: `${projectName}`, `${sourceFileName}`, `${sourceText}`.

Seed policy: `USER_OVERRIDE_ALLOWED`; fallback: `FALLBACK_TO_ADMIN`. Seed не привязывает corporate configuration и не содержит credential. Администратор вправе изменить policy, prompt, model и подключение после миграции.

## 3. Почему не изменяется Project/SkillTree

Текущая сущность `Project` не имеет коллекции skills. В рамках задачи нет подтверждённого бизнес-контракта, по которому AI должен создавать/назначать элементы глобального `SkillTree`. Поэтому новая связь и автоматическое наполнение справочника не вводятся. Результат upload сохраняется только в существующее `Project.projectDescription`.

Это предотвращает неявное создание справочных данных и миграционные риски на production.

## 4. Файловый контур

PDF/DOCX/TXT до 10 MiB. Документ является временным транспортом и не хранится как часть Project. DOCX парсится без внешних XML entities. Выходной текст экранируется перед RichTextArea.

## 5. Failure isolation

Внешний AI вызывается только после установки raw fallback. Ошибка resolver/provider не должна обнулять поле описания и не должна раскрывать credential. BackgroundWorker отделяет сетевую задержку от UI thread.

## 6. Production data

Требуется одна идемпотентная конфигурационная запись AI-функции. Отдельный production script:

`modules/core/db/update/postgres/26/260812-2-addProjectDescriptionAiFunction.sql`.

Подробный runbook: [../database/migrations/project-description-ai-function-2026-08-12.md](../database/migrations/project-description-ai-function-2026-08-12.md).

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Зафиксирован Project description upload как function-based потребитель AI Control Plane |
