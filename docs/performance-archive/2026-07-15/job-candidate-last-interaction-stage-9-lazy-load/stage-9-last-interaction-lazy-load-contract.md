# Stage 9 — отложенная загрузка последнего взаимодействия

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `221c74e9c5c4141bf071cace16b2d07fbdcdb21e`  
**Тип этапа:** performance implementation

## 1. Цель

Удалить синхронный middleware-вызов `InteractionService.getLastIteraction()` из критического пути открытия `JobCandidateEdit` и выполнять его только тогда, когда пользователь нажимает «Копировать взаимодействие» без выбранной строки.

Бизнес-поведение копирования должно сохраниться:

- выбрана строка → копируется выбранное взаимодействие;
- строка не выбрана, последнее взаимодействие существует → новая запись получает его vacancy;
- строка не выбрана, взаимодействий нет → показывается прежний диалог создания нового взаимодействия.

## 2. Разрешённые изменения

Разрешено изменить только:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java
docs/ui/JobCandidateEdit_Spec.md
docs/performance-archive/2026-07-15/job-candidate-last-interaction-stage-9-lazy-load/
.ai/active-work.yml
```

Допускается один узкий unit-тест в:

```text
modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/
```

Запрещено изменять:

- `job-candidate-edit.xml`;
- `InteractionService` и `InteractionServiceBean`;
- JPQL сервиса и `iteractionList-picker-view`;
- entities;
- `views.xml`;
- Liquibase, индексы и БД;
- component ID, actions и captions;
- SCSS;
- production-данные.

## 3. Точная реализация

### 3.1 Удалить запрос из открытия формы

Из `onBeforeShow()` удалить:

```java
lastIteraction = interactionService.getLastIteraction(getEditedEntity());
lastIteractionLoaded = true;
```

После изменения простое открытие карточки не должно вызывать `InteractionService.getLastIteraction()`.

### 3.2 Добавить ленивую загрузку

Добавить метод с содержательным русским комментарием:

```java
private IteractionList ensureLastInteractionLoaded() {
    if (lastIteractionLoaded) {
        return lastIteraction;
    }

    if (PersistenceHelper.isNew(getEditedEntity()) || getEditedEntity().getId() == null) {
        lastIteractionLoaded = true;
        lastIteraction = null;
        return null;
    }

    lastIteraction = interactionService.getLastIteraction(getEditedEntity());
    lastIteractionLoaded = true;
    return lastIteraction;
}
```

Допустима эквивалентная реализация, но обязательны условия:

- новый кандидат не вызывает middleware;
- первое обращение существующего кандидата выполняет не более одного запроса;
- повторное обращение без изменений использует кеш текущего экрана;
- `null` также кешируется и не вызывает повторный запрос.

### 3.3 Использовать метод только в copy-flow

В `copyIteractionJobCandidate()` при отсутствии выбранной строки сначала вызвать `ensureLastInteractionLoaded()`, затем сохранить существующую ветвящуюся логику.

Запрещено менять сценарий при выбранной строке: он не должен выполнять запрос последнего взаимодействия.

### 3.4 Инвалидация после изменения взаимодействий

Добавить метод:

```java
private void invalidateLastInteractionCache() {
    lastIteraction = null;
    lastIteractionLoaded = false;
}
```

Вызывать его в единой точке `reloadInteractions()` перед или после актуализации списка. Это гарантирует, что после создания или изменения взаимодействия последующее копирование получит актуальную последнюю запись.

Не добавлять инвалидацию в многочисленные listeners, если они уже сходятся в `reloadInteractions()`.

### 3.5 Удаление мёртвого legacy-кода

`QUERY_GET_LAST_ITERACTION` можно удалить только после подтверждения, что он не используется активным кодом. Закомментированный legacy-метод `getLastIteraction()` допускается удалить в рамках этого же этапа, поскольку он не компилируется и дублирует сервис.

Удаление других методов или рефакторинг соседнего кода запрещены.

## 4. Документация

Обновить `docs/ui/JobCandidateEdit_Spec.md`:

- в Behavior Summary указать, что последнее взаимодействие не загружается при открытии;
- описать lazy-copy flow;
- зафиксировать кеширование `null` и инвалидацию после `reloadInteractions()`;
- добавить первую строку истории изменений с датой `2026-07-15`.

## 5. Обязательные проверки

### 5.1 Статические

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava
```

Проверить поиском:

- в `onBeforeShow()` отсутствует `getLastIteraction`;
- вызов сервиса находится только внутри lazy-метода;
- `copyIteractionJobCandidate()` вызывает lazy-метод только при отсутствии выбранной строки;
- `reloadInteractions()` инвалидирует кеш.

### 5.2 Автотесты

Обязательно:

```bash
./gradlew :app-web:test --tests '*ScreenViewIntegrityTest*'
./gradlew clean assemble
```

`ScreenViewIntegrityTest`: 8/8 PASS.

При добавлении unit-теста проверить минимум:

1. первый lazy-вызов загружает значение;
2. повторный вызов не обращается к loader;
3. `null` кешируется;
4. инвалидация разрешает новую загрузку;
5. новый кандидат не обращается к loader.

### 5.3 SQL/runtime verification

С SQL-логированием или счётчиком middleware-вызовов подтвердить:

| Сценарий | Ожидаемый результат |
|---|---|
| Открытие существующего кандидата | 0 вызовов `getLastIteraction` |
| Открытие нового кандидата | 0 вызовов |
| Copy при выбранной строке | 0 вызовов |
| Первый Copy без выбранной строки | 1 вызов |
| Повторный Copy без изменений | 0 дополнительных вызовов |
| После создания взаимодействия и `reloadInteractions()` | следующий Copy выполняет 1 новый вызов |

### 5.4 Ручной smoke-test

Проверить:

- существующий кандидат с взаимодействиями открывается;
- кандидат без взаимодействий открывается;
- новый кандидат открывается;
- Copy с выбранной строкой сохраняет прежний сценарий;
- Copy без выбранной строки переносит vacancy последнего взаимодействия;
- при отсутствии взаимодействий показывается прежний диалог;
- после добавления нового взаимодействия Copy использует новую последнюю запись;
- нет `unfetched`, `detached`, NPE и дублирования взаимодействий;
- `/hrm` отвечает HTTP 200.

## 6. Acceptance gate

Stage 9 нельзя объявлять завершённым только по `HTTP 200`.

Обязательны одновременно:

- функциональный Java-коммит;
- diff только в разрешённом scope;
- обновлённая спецификация;
- `ScreenViewIntegrityTest` 8/8;
- `clean assemble` — BUILD SUCCESSFUL;
- таблица SQL/runtime verification;
- ручной smoke-test;
- итоговый отчёт.

При отсутствии любого пункта выставить:

```text
STAGE_9_BLOCKED
```

а не «готов к следующему этапу».

## 7. Итоговый отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-last-interaction-stage-9-lazy-load/
stage-9-last-interaction-hermes-report.md
```

Отчёт должен содержать:

- точный исходный и итоговый SHA;
- перечень изменённых файлов;
- полный смысловой diff;
- результаты compile/test/assemble;
- SQL/runtime verification по шести сценариям;
- результаты ручного smoke-test;
- HTTP 200;
- финальный вердикт `STAGE_9_ACCEPTED` или `STAGE_9_BLOCKED`.
