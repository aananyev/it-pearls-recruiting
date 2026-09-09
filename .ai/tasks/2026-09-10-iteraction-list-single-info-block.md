# Задача: Объединение всех блоков информации IteractionListEdit в единый блок и удаление label-навигации из sidebar

## Дата: 2026-09-10
## Ветка: agent/antigravity-dev
## Роли: Аналитик, UI/UX-дизайнер, Java Backend-разработчик, Автоматизированный тестировщик

### Описание задачи
1. В экране `IteractionListEdit` все блоки информации объединить в один блок.
2. Зеленые кнопки «Частые взаимодействия» оставить в отдельном блоке (`mostPopularQuickActions`).
3. В sidebar удалить блок label-навигации (`iteractionListNavigation`) за ненадобностью.

### Архитектурное проектирование
#### 1. Sidebar (iteraction-list-edit.xml)
- Удаляется контейнер `<vbox id="iteractionListNavigation" ...>` и его заголовок.
- После карточки сводки вакансии (`vacancyStateSummary`) сразу располагается карточка контекста вакансии (`iteractionVacancyCard`).
- Порядок в sidebar:
  1. `iteractionCandidateNameLabel`
  2. `iteractionVacancyNameLabel`
  3. `iteractionServiceCard` (номер и дата)
  4. `vacancyStateSummary` (статус и приоритет)
  5. `iteractionVacancyCard` (детали вакансии, компания, проект, ставки, рейтинг)
  6. `iteractionListSidebarSpacer`

#### 2. Workspace (iteraction-list-edit.xml)
- Верхний блок быстрых действий `mostPopularQuickActions` («Частые взаимодействия» с зелеными кнопками `mostPopularHbox`) сохраняется как отдельная карточка `edit-card`.
- В прокручиваемой области `iteractionListContentScrollBox` все 4 ранее разрозненных блока (`participantsAccordion`, `interactionAccordion`, `resultAccordion`, `commentAccordion`) объединяются в один единый блок `iteractionMainInfoCard` (`edit-card`):
  - Поля участников: `candidateField`, `vacancyFiels`, чекбокс `onlyMySubscribeCheckBox`.
  - Поля типа взаимодействия: `iteractionTypeField`, динамическая панель `buttonsPanelCallAction` (`buttonCallAction`, `addString`, `addDate`, `addInteger`).
  - Поля результата: `ratingField`, `recrutierField`, `communicationMethodField`.
  - Поле комментария: `commentField`.
- Подвал `editActions` (кнопки подписки, сохранения и отмены) сохраняется без изменений.

#### 3. Java Controller (IteractionListEdit.java)
- Удаляются инжекции `iteractionListNavigation`, `participantsAccordion`, `interactionAccordion`, `resultAccordion`, `commentAccordion`.
- Удаляются навигационные кнопки `participantsAccordionNav`, etc. и методы переключения активных стилей секций.
- Сохраняется метод `onInitIteractionNavigation` для обратной совместимости с наследниками.
- Все бизнес-обработчики, валидации и listeners сохраняются в неизменном виде.

#### 4. Тесты
- Обновить тесты компоновки и сайдбара:
  - `IteractionListEditAccordionLayoutTest`
  - `IteractionListAccordionNavigationTest`
  - `IteractionListVisualAlignmentTest`
  - `IteractionListSidebarContextPanelTest`
  - `IteractionListLayoutStorageContractTest`
  - `IteractionListMostPopularInteractionTest`
- Валидация через `ocr review --audience agent`.

### Статус выполнения
- [x] Анализ требований и архитектурное проектирование
- [x] Модификация XML-дескриптора `iteraction-list-edit.xml`
- [x] Модификация Java-контроллера `IteractionListEdit.java`
- [x] Актуализация SCSS-стилей во всех 7 темах
- [x] Актуализация тестов
- [x] Сборка и запуск тестов Gradle (BUILD SUCCESSFUL)
- [x] OCR review (`ocr review --audience agent`: 0 findings across 9 files)
- [x] Подготовка к коммиту и push в git
