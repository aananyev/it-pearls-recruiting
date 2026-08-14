# Task: Исправить Vaadin RPC IllegalArgumentException в IteractionListEdit

## Git контекст
- Repo: `aananyev/it-pearls-recruiting`
- Ветка: `agent/iteraction-list-accordion-reference-finish`
- HEAD: `73b34aef69bdc6421abdccf5ea51130ed31d5e0c`
- Base: `master`
- PR: #53

## Ошибка

После локального deploy ветки и открытия IteractionListEdit, при клике на элементы UI возникает:

```
java.lang.IllegalArgumentException: object is not an instance of declaring class
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at com.vaadin.server.ServerRpcManager.applyInvocation(ServerRpcManager.java:153)
	at com.vaadin.server.ServerRpcManager.applyInvocation(ServerRpcManager.java:115)
	at com.vaadin.server.communication.ServerRpcHandler.handleInvocation(ServerRpcHandler.java:442)
	at com.vaadin.server.communication.ServerRpcHandler.handleInvocations(ServerRpcHandler.java:407)
	at com.vaadin.server.communication.ServerRpcHandler.handleRpc(ServerRpcHandler.java:275)
	at com.vaadin.server.communication.UidlRequestHandler.synchronizedHandleRequest(UidlRequestHandler.java:83)
	at com.vaadin.server.SynchronizedRequestHandler.handleRequest(SynchronizedRequestHandler.java:40)
	at com.vaadin.server.VaadinService.handleRequest(VaadinService.java:1636)
	at com.vaadin.server.VaadinServlet.service(VaadinServlet.java:465)
	at com.haulmont.cuba.web.sys.CubaApplicationServlet.serviceAppRequest(CubaApplicationServlet.java:329)
	at com.haulmont.cuba.web.sys.CubaApplicationServlet.service(CubaApplicationServlet.java:215)
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:733)
```

## Что изменено в этом PR

23 файла, +1649/-128 строк. Основные изменения в `iteraction-list-edit.xml`:

1. Добавлена полноширинная карточка `mostPopularQuickActions` перед аккордеонами с 5 зелёными pill-кнопками
2. `participantsAccordion` оставлен видимым и раскрытым при открытии (`collapsed=false`, `visible=true`)
3. Изменён порядок sidebar: изображения → номер/дата → индекс разделов → карточка вакансии
4. `projectLogoImage` переведён с `Image` на `OvaFallbackImage` 80×80px
5. Добавлены SCSS-стили `iteraction-list-reference-finish.scss` во все 7 тем

Java-контроллер `IteractionListEdit.java` — **НЕ изменялся**.

## Предполагаемая причина

Vaadin `ServerRpcManager.applyInvocation` выдаёт `object is not an instance of declaring class` когда RPC-вызов от клиента не может быть применён к серверному объекту. Это может происходить если:

1. После deploy не сделан hard refresh браузера (Vaadin UIDL кеширует старые компоненты)
2. Есть несоответствие между ID компонента в XML и тем, что ожидает клиентский Vaadin state
3. Метод action'а в XML ссылается на несуществующий или переименованный метод в контроллере
4. Компонент был удалён/перемещён, но client-side Vaadin state ещё ссылается на старую иерархию

## Что требуется

1. Открыть `iteraction-list-edit.xml` и проверить все `@Action` / `invoke` / `@Subscribe` ссылки на соответствие `IteractionListEdit.java`
2. Проверить, не было ли удалено или переименовано component id, на которое есть ссылка из другого места
3. Проверить `selectAccordion` — sidebar клик вызывает этот метод с reference на аккордеон; убедиться что `participantsAccordion` (и другие секции) имеют корректный component id, совпадающий с Java-ссылками
4. Если все ссылки корректны — возможно требуется clean build + hard refresh браузера; если после hard refresh ошибка воспроизводится — искать причину глубже

## Статус Hermes
- Сборка: `BUILD SUCCESSFUL`
- Профильные тесты: 6/6 PASS (23 теста)
- ScreenViewIntegrityTest: 5/8 PASS (3 pre-existing itpearls_* fail)
- SCSS 7 тем: `BUILD SUCCESSFUL`
- Deploy: `BUILD SUCCESSFUL`
- HTTP 200: OK
- Tomcat critical errors: нет (кроме ошибки выше)
- Visual smoke: не завершён — нужна проверка ChatGPT
