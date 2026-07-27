# Task: Исправить пользовательский фон — connector URL 301

## СТРОГОЕ ПРАВИЛО

Менять ТОЛЬКО `HrmMainScreen.java` (метод `registerBackgroundResource()` или `applyBackground()`).
НИЧЕГО другого не трогать:
- `MainScreenBackgroundService.java` — НЕ ТРОГАТЬ
- `ExtSettingsWindow*.java` — НЕ ТРОГАТЬ
- ThemeResource / системные фоны — НЕ ТРОГАТЬ
- XML, SCSS, entity, views.xml — НЕ ТРОГАТЬ
- ExtMainScreen, любые другие контроллеры — НЕ ТРОГАТЬ

## Проблема

`HrmMainScreen.registerBackgroundResource()` создаёт `StreamResource`, регистрирует через
`ResourceReference.create(resource, image, "src")`, получает URL вида
`/hrm/connector/0/594/src/hrm-main-background-....jpg` и вставляет его в CSS
`background-image`.

Но Vaadin 8 не обслуживает `StreamResource` через connector URL для CSS. Браузер
получает 301 redirect и фоновое изображение не загружается.

## Что сделать — один из вариантов

**Вариант A (самый простой):** после того как байты прочитаны из FileStorage в
`createCustomResource()`, передать их в `HrmMainScreen.applyBackground()` и
закодировать в base64 data URI.

Для этого можно:
1. В `MainScreenBackgroundService` добавить поле `byte[] lastCustomBackgroundBytes`
   и заполнять его в `createCustomResource()` при успешной загрузке
2. В `HrmMainScreen` после вызова `resolveForUser()` — если это StreamResource —
   получить байты из сервиса и построить data URI

ИЛИ проще: 

**Вариант B:** не использовать `ResourceReference` вообще.
В `HrmMainScreen.registerBackgroundResource()` — после того как `StreamResource`
создан, получить его URL через `StreamResource.getStreamResourceRegistration()` 
(если есть), либо заменить весь подход на `ExternalResource` с URL вида
`/hrm/dispatch/download?f={uuid}`.

dispatch/download НЕ работает для файлов, сохранённых через `fileLoader.saveStream()`
(возвращает 404). Поэтому dispatch URL не подходит.

**Вариант C (рекомендуемый):** закодировать байты в base64 в `createCustomResource()`
(в `MainScreenBackgroundService`) и передать в CSS через `data:` URI.

Порядок:
1. В `MainScreenBackgroundService.createCustomResource()` после `readAllBytes()`
   закодировать байты в base64 и вернуть их как часть `Resource`
   (например, `StreamResource` + сохранить base64 в поле сервиса)
2. В `HrmMainScreen.buildBackgroundUrl()` — для StreamResource не вызывать
   `registerBackgroundResource()`, а построить `data:image/jpeg;base64,...` URI

ИЛИ:

**Вариант D (максимально изолированный):**
Заменить в `HrmMainScreen.registerBackgroundResource()` всё тело метода на:

```java
// Пропускаем ResourceReference — он не работает для StreamResource в CSS
// Вместо этого возвращаем data URI
StreamResource sr = (StreamResource) resource;
// через StreamVariable или Registration получить байты...
// или просто вернуть путь до файла в FileStorage
```

## Проверка

После правки:
1. Собрать: `./gradlew :app-web:compileJava`
2. Тесты: `./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest'`
3. Деплой, рестарт
4. Залогиниться, загрузить фон в SettingsWindow → OK → фон должен отобразиться
5. Системные фоны (без пользовательского) должны продолжать работать
