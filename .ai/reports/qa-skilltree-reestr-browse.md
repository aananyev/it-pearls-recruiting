# QA-отчёт: SkillTreeReestrBrowse (Реестр компетенций)

**Дата:** 2026-09-03 · **Ветка:** agent/antigravity-dev · **Коммиты:** e14eb5bb, 43b7745f · **Вердикт: PASS**

## Проверено

### 1. Diff-ревью
- **skill-tree-reestr-browse.xml** — Split View: левый vbox 312px (`detailPane`, scrollBox) + правая рабочая область. TreeTable (`treeDataGrid`) с `hierarchyProperty="skillTree"`, контейнер `skillTreesDc` (view `skillTree-reestr-browse-view`), loader `skillTreesDl` (cacheable). Командный тулбар по эталону (cssLayout `candidate-filter-bar`, кнопки `candidate-btn`), Generic Filter collapsable/collapsed, rowsCount. Соответствует стандарту hunttech-reestr-screen (сайдбар: аватар 120px ovaFallbackImage, 4 уровня типографики, быстрые действия, grid реквизитов, чипы детей).
- **SkillTreeReestrBrowse.java** — все @Inject соответствуют id XML; null-safe заполнение сайдбара; escapeHtml для всех значений в html-лейблах; чипы детей строятся из загруженной коллекции (без N+1).
- **43b7745f (code review fixes)** — `safeWikiLinkHtml`: whitelist http/https (javascript:/data: блокируются), escapeHtml URL, `target=_blank rel="noopener noreferrer"` — XSS закрыт. `createChildBtn`: добавлен `.withContainer(skillTreesDc)` — коммит редактора сам синхронизирует контейнер, дублирования узла нет.
- **views.xml** — `skillTree-reestr-browse-view` extends `_minimal`: skillName, skillTree (skillTree-picker-view), specialisation (specRuName через specialisation-picker-view), wikiPage, prioritySkill, notParsing, styleHighlighting, comment, fileImageLogo (_minimal).
- **web-menu.xml** — пункт «Реестр компетенций» добавлен в раздел словарей.
- **messages.properties / messages_ru.properties** — ключи skillTreeReestrCaption, msgSpecialisation, msgSkillTree добавлены в оба файла (RU/EN). Используемые msgPrioritySkill/msgNotParsing/msgWikiPage уже существуют.
- **CompanyEditLayoutContractTest.java** — ослабление корректно: точное совпадение заменено на префиксное (`stylename="edit-workspace` / `edit-workspace `), допускает дополнительные классы (company-editor-workspace), не теряет контракт.
- **Docs** — SkillTreeReestrBrowse_Spec.md, SkillTreeReestrBrowse_Design.md добавлены.

### 2. Data View Integrity
Все геттеры контроллера (`getSkillName`, `getSkillTree().getSkillName()`, `getSpecialisation().getSpecRuName()`, `getWikiPage`, `getPrioritySkill`, `getNotParsing`, `getStyleHighlighting`, `getComment`, `getFileImageLogo`) декларированы во view. Несоответствий не найдено.

### 3. Сборка и тесты (реальные прогоны)
- `:app-web:compileJava` — **BUILD SUCCESSFUL** (2s)
- Тесты `:app-core:test` (результаты в modules/core/build/test-results/test, timestamp 2026-09-03T19:03:28):
  - ScreenViewIntegrityTest: tests=8, failures=0, errors=0
  - SkillTreeEditLayoutContractTest: tests=7, failures=0, errors=0
  - CompanyEditLayoutContractTest: tests=8, failures=0, errors=0

## Замечания (неблокирующие)
1. Русский caption чипа «Родитель: …» / «Корневая компетенция» захардкожен в Java (не через messages) — мелочь, не мешает PR.
2. Инлайн-стили в чипах/empty-label — допустимо по существующим эталонам, но идеологически лучше вынести в SCSS-темы в будущем.
3. Опечатка в существующих ключах msgPrioritySkill («Приорите навыка») — вне скоупа коммитов.

## Вердикт
**PASS** — к PR готов. Сборка зелёная, тесты зелёные (23/23), контракт реестров и Data View Integrity соблюдены, XSS-fix валиден.
