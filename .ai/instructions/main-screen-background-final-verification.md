# Финальная проверка runtime-контракта фона главного экрана

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/main-screen-background-final-verification`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`

Проверять только точный HEAD PR. До команд подтвердить:

- ветка существует;
- HEAD ветки равен HEAD PR;
- `base=master`;
- HEAD PR равен переданному SHA;
- conflicts NONE.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

Hermes выполняет проверку без изменения Java, XML, SCSS, tests и docs; без commit, push, rebase, merge, БД и production.

## Область этапа

1. Один `mainScreenBackgroundLayer`; `mainVBox` и dashboard не владеют background-image.
2. Screen-level CUBA integration test через `TestContainer`.
3. Runtime browser verification computed style, connector URL, MIME и screenshot.
4. UI-scoped мгновенное обновление после сохранения SettingsWindow.
5. Исключение немедленного повтора системного SVG.
6. Проверка/нормализация PNG/JPEG/WEBP, resize, очистка metadata и лимит результата.
7. Безопасный cleanup и Cancel.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
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

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `MainScreenBackgroundContractTest` — `10/10 PASS`;
- `HrmMainScreenIntegrationTest` — все сценарии PASS;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- WEBP ImageIO reader зарегистрирован;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Clean local deploy

1. Checkout точного HEAD.
2. Удалить stale exploded webapp/widgetset artifacts по действующему протоколу HRM HuntTech.
3. Выполнить:
   ```bash
   APP_CONTEXT=hrm ./scripts/rebuild-widgetset-and-start.sh
   ```
4. Проверить `http://localhost:8080/hrm/` → HTTP 200.
5. Проверить effective `${app.home}/local.app.properties`: `cuba.web.mainScreenId` не переопределён значением `extMainScreen`.
6. Открыть новый Incognito/Private-сеанс:
   `http://localhost:8080/hrm/?restartApplication`.

## Browser/runtime smoke

### Root screen и layer

1. После login фактический controller — `HrmMainScreen`.
2. `mainScreenBackgroundLayer` существует.
3. На layer:
   - `data-hrm-main-background="applied"`;
   - `data-hrm-main-background-resource` содержит connector URL;
   - computed `background-image` содержит этот URL;
   - `position:absolute`;
   - `z-index:0`;
   - `pointer-events:none`;
   - `background-size:cover`.
4. На `mainVBox`:
   - `data-hrm-main-controller="HrmMainScreen"`;
   - computed `background-image` не содержит ресурс фона.
5. На dashboard:
   - computed `background-image` не содержит ресурс фона;
   - `position:relative`;
   - `z-index:1`;
   - фон прозрачен.
6. Нет двойного кадрирования/двойной отрисовки.
7. Сделать screenshot системного режима.

### Connector resource

1. Извлечь URL из computed `background-image`.
2. Выполнить HTTP request в том же authenticated browser session.
3. Системный фон:
   - HTTP 200;
   - MIME `image/svg+xml`.
4. Пользовательский фон:
   - HTTP 200;
   - MIME `image/jpeg`.

### Системный каталог

1. Выполнить минимум пять последовательных обновлений/повторных входов.
2. Зафиксировать имя SVG/variant.
3. Ни один вариант не повторяется непосредственно после самого себя.
4. Проверить минимум темы `hover`, `halo`, `hunttech-modern-dark`.

### Upload и нормализация

1. SettingsWindow → «Интерфейс» → «Фон главного экрана».
2. Нажать «Выбрать изображение» — системный file chooser реально открывается.
3. Проверить PNG, JPG/JPEG и WEBP.
4. После выбора отображается имя файла.
5. До «ОК» главный экран не меняется.
6. После «ОК» без перезахода:
   - событие доставлено текущей вкладке;
   - layer получает новый connector URL;
   - фон отображается;
   - MIME итогового ресурса `image/jpeg`.
7. Проверить, что сохранённый descriptor:
   - начинается с `hrm-main-background-`;
   - extension `jpg`;
   - размер ≤ 4 МБ.
8. Проверить изображение больше 2560 × 1440: сохранённый результат уменьшен.
9. Проверить JPEG с EXIF: итоговый файл не содержит EXIF.
10. Проверить spoofed extension, corrupt image, файл >15 МБ и изображение сверх pixel/dimension limit:
    - загрузка отклонена;
    - datasource не переключён;
    - старый фон не потерян;
    - временный upload не остаётся в storage.
11. Сделать screenshot пользовательского режима.

### Очистка и Cancel

1. С сохранённым пользовательским фоном нажать «Использовать системные фоны».
2. До «ОК» изменение остаётся локальным SettingsWindow.
3. «Отмена» → «Остаться» — экран остаётся открыт.
4. «Отмена» → «Выйти без сохранения»:
   - старый сохранённый фон остаётся;
   - pending-created файлы удалены;
   - event не опубликован.
5. Повторить очистку → «ОК»:
   - layer без перезахода получает системный SVG;
   - старый маркированный пользовательский файл удалён;
   - legacy-файлы и другие изображения пользователя не удалены.

### Регрессия ExtMainScreen

Проверить:

- dashboard `recruiting-dashboard` работает;
- меню и WorkArea работают;
- favicon работает;
- уведомления и проверки резерва выполняются;
- ошибок `Cannot get unfetched attribute`, Vaadin RPC, connector, FileStorage, ImageIO и WEBP reader нет.

## HTTP и логи

- `/hrm/` = HTTP 200;
- Tomcat critical errors NONE;
- Upload/FileStorage/FileDescriptor/ImageIO/WEBP/Vaadin RPC/connector errors NONE;
- P1=0;
- P2=0.

## Отчёт

Сохранить:

`.ai/reports/2026-07-26-main-screen-background-final-verification.md`

Обязательно указать:

- Repo, Branch, PR, Base;
- `проверен HEAD: <SHA>`;
- HEAD match;
- conflicts;
- результаты каждой команды;
- `10/10`, integration test и `8/8`;
- SCSS и build;
- deploy и HTTP;
- effective mainScreenId;
- фактический controller;
- layer/computed-style evidence;
- connector URL/HTTP/MIME;
- screenshots;
- upload/validation/normalization;
- no-repeat evidence;
- event refresh;
- Cancel/cleanup;
- Tomcat errors;
- P1/P2;
- docs/history synchronized;
- merge не выполнен;
- production не изменён.

Успех:

`PROJECT: HRM HuntTech`  
`STATUS: READY_TO_MERGE`

Любая неполная browser/runtime-проверка либо ошибка:

`STATUS: FAILED_VERIFICATION`
