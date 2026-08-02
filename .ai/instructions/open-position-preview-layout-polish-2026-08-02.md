# Hermes — проверка улучшенной компоновки OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repo: `aananyev/it-pearls-recruiting`
- Branch: `agent/open-position-preview-layout-polish`
- Base: `master`
- Base SHA: `b6cb95b17d654d85daec7369a32270db6a6b9071`
- PR: указан в комментарии передачи
- Verified HEAD: полный SHA указан в последнем комментарии PR
- Режим: проверка без изменения функционального кода и документации

Перед запуском подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `Verified HEAD` из комментария PR;
3. HEAD PR совпадает с тем же SHA;
4. PR открыт из `agent/open-position-preview-layout-polish` прямо в `master`;
5. conflicts = `NONE`;
6. рабочее дерево чистое.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Улучшена только компоновка уже существующего `OpenPositionEditPreview` по фактическому screenshot baseline от 2026-08-02.

Исправляются visual defects:

- чрезмерная высота карточки длинного названия вакансии;
- уход label-навигации и summary ниже видимой области;
- широкие пустые разрывы между связанными полями;
- обрезание правых controls из-за legacy-процентных ширин;
- высокая и перегруженная tab bar;
- слабая иерархия основных и вложенных accordion;
- footer actions, разнесённые по ширине.

Бизнес-логика, data contract, route guard, структура 12 вкладок и legacy-экран не изменяются.

## Разрешённый diff

Ожидаются только:

- `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java`;
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewSharedStyleContractTest.java`;
- семь идентичных `modules/web/themes/<theme>/com.company.hunttech/open-position-preview.scss`;
- `docs/ui/OpenPositionEditPreview_Spec.md`;
- `.ai/instructions/open-position-preview-layout-polish-2026-08-02.md`.

Не должны изменяться:

- `OpenPositionEdit.java`;
- `open-position-edit.xml`;
- `open-position-edit-preview.xml`;
- browse-экраны и `web-menu.xml`;
- entities, services, `views.xml`, JPQL и Liquibase;
- `edit-screen-shared-styles.scss`;
- SCSS других экранов;
- `styles.scss` тем;
- другие формы и бизнес-логика.

## Ожидаемая presentation-реализация

- sidebar остаётся 270px / 250px по shared contract;
- logo: 112px, до 1366px — 92px;
- title: максимум 7 визуальных строк, полный текст доступен в tooltip;
- label-навигация компактна и сохраняет `label-nav-item` + `label-nav-item-active`;
- `vacancyTitleSpacerHBox` скрыт как пустой visual spacer;
- существующие HBox получают responsive layout roles без перестановки полей;
- до 1100px — две колонки, до 820px — одна колонка;
- tab bar — 40px, captions с ellipsis и штатными overflow controls;
- основные accordion имеют первичный акцент, `commandFieldHBox` — вторичный subsection;
- footer actions сгруппированы справа;
- никаких новых loaders, listeners бизнес-событий, entity writes или commit-вызовов.

## Обязательные команды

```bash
git diff --check origin/master...HEAD

git diff --name-only origin/master...HEAD

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests '*OpenPositionEditPreviewLayoutTest*' \
          --tests '*OpenPositionEditPreviewRouteGuardTest*' \
          --tests '*OpenPositionEditPreviewSharedStyleContractTest*' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- compile: PASS;
- `OpenPositionEditPreviewLayoutTest`: 8/8 PASS;
- `OpenPositionEditPreviewRouteGuardTest`: PASS;
- `OpenPositionEditPreviewSharedStyleContractTest`: 6/6 PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- local SCSS семи тем идентичен: PASS;
- `buildScssThemes`: PASS;
- `clean assemble`: `BUILD SUCCESSFUL`.

## Local deploy

Развернуть точный `Verified HEAD` локально. Production не изменять.

Проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200 и доступность widgetset.

## Runtime-регрессия route

Использовать существующую заполненную вакансию:

```text
UUID: fffa4236-4d35-8b34-3bf3-baa240aa3722
Route id: 7zz913ck9nhct3qwxtm90amds2
URL: http://localhost:8080/hrm/#main/open-position-edit-preview?id=7zz913ck9nhct3qwxtm90amds2
```

Подтвердить:

1. preview открывается без EclipseLink `ValidationException`;
2. нет `instantiatingValueholderWithNullSession`;
3. `positionType`, grade, project и связанные данные отображаются;
4. legacy `hunttech_OpenPosition.edit` открывается как раньше.

## Visual smoke

Проверить темы:

1. `halo`;
2. `havana`;
3. `helium`;
4. `hover`;
5. `hunttech-modern`;
6. `hunttech-modern-light`;
7. `hunttech-modern-dark`.

Проверить viewport:

- 1920×1080;
- 1600×900;
- 1366×768;
- 1280×800.

Для каждой применимой комбинации подтвердить:

- sidebar и workspace не перекрываются;
- горизонтальный overflow экрана отсутствует;
- logo не деформирован;
- длинное название не занимает всю высоту sidebar;
- полный title доступен при hover;
- navigation и summary доступны без потери содержимого;
- active-state не меняет геометрию navigation;
- toolbar и tabs не перекрываются;
- tab bar имеет компактную высоту;
- штатные overflow-стрелки TabSheet работают;
- captions вкладок не выходят за tab item;
- accordion-карточки имеют единый ритм;
- вложенная настройка команды визуально вторична;
- в `vacancyNameHBox` ID, название, grade и action не перекрываются;
- `hboxProject1`, `hboxVacansy`, `hboxProject`, `hboxCompany`, `hboxSalary`, `space2Box` не имеют широких пустых разрывов;
- правые controls не обрезаны;
- picker actions не перекрывают текст;
- checkbox и caption не выходят за карточку;
- footer actions находятся справа и доступны;
- таблицы и RichTextArea не расширяют весь экран;
- legacy-форма визуально не изменилась.

## Functional smoke

1. Открыть все доступные вкладки через TabSheet.
2. Повторить переходы через label-навигацию.
3. Проверить скрытие/показ «Оплаты» для вакансии/команды.
4. Раскрыть и свернуть accordion — значения не сбрасываются.
5. Проверить таблицы договоров, файлов, навыков и новостей.
6. Проверить RichTextArea описания, тестового задания, памятки и письма.
7. Проверить BPM fragment и комментарии.
8. Сохранить существующую вакансию без изменения значений.
9. Изменить безопасное тестовое значение, сохранить, повторно открыть и вернуть значение.
10. Проверить «Отмена» без сохранения.
11. Проверить повторное открытие без дублирования коллекций.

## Runtime-контроль

В Tomcat logs не допускаются новые:

- `Cannot get unfetched attribute`;
- `instantiatingValueholderWithNullSession`;
- detached entity errors;
- `IllegalStateException`;
- `NullPointerException`;
- Vaadin RPC / `ClassCastException`;
- XML/DI ошибки;
- DataContext merge/commit ошибки;
- P1/P2 UI-дефекты.

## Отчёт

Создать:

```text
.ai/reports/2026-08-02-open-position-preview-layout-polish-verification.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-preview-layout-polish
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Allowed diff: PASS
Compile: PASS
Preview layout test: 8/8 PASS
Preview route guard test: PASS
Preview shared style test: 6/6 PASS
ScreenViewIntegrityTest: 8/8 PASS
Local SCSS seven themes: IDENTICAL/PASS
SCSS build: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Route regression: PASS
Visual smoke seven themes: PASS
Responsive smoke: PASS
Functional smoke: PASS
Legacy screen unchanged: PASS
Tomcat errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <full SHA>
```

Ошибка → `STATUS: FAILED_VERIFICATION` с FAILED STEP, ROOT CAUSE, релевантным stack trace, темой/viewport и перечнем невыполненных проверок.

Hermes не изменяет код или документацию, не создаёт commit, не выполняет push, rebase, merge, разрешение конфликтов и production-действия.
