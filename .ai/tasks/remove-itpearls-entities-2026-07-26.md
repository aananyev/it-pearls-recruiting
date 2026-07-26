# Task: Удалить мёртвые ссылки на itpearls_* в проекте

## Git контекст
- Repo: `aananyev/it-pearls-recruiting`
- Ветка: `agent/iteraction-list-accordion-reference-finish`
- HEAD: `73b34aef69bdc6421abdccf5ea51130ed31d5e0c`

## Проблема

В проекте остались две мёртвые ссылки на `itpearls_*` неймспейс. Ни одной `itpearls_*` сущности, view, экрана или сервиса в коде нет — все сущности мигрированы в `hunttech_*`:

- `persistence.xml` — 62 entity, все `hunttech_*`
- `views.xml` (1374 строки) — ни одной `itpearls_*`
- Web screens, Java контроллеры, сервисы — чисто
- `app.properties`, `build.gradle` — чисто

## Что нужно исправить

### 1. `modules/global/src/com/company/hunttech/metadata.xml`

Строка 4 — пустое объявление неймспейса, ни одного класса не существует в `com.company.itpearls`:
```xml
<metadata-model root-package="com.company.itpearls" namespace="itpearls"/>
```

**Действие:** удалить строку 4. Оставить только:
```xml
<metadata-model root-package="com.company.hunttech" namespace="hunttech"/>
```

### 2. `modules/core/test/com/company/hunttech/core/ScreenViewIntegrityTest.java`

Три теста проверяют несуществующие `itpearls_*` entity:

- **test6 (строка 69):** `metadata.getClassNN("itpearls_ExtUser")` — нет такого класса
- **test7 (строка 75):** `metadata.getClassNN("itpearls_JobCandidate")` — нет такого класса
- **test8 (строка 81):** `metadata.getClassNN("itpearls_ExtUser")` — дубль test6

**Действие:** удалить test6, test7, test8 — они проверяют сущности из старого IT-Pearls неймспейса, которых больше нет в проекте. Тест test1–test5 (UserAiProfile) остаются, они проходят.

## Проверка

После изменений:
```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается: `8/8 PASS` (5 существующих + 3 удалены — будет 5/5 PASS).
