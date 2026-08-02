# Hermes — проверка общего стиля персонального dashboard HRM HuntTech

## Контекст

- PROJECT: HRM HuntTech
- REPOSITORY: `aananyev/it-pearls-recruiting`
- BRANCH: `agent/recruiter-dashboard-shared-style-contract`
- BASE: `master`
- PR: указан в сообщении передачи
- VERIFIED HEAD: использовать только полный PR HEAD SHA из блока `Hermes` в описании PR
- MODE: проверка без изменения функционального кода и документации

Перед любой проверкой подтвердить:

1. ветка существует;
2. её HEAD равен SHA из описания PR;
3. PR открыт из этой ветки напрямую в `master`;
4. PR HEAD равен проверяемому SHA;
5. conflicts отсутствуют.

Любое несовпадение — `HEAD_MISMATCH`, проверку остановить. Новый commit аннулирует предыдущий отчёт.

## Область изменений

Задача применяет к существующему `recruiting-dashboard` visual tokens общего контракта Edit-форм:

- root `edit-workspace recruiter-dashboard-root`;
- карточки 8 px, theme-aware border и лёгкая тень;
- заголовки, таблицы, Panel, controls 38 px;
- focus/hover/selected states с акцентом `#ffb11b`;
- responsive padding для 1366 и 1280 px;
- идентичный partial во всех семи темах.

Не изменены persistent dashboard model, widgets, entities, services, JPQL, loaders, views, actions, права, фон и бизнес-логика.

## Команды

```bash
git status --short
git rev-parse HEAD
git diff --check master...HEAD

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.MainScreenDashboardSharedStyleContractTest' \
  --no-daemon --stacktrace

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' \
  --no-daemon --stacktrace

./gradlew :app-core:test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

После успешной сборки выполнить clean local deploy штатным способом проекта и проверить:

```text
http://localhost:8080/hrm/ → HTTP 200
```

## Browser smoke

Проверить `recruiting-dashboard` в темах:

- halo;
- havana;
- helium;
- hover;
- hunttech-modern;
- hunttech-modern-light;
- hunttech-modern-dark.

Контрольные viewport:

- 1366×768;
- 1920×1080;
- 1920×1200;
- 3440×1440 либо максимально доступный ultrawide.

Проверить:

1. фон главного экрана виден между widget-карточками;
2. dashboard root остаётся прозрачным;
3. `widget-border` и `widget-border-line` имеют radius 8 px, лёгкую тень и theme-aware поверхность;
4. заголовки не обрезаны и переносятся;
5. поля и управляющие кнопки имеют полезную высоту 38 px;
6. таблицы читаемы, hover/selected/focus различимы;
7. global horizontal scroll отсутствует;
8. виджеты загружают прежние данные;
9. переходы и действия существующих widgets работают;
10. dark/light темы сохраняют достаточный контраст;
11. background lifecycle и смена пользовательского фона не регрессировали;
12. в Tomcat logs нет новых critical errors.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/recruiter-dashboard-shared-style-contract
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Dashboard shared style contract: PASS
Main screen background contract: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS seven themes: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Browser smoke: PASS
Tomcat errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

При ошибке: `STATUS: FAILED_VERIFICATION`, указать FAILED STEP, ROOT CAUSE, фрагмент лога, выполненные и невыполненные проверки. Код, commit, push, rebase, merge и production не выполнять.
