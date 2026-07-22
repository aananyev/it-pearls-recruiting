# AiAnalysisService — системный анализ сущностей HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл

`AiAnalysisService` подготавливает данные вакансии, резюме, кандидата или взаимодействия для системного AI-анализа. Сервис перезагружает экранную detached-сущность в core-tier с минимальным специализированным view, подставляет значения в системный промпт и передаёт сформированный запрос в `HrmAiService`.

### 2. Связи в интерфейсе и Навигация

Сервис вызывается через `AiAnalysisHelper` из AI-кнопок `OpenPositionEdit`, `CandidateCVEdit`, `IteractionListEdit` и других экранов HRM HuntTech. Пользователь не открывает сервис напрямую: экран передаёт сущность и код промпта, а результат отображается в диалоге текущего UI-контекста.

Связанный сервис выбора провайдера: [HrmAiService.md](HrmAiService.md).

### 3. Краткий обзор бизнес-логики поведения

- Нажатие AI-кнопки → экран передаёт detached-сущность → `reloadWithAnalysisView` определяет её тип → выполняется core-загрузка со специализированным view.
- Загрузка вакансии → строится граф `OpenPosition.projectName.projectDepartment.companyName` → экстракторы получают проект, подразделение и компанию без обращения к unfetched-полям.
- Заполнение шаблона → `EntityDataExtractors` заменяет placeholder-ы → сформированный промпт отправляется через текущую AI-конфигурацию пользователя.
- Неизвестный тип сущности → анализ не запускается → возвращается диагностическая `DevelopmentException`.

## 1. Контракт сервиса

| Метод | Назначение |
|---|---|
| `analyze(Entity, String)` | Выполняет полный системный AI-анализ сущности по коду промпта. |
| `reloadWithAnalysisView(Entity)` | Перезагружает detached-сущность с графом, необходимым экстракторам. |
| `buildCandidateCVAnalysisView()` | Формирует граф резюме, вакансии и кандидата. |
| `buildOpenPositionAnalysisView()` | Формирует граф вакансии, проекта, подразделения и компании. |
| `buildIteractionListAnalysisView()` | Формирует граф взаимодействия, типа взаимодействия и кандидата. |
| `buildJobCandidateAnalysisView()` | Формирует минимальный граф кандидата. |

## 2. Специализированный view вакансии

`ViewBuilder.addView(View)` без имени свойства не определяет родительскую связь автоматически. Поэтому каждый уровень графа вакансии привязывается к конкретному property:

```text
OpenPosition
├── shortDescription
├── comment
└── projectName → Project
    ├── projectName
    └── projectDepartment → CompanyDepartament
        └── companyName → Company
            └── comanyName
```

Каноническая конструкция:

```java
ViewBuilder.of(OpenPosition.class)
        .addAll("shortDescription", "comment")
        .addView("projectName",
                ViewBuilder.of(Project.class)
                        .addAll("projectName")
                        .addView("projectDepartment",
                                ViewBuilder.of(CompanyDepartament.class)
                                        .addView("companyName",
                                                ViewBuilder.of(Company.class)
                                                        .addAll("comanyName")
                                                        .build())
                                        .build())
                        .build())
        .build();
```

Без явных property CUBA пытается разрешать `projectDepartment` и `companyName` относительно `OpenPosition`, что приводит к ошибке построения analysis-view до вызова AI-провайдера.

## 3. Legacy-имена модели данных

В `CompanyDepartament` поле `companyName` является ссылкой типа `Company`.

В `Company` строковое наименование исторически называется `comanyName` и соответствует колонке `COMANY_NAME`. Это legacy-идентификатор HRM HuntTech; переименование запрещено в рамках исправления analysis-view.

## 4. Инварианты

- Экранная сущность всегда перезагружается в core-tier до чтения LAZY-связей.
- Каждый вложенный `View` привязан к реальному property родительской сущности.
- `Company.comanyName` сохраняется без переименования.
- Выбор текущего AI-провайдера и отправка промпта остаются в `HrmAiService`.
- API-ключи и полный текст промпта не выводятся в журнал.

## 5. Проверки

Обязательные проверки после изменения:

```bash
./gradlew :app-core:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test --tests '*AiAnalysisOpenPositionViewContractTest*' --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ручной smoke-test:

1. Открыть существующую вакансию с проектом, подразделением и компанией.
2. Нажать AI-анализ в `OpenPositionEdit`.
3. Убедиться, что view строится без `DevelopmentException` по `projectDepartment`, `companyName` или `comanyName`.
4. Подтвердить получение результата либо корректной ошибки реального AI-провайдера.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | `buildOpenPositionAnalysisView()` переведён на `addView(String propertyName, View view)` для проекта, подразделения и компании; подтверждено legacy-поле `Company.comanyName`. |
