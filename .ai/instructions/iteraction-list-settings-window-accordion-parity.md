# Hermes — проверка унификации аккордеонов IteractionListEdit с SettingsWindow

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-settings-window-accordion-parity`
- Base: `master`
- Проверяемый HEAD: точный `HEAD` PR, указанный в описании PR
- Режим: проверка без изменения функционального кода и `docs/`

До запуска подтвердить: ветка существует; branch HEAD = PR HEAD = SHA из PR; `base=master`; conflicts `NONE`; рабочее дерево чистое. Несовпадение означает `HEAD_MISMATCH` и остановку проверки.

## Область изменения

Изменены только локальные SCSS-файлы `IteractionListEdit` семи тем, `IteractionListAccordionCssContractTest`, `docs/ui/IteractionListEdit_Spec.md` и эта инструкция. Java, XML, entity, БД, Liquibase, component ID, captions, dataContainer, property bindings, loaders, JPQL, actions, `invoke`, required/visible и бизнес-логика не изменялись.

## Автоматические проверки

```bash
git diff --check

./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace

./gradlew :app-core:test --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: профильные тесты PASS; `ScreenViewIntegrityTest 8/8 PASS`; SCSS семи тем PASS; `BUILD SUCCESSFUL`.

## Local deploy и visual smoke

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. В каждой из семи тем открыть `IteractionListEdit` и `SettingsWindow` для прямого визуального сравнения.
4. Подтвердить одинаковые:
   - поверхность, границу, радиус и тень секций;
   - высоту, отступы, цвет, фон и типографику заголовков;
   - высоту, шрифт, фон, границу и радиус полей;
   - подписи, checkbox, option group и кнопки;
   - focus, hover, readonly и disabled-состояния.
5. Проверить раскрытие и сворачивание всех четырёх рабочих аккордеонов, естественную высоту, отсутствие наложений и сохранение видимости блока пяти быстрых действий.
6. Проверить candidate/vacancy, dynamic fields, rating, recruiter, communication method, comment, subscription, save/cancel без регрессии.
7. Tomcat critical errors: `NONE`; P1=0; P2=0.

Hermes не меняет код или документацию, не делает commit, push, rebase, merge и не изменяет production.

Успешный отчёт: `STATUS: READY_TO_MERGE` с Repo/Branch/PR/Base/Verified HEAD, результатами всех проверок и формулировкой `проверен HEAD: <SHA>`.
