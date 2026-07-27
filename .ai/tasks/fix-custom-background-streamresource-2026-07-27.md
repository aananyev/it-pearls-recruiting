# Task: Исправить пользовательский фон — код отображает ExternalResource, а должен StreamResource

## Проблема

После загрузки пользовательского фона в SettingsWindow, после логина система продолжает возвращать `ExternalResource` с dispatch URL, а не `StreamResource` с байтами файла. В результате браузер не загружает изображение.

**Логи показывают:**
```
CHECKPOINT-6: resource = com.vaadin.server.ExternalResource@...
background-image: url('/hrm/dispatch/download?f=...')
```

**Должно быть:**
```
CHECKPOINT-6: resource = com.vaadin.server.StreamResource@...
background-image: url('/connector/0/.../src/hrm-main-background-...')
```

**Корень:** `dispatch/download?f=` не обслуживает файлы, сохранённые через `fileLoader.saveStream()`.

## Факты из кода

### 1. `MainScreenBackgroundService.createCustomResource()` (строка 127–140)

Текущая реализация:
```java
try (InputStream ignored = fileLoader.openStream(descriptor)) {
    return Optional.of(new ExternalResource(
            FileDescriptorImageHelper.buildDispatchDownloadUrl(descriptor)));
}
```

Этот код:
- Открывает stream для проверки существования файла (и сразу закрывает)
- Создаёт ExternalResource с dispatch URL
- **Не сохраняет байты файла** — только URL

**Правильная реализация:**
```java
try (InputStream stream = fileLoader.openStream(descriptor)) {
    byte[] bytes = stream.readAllBytes();
    StreamResource resource = new StreamResource(
            (StreamResource.StreamSource) () -> new ByteArrayInputStream(bytes),
            descriptor.getName());
    resource.setMIMEType(mimeType(descriptor));
    resource.setCacheTime(-1);
    return Optional.of(resource);
}
```

### 2. `HrmMainScreen.buildBackgroundUrl()` (строка 90–102)

Текущая реализация:
```java
if (resource instanceof ExternalResource) {
    return ((ExternalResource) resource).getURL();
}
```

**Правильная реализация:**
```java
if (resource instanceof StreamResource) {
    return registerBackgroundResource(currentUi, mainVBox.unwrap(...), resource);
}
```

Метод `registerBackgroundResource()` уже существует в классе — он:
- Создаёт `Image(0×0)` с ресурсом
- Добавляет в layout (привязка к connector tree)
- Получает URL через `ResourceReference.create().getURL()`
- Конвертирует `app://APP` → `""`

### 3. `FileDescriptorImageHelper.buildDispatchDownloadUrl()`

Использует `fileDescriptor.getUuid()`, но CUBA 7.3 может не заполнять поле `uuid` при `metadata.create()` + `dataManager.commit()`.

### 4. Тест `customBackgroundUsesDirectDispatchUrlAndFallsBackOnStorageFailure`

Должен проверять `StreamResource`, `readAllBytes()`, `registerBackgroundResource()`, а не `ExternalResource` и `dispatch download`.

## Что нужно сделать

1. Переписать `createCustomResource()` → читает байты → StreamResource
2. Переписать `buildBackgroundUrl()` → для StreamResource вызывает `registerBackgroundResource()`
3. Синхронизировать тест
4. Убедиться, что системные фоны (ThemeResource) не затронуты
5. Проверить что `readAllBytes()` не вызывает OutOfMemoryError для больших файлов (fileSizeLimit = 15МБ)

## Запрещено

- Менять `ThemeResource`/системные фоны
- Менять `ExtSettingsWindowMainBackground` (загрузка в SettingsWindow)
- Менять `HrmMainScreenIntegrationTest`
- Менять Entity, DB, datasource, FileStorageService
