# Проверка отображения фоновых изображений главного экрана

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/main-screen-background-runtime-fix`  
BASE: `master`  
BASE SHA: `144585f25604f28fa1b38fee3acce5403d9dd76d`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка точного HEAD без изменения Java, XML, SCSS, tests, docs, БД и production.

Точный HEAD указан в PR. Перед проверкой Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из этой ветки напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Причина исправления

Первоначальная реализация формировала `ResourceReference` в `BeforeShow` и не закрепляла `StreamResource` за Vaadin-компонентом. Connector мог ещё не иметь UI, исключение подавлялось защитным `catch`, поэтому экран открывался без системного SVG и без пользовательского изображения.

В исправлении:

- фон применяется в `AfterShowEvent`;
- скрытый Vaadin `Image` регистрирует ресурс по ключу `src`;
- URL формируется после добавления владельца ресурса в `mainVBox`;
- один локальный CSS-класс назначается `mainVBox` и `mainDashboard`;
- карточка «Фон главного экрана» улучшена только внутри namespace `main-screen-background-*`;
- генерация подтверждается для семи тем и десяти SVG-вариантов каждой темы.

## Обязательные команды

```bash
git diff --check

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

- `MainScreenBackgroundContractTest` — `7/7 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- compile — PASS;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy и HTTP

1. Развернуть точный HEAD локально.
2. Перезапустить Tomcat.
3. Проверить `http://localhost:8080/hrm/` → HTTP `200`.
4. Открыть новый Incognito/Private-сеанс через `/hrm/?restartApplication`.

## Runtime smoke фонового ресурса

Для каждой темы `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`:

1. очистить персональный фон и сохранить;
2. выполнить не менее трёх выходов и входов;
3. подтвердить видимый фон внутри dashboard, а не только вокруг него;
4. подтвердить смену варианта и соответствие палитры теме;
5. изменить размер окна — `cover`, центрирование и отсутствие повторения сохраняются;
6. в DevTools Network открыть connector resource: ответ `200`, MIME `image/svg+xml`;
7. ошибок `ResourceReference`, `getUI`, `connector`, `resource did not handle connector request` нет.

## Пользовательский файл

1. `SettingsWindow` → «Интерфейс» → «Фон главного экрана».
2. Загрузить PNG, сохранить, выйти и войти — отображается точный файл.
3. Повторить для JPG/JPEG и WEBP.
4. Повторить вход три раза — персональный файл имеет приоритет.
5. Нажать «Использовать системные фоны», сохранить, войти — отображается системный SVG.
6. Выполнить очистку и Cancel — ранее сохранённый персональный фон остаётся.
7. Проверить отказ неподдерживаемого расширения и файла > 15 МБ.

## Visual smoke одной карточки

Проверить все семь тем:

- заголовок и status pill читаемы;
- две опции визуально разделены;
- кнопки имеют одинаковую ширину внутри своих панелей;
- красная destructive-кнопка отсутствует;
- тексты не накладываются и не получают line-height `38px`;
- соседний блок «Рабочее пространство», toolbar и остальные вкладки не изменились;
- горизонтальная прокрутка отсутствует.

## Регрессия

- dashboard `recruiting-dashboard` загружается;
- уведомления, резервы, favicon и действия `ExtMainScreen` работают;
- настройки режима окна, темы, часового пояса и стартового экрана сохраняются;
- вкладки «Обо мне», email и AI работают;
- Tomcat critical errors — NONE;
- P1 = 0;
- P2 = 0.

Отчёт сохранить в:

`.ai/reports/2026-07-26-main-screen-background-runtime-fix.md`

Указать: Repo, Branch, PR, Base, `проверен HEAD: <SHA>`, HEAD match, conflicts, команды, результаты 7/7 и 8/8, SCSS, build, deploy, HTTP, Network resource, visual/functional smoke, Tomcat errors, P1/P2.

Hermes не меняет код или документацию, не делает commit, push, rebase или merge, не изменяет БД и production.
