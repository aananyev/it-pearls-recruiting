# Задача ChatGPT: фикс компиляции PR #110 (OpenPositionEditPreview)

Дата: 2026-08-01
Источник: верификация PR #110 `agent/open-position-edit-preview` (WAITING_FOR_HERMES → FAILED_VERIFICATION)

## Ошибка

PR #110 не компилируется: `:app-web:compileJava` → BUILD FAILED.

```
modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java:9: error: cannot find symbol
import com.haulmont.cuba.gui.screen.AfterShowEvent;
                                   ^
symbol:   class AfterShowEvent
location: package com.haulmont.cuba.gui.screen
```

## Root cause

В CUBA 7.3 `AfterShowEvent` — вложенный класс `com.haulmont.cuba.gui.screen.Screen.AfterShowEvent`, а не отдельный класс пакета `com.haulmont.cuba.gui.screen`. Во всём проекте используется именно вложенный вариант:

```java
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
```

## Требуемый фикс

В `OpenPositionEditPreview.java` заменить строку 9:

```java
import com.haulmont.cuba.gui.screen.AfterShowEvent;
```

на:

```java
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
```

## Проверка после фикса

```bash
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
```

Дождаться BUILD SUCCESSFUL, затем обновить PR (force-push в `agent/open-position-edit-preview`) и повторно запросить Hermes-верификацию (HEAD изменится — обновить Verified HEAD в PR).
