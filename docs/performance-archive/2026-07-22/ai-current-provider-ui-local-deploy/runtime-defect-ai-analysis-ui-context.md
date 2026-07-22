# Runtime-дефект AI-анализа: UI-фасады получались через Spring

> **Дата обнаружения:** 2026-07-22  
> **Ветка:** `feat/ai-entity-analysis`  
> **Проверенный дефектный код:** `9c5ce99f3797134bd403e117850f18e0cb82b203`  
> **Docs-only отчёт с преждевременным PASS:** `620f9e4cb1a5ed599de46a30fa6d9451135b4525`  
> **Статус:** исправление закоммичено, локальная runtime-проверка нового HEAD обязательна

## 1. Фактический сценарий

При реальном нажатии кнопки AI-анализа в `OpenPositionEdit` приложение завершило обработчик исключением:

```text
NoSuchBeanDefinitionException: No qualifying bean of type
'com.haulmont.cuba.gui.Notifications' available
```

Цепочка вызова:

```text
OpenPositionEdit.onAiAnalysisClick
→ AiAnalysisHelper.analyze
→ AppBeans.get(Notifications.class)
```

## 2. Причина

`Notifications` и `Dialogs` являются UI-фасадами CUBA Platform. Они создаются внутренней инфраструктурой UI-контекста и не являются обычными Spring beans, доступными через `AppBeans.get(Class)`.

Исходный helper использовал Spring lookup для обоих UI-фасадов. Поэтому:

- ошибка бизнес-/AI-сервиса не могла быть показана пользователю, поскольку обработчик ошибки сам падал на получении `Notifications`;
- успешный ответ также не мог быть показан, поскольку получение `Dialogs` через `AppBeans` имело тот же дефект;
- source-contract tests и компиляция не обнаруживали ошибку, поскольку реальный Vaadin/CUBA UI-контекст не запускался.

## 3. Исправление

UI-фасады теперь получаются из контекста текущего экрана:

```java
Notifications notifications = UiControllerUtils.getScreenContext(screen).getNotifications();
Dialogs dialogs = UiControllerUtils.getScreenContext(screen).getDialogs();
```

Дополнительно удалена повторная загрузка сущности с `View.LOCAL` на web-tier. `AiAnalysisServiceBean` самостоятельно перезагружает сущность специализированным analysis-view на core-tier.

## 4. Регрессия

Добавлен `AiAnalysisHelperUiContextContractTest`, который проверяет:

1. получение `Notifications` из `ScreenContext`;
2. получение `Dialogs` из `ScreenContext`;
3. отсутствие `AppBeans.get(Notifications.class)`;
4. отсутствие `AppBeans.get(Dialogs.class)`;
5. отсутствие повторной загрузки `View.LOCAL`;
6. сохранение трёхаргументного контракта helper-а.

## 5. Влияние на прежний отчёт

Итог `PASS` в `local-verification-report.md` не подтверждал реальную работу AI-кнопок. В отчёте сценарии были отмечены успешными на основании чтения кода, несмотря на то что ручной браузерный AI runtime smoke-test был указан как невыполненный.

Поэтому прежний итог следует трактовать так:

```text
Компиляция и статические tests: PASS
Локальный deploy и HTTP: PASS
Реальный AI UI runtime: FAIL
Полная приёмка: НЕ ЗАВЕРШЕНА
```

## 6. Обязательная повторная проверка

После сборки нового HEAD Hermes должен:

- выполнить `AiAnalysisHelperUiContextContractTest`;
- выполнить `ScreenViewIntegrityTest` 8/8;
- выполнить `clean assemble` и локальный deploy;
- нажать AI-анализ в `OpenPositionEdit`;
- подтвердить отсутствие `NoSuchBeanDefinitionException` для `Notifications` и `Dialogs`;
- подтвердить показ ответа AI либо корректного бизнес-сообщения об ошибке;
- повторить минимум один сценарий в `CandidateCVEdit` или `IteractionListEdit`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Зафиксирован runtime-дефект Spring lookup UI-фасадов, описание исправления и обязательный повторный smoke-test. |
