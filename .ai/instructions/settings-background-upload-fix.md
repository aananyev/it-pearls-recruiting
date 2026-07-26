# Проверка исправления загрузки фона SettingsWindow

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/settings-background-upload-fix`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`

Проверять только точный HEAD, указанный в PR. До выполнения команд подтвердить: ветка существует; HEAD ветки равен HEAD PR; `base=master`; conflicts NONE. Несовпадение означает `HEAD_MISMATCH`, проверку остановить.

## Область проверки

Исправлена только карточка SettingsWindow → «Интерфейс» → «Фон главного экрана»:

- реальная кнопка CUBA `FileUploadField` использует `uploadButtonCaption`;
- file chooser имеет ненулевую интерактивную область;
- имя файла отображается;
- успешный и ошибочный upload обрабатываются раздельно;
- статусы имеют точные формулировки;
- очистка и Cancel сохраняют прежний файловый контракт.

Hermes не меняет Java, XML, SCSS, tests или docs, не делает commit, push, rebase, merge, не изменяет БД и production.

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

- `MainScreenBackgroundContractTest` — `10/10 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Browser smoke

После чистого local deploy точного HEAD:

1. Открыть новый Incognito/Private-сеанс: `http://localhost:8080/hrm/?restartApplication`.
2. Выполнить вход и открыть SettingsWindow → «Интерфейс» → «Фон главного экрана».
3. При отсутствии пользовательского файла подтвердить статус `Используется случайный фон активной темы.`.
4. Нажать «Выбрать изображение» и подтвердить реальное открытие системного file chooser.
5. Выбрать поддерживаемый PNG/JPG/JPEG/WEBP до 15 МБ.
6. Подтвердить отображение имени файла и статус `Используется пользовательский фон.`.
7. Нажать «ОК», повторно открыть SettingsWindow и подтвердить сохранённый пользовательский режим.
8. Повторно войти в HRM HuntTech и подтвердить отображение выбранного фона.
9. Нажать «Использовать системные фоны» и подтвердить системный статус.
10. Нажать «Отмена» → «Остаться» и подтвердить, что экран остаётся открыт.
11. Повторить очистку → «Отмена» → «Выйти без сохранения» и подтвердить, что сохранённый пользовательский фон не потерян.
12. Снова очистить → «ОК» → повторно войти и подтвердить системный фон.
13. Проверить ошибку неподдерживаемого файла и превышения лимита: datasource и статус не должны перейти в пользовательский режим.
14. Проверить `/hrm/` = HTTP 200.
15. Проверить Tomcat logs: Upload/FileStorage/FileDescriptor/Vaadin RPC/connector errors NONE; P1=0; P2=0.

## Отчёт

Сохранить `.ai/reports/2026-07-26-settings-background-upload-fix.md`.

Указать:

- Repo, Branch, PR, Base;
- `проверен HEAD: <SHA>`;
- HEAD match и conflicts;
- результаты всех команд;
- browser smoke по каждому шагу;
- HTTP 200;
- Tomcat errors;
- docs/history synchronized;
- P1/P2;
- merge не выполнен;
- production не изменён.

Успех: `STATUS: READY_TO_MERGE`.  
Любая ошибка либо неполный browser smoke: `STATUS: FAILED_VERIFICATION`.
