# Task: Исправить пользовательский фон — connector URL не работает в CSS

## Проблема

После PR #76 URL стал `/hrm/connector/0/594/src/...` (с контекстом), но всё равно 301.
Vaadin 8 не обслуживает `StreamResource` через `ResourceReference` в CSS `background-image`.

## Причина

`ResourceReference.create(resource, image, "src").getURL()` генерирует URL формата
`/connector/<uiId>/<connectorId>/src/<filename>`, который Vaadin не может разрезолвить
для StreamResource. Даже с правильным контекстом `/hrm` — 301.

## Решение 1 (рекомендуемое): base64 data URI

В `HrmMainScreen.applyBackground()` — если ресурс `StreamResource`, закодировать
байты в base64 и вставить как data URI напрямую в CSS, без внешнего URL.

Замена в `HrmMainScreen.java`:

```java
// Вместо buildBackgroundUrl для StreamResource:
if (resource instanceof StreamResource) {
    // Читаем байты уже загружены в createCustomResource(),
    // но они недоступны здесь. Нужно либо:
    // 1. Сохранять байты в поле класса HrmMainScreen, либо
    // 2. Переделать MainScreenBackgroundService.createCustomResource()
    //    чтобы он возвращал и байты вместе с Resource
}
```

Вариант А: сохранить байты в `MainScreenBackgroundService.createCustomResource()` как
`byte[]` и вернуть их вместе с `Resource`, либо сделать поле в сервисе.
Затем в `HrmMainScreen.applyBackground()`:
```java
String base64 = Base64.getEncoder().encodeToString(bytes);
String css = "..." + "background-image: url('data:" + mimeType + ";base64," + base64 + "') !important;";
```

## Решение 2: передавать StreamResource через dispatch/download

```java
// В createCustomResource(): 
// Вместо StreamResource вернуть ExternalResource с dispatch URL
String dispatchUrl = "/" + AppContext.getProperty("cuba.webContextName")
    + "/dispatch/download?f=" + descriptor.getId();
return new ExternalResource(dispatchUrl);
```

Но dispatch/download может не работать (404 в логах).

## Решение 3: Зарегистрировать Vaadin RequestHandler

Создать статический `RequestHandler` в `HrmMainScreen`, который по URL
`/hrm/background-stream` сервит последние загруженные байты.

## Что нельзя трогать

- ThemeResource / системные фоны
- ExtSettingsWindow, ExtSettingsWindowMainBackground
- XML, SCSS, entity, views.xml
- MainScreenBackgroundService (кроме возврата байт вместе с Resource)

## Файлы для правки

- `modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java`
- `modules/web/src/com/company/hunttech/web/screens/mainscreen/MainScreenBackgroundService.java`
