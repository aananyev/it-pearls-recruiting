# Диагностика PR #110 — OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-edit-preview
PR: #110
Base: master
Verified HEAD: 90486e5548e56040441925fe55c6098e4e4b3d6d
HEAD match: PASS
Conflicts: NONE

## FAILED STEP

```bash
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

## ROOT CAUSE

Неверный импорт в `OpenPositionEditPreview.java` (строка 9): импортирован
`com.haulmont.cuba.gui.screen.AfterShowEvent`, но в CUBA 7.3 класс
`AfterShowEvent` является вложенным классом `com.haulmont.cuba.gui.screen.Screen.AfterShowEvent`
и не существует как отдельный класс пакета `com.haulmont.cuba.gui.screen`.
Во всём проекте (OpenPositionEdit и др.) используется именно вложенный вариант
`import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;`.

## ERROR

```
/Users/alekseyananyev/StudioProjects/hunttech_recruiting/modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java:9: error: cannot find symbol
import com.haulmont.cuba.gui.screen.AfterShowEvent;
                                   ^
  symbol:   class AfterShowEvent
  location: package com.haulmont.cuba.gui.screen
> Task :app-web:compileJava FAILED
BUILD FAILED in 15s
```

Класс: `OpenPositionEditPreview`
Файл: `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java`
Строка: 9

## STACK TRACE

```
/Users/alekseyananyev/StudioProjects/hunttech_recruiting/modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java:9: error: cannot find symbol
import com.haulmont.cuba.gui.screen.AfterShowEvent;
                                   ^
  symbol:   class AfterShowEvent
  location: package com.haulmont.cuba.gui.screen
```

(единственная ошибка компиляции: `grep -E "error:"` даёт ровно одну строку —
указанная выше; иных ошибок до остановки компилятора нет)

## REPRODUCTION

```bash
git fetch origin agent/open-position-edit-preview
git checkout agent/open-position-edit-preview
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

## EXPECTED

`BUILD SUCCESSFUL` — компиляция `:app-web:compileJava` и `:app-web:compileTestJava`
проходит без ошибок.

## ACTUAL

`BUILD FAILED in 15s` — задача `:app-web:compileJava` падает на ошибке импорта
в строке 9 файла `OpenPositionEditPreview.java`.

## COMPLETED CHECKS

- HEAD match: PASS (ветка = Verified HEAD 90486e5548e56040441925fe55c6098e4e4b3d6d)
- Conflicts: NONE (git merge-tree = 0 конфликтов)
- Diff scope: PASS (только новые файлы + docs/ui/README.md индекс; legacy не тронут)
- git diff --check: 1 trailing whitespace в `.ai/instructions/open-position-edit-preview-2026-08-01.md:3` — минорное, не влияет на сборку

## NOT EXECUTED

(не выполнялись из-за падения компиляции)

- `OpenPositionEditPreviewLayoutTest`
- `ScreenViewIntegrityTest`
- `:app-web:buildScssThemes`
- `clean assemble`
- Local deploy + HTTP /hrm/
- Smoke-сценарии

## RECOMMENDATION

Исправить `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java`, строка 9:

```java
// было:
import com.haulmont.cuba.gui.screen.AfterShowEvent;
// нужно (как во всём проекте):
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
```

После фикса — пересобрать, обновить PR (новый Verified HEAD) и запросить повторную
верификацию Hermes. Код Hermes самостоятельно не меняет.
