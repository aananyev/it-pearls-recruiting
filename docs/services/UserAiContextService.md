# UserAiContextService и UserAiContextBuilder

> Middleware-сервис HRM HuntTech для формирования безопасного пользовательского контекста без обращения к внешнему LLM.  
> Service API: `com.company.hunttech.service.UserAiContextService`.  
> Stateless builder: `com.company.hunttech.service.UserAiContextBuilder`.  
> Spring bean: `hunttech_UserAiContextService`.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Контур преобразует `UserAiProfile` в ограниченный и проверяемый набор данных для персонализации ИИ-ответов. Он предотвращает случайную передачу конфигурации подключения, почтовых реквизитов, API-ключей и несвязанных данных пользователя.

Общие правила sanitization и лимиты должны совпадать в двух сценариях:

- фактический AI-запрос использует сохранённый профиль, загруженный middleware-сервисом;
- кнопка «Показать передаваемые данные» использует текущее состояние UI datasource, включая несохранённые изменения.

Для исключения дублирования правила вынесены в `UserAiContextBuilder` модуля `global`, доступный и core, и web.

### UI Context & Navigation

Экран `settings` открывает вкладку «Обо мне». Фактический контроллер `ExtSettingsWindowEmailNavigation` переопределяет `previewAiContext()` и вызывает `UserAiContextBuilder.buildPreview(userAiProfileDs.getItem())` локально.

Локальный preview не передаёт редактируемую CUBA entity через remoting, не выполняет HTTP-вызов к LLM и не сохраняет профиль. Middleware-сервис остаётся зарегистрированным в `web-spring.xml` для удалённых сценариев Service API и будущей интеграции с `HrmAiService`.

### Behavior Summary

- фактический AI-запрос → `UserAiContextServiceBean` загружает сохранённый профиль текущей сессии → делегирует `UserAiContextBuilder` → возвращает `AiUserContext`;
- UI-предпросмотр → контроллер получает текущий `userAiProfileDs.item` → локально вызывает тот же builder → показывает актуальные несохранённые значения;
- профиль отсутствует, выключен или без согласия → возвращается пустой контекст → preview явно сообщает, что данные не передаются;
- активный профиль → разрешённые поля очищаются и ограничиваются → обычные поля попадают в `profileData`;
- `customAiInstructions` → попадает в отдельный список инструкций;
- внешний LLM и HTTP → не вызываются при построении preview;
- runtime-ошибка UI-preview → журналируется → окно остаётся открытым и показывает warning.

## 1. Архитектура

| Компонент | Размещение | Ответственность |
|---|---|---|
| `UserAiContextService` | `modules/global` | удалённый Service API |
| `UserAiContextBuilder` | `modules/global` | stateless sanitization, ограничения, context и preview |
| `UserAiContextServiceBean` | `modules/core` | загрузка сохранённого профиля и делегирование builder |
| `AiUserContext` | `modules/global` | сериализуемый DTO данных и инструкций |
| `ExtSettingsWindowEmailNavigation` | `modules/web` | preview текущего datasource без remote entity call |

Потоки:

```text
Сохранённый профиль
→ UserAiContextServiceBean
→ UserAiContextBuilder
→ AiUserContext
```

```text
Текущий userAiProfileDs, включая несохранённые изменения
→ UserAiContextBuilder
→ текст preview
→ previewGroup / aiContextPreviewArea
```

## 2. Service API

```java
String NAME = "hunttech_UserAiContextService";
AiUserContext buildCurrentUserContext();
AiUserContext buildContext(UserAiProfile profile);
String buildCurrentUserContextPreview();
String buildContextPreview(UserAiProfile profile);
```

| Параметр | Значение |
|---|---|
| Реализация | `com.company.hunttech.service.UserAiContextServiceBean` |
| Bean name | `hunttech_UserAiContextService` |
| Реестр web proxy | `modules/web/src/com/company/hunttech/web-spring.xml` |
| Entity | `hunttech_UserAiProfile` |
| DTO | `com.company.hunttech.service.dto.AiUserContext` |

Удалённый proxy сохраняется:

```xml
<entry key="hunttech_UserAiContextService"
       value="com.company.hunttech.service.UserAiContextService"/>
```

Аннотация `@Service(UserAiContextService.NAME)` регистрирует core-реализацию, а `WebRemoteProxyBeanCreator` — proxy в отдельном webapp. Кнопка preview больше не зависит от этого proxy, но сам Service API остаётся рабочим.

## 3. Источники данных

Middleware загружает только `UserAiProfile` текущего пользователя:

```jpql
select e from hunttech_UserAiProfile e where e.user = :user
```

Сервис не загружает `UserAiConfiguration`, `UserSettings`, роли, замещения и почтовые реквизиты.

UI-preview использует `userAiProfileDs.getItem()` и поэтому отражает текущие значения полей до Save. Builder не выполняет `DataManager.commit()` и не изменяет entity.

## 4. Sanitization и лимиты

- управляющие символы удаляются;
- пробелы и переносы нормализуются;
- ограничение выполняется по Unicode code points;
- лимиты полей соответствуют форме;
- общий лимит — 16 000 кодовых точек;
- пустые значения не добавляются;
- `customAiInstructions` отделяется от профильных данных;
- SMTP/POP3/IMAP-пароли, API-ключи и конфигурации подключений не читаются.

`customAiInstructions` — единственное поле с семантикой пользовательской инструкции. Должность, опыт, образование, цели и предпочтения остаются структурированными данными и не могут переопределить системный prompt.

## 5. Достоверность

Builder и сервис не подтверждают фактическую истинность введённого пользователем текста. Источник маркируется как сведения пользователя. ИИ-сервисы обязаны отличать подтверждённые данные HRM HuntTech от пользовательского контекста, выводов и рекомендаций.

## 6. Тесты

`UserAiContextServiceBeanTest` проверяет:

- выключенный профиль и отсутствие согласия;
- разделение данных и инструкций;
- sanitization и Unicode-лимиты;
- отсутствие секретных полей;
- пропуск пустых значений;
- совпадение Service API и общего builder;
- preview текущих несохранённых значений.

`ExtSettingsWindowCoreBeanLookupTest` проверяет:

- регистрацию middleware proxy;
- запрет class-based lookup;
- XML invoke кнопки;
- override `previewAiContext()` фактического контроллера;
- локальный `UserAiContextBuilder.buildPreview(profile)`;
- раскрытие и фокус результата;
- отсутствие remote-вызова с редактируемой entity.

Runtime smoke должен включать ввод нового значения без Save, нажатие кнопки и проверку этого значения в preview.

## 7. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Общие правила контекста вынесены в stateless `UserAiContextBuilder` модуля `global`; кнопка preview использует текущий datasource локально, а core-сервис делегирует тому же builder |
| 2026-07-25 | `hunttech_UserAiContextService` зарегистрирован в `WebRemoteProxyBeanCreator`; зафиксирован обязательный web-side proxy для удалённого Service API |
| 2026-07-22 | Контракт, bean name, DTO, JPQL и тест перенесены из контура `itpearls` в `com.company.hunttech`/`hunttech_*` без изменения поведения |
| 2026-07-22 | Созданы контракт, DTO, core-реализация, sanitization, предпросмотр и unit-тесты |
