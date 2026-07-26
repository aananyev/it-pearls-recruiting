# Проверка inline-фона HrmMainScreen

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/main-screen-background-inline-fix`  
BASE: `master`  
BASE SHA: `8c6d97118366f8cb564f3bed634ee0787820e483`  
STATUS: `WAITING_FOR_HERMES`

Проверять только точный HEAD из PR. До выполнения команд подтвердить: ветка существует, HEAD ветки совпадает с HEAD PR, `base=master`, conflicts NONE. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Корневая причина

1. `modules/web/src/com/company/hunttech/web-app.properties` выбирал `hrmMainScreen`, но app-component файл `com/company/hunttech/web-app.properties` продолжал выбирать `extMainScreen`. В зависимости от состава classpath/deploy мог создаваться базовый `ExtMainScreen`, поэтому lifecycle `HrmMainScreen` не выполнялся.
2. Фон назначался через `Page.getStyles().add()` с динамическим selector. Vaadin 7/8 может зарегистрировать Page CSS, но не применить его к уже отрисованным компонентам в том же UI lifecycle.
3. Исправление синхронизирует оба `mainScreenId` и использует штатный CUBA `HtmlAttributes` для inline `background-*` на `mainVBox` и `mainDashboard`.

## Обязательные команды

```bash
git diff --check

grep -R "^cuba.web.mainScreenId" \
  com/company/hunttech/web-app.properties \
  modules/web/src/com/company/hunttech/web-app.properties

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' \
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

- оба properties-файла содержат только `cuba.web.mainScreenId=hrmMainScreen`;
- `MainScreenBackgroundContractTest` — `10/10 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- compile — PASS;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy

1. Развернуть точный HEAD PR локально штатным скриптом проекта.
2. Удалить stale exploded webapp/widgetset artifacts согласно действующему протоколу проекта.
3. Перезапустить Tomcat.
4. Проверить `http://localhost:8080/hrm/` → HTTP 200.
5. Открыть новый Incognito/Private-сеанс через `/hrm/?restartApplication`.
6. Проверить external `${app.home}/local.app.properties`: он не должен переопределять `cuba.web.mainScreenId` значением `extMainScreen`.

## Runtime smoke

После входа:

1. фактический controller — `HrmMainScreen`;
2. ошибки `type FRAGMENT`, legacy `screens.xml`, `ResourceReference`, connector и `getUI` отсутствуют;
3. в DevTools у DOM-элементов `mainVBox` и dashboard есть `data-hrm-main-background="applied"`;
4. у обоих элементов inline-style содержит `background-image`, `background-position`, `background-repeat`, `background-size: cover`;
5. `Page.getStyles().add()` и случайный session CSS-класс больше не используются;
6. connector resource отвечает HTTP 200;
7. для системного варианта MIME `image/svg+xml`;
8. фон видим внутри dashboard, а не только по его краям;
9. выполнить не менее трёх повторных входов без персонального файла — системный вариант меняется;
10. загрузить PNG/JPG/JPEG/WEBP, сохранить, повторно войти — отображается точный персональный файл;
11. вернуть системные фоны, сохранить, повторно войти — отображается SVG активной темы;
12. favicon, dashboard `recruiting-dashboard`, уведомления и проверки резерва `ExtMainScreen` работают без регрессии.

## Отчёт

Сохранить:

`.ai/reports/2026-07-26-main-screen-background-inline-fix.md`

Указать Repo, Branch, PR, Base, `проверен HEAD: <SHA>`, HEAD match, conflicts, команды, результаты 10/10 и 8/8, SCSS, build, local deploy, HTTP, effective mainScreenId, DOM marker, inline style, connector response/MIME, системный и пользовательский smoke, Tomcat errors, P1/P2.

Hermes не меняет Java/XML/SCSS/tests/docs, не делает commit, push, rebase или merge, не изменяет БД и production.
