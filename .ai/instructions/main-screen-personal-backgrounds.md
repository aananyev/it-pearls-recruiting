# Проверка персональных фоновых изображений главного экрана

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/main-screen-personal-backgrounds`  
BASE: `master`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации, БД и production.

Точный полный HEAD SHA указан в PR. Перед проверкой Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из этой ветки напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение — `HEAD_MISMATCH`, проверку остановить. Отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Область изменения

- новый `HrmMainScreen` наследует действующий `ExtMainScreen` и добавляет только фон рабочей области;
- `MainScreenBackgroundService` формирует десять нейтральных светлых SVG-композиций для каждой из семи тем;
- при отсутствии персонального изображения вариант выбирается заново при каждом создании главного экрана после входа;
- персональный файл с маркером `hrm-main-background-` имеет абсолютный приоритет;
- новый `ExtSettingsWindowMainBackground` наследует действующую цепочку контроллеров настроек;
- во вкладку «Интерфейс» добавлена одна карточка загрузки и кнопка «Очистить фоновое изображение»;
- хранение использует существующее `UserSettings.fileImageFace`; entity, views и структура БД не изменены;
- SCSS и существующая компоновка `SettingsWindow` не изменены.

## Запрещённые регрессии

Не допускаются изменения поведения:

- уведомлений, резервов, dashboard, favicon и lifecycle действующего `ExtMainScreen`;
- вкладок «Обо мне», email и AI;
- штатных настроек режима окна, темы, языка, часового пояса и стартового экрана;
- entity, таблиц, Liquibase, JPQL, views, loaders, DataContext и production.

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

- `MainScreenBackgroundContractTest` — `5/5 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy и HTTP

1. Развернуть точный HEAD локально и перезапустить Tomcat.
2. Проверить `http://localhost:8080/hrm/` — HTTP `200`.
3. Открыть новый браузерный сеанс после deploy.

## Functional smoke

### Системный каталог

Для каждой темы `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`:

1. убедиться, что персональный фон не задан;
2. выполнить не менее пяти последовательных выходов и входов;
3. подтвердить смену нейтральных светлых композиций;
4. подтвердить соответствие палитры активной теме;
5. проверить отсутствие текста, логотипов, рекламы и тёмного визуального шума;
6. проверить `background-size: cover`, отсутствие повторения и искажений при изменении размера окна.

### Персональный фон

1. Открыть `SettingsWindow` → «Интерфейс».
2. Убедиться, что существующая компоновка и остальные поля не изменились.
3. Загрузить PNG, сохранить настройки, выйти и войти — отображается строго выбранный файл.
4. Повторить вход пять раз — персональный файл не заменяется случайным вариантом.
5. Повторить для JPG/JPEG и WEBP.
6. Проверить отказ для неподдерживаемого расширения и файла больше 15 МБ.
7. Нажать «Очистить фоновое изображение», сохранить, выйти и войти — снова используется случайный фон активной темы.
8. Нажать очистку и закрыть окно через Cancel — ранее сохранённая настройка не должна измениться.

### Регрессия главного экрана и настроек

- dashboard `recruiting-dashboard` загружается;
- уведомления, проверки резервов, favicon и существующие действия главного экрана работают;
- вкладки «Обо мне», «Настройка email» и AI открываются и сохраняются;
- режим окна, тема, язык, часовой пояс и стартовый экран сохраняются штатно;
- в Tomcat logs отсутствуют `IllegalStateException`, `FileStorageException`, XML inheritance errors, unfetched/detached errors, ошибки Vaadin resource registration и critical errors;
- P1 = 0; P2 = 0.

## Отчёт

Сохранить отчёт в:

`.ai/reports/2026-07-26-main-screen-personal-backgrounds.md`

Указать Repo, Branch, PR, Base, `проверен HEAD: <SHA>`, HEAD match, conflicts, все команды и smoke-сценарии, Tomcat errors, P1/P2.

Hermes не меняет Java, XML, properties, tests или docs; не делает commit, push, rebase или merge; не изменяет БД и production.
