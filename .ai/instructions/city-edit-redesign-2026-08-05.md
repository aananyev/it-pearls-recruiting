# HRM HuntTech — проверка редизайна CityEdit

## Статус

`WAITING_FOR_HERMES`

Проверить ветку `agent/city-edit-redesign` и точный HEAD PR с `base=master`. Код, документацию, merge и production не менять.

## Проверка HEAD

Сверить HEAD ветки, HEAD PR и переданный SHA. Несовпадение — `HEAD_MISMATCH`, проверку остановить.

## Команды

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test --tests '*GeolocationEditFormsContractTest*' --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: профильный тест PASS, `ScreenViewIntegrityTest 8/8 PASS`, SCSS PASS, `BUILD SUCCESSFUL`.

## Local deploy и smoke

- `http://localhost:8080/hrm/` → HTTP 200;
- открыть `CityEdit` из списка городов;
- sidebar содержит название, label-навигацию, региональную сводку и подсказку;
- пункты «Наименование» и «Регион и связь» фокусируют соответствующие блоки и переключают active-state;
- справа две логические карточки без horizontal overflow на 1920×1080 и 1366×768;
- поля `cityRuName`, `cityPhoneCode`, `cityRegion`, lookup региона, сохранение и отмена работают штатно;
- повторное открытие сохранённого города не вызывает detached/unfetched-ошибок;
- Tomcat logs не содержат новых `IllegalStateException`, `NullPointerException`, `Cannot get unfetched attribute` и ошибок XML binding.

## Успех

`STATUS: READY_TO_MERGE`, HEAD match PASS, conflicts NONE, проверки PASS, HTTP 200, smoke PASS, Tomcat errors NONE, P1=0, P2=0, merge не выполнен, production не изменён.

При ошибке — `STATUS: FAILED_VERIFICATION` с шагом, root cause и фрагментом лога.
