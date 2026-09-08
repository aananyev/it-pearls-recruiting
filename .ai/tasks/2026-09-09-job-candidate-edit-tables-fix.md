# Анализ и задача: Восстановление отображения таблиц во вкладках «Взаимодействия» и «Резюме и файлы» в JobCandidateEdit

**Дата**: 2026-09-09  
**Исполнители**: Аналитик, UI/UX-дизайнер, Frontend-разработчик  
**Область**: `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml`, темы оформления SCSS (`job-candidate-editor.scss`).

---

## 1. Описание проблемы
В экране редактирования кандидата (`JobCandidateEdit`) во вкладках «Список резюме» (`tabResume`) и «Список взаимодействий» (`tabIteraction`) таблицы данных (`jobCandidateIteractionListTable` и `jobCandidateCandidateCvTable`) полностью перестали отображаться (высота строк и сетки схлопнулась в 0px).

## 2. Первопричина (Root Cause Analysis)
1. **Деструктивное CSS-правило во всех 7 SCSS-темах**:
   В коммите от 2026-08-03 при адаптации таблицы соцсетей (`socialNetworkTable`) в `job-candidate-editor.scss` было внесено правило:
   ```scss
   .v-slot-job-candidate-table,
   .c-data-grid-composition {
     min-height: 320px !important;
     height: auto !important;
   }
   ```
   Правило `height: auto !important` применилось глобально ко всем DataGrid формы кандидата (`.c-data-grid-composition`).
   В Vaadin Grid виртуальный скроллинг требует определённой высоты контейнера. При `height: auto` процентная высота `height: 100%` дочернего `.v-grid` не может рассчитаться и схлопывается в 0. Движок Grid считает высоту области просмотра равной 0 и рендерит ровно 0 строк таблицы.
   При этом правило `min-height: 320px` распространялось только на `.v-slot-job-candidate-table .v-grid`, в то время как слоты таблиц резюме и взаимодействий имеют другие классы (`candidate-browse-grid`), из-за чего их внутренний `.v-grid` не получал даже минимальной высоты.

2. **Отсутствие `height="100%"` в XML-декларации**:
   В `job-candidate-edit.xml`:
   - `jobCandidateIteractionListTable` имел только `width="100%"`.
   - `jobCandidateCandidateCvTable` имел только `width="100%"`.
   При отсутствии атрибута `height` CUBA назначает компоненту неопределённую высоту (`height: auto`).

3. **Специфика селекторов в темах**:
   CUBA DataGrid не выставляет свой XML-ID в атрибут `id` DOM-элемента `.v-grid`. Селекторы вида `#jobCandidateIteractionListTable` не матчились в браузере.

## 3. Архитектурное решение (UI/UX и Frontend)
1. **XML (`job-candidate-edit.xml`)**:
   - `jobCandidateIteractionListTable`: установить `height="100%"`, добавить класс `job-candidate-iteraction-grid` в `stylename`.
   - `jobCandidateCandidateCvTable`: установить `height="100%"`, добавить класс `job-candidate-resume-grid` в `stylename`.
   - `socialNetworkTable`: установить `height="320px"`, добавить класс `job-candidate-social-grid` в `stylename`.

2. **SCSS во всех 7 темах** (`hunttech-modern-dark`, `hunttech-modern-light`, `hunttech-modern`, `helium`, `havana`, `halo`, `hover`):
   - Удалить деструктивное `.c-data-grid-composition { height: auto !important; }`.
   - Настроить flex-контейнер для `.c-data-grid-composition` с `height: 100% !important;` и растягиваемым `.v-grid` (`flex: 1 1 auto; height: 100% !important; min-height: 200px !important;`).
   - Изолировать стилизацию `socialNetworkTable` через `.job-candidate-social-grid` и `#socialNetworkTableHbox`.
   - Обеспечить корректные высоты строк (38px для взаимодействий, 55px для резюме) через классовые селекторы `.job-candidate-iteraction-grid` и `.job-candidate-resume-grid`.

## 4. План валидации
- Сборка: `bash ../hunttech_recruiting/scripts/agent-gradle.sh assemble`
- Code review: `ocr review --audience agent`
- Коммит и пуш в `agent/antigravity-dev`
