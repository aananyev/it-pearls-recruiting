# ProjectEdit (`hunttech_Project.edit`)

Cross-links: [Project](../entities/project/Project.md) · [Project AI upload architecture](../architecture/HRM_HuntTech_Project_Description_AI_Upload.md) · [Edit Screen Shared Style Contract](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)

## Назначение и бизнес-смысл (What & Why)

`ProjectEdit` редактирует проект HRM HuntTech: его наименование, владельца, департамент, даты, чаты, описание, вакансии и шаблон сопроводительного письма. Вкладка «Описание проекта» дополнена безопасным upload-сценарием: пользователь может загрузить PDF, DOCX или TXT с исходным описанием, а AI приводит текст к административно управляемому формату.

Ключевой принцип после внедрения AI Control Plane: экран проекта не содержит системный prompt, не выбирает AI-провайдера, модель или API-ключ. Экран знает только бизнес-функцию `PROJECT_DESCRIPTION_GENERATE` через `ProjectAiService`; содержание prompt и маршрутизация управляются администратором.

## UI Context & Navigation

Точка входа: `hunttech_Project.browse` → создать/редактировать → `hunttech_Project.edit` → sidebar «Описание проекта» → вкладка `tabProjectDescription`.

Экран остаётся полноэкранным модальным `StandardEditor<Project>` с двухпанельным Edit-layout: слева логотип, идентификация и label-navigation; справа toolbar, `projectTab`, карточки и footer. XML-компоновка вкладки «Описание проекта» не перестраивается: контроллер программно добавляет строку upload/status внутрь существующей `projectDescriptionCard`.

## Behavior Summary

- открыть сохранённый проект → вкладка «Описание проекта» впервые активирована → LOB `projectDescription` лениво перечитывается узким view;
- загрузить PDF/DOCX/TXT → файл успешно принят → текст извлекается локально, временный файл удаляется, извлечённый текст сразу отображается как fallback;
- текст извлечён → AI-функция настроена → `BackgroundWorker` вызывает `ProjectAiService`, результат заменяет fallback в `projectDescription`;
- AI-функция/credential недоступны или provider вернул ошибку → исходный извлечённый текст остаётся в форме, пользователь получает контролируемое предупреждение;
- администратор меняет `SYSTEM_PROMPT`, `PROMPT_TEMPLATE`, provider/model/policy для `PROJECT_DESCRIPTION_GENERATE` → следующий upload использует новую конфигурацию без изменения `ProjectEdit`;
- сохранить проект → стандартный `DataContext` сохраняет текущее значение `projectDescription` вместе с остальными изменениями.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Controller | `hunttech_Project.edit` |
| Descriptor | `project-edit.xml` |
| Edited container | `projectDc` |
| Main view | `project-edit-view` |
| Parent screen | `hunttech_Project.browse` |
| AI facade | `hunttech_ProjectAiService` |
| AI function code | `PROJECT_DESCRIPTION_GENERATE` |

## 2. Data View Integrity

`projectDescription` остаётся существующим LOB-полем `Project`; новых полей, таблиц и связей сущности не добавляется. Поле намеренно не включается в основной `project-edit-view`: при первом открытии вкладки контроллер выполняет `dataManager.reload()` с узким `ViewBuilder.of(Project.class).add("projectDescription")`.

AI-upload не читает незагруженные getters detached-сущностей. Для AI передаются только уже доступные значения `projectName`, имя файла и извлечённый текст.

## 3. Upload-контракт

Поддерживаемые форматы:

| Формат | Извлечение |
|---|---|
| PDF | существующая зависимость PDFBox |
| DOCX | `word/document.xml` через ZIP + StAX, DTD/external entities отключены |
| TXT | UTF-8; fallback Windows-1251 при replacement characters |

Максимальный размер upload: 10 MiB. Legacy `.doc` не принимается.

Файл используется только как транспорт. После извлечения текста `FileDescriptor`/FileStorage очищаются; документ не становится частью `Project` и не создаёт новый бизнес-справочник.

## 4. AI Control Plane

`ProjectEdit` вызывает:

```text
ProjectAiService.processUploadedDescription(projectName, sourceFileName, sourceText)
    → AiExecutionService.executeText("PROJECT_DESCRIPTION_GENERATE", context)
    → AiFunctionConfiguration
    → effective credential/model/provider
    → AIProviderRegistry
```

Доступные переменные административного prompt template:

- `${projectName}` — наименование проекта;
- `${sourceFileName}` — имя исходного файла;
- `${sourceText}` — извлечённый текст.

В screen/controller запрещено дублировать system prompt, provider code, model name или API key.

## 5. Background и отказоустойчивость

AI-вызов выполняется через `BackgroundWorker`, timeout задачи — 120 секунд. На время вызова upload блокируется от повторного запуска. Исходный текст устанавливается в `projectDescription` **до** внешнего AI-вызова, поэтому сетевой сбой не уничтожает пользовательские данные.

Текст файла и результат LLM перед установкой в `RichTextArea` HTML-экранируются. Текст исключений внешнего provider не показывается пользователю и не должен включать credential/payload.

## 6. Существующая бизнес-логика ProjectEdit

Сохраняются без изменения:

- lazy loading `projectDescription`, `templateLetter`, вакансий;
- lookup департамента, владельца и parent project;
- закрытие проекта и предложение закрыть открытые вакансии;
- даты старта/окончания;
- ссылки общих/резюме-чатов;
- логотип проекта;
- label-navigation по четырём вкладкам;
- стандартные save/close actions.

## 7. Модель данных и справочники

Не создаются:

- новые поля `Project`;
- связь `Project ↔ SkillTree`;
- новые записи `SkillTree`;
- автоматическое изменение существующих справочников HRM HuntTech.

Единственное предзаполнение — конфигурационная строка AI-функции `PROJECT_DESCRIPTION_GENERATE` в `HUNTTECH_AI_FUNCTION_CONFIGURATION`. Она относится к AI Control Plane, а не к бизнес-справочнику Project.

## 8. Проверки

Обязательны:

```bash
./gradlew :app-core:test \
  --tests '*ProjectAiServiceTest*' \
  --tests '*ProjectDescriptionTextExtractorTest*' \
  --tests '*ProjectDescriptionAiUploadContractTest*' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime smoke: PDF/DOCX/TXT, configured AI, unavailable AI fallback, сохранение/повторное открытие проекта, новый проект, повторная загрузка, отсутствие временного файла после извлечения.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Вкладка «Описание проекта» адаптирована к AI Control Plane: upload PDF/DOCX/TXT, raw fallback, background AI и административный prompt `PROJECT_DESCRIPTION_GENERATE` |
| 2026-08-04 | Зафиксирован двухпанельный Edit-layout, label-navigation и lazy-loading тяжёлых вкладок |
