# Task: Исправить Vaadin RPC ClassCastException в IteractionListEdit

## Контекст

Пользователь получает ошибку при открытии формы IteractionListEdit из экрана IteractionListBrowse — `IllegalArgumentException` с вложенным `ClassCastException` в Vaadin `ServerRpcManager.applyInvocation`. Ошибка происходит на ветке master (без PR #53/#54 изменений).

## Stacktrace

```
java.lang.IllegalArgumentException: java.lang.ClassCastException@6e749119
	at jdk.internal.reflect.GeneratedMethodAccessor399.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(...)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at com.vaadin.server.ServerRpcManager.applyInvocation(ServerRpcManager.java:153)
	at com.vaadin.server.ServerRpcManager.applyInvocation(ServerRpcManager.java:115)
	at com.vaadin.server.communication.ServerRpcHandler.handleInvocation(...)
	at com.vaadin.server.communication.ServerRpcHandler.handleInvocations(...)
	at com.vaadin.server.communication.ServerRpcHandler.handleRpc(...)
	at com.vaadin.server.communication.UidlRequestHandler.synchronizedHandleRequest(...)
	... Vaadin servlet chain
```

## Диагноз

Ключевое отличие от предыдущего стека: **`ClassCastException`** как cause (раньше было `object is not an instance of declaring class` без cause). Это указывает на реальную проблему типов на сервере, а не на stale browser session.

`GeneratedMethodAccessor399` — это сгенерированный reflection accessor. Vaadin `ServerRpcManager` использует reflection, чтобы вызывать setter/state change методы на компонентах. `ClassCastException` возникает когда reflection пытается привести объект к типу, который он не реализует.

## Текущее состояние кода

Ветка: `agent/entity-namespace-hunttech-guard` (PR #55), base = master (`1ebe4b26`).

Актуальный `iteraction-list-edit.xml` (строки 96–118):
```xml
<ovaFallbackImage id="candidateImage"
                  dataContainer="iteractionListDc"
                  property="candidate.fileImageFace"
                  width="112px" height="112px"
                  ovalWidth="112px" ovalHeight="112px"
                  fallbackThemePath="icons/no-programmer.jpeg"
                  scaleMode="SCALE_DOWN"/>
<image id="projectLogoImage"
       width="80px" height="80px"
       scaleMode="FILL"/>
```

## Вопросы

1. На каком экране происходит ошибка? (IteractionListEdit или другой?)
2. Что именно делает пользователь перед ошибкой? (клик, скролл, открытие формы, сохранение?)
3. Ошибка происходит на master без PR #53/#54 изменений, или после мержа этих PR?

## Гипотезы

1. **OvaFallbackImage connector mapping**: `WebOvaFallbackImage extends WebImage` использует стандартный `CubaImage` widget. Vaadin должен корректно маппить connector, но если `@Connect` аннотация отсутствует или widgetset не содержит корректной карты connector'ов — может быть ClassCastException.

2. **Ошибка не связана с PR**: ошибка была на master до PR #53 и возникает при открытии другого экрана (например, CandidateCVEdit, JobCandidateEdit, OpenPositionEdit) — там тоже используется `OvaFallbackImage`/`OvalImage`.

3. **Гонка состояний**: ClassCastException в `ServerRpcManager` может возникать при быстрой смене состояния компонента (например, клик до завершения предыдущего RPC).

## Что требуется

1. Определить точное место ошибки — на каком экране и действии
2. Исправить причину ClassCastException
3. Добавить тест, предотвращающий регрессию:
   - Вариант A: расширить `IteractionListRpcCompatibilityContractTest` — проверить что все `@Inject` поля в контроллере соответствуют типам в XML
   - Вариант B: добавить `ScreenLoadingIntegrationTest` — открыть экран в тестовом контейнере CUBA и проверить, что открытие не вызывает исключений
   - Вариант C: если ClassCastException связан с OvaFallbackImage → добавить проверку widgetset connector mapping

## Файлы для изучения

- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java`
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`
- `modules/web/src/com/hunttech/hrm/web/components/WebOvaFallbackImage.java`
- `modules/web/src/com/hunttech/hrm/web/config/HunttechUiComponentsRegistrar.java`
- `modules/web/src/com/company/hunttech/web/gui/components/WebOvalImage.java`
- `modules/web-toolkit/src/com/company/hunttech/web/toolkit/ui/AppWidgetSet.gwt.xml`
