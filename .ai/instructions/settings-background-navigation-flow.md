# Проверка регистрации HrmMainScreen и поведения SettingsWindow

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/settings-background-navigation-flow`  
BASE: `master`  
BASE SHA: `c34841a4a95c699124cce3006e9aa77d9e4aa52a`  
STATUS: `WAITING_FOR_HERMES`

Проверять только точный HEAD из PR. Подтвердить HEAD ветки и PR, `base=master`, conflicts NONE. Несовпадение → `HEAD_MISMATCH`.

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
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Runtime main screen

1. Подтвердить `cuba.web.mainScreenId=hrmMainScreen` и явную запись в `web-screens.xml`.
2. После входа подтвердить фактический controller `HrmMainScreen`.
3. Проверить общий Vaadin UI у `mainVBox`, `mainDashboard`, `backgroundResourceHolder`.
4. Connector resource → HTTP 200; системный MIME `image/svg+xml`.
5. Проверить семь тем, минимум три входа, смену системного варианта.
6. Проверить персональные PNG/JPG/JPEG/WEBP и возврат к системному каталогу.
7. Подтвердить работу favicon, уведомлений и резервов `ExtMainScreen`.

## SettingsWindow

1. На вкладке «Интерфейс» есть пятый пункт «Фон главного экрана».
2. Нажатие фокусирует upload; active state только один.
3. «ОК» сохраняет и закрывает без диалога выхода.
4. «Отмена» показывает: `Остаться в экране или выйти без сохранения?`.
5. Действия: `Остаться`, `Выйти без сохранения`.
6. Discard не сохраняет изменения.
7. Проверить отсутствие регрессии «Обо мне», email, AI и штатных интерфейсных настроек.

## Deploy

Local deploy точного HEAD, `/hrm/` = 200, Tomcat critical errors NONE, P1=0, P2=0.

Отчёт: `.ai/reports/2026-07-26-settings-background-navigation-flow.md`.

Hermes не меняет код/docs/БД, не делает commit, push, rebase, merge и не трогает production.
