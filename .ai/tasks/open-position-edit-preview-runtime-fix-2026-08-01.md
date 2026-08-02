# Диагностика PR #110 — runtime-ошибка открытия OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-edit-preview
PR: #110
Base: master
Verified HEAD: 6828aeb70fcc4e49362daf405ee2a1621951083d
HEAD match: PASS
Conflicts: NONE

## FAILED STEP

Открытие preview по прямому маршруту (local deploy, HTTP 200 достигнут, форма не открывается):

```
http://localhost:8080/hrm/#main/open-position-edit-preview?id=7zz913ck9nhct3qwxtm90amds2
```

(id — Crockford-код вакансии fffa4236-4d35-8b34-3bf3-baa240aa3722)

## ROOT CAUSE

Унаследованный lifecycle `OpenPositionEdit.onBeforeShow` (строка 395) → `ensurePositionLobsLoaded`
(строка 474) обращается к `getEditedEntity().getPositionType()` — lazy-связь `positionType`
не инициализирована (indirection с null Session). При открытии через URL-маршрут entity
редактора находится в detached-состоянии (CUBA сериализует/восстанавливает её при
навигации), и доступ к lazy FK `positionType` после этой сериализации бросает
EclipseLink ValidationException. В legacy-экране (открытие через ScreenBuilder из browse)
ошибки нет — там entity загружается штатным loader'ом в активной сессии.

## ERROR

```
com.vaadin.server.ServerRpcManager$RpcInvocationException: Unable to invoke method popstate in com.vaadin.shared.ui.ui.UIServerRpc
Caused by: java.lang.reflect.InvocationTargetException: null
Caused by: com.vaadin.event.ListenerMethod$MethodException: Invocation of method uriChanged in com.haulmont.cuba.web.AppUI$$Lambda failed.
Caused by: org.eclipse.persistence.exceptions.ValidationException:
Exception Description: An attempt was made to traverse a relationship using indirection that had a null Session.  This often occurs when an entity with an uninstantiated LAZY relationship is serialized and that relationship is traversed after serialization.  To avoid this issue, instantiate the LAZY relationship prior to serialization.
	at org.eclipse.persistence.exceptions.ValidationException.instantiatingValueholderWithNullSession(ValidationException.java:1026)
	at org.eclipse.persistence.internal.indirection.UnitOfWorkValueHolder.instantiate(UnitOfWorkValueHolder.java:235)
```

## STACK TRACE

```
... (RpcInvocationException, InvocationTargetException, ListenerMethod$MethodException — Vaadin RPC)
Caused by: org.eclipse.persistence.exceptions.ValidationException:
Exception Description: An attempt was made to traverse a relationship using indirection that had a null Session.
	at org.eclipse.persistence.exceptions.ValidationException.instantiatingValueholderWithNullSession(ValidationException.java:1026)
	at org.eclipse.persistence.internal.indirection.UnitOfWorkValueHolder.instantiate(UnitOfWorkValueHolder.java:235)
	at com.company.hunttech.entity.OpenPosition._persistence_get_positionType(OpenPosition.java)
	at com.company.hunttech.entity.OpenPosition.getPositionType(OpenPosition.java:584)
	at com.company.hunttech.web.screens.openposition.OpenPositionEdit.ensurePositionLobsLoaded(OpenPositionEdit.java:474)
	at com.company.hunttech.web.screens.openposition.OpenPositionEdit.onBeforeShow(OpenPositionEdit.java:395)
```

## REPRODUCTION

1. Локальный deploy ветки `agent/open-position-edit-preview` (Verified HEAD 6828aeb7).
2. Открыть в браузере: `http://localhost:8080/hrm/#main/open-position-edit-preview?id=7zz913ck9nhct3qwxtm90amds2`
3. Форма не открывается; в `deploy/tomcat/logs/catalina.out` появляется ValidationException
   (стек выше), время 13:23:44.

## EXPECTED

Форма preview открывается: sidebar (312px) + TabSheet из 12 вкладок, данные вакансии
загружены (включая `positionType`), без исключений.

## ACTUAL

`Exception in com.haulmont.cuba.web.AppUI` — ValidationException на `getPositionType()`
в `ensurePositionLobsLoaded` (унаследованный `onBeforeShow`). Форма не открывается.

## COMPLETED CHECKS

- HEAD match: PASS
- Conflicts: NONE
- Compile (`:app-web:compileJava/compileTestJava`): PASS
- OpenPositionEditPreviewLayoutTest: 8/8 PASS
- ScreenViewIntegrityTest: 8/8 PASS
- buildScssThemes: PASS
- clean assemble: PASS
- Local deploy (restart, startup 30356 ms): PASS
- HTTP /hrm/ = 200, widgetset = 200: PASS

## NOT EXECUTED

- Smoke-сценарии (12 вкладок, accordion, сохранение/отмена и т.д.) — форма не открывается,
  smoke невозможен до фикса.
- Проверка legacy-экрана по маршруту `open-position-edit?id=...` — legacy работает
  (ошибок в логе нет).

## RECOMMENDATION

Обеспечить загрузку lazy FK `positionType` до обращения в `ensurePositionLobsLoaded`
для сценария URL-маршрута. Варианты (на усмотрение разработчика):

1. В `OpenPositionEditPreview` переопределить `onBeforeShow`/`onPreviewAfterShow` так,
   чтобы `positionType` догружался через `dataManager.reload(...)` с view,
   включающим `positionType` (паттерн `loadPositionWithDescriptionLobs` уже существует
   в классе), до вызова унаследованной логики.
2. Либо в унаследованном `ensurePositionLobsLoaded` обернуть доступ в защиту
   `PersistenceHelper.isLoaded(...)` + reload в активной транзакции.
3. Либо исключить вызов проблемного `onBeforeShow` в preview (переопределение lifecycle
   с вызовом только безопасных инициализаций).

Код Hermes самостоятельно не меняет.

## РЕАЛИЗОВАННОЕ ИСПРАВЛЕНИЕ

Исправление подготовлено в preview-контроллере без изменения legacy-файлов:

- `OpenPositionEditPreview.onBeforeShow()` до вызова `super.onBeforeShow(event)` проверяет загрузку `positionType`;
- для detached editor entity связь догружается через `DataManager` узким view с описаниями позиции;
- загруженная связь устанавливается в текущий редактируемый экземпляр;
- новая сущность и уже загруженная связь не создают дополнительного запроса;
- добавлен `OpenPositionEditPreviewRouteGuardTest`;
- обновлена `OpenPositionEditPreview_Spec.md` и инструкция повторной проверки.

Точный HEAD повторной проверки указан в актуальном комментарии PR #110. Предыдущий отчёт относится только к SHA `6828aeb70fcc4e49362daf405ee2a1621951083d`.
