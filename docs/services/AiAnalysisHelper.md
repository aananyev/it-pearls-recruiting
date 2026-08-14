# AiAnalysisHelper — UI-мост системного AI-анализа

## Business & Context Intro

### 1. Назначение и Бизнес-смысл

`AiAnalysisHelper` предоставляет единый сценарий запуска AI-анализа из экранов HRM HuntTech: проверяет наличие сущности, вызывает middleware-сервис и показывает результат или понятную ошибку пользователю.

### 2. Связи в интерфейсе и Навигация

Helper вызывается кнопками AI-анализа из `OpenPositionEdit`, `CandidateCVEdit`, `IteractionListEdit` и других экранов, использующих системные промпты. Результат отображается в контексте того же открытого экрана.

### 3. Краткий обзор бизнес-логики поведения

- Пользователь нажимает AI-анализ → сущность отсутствует → экран показывает предупреждение и не вызывает middleware.
- Пользователь нажимает AI-анализ → сущность присутствует → `AiAnalysisService` выполняет анализ через текущую AI-конфигурацию → экран показывает ответ в диалоге.
- Middleware возвращает ошибку → helper перехватывает исключение → экран показывает уведомление об ошибке.

## UI-контекст CUBA

`Dialogs` и `Notifications` относятся к UI-инфраструктуре CUBA Platform и не зарегистрированы как обычные Spring beans. Поэтому их запрещено получать через:

```java
AppBeans.get(Notifications.class)
AppBeans.get(Dialogs.class)
```

Корректный источник — `ScreenContext` текущего экрана:

```java
Notifications notifications = UiControllerUtils.getScreenContext(screen).getNotifications();
Dialogs dialogs = UiControllerUtils.getScreenContext(screen).getDialogs();
```

Такой контракт сохраняет общий трёхаргументный вызов:

```java
AiAnalysisHelper.analyze(this, getEditedEntity(), "VACANCY_ANALYSIS");
```

## Загрузка сущности

Helper не перезагружает сущность на web-tier с `View.LOCAL`. Полный специализированный граф загружается в `AiAnalysisServiceBean` на core-tier. Это исключает лишний запрос и предотвращает потерю полей, необходимых placeholder-экстракторам.

## Ошибка, устранённая 2026-07-22

До исправления реальный клик по AI-кнопке завершался:

```text
NoSuchBeanDefinitionException: No qualifying bean of type
'com.haulmont.cuba.gui.Notifications' available
```

Причина: статический helper запрашивал UI-фасады через Spring `AppBeans`. Автоматизированные source-contract tests не выполняли реальный UI-вызов и не обнаружили дефект до ручного smoke-test.

## Проверки

Обязательные проверки после изменения:

- `AiAnalysisHelperUiContextContractTest`;
- `ScreenViewIntegrityTest` — 8/8;
- компиляция `app-web`;
- локальный deploy;
- реальное нажатие AI-кнопки минимум в `OpenPositionEdit`;
- отображение ответа или бизнес-ошибки без `NoSuchBeanDefinitionException`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | UI-фасады переведены с `AppBeans` на `UiControllerUtils.getScreenContext`; удалена лишняя загрузка `View.LOCAL`; добавлен regression-test runtime-контракта. |
