# Hermes — проверка общих стилей OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repo: `aananyev/it-pearls-recruiting`
- Branch: `agent/open-position-preview-shared-styles`
- Base: `master`
- PR: указан в комментарии передачи
- Verified HEAD: полный SHA указан в последнем комментарии PR
- Режим: проверка без изменения функционального кода и документации

Перед запуском подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `Verified HEAD` из комментария PR;
3. HEAD PR совпадает с тем же SHA;
4. PR открыт из `agent/open-position-preview-shared-styles` прямо в `master`;
5. conflicts = `NONE`;
6. рабочее дерево чистое.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение изменения

К существующему `OpenPositionEditPreview` применён общий UI API из:

- `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md`;
- `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`;
- `docs/ui/OpenPositionEditPreview_Spec.md`.

Текущий runtime-вид до исправления: светлая неоформленная sidebar, синий active-state, стандартные GroupBox и неоднородные поля. После изменения ожидаются:

- фирменная тёмная sidebar HRM HuntTech;
- жёлтый active-state label-навигации;
- общий toolbar/tabs/workspace/footer;
- полноширинные panel-accordion;
- единый `edit-form-control` для типовых полей;
- одинаковая геометрия во всех семи темах.

Изменение presentation-only. Бизнес-логика, data contract, route guard и legacy-экран не изменяются.

## Разрешённый diff

Разрешены только:

- `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java`;
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewSharedStyleContractTest.java`;
- `docs/ui/OpenPositionEditPreview_Spec.md`;
- `.ai/instructions/open-position-preview-shared-styles-2026-08-02.md`;
- семь идентичных файлов `modules/web/themes/<theme>/com.company.hunttech/open-position-preview.scss`;
- семь `modules/web/themes/<theme>/styles.scss` только с import/include `open-position-preview` после shared mixin.

Не должны изменяться:

- `OpenPositionEdit.java`;
- `open-position-edit.xml`;
- `open-position-edit-preview.xml`;
- browse-экраны и `web-menu.xml`;
- entities, services, `views.xml`, JPQL и Liquibase;
- `edit-screen-shared-styles.scss`;
- SCSS других экранов;
- другие формы и бизнес-логика.

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
- `OpenPositionEditPreviewSharedStyleContractTest`: 5/5 PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- shared SCSS семи тем идентичен: PASS;
- local `open-position-preview.scss` семи тем идентичен: PASS;
- import/include local partial строго после shared: PASS;
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

## Visual smoke семи тем

Проверить:

1. `halo`;
2. `havana`;
3. `helium`;
4. `hover`;
5. `hunttech-modern`;
6. `hunttech-modern-light`;
7. `hunttech-modern-dark`.

Для каждой темы подтвердить:

- sidebar тёмная, без светлой подложки текущего старого вида;
- sidebar имеет 270px на обычном viewport и 250px при ширине до 1366px;
- sidebar и workspace не перекрываются, горизонтальный overflow отсутствует;
- логотипы, title, subtitle, summary, hint и warning помещаются внутри sidebar;
- active navigation — жёлтый `#ffb11b` с жёлтой левой границей;
- hover navigation — белый текст на полупрозрачном белом фоне;
- базовый `label-nav-item` сохраняется при active-state;
- toolbar имеет единую поверхность и высоту;
- TabSheet сохраняет все 12 вкладок, captions и icons;
- GroupBox отображаются как полноширинные panel-accordion с единым caption;
- раскрытие/сворачивание не сбрасывает значения;
- TextField, TextArea, LookupField, LookupPickerField, DateField и RichTextArea имеют единый visual contract;
- required/read-only/disabled/validation состояния различимы;
- picker-actions не перекрывают текст;
- таблицы не расширяют весь экран;
- footer save/cancel видим и не перекрывает содержимое;
- legacy-формы визуально не изменились.

Проверить минимум viewport:

- 1920×1080;
- 1440×900;
- 1366×768;
- 1280×800.

## Functional smoke

1. Открыть все доступные вкладки через TabSheet.
2. Повторить переходы через label-навигацию.
3. Проверить скрытие/показ «Оплаты» для вакансии/команды.
4. Проверить таблицы договоров, файлов, навыков и новостей.
5. Проверить RichTextArea описания, тестового задания, памятки и письма.
6. Проверить BPM fragment и комментарии.
7. Сохранить существующую вакансию без изменения значений.
8. Изменить безопасное тестовое значение, сохранить, повторно открыть и вернуть значение.
9. Проверить «Отмена» без сохранения.
10. Проверить повторное открытие без дублирования коллекций.

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
.ai/reports/2026-08-02-open-position-preview-shared-styles-verification.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-preview-shared-styles
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Allowed diff: PASS
Compile: PASS
Preview layout test: 8/8 PASS
Preview route guard test: PASS
Preview shared style test: 5/5 PASS
ScreenViewIntegrityTest: 8/8 PASS
Shared SCSS seven themes: IDENTICAL/PASS
Local preview SCSS seven themes: IDENTICAL/PASS
SCSS import/include order: PASS
SCSS build: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Route regression: PASS
Visual smoke seven themes: PASS
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
