# Проверка пользовательского фона через FileStorage dispatch

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/custom-background-dispatch-fallback`
- Base: `master`
- Проверять только точный HEAD PR.
- Режим: проверка без изменения функционального кода и документации.

## Обязательные проверки

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

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## Runtime smoke

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. Пользовательский файл существует:
   - URL фона имеет вид `/hrm/dispatch/download?f={uuid}`;
   - URL не содержит `connector`, `app://APP` или Vaadin resource key;
   - запрос возвращает HTTP 200 и `image/jpeg`;
   - фон отображается и растягивается до `100% 100%`.
4. Перезапустить Tomcat и повторить проверку без повторной загрузки файла.
5. Временно сделать физический файл недоступным при сохранённом `FileDescriptor`:
   - главный экран открывается;
   - применяется системный JPG активной темы;
   - `FileDescriptor` не удаляется из БД;
   - в логах есть только диагностический warning без stack trace, блокирующего UI.
6. Восстановить файл и подтвердить возврат пользовательского фона.
7. Проверить системные фоны семи тем и отсутствие регрессии антиповтора.
8. Tomcat critical errors: NONE; P1=0; P2=0.

## Запреты

Не выполнять commit, push, rebase, merge, изменение БД и любые действия на production.
