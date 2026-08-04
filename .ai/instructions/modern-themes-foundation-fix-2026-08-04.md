# Проверка исправления тем HRM HuntTech Modern

## Контекст

Проект: **HRM HuntTech**  
Репозиторий: `aananyev/it-pearls-recruiting`  
Ветка: `agent/modern-themes-foundation-fix`  
Base: `master`  
PR и точный `VERIFIED HEAD` берутся из актуального описания PR. Новый коммит после
начала проверки аннулирует отчёт.

Режим: **проверка без изменения функционального кода, XML, SCSS и docs**.  
Merge запрещён. Production запрещён.

## Preflight

1. Проверить существование ветки и открытого PR.
2. Проверить `base=master`.
3. Проверить, что HEAD ветки, HEAD PR и `VERIFIED HEAD` из PR совпадают.
4. При любом несовпадении:
   - статус `HEAD_MISMATCH`;
   - дальнейшие проверки остановить;
   - код не менять.

## Команды

```bash
git diff --check master...HEAD

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.ModernThemesFoundationContractTest' \
  --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
  --no-daemon --stacktrace

./gradlew clean assemble \
  --no-daemon --stacktrace
```

После успешной сборки выполнить локальный deploy штатным способом Hermes и проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

## Runtime smoke

Проверить обе темы на одном и том же пользователе.

### `hunttech-modern-light`

1. Выбрать тему в SettingsWindow и применить её.
2. Перезагрузить `/hrm/`.
3. Проверить:
   - логотип не превышает высоту menubar и не растягивает страницу;
   - главное меню имеет штатную CUBA-компоновку;
   - раскрываются «Отчёты» и «Администрирование»;
   - открываются Table/DataGrid, Edit-форма, popup и окно;
   - TextField/TextArea, primary/secondary button, tabs и selection читаемы;
   - отсутствуют сырой HTML, наложения и горизонтальный overflow.

### `hunttech-modern-dark`

Повторить тот же сценарий и дополнительно проверить:

- фон действительно тёмный;
- основной и вторичный текст читаемы;
- поля, таблицы, popup и окно не имеют белых провалов;
- hover, focus, selected, disabled и read-only различимы;
- корпоративный красный акцент не ухудшает контраст.

### Viewport

Проверить минимум:

- `1920×1080`;
- `1366×768`.

## Логи

После smoke проверить Tomcat logs на:

```text
ERROR
SEVERE
SassException
Theme
Widgetset
IllegalStateException
NullPointerException
```

Известные unrelated warnings перечислить отдельно, не скрывать.

## Критерий PASS

- HEAD match PASS;
- conflicts NONE;
- `ModernThemesFoundationContractTest` PASS;
- `buildScssThemes` PASS;
- `clean assemble` — `BUILD SUCCESSFUL`;
- локальный deploy PASS;
- `/hrm/` — HTTP 200;
- smoke обеих тем PASS на двух viewport;
- сырой HTML и гигантский логотип отсутствуют;
- Tomcat errors по изменённому сценарию NONE;
- docs/history synchronized;
- P1=0, P2=0.

Отчёт должен содержать:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE | FAILED_VERIFICATION
Repo:
Branch:
PR:
Base:
Verified HEAD:
HEAD match:
Conflicts:
Tests:
SCSS:
Build:
Deploy:
HTTP:
Smoke light:
Smoke dark:
Tomcat errors:
Docs:
P1:
P2:
Merge: NOT PERFORMED
Production: NOT CHANGED
```
