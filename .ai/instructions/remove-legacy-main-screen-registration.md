# Проверка удаления legacy-регистрации HrmMainScreen

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/remove-legacy-main-screen-registration`  
BASE: `master`  
BASE SHA: `ce35650ad91f2a470e5b99d7cb02de88e0761a2a`  
STATUS: `WAITING_FOR_HERMES`

Проверять только точный HEAD из PR. Подтвердить HEAD ветки и PR, `base=master`, conflicts NONE. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Причина

`HrmMainScreen` реализован через новый Screens API: `@UiController("hrmMainScreen")` и `@UiDescriptor("hrm-main-screen.xml")`. Одновременная запись `<screen id="hrmMainScreen" .../>` в legacy `web-screens.xml` заставляет CUBA Platform 7.3 трактовать экран как legacy screen/fragment и приводит при входе к:

`DevelopmentException: Unable to create screen hrmMainScreen with type FRAGMENT`.

Исправление удаляет только legacy-запись. Источником screen ID остаются `cuba.web.mainScreenId=hrmMainScreen` и `@UiController("hrmMainScreen")`.

## Команды

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- `MainScreenBackgroundContractTest` — `10/10 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- compile — PASS;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Runtime smoke

1. Подтвердить отсутствие `<screen id="hrmMainScreen"` в `modules/web/src/com/company/hunttech/web-screens.xml`.
2. Подтвердить наличие `cuba.web.mainScreenId=hrmMainScreen` и `@UiController("hrmMainScreen")`.
3. Выполнить local deploy точного HEAD и открыть новый сеанс через `/hrm/?restartApplication`.
4. Вход должен завершаться без `DevelopmentException`, `type FRAGMENT` и ошибки legacy `screens.xml`.
5. Фактический controller главного экрана — `HrmMainScreen`.
6. Системный либо персональный фон отображается; connector resource возвращает HTTP 200.
7. Dashboard, favicon, уведомления и проверки резервов из `ExtMainScreen` работают без регрессии.
8. HTTP `/hrm/` = 200; Tomcat critical errors NONE; P1=0; P2=0.

Отчёт сохранить в `.ai/reports/2026-07-26-remove-legacy-main-screen-registration.md`.

Hermes не меняет Java/XML/tests/docs, не делает commit, push, rebase или merge, не изменяет БД и production.
