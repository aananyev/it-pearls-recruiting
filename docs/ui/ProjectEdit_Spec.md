# ProjectEdit (`hunttech_Project.edit`)

Cross-links: [Project](../entities/project/Project.md) · [Project AI upload architecture](../architecture/HRM_HuntTech_Project_Description_AI_Upload.md) · [Edit Screen Shared Style Contract](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)

## Назначение и бизнес-смысл (What & Why)

`ProjectEdit` редактирует проект HRM HuntTech: его наименование, владельца, департамент, даты, чаты, описание, вакансии и шаблон сопроводительного письма. Вкладка «Описание проекта» дополнена безопасным upload-сценарием: пользователь может загрузить PDF, DOCX или TXT с исходным описанием, а AI приводит текст к административно управляемому формату.

Кнопка «Кратко» в той же строке генерирует из описания краткое описание сути проекта (не более 5 предложений) в поле сущности `shortDescription`; sidebar-раздел «Коротко» показывает его, если значение не пустое.

Ключевой принцип после внедрения AI Control Plane: экран проекта не содержит системный prompt, не выбирает AI-провайдера, модель или API-ключ. Экран знает только бизнес-функции `PROJECT_DESCRIPTION_GENERATE` и `PROJECT_SHORT_DESCRIPTION_GENERATE` через `ProjectAiService`; содержание prompt и маршрутизация управляются администратором.

## UI Context & Navigation

Точка входа: `hunttech_Project.browse` → создать/редактировать → `hunttech_Project.edit` → sidebar «Описание проекта» → вкладка `tabProjectDescription`.

Экран остаётся полноэкранным модальным `StandardEditor<Project>` с двухпанельным Edit-layout: слева логотип, идентификация и label-navigation; справа toolbar, `projectTab`, карточки и footer. XML-компоновка вкладки «Описание проекта» не перестраивается: контроллер программно добавляет строку upload/status внутрь существующей `projectDescriptionCard`.

## Behavior Summary

- открыть сохранённый проект → вкладка «Описание проекта» впервые активирована → LOB `projectDescription` лениво перечитывается узким view;
- загрузить PDF/DOCX/TXT → файл успешно принят → текст извлекается локально, временный файл удаляется, извлечённый текст сразу отображается как fallback;
- текст извлечён → AI-функция настроена → `BackgroundWorker` вызывает `ProjectAiService`, результат заменяет fallback в `projectDescription`;
- AI-функция/credential недоступны или provider вернул ошибку → исходный извлечённый текст остаётся в форме, пользователь получает контролируемое предупреждение;
- администратор меняет `SYSTEM_PROMPT`, `PROMPT_TEMPLATE`, provider/model/policy для `PROJECT_DESCRIPTION_GENERATE` → следующий upload использует новую конфигурацию без изменения `ProjectEdit`;
- сохранить проект → стандартный `DataContext` сохраняет текущее значение `projectDescription` вместе с остальными изменениями;
- открыть проект с непустым `shortDescription` → sidebar-раздел «Коротко» виден с текстом краткого описания; при пустом значении раздел скрыт;
- во вкладке «Описание проекта» нажать «Кратко» → текст описания есть → AI генерирует краткое описание (до 5 предложений) → результат записывается в `shortDescription` и сразу появляется в sidebar-разделе «Коротко»;
- нажать «Кратко» при пустом описании → кнопка disabled (текст отсутствует → генерация невозможна);
- AI-функция/credential недоступны при генерации «Кратко» → `shortDescription` не меняется, пользователь получает предупреждение.

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

`projectDescription` и `templateLetter` остаются LOB-полями `Project` и намеренно не включаются в основной `project-edit-view`: при первом открытии вкладок контроллер выполняет `dataManager.reload()` с узким `ViewBuilder`.

Исключение — новое поле `shortDescription` («Коротко о проекте»): оно **включено** в `project-edit-view`, потому что sidebar-раздел «Коротко» должен быть виден сразу при открытии формы (контроллер читает `getShortDescription()` в `onBeforeShowSidebar` и после AI-генерации пишет `setShortDescription()`). Это единственный CLOB в edit-view; на browse-view и другие views поля нет.

AI-upload и «Кратко» не читают незагруженные getters detached-сущностей. Для AI передаются только уже доступные значения `projectName` и текст описания (у «Кратко» — из текущего значения RichTextArea, приведённый к обычному тексту).

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
  --tests '*ProjectEditLayoutContractTest*' \
  --tests '*ProjectAiServiceTest*' \
  --tests '*ProjectDescriptionTextExtractorTest*' \
  --tests '*ProjectDescriptionAiUploadContractTest*' \
  --tests '*ProjectShortDescriptionAiContractTest*' \
  --tests '*ProjectDetachedObjectTest*' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime smoke: PDF/DOCX/TXT, configured AI, unavailable AI fallback, сохранение/повторное открытие проекта, новый проект, повторная загрузка, отсутствие временного файла после извлечения; кнопка «Кратко» disabled/enabled, генерация краткого описания, появление/скрытие sidebar-раздела «Коротко», сохранение `shortDescription` при повторном открытии.

Компоновка рабочей области (2026-08-14): все элементы ввода вкладки «Проект» растянуты на ширину страницы (было 50%); строка дат занимает всю ширину, поля дат делят её поровну; RichTextArea описания и dataGrid вакансий ограничены по ширине локальным SCSS (min-width: 0 / max-width: 100%, как open-position-editor-richtext-variant5 / -table-variant5) — устранён выход за границы экрана.

Логотип проекта в sidebar (2026-08-14): `projectLogoFileImage` (`ovaFallbackImage`) получил XML-атрибут `ovalBackground="#3a3e44"` — тёмно-серая круглая подложка под прозрачный логотип после `removeAllWhite`; атрибут читается `OvaFallbackImageLoader` (общий механизм с `ovalImage`, динамический CSS-класс через `OvalImageBackgroundSupport`).

## 9. Кнопка «Кратко» и sidebar-раздел «Коротко»

### 9.1 Модель данных

Сущность `Project` получила поле `shortDescription` («Коротко о проекте», колонка `SHORT_DESCRIPTION`, CLOB). Значение заполняется только AI (отдельного поля ввода в форме нет) и сохраняется стандартным `DataContext` при commit.

### 9.2 Кнопка «Кратко» (вкладка «Описание проекта»)

Строка upload (создаётся программно в `initProjectDescriptionUpload`) содержит: upload → кнопка «Кратко» → status-label.

| Условие | Поведение |
|---------|-----------|
| Текст RichTextArea пуст (null/пробелы/без контента) | кнопка disabled |
| Текст появился (ввод или lazy load вкладки) | кнопка enabled (обработчик `onProjectDescriptionRichTextAreaValueChange`) |
| Клик по «Кратко» | HTML-контент RichTextArea → обычный текст (`stripHtmlToPlainText`) → `BackgroundWorker` (timeout 120 с) → `ProjectAiService.generateShortDescription(projectName, text)` |
| AI вернул текст | `setShortDescription(result)` на сущности; sidebar-раздел «Коротко» показывается с этим текстом; TRAY-уведомление |
| AI недоступен / ошибка провайдера | `shortDescription` не меняется; WARNING-уведомление; кнопка снова enabled |

### 9.3 Sidebar-раздел «Коротко»

XML-контейнер `projectEditorSidebarShortDescription` (`visible="false"` по умолчанию) расположен между идентификацией (`projectEditorSidebarIdentity`) и навигацией «Разделы». Заголовок — label «Коротко» (`projectSidebarShortDescriptionTitle`, stylename `project-editor-short-description-title`), текст — label `projectSidebarShortDescriptionText` (stylename `project-editor-short-description-text`).

Контроллер (`applyShortDescriptionSidebar`):
- `shortDescription == null || пустая строка` → раздел скрыт;
- иначе → раздел виден, текст заполнен.

Вызывается в `onBeforeShowSidebar` (при открытии формы) и в `done()` AI-задачи (сразу после генерации).

### 9.4 AI Control Plane

```text
ProjectAiService.generateShortDescription(projectName, descriptionText)
    → AiExecutionService.executeText("PROJECT_SHORT_DESCRIPTION_GENERATE", context)
    → AiFunctionConfiguration (capability TEXT_GENERATION)
    → effective credential/model/provider
```

Переменные административного prompt template: `${projectName}`, `${sourceText}`. Seed-миграция `260814-2-addProjectShortDescriptionAiFunction` INSERT-only и идемпотентна; административные prompt/model/policy не перезаписываются.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-14 | «Кратко» и sidebar «Коротко»: поле `Project.shortDescription` (`SHORT_DESCRIPTION`, включено в `project-edit-view`); кнопка «Кратко» в строке upload вкладки «Описание проекта» генерирует краткое описание сути проекта (до 5 предложений) через `PROJECT_SHORT_DESCRIPTION_GENERATE`; раздел sidebar виден только при непустом значении; кнопка disabled без текста описания; SCSS-стили в 7 темах; контракт-тест `ProjectShortDescriptionAiContractTest` |
| 2026-08-14 | Логотип проекта в sidebar: фон-подложка `ovalBackground="#3a3e44"` (тёмно-серая) под прозрачный логотип после removeAllWhite; `OvaFallbackImageLoader` начал читать атрибут `ovalBackground` |
| 2026-08-14 | Исправлен рендер строки дат: обе даты на одной строке одинакового размера (`box.expandRatio="1"` 50/50; два width=100% без expandRatio выталкивали «Окончание проекта» за границы) |
| 2026-08-14 | Компоновка рабочей области: поля вкладки «Проект» на всю ширину (50%→100%, даты делят строку), RichTextArea описания и dataGrid вакансий ограничены по ширине локальным SCSS (min-width: 0 / max-width: 100%) — устранён выход за границы экрана; контрактный тест ProjectEditLayoutContractTest.mainTabInputsSpanFullWidthAndTabsDoNotOverflow |
| 2026-08-12 | Вкладка «Описание проекта» адаптирована к AI Control Plane: upload PDF/DOCX/TXT, raw fallback, background AI и административный prompt `PROJECT_DESCRIPTION_GENERATE` |
| 2026-08-04 | Зафиксирован двухпанельный Edit-layout, label-navigation и lazy-loading тяжёлых вкладок |
