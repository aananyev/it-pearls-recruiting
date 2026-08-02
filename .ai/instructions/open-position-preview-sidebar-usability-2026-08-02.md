# Hermes — проверка sidebar usability OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repo: `aananyev/it-pearls-recruiting`
- Branch: `agent/open-position-preview-sidebar-usability`
- Base: `master`
- Base SHA: `c3f85aa91bcd67b1030e422acae8d57c1eed3727`
- PR: указан в комментарии передачи
- Verified HEAD: полный SHA указан в последнем комментарии PR
- Режим: проверка без изменения кода и документации

Перед запуском подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `Verified HEAD`;
3. HEAD PR совпадает с тем же SHA;
4. PR открыт из `agent/open-position-preview-sidebar-usability` прямо в `master`;
5. conflicts = `NONE`;
6. рабочее дерево чистое.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Исправляется только presentation-компоновка sidebar `OpenPositionEditPreview` по runtime-скриншоту:

- логотип `OvaFallbackImage` перекрывает название вакансии;
- title занимает чрезмерную высоту;
- summary GridLayout показывает captions без читаемых values и создаёт большие вертикальные интервалы;
- navigation и summary трудно использовать на viewport 768–820px по высоте.

Бизнес-логика, Java-контроллеры, XML-дескрипторы, data bindings, loaders, views, JPQL, actions и lifecycle не изменяются.

## Разрешённый diff

Ожидаются только:

- семь файлов `modules/web/themes/<theme>/com.company.hunttech/open-position-preview-sidebar-usability.scss`;
- семь `modules/web/themes/<theme>/styles.scss` — только import/include corrective mixin;
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewSidebarUsabilityContractTest.java`;
- `docs/ui/OpenPositionEditPreview_Spec.md`;
- `.ai/instructions/open-position-preview-sidebar-usability-2026-08-02.md`.

Не должны изменяться:

- `OpenPositionEdit.java`;
- `OpenPositionEditPreview.java`;
- `open-position-edit.xml`;
- `open-position-edit-preview.xml`;
- browse-экраны и `web-menu.xml`;
- entities, services, `views.xml`, JPQL и Liquibase;
- `edit-screen-shared-styles.scss`;
- `open-position-preview.scss`;
- SCSS других экранов;
- production.

## Ожидаемый presentation-контракт

Corrective partial подключается строго после `open-position-preview`.

Identity-card:

- `openPositionPreviewLogoBox` имеет visual-stage `112px`;
- compact mode (`max-width:1366px` или `max-height:820px`) — `94px`;
- логотип `96×96px`, compact — `82×82px`;
- logo slot и title slot находятся в нормальном flow;
- title не пересекает изображение;
- title — максимум 4 строки, compact — 3 строки;
- полный текст доступен через уже существующий tooltip.

Summary:

- существующий `GridLayout` визуально отображается как CSS grid;
- базовые колонки `72px + minmax(0,1fr)`;
- compact — `66px + minmax(0,1fr)`;
- DOM-порядок captions/values сохраняется;
- values видимы и не выходят за правую границу;
- никакие значения сущности не вычисляются и не меняются CSS-слоем.

## Обязательные команды

```bash
git diff --check origin/master...HEAD

git diff --name-only origin/master...HEAD

./gradlew :app-core:test \
  --tests '*OpenPositionEditPreviewSidebarUsabilityContractTest*' \
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

- новый contract test: PASS;
- существующие preview-тесты: PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- corrective SCSS семи тем идентичен;
- import/include corrective layer идёт после `open-position-preview`;
- `buildScssThemes`: PASS;
- `clean assemble`: `BUILD SUCCESSFUL`.

## Local deploy

Развернуть точный `Verified HEAD` локально. Production не изменять.

Проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

## Runtime route

Открыть заполненную вакансию через preview route:

```text
http://localhost:8080/hrm/#main/open-position-edit-preview?id=<existing-id>
```

Проверить в темах:

1. `halo`;
2. `havana`;
3. `helium`;
4. `hover`;
5. `hunttech-modern`;
6. `hunttech-modern-light`;
7. `hunttech-modern-dark`.

Viewport:

- 1920×1080;
- 1600×900;
- 1366×768;
- 1280×800.

Обязательный visual smoke:

1. logo и title не пересекаются при первом открытии;
2. переключение вкладок не вызывает повторного наезда;
3. смена темы не вызывает наезда;
4. title центрирован и ограничен 4/3 строками;
5. tooltip содержит полный title;
6. navigation остаётся доступной;
7. summary показывает пары ID/value, Должность/value, Грейд/value, Проект/value, Город/value, Позиции/value, Оформление/value;
8. длинные values summary переносятся максимум на две строки;
9. sidebar scroll работает;
10. workspace и footer не изменились;
11. legacy editor визуально и функционально не изменился.

## Functional smoke

- открыть все вкладки через TabSheet и label-навигацию;
- раскрыть/свернуть accordion;
- проверить «Отмена» без сохранения;
- на тестовой записи проверить сохранение и повторное открытие;
- подтвердить отсутствие новых ошибок Tomcat.

## Отчёт

Создать:

```text
.ai/reports/2026-08-02-open-position-preview-sidebar-usability-verification.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-preview-sidebar-usability
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Contract tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
Build: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Visual smoke: PASS
Tomcat errors: NONE
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Ошибка:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: ...
ROOT CAUSE: ...
ERROR: ...
STACK TRACE: ...
COMPLETED CHECKS: ...
NOT EXECUTED: ...
```

Hermes не меняет код или документацию, не делает commit/push/rebase/merge и не трогает production.
