# Проверка пользовательского фона через StreamResource

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/fix-custom-background-streamresource`
- Base: `master`
- PR: номер и точный итоговый HEAD указаны в описании PR.
- Режим: проверка без изменения функционального кода и документации.
- Проверять только HEAD PR. Несовпадение branch HEAD, PR HEAD или SHA из PR → `HEAD_MISMATCH`, проверку остановить.

## Область изменения

Проверяется исправление пользовательского фона главного экрана:

- `MainScreenBackgroundService.createCustomResource()` читает FileStorage через `fileLoader.openStream()` и `readAllBytes()`;
- пользовательский файл упаковывается в `StreamResource` с `ByteArrayInputStream`, именем файла, MIME-типом и `cacheTime = -1`;
- `HrmMainScreen.buildBackgroundUrl()` передаёт `StreamResource` в `registerBackgroundResource()`;
- ресурс регистрируется через `Image 0 × 0`, `ResourceReference` и Vaadin connector tree;
- `app://APP` преобразуется в HTTP-путь;
- системные `ThemeResource` остаются без изменений;
- `ExtSettingsWindowMainBackground`, entity, БД, datasource, FileStorageService и `HrmMainScreenIntegrationTest` не изменяются.

## Проверка HEAD и diff

```bash
git fetch --all --prune
git checkout agent/fix-custom-background-streamresource
git reset --hard origin/agent/fix-custom-background-streamresource

git rev-parse HEAD
git status --short
git diff --check
git diff --stat origin/master...HEAD
git diff origin/master...HEAD -- \
  modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java \
  modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java \
  modules/core/test/com/company/hunttech/core/MainScreenBackgroundContractTest.java \
  docs/ui/HrmMainScreen_Spec.md
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = SHA из описания PR;
- PR открыт из `agent/fix-custom-background-streamresource`;
- base PR = `master`;
- conflicts = `NONE`;
- рабочее дерево чистое;
- diff не содержит изменений системных `ThemeResource`, SettingsWindow, entity, БД, datasource или FileStorageService.

## Compile и профильные тесты

```bash
./gradlew :app-core:compileTestJava \
          :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests 'com.company.hunttech.web.screens.mainscreen.HrmMainScreenIntegrationTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace
```

Ожидается:

- compile PASS;
- `MainScreenBackgroundContractTest` PASS;
- `HrmMainScreenIntegrationTest` PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- контрактный тест подтверждает `StreamResource`, `readAllBytes()`, MIME-тип, `registerBackgroundResource()`, `ResourceReference` и `app://APP`;
- `ExternalResource` отсутствует в `HrmMainScreen`.

## SCSS и полная сборка

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- семь тем собраны;
- `BUILD SUCCESSFUL`;
- системные каталоги `backgrounds/1.jpg … 10.jpg` сохранены без изменений.

## Clean local deploy и HTTP

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. Войти пользователем без пользовательского фона и подтвердить системный фон.
4. Открыть SettingsWindow → «Интерфейс» → «Фон главного экрана».
5. Загрузить PNG, JPEG и WebP размером до 15 МБ, сохранить через «ОК».

## Functional smoke пользовательского фона

Для каждого поддерживаемого формата подтвердить:

1. фон обновляется после сохранения без повторного входа;
2. после повторного входа пользовательский фон снова отображается;
3. computed `background-image` содержит connector URL;
4. URL не содержит `/dispatch/download?f=`;
5. URL не содержит необработанный `app://APP`;
6. HTTP-запрос ресурса возвращает 200;
7. `Content-Type` соответствует `image/png`, `image/jpeg` или `image/webp`;
8. изображение заполняет `mainVBox` с `background-size: 100% 100%`;
9. dashboard остаётся прозрачным;
10. в DOM присутствуют `data-hrm-main-background="applied"` и `data-hrm-main-controller="HrmMainScreen"`.

## Fallback и системные ThemeResource

1. Временно сделать физический файл пользовательского фона недоступным при сохранённом `FileDescriptor`.
2. Повторно войти в приложение.
3. Подтвердить:
   - вход не блокируется;
   - применяется системный фон активной темы;
   - descriptor не удаляется из БД;
   - после восстановления файла пользовательский фон возвращается.
4. Проверить системные фоны всех семи тем:
   - `halo`;
   - `havana`;
   - `helium`;
   - `hover`;
   - `hunttech-modern`;
   - `hunttech-modern-light`;
   - `hunttech-modern-dark`.
5. Для ThemeResource подтвердить URL `VAADIN/themes/{theme}/backgrounds/{n}.jpg` и HTTP 200.

## Runtime logs

После всех сценариев проверить Tomcat logs. Недопустимы новые:

- `IllegalStateException` регистрации ресурса;
- `FileStorageException`, блокирующий UI;
- `OutOfMemoryError`;
- `NullPointerException` в изменённом сценарии;
- ошибки Vaadin connector/resource key;
- HTTP 404/500 для пользовательского connector URL;
- detached/unfetched ошибки.

## Отчёт

Сохранить отчёт:

```text
.ai/reports/2026-07-27-fix-custom-background-streamresource.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/fix-custom-background-streamresource
PR: <номер>
BASE: master
VERIFIED HEAD: <полный SHA>
HEAD MATCH: PASS
CONFLICTS: NONE
PROFILE TESTS: PASS
HrmMainScreenIntegrationTest: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
BUILD: SUCCESSFUL
LOCAL DEPLOY: PASS
HTTP /hrm/: 200
CUSTOM STREAM RESOURCE: PASS
CONNECTOR URL: PASS
SYSTEM THEME RESOURCE: PASS
FILESTORAGE FALLBACK: PASS
TOMCAT ERRORS: NONE
P1: 0
P2: 0
MERGE: NOT PERFORMED
PRODUCTION: NOT CHANGED
```

Ошибка:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: <шаг>
ROOT CAUSE: <причина>
VERIFIED HEAD: <полный SHA>
```

Указать выполненные и невыполненные проверки, релевантный log/stack trace и рекомендацию. Код и документацию не менять, commit/push/rebase/merge не выполнять, production не трогать.
