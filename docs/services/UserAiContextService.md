# UserAiContextService

> Middleware-сервис HRM HuntTech для формирования безопасного пользовательского контекста без обращения к внешнему LLM.  
> Контракт: `com.company.hunttech.service.UserAiContextService`.  
> Spring bean: `hunttech_UserAiContextService`.

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сервис преобразует `UserAiProfile` в ограниченный и проверяемый контекст, который будущие ИИ-сценарии смогут использовать для персонализации ответа. Он предотвращает случайную передачу конфигурации подключения, почтовых реквизитов и несвязанных данных пользователя.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Вызывается вкладкой «Обо мне» для предпросмотра. В дальнейшем подключается к `HrmAiService` перед вызовом провайдера. Источник данных — профиль текущего пользователя из `UserSessionSource`.

Web-контроллер не обращается к core Spring context напрямую. Интерфейс зарегистрирован в `modules/web/src/com/company/hunttech/web-spring.xml` как удалённый сервис `hunttech_UserAiContextService`; `WebRemoteProxyBeanCreator` создаёт локальный proxy-bean, который получает `ExtSettingsWindow` через `AppBeans.get(UserAiContextService.NAME)`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- запуск web-приложения → `WebRemoteProxyBeanCreator` регистрирует proxy `hunttech_UserAiContextService`;
- открытие `ExtSettingsWindow` → proxy получается до загрузки datasource → окно не зависит от наличия core-бина в локальном BeanFactory;
- профиль отсутствует/выключен/без согласия → пустой контекст;
- активный профиль → разрешённые поля очищаются и ограничиваются;
- обычные поля → попадают в `profileData`;
- `customAiInstructions` → попадает в отдельный список инструкций;
- предпросмотр → текст без внешнего HTTP-вызова;
- отсутствие записи в `remoteServices` → `AppBeans.get(UserAiContextService.NAME)` завершается `NoSuchBeanDefinitionException` до открытия окна.

## 1. Контракт

```java
String NAME = "hunttech_UserAiContextService";
AiUserContext buildCurrentUserContext();
AiUserContext buildContext(UserAiProfile profile);
String buildCurrentUserContextPreview();
String buildContextPreview(UserAiProfile profile);
```

`buildContext(UserAiProfile)` выделен для детерминированного тестирования и повторного использования экраном без повторной загрузки.

| Компонент | Значение |
|---|---|
| Интерфейс | `com.company.hunttech.service.UserAiContextService` |
| Реализация | `com.company.hunttech.service.UserAiContextServiceBean` |
| DTO | `com.company.hunttech.service.dto.AiUserContext` |
| Bean name | `hunttech_UserAiContextService` |
| Реестр web proxy | `modules/web/src/com/company/hunttech/web-spring.xml` |
| Entity | `hunttech_UserAiProfile` |

## 2. Источники и границы

Сервис читает только `UserAiProfile`. Он не загружает `UserAiConfiguration`, `UserSettings`, роли, замещения и почтовые реквизиты.

Профиль текущего пользователя загружается запросом:

```jpql
select e from hunttech_UserAiProfile e where e.user = :user
```

Удалённый интерфейс должен быть явно зарегистрирован в web-контексте:

```xml
<entry key="hunttech_UserAiContextService"
       value="com.company.hunttech.service.UserAiContextService"/>
```

Аннотация `@Service(UserAiContextService.NAME)` регистрирует core-реализацию, но сама по себе не создаёт proxy-bean в отдельном webapp.

## 3. Sanitization

- управляющие символы удаляются;
- пробелы и переносы нормализуются;
- ограничение выполняется по Unicode code points;
- лимиты полей соответствуют форме;
- общий лимит — 16 000 кодовых точек;
- пустые значения не сериализуются.

## 4. Данные и инструкции

`customAiInstructions` — единственное поле с семантикой инструкции. Должность, опыт, образование, цели и предпочтения остаются структурированными данными и не могут переопределить системный prompt.

## 5. Достоверность

Сервис не подтверждает фактическую истинность введённого пользователем текста; он маркирует источник как пользовательский профиль. Будущие ИИ-сервисы обязаны отличать подтверждённые данные HRM HuntTech от пользовательского контекста, выводов и рекомендаций.

## 6. Тесты

`UserAiContextServiceBeanTest` в package `com.company.hunttech.service` проверяет выключенный профиль, отсутствие согласия, разделение данных и инструкций, sanitization, Unicode-лимиты, отсутствие конфигурационных полей и пропуск пустых значений.

`ExtSettingsWindowCoreBeanLookupTest` дополнительно проверяет наличие `hunttech_UserAiContextService` в `WebRemoteProxyBeanCreator` и запрещает class-based lookup в web-контроллере.

Runtime smoke: открыть `ExtSettingsWindow`, нажать «Показать передаваемые данные», подтвердить предпросмотр либо штатный warning без `NoSuchBeanDefinitionException` и закрытия формы.

## 7. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | `hunttech_UserAiContextService` зарегистрирован в `WebRemoteProxyBeanCreator`; зафиксирован обязательный web-side proxy для предпросмотра в `ExtSettingsWindow` |
| 2026-07-22 | Контракт, bean name, DTO, JPQL и тест перенесены из контура `itpearls` в `com.company.hunttech`/`hunttech_*` без изменения поведения |
| 2026-07-22 | Созданы контракт, DTO, core-реализация, sanitization, предпросмотр и unit-тесты |
