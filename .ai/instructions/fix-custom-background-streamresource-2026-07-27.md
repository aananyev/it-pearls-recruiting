# Проверка пользовательского фона через StreamResource

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/fix-custom-background-streamresource-clean`
- Base: `master`
- PR: указать номер после создания.
- Проверять только точный HEAD SHA из PR.
- Режим: проверка без изменения функционального кода и документации.
- Несовпадение branch HEAD, PR HEAD или переданного SHA → `HEAD_MISMATCH`, проверку остановить.

## Область проверки

Проверяется фактический контракт пользовательского фона:

```text
FileLoader.openStream(descriptor)
→ InputStream.readAllBytes()
→ byte[]
→ StreamResource(ByteArrayInputStream)
→ MIME type
→ HrmMainScreen.registerBackgroundResource()
→ ResourceReference / Vaadin connector URL
→ app://APP удаляется перед записью в CSS
```

Дополнительно проверить:

- `FileStorageException` и `IOException` включают системный fallback;
- descriptor не удаляется при временной недоступности файла;
- `ExternalResource` и `/dispatch/download?f=` не используются;
- системные `ThemeResource` и каталоги `backgrounds/1.jpg … 10.jpg` не изменены;
- `ExtSettingsWindowMainBackground`, entity, БД, datasource и FileStorageService не изменены.

## Проверка HEAD и diff

```bash
git fetch --all --prune
git checkout agent/fix-custom-background-streamresource-clean
git reset --hard origin/agent/fix-custom-background-streamresource-clean

git rev-parse HEAD
git status --short
git diff --check
git diff --name-status origin/master...HEAD
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- base PR = `master`;
- conflicts = `NONE`;
- рабочее дерево чистое;
- diff содержит только согласованные изменения главного фона, документации и инструкции;
- системные темы, SettingsWindow, entity, БД и FileStorageService не изменены.

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
- контрактный тест подтверждает `readAllBytes`, `StreamResource`, MIME, `registerBackgroundResource`, `ResourceReference`, `app://APP` и отсутствие `ExternalResource`.

## SCSS и полная сборка

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- семь тем собраны;
- системные каталоги фонов не изменены;
- `BUILD SUCCESSFUL`.

## Clean local deploy и HTTP

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. Войти пользователем без пользовательского фона и подтвердить системный фон.
4. Открыть `SettingsWindow` → «Интерфейс» → «Фон главного экрана».
5. Загрузить PNG, JPEG и WebP, сохранить через «ОК».

## Functional smoke

Для каждого формата подтвердить:

1. фон обновляется после сохранения без повторного входа;
2. после повторного входа фон снова отображается;
3. computed `background-image` содержит connector URL;
4. URL не содержит `/dispatch/download?f=`;
5. URL не содержит необработанный `app://APP`;
6. HTTP-запрос ресурса возвращает 200;
7. `Content-Type` равен `image/png`, `image/jpeg` или `image/webp`;
8. `background-size` равен `100% 100%`;
9. dashboard остаётся прозрачным;
10. DOM содержит `data-hrm-main-background="applied"`.

## FileStorage fallback

1. Сохранить пользовательский фон.
2. Временно сделать физический файл недоступным при сохранённом `FileDescriptor`.
3. Повторно войти.
4. Подтвердить:
   - вход не блокируется;
   - применяется системный фон;
   - descriptor остаётся в БД;
   - после восстановления файла пользовательский фон возвращается.

## Системные ThemeResource

Проверить темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Для каждой темы подтвердить URL `VAADIN/themes/{theme}/backgrounds/{n}.jpg`, HTTP 200 и отсутствие регрессий выбора системного варианта.

## Runtime logs

После всех сценариев недопустимы новые:

- `IllegalStateException` регистрации ресурса;
- необработанный `FileStorageException`;
- `OutOfMemoryError`;
- ошибки Vaadin connector/resource key;
- HTTP 404/500 для пользовательского connector URL;
- `NullPointerException` в изменённом сценарии;
- detached/unfetched ошибки.

## Отчёт Hermes

Сохранить:

```text
.ai/reports/2026-07-27-fix-custom-background-streamresource.md
```

Успешный отчёт:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/fix-custom-background-streamresource-clean
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

При ошибке использовать `STATUS: FAILED_VERIFICATION`, указать FAILED STEP, ROOT CAUSE, log/stack trace и невыполненные проверки. Код, документацию, commit, push, rebase, merge и production не менять.
