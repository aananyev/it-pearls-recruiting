# Task: Исправить ошибку FileStorage для пользовательского фона при перезапуске

## Проблема

После загрузки пользовательского фона в SettingsWindow, при следующем входе/перезапуске Tomcat бросается ошибка `File not found in storage` для файла `hrm-main-background-{uuid}-....jpg`.

Корневая причина: CUBA `FileLoader.openStream()` выбрасывает `FileStorageException`, когда файл присутствует в БД (FileDescriptor), но отсутствует в локальной файловой системе (`app_home/fileStorage/`).

## Требования к исправлению

### 1. Безопасная загрузка пользовательского файла в `MainScreenBackgroundService`

Метод `createCustomResource()` должен:

```java
private Optional<Resource> createCustomResource(FileDescriptor descriptor) {
    if (descriptor == null) return Optional.empty();
    try {
        byte[] bytes = fileLoader.openStream(descriptor).readAllBytes();
        return Optional.of(byteResource(bytes, descriptor.getName(), mimeType(descriptor)));
    } catch (FileStorageException | IOException e) {
        log.warn("Cannot load custom background file id={}: {}", descriptor.getId(), e.getMessage());
        return Optional.empty();
    }
}
```

- При ошибке FileStorage — fallback на системный фон (возврат `Optional.empty()`)
- **Не удалять** FileDescriptor из БД — файл может быть доступен после восстановления хранилища

### 2. Защита `loadUserBackground()` от отсутствующего файла

Метод уже возвращает `null`, если `fileImageFace == null` или не имеет префикса. Ошибка чтения файла обрабатывается в `createCustomResource()`.

### 3. Убрать зависимость от Vaadin-connector

Пользовательский фон НЕ должен полагаться на `ResourceReference` (Vaadin connector URL). Вместо этого использовать прямую HTTP-ссылку:

```
/{contextPath}/dispatch/download?f={fileDescriptor.uuid}
```

Этот URL:
- Не зависит от сессии/connector
- Выживает при перезапуске Tomcat
- Использует CUBA `FileDownloadServlet`

### 4. Изменения в `HrmMainScreen.buildBackgroundUrl()`

Для `StreamResource` (пользовательский фон):
- Вместо `ResourceReference.create()` + `app://APP` → HTTP
- Использовать прямой URL: `hrm/dispatch/download?f={uuid}`

Контекст приложения получать через:
```java
AppContext.getProperty("cuba.webContextName")
```

### 5. Не трогать системные фоны

ThemeResource для `VAADIN/themes/{theme}/backgrounds/{n}.jpg` работает корректно, менять не нужно.

## Приоритет

P1 — блокирует использование пользовательских фонов после перезапуска.

## Файлы для изменения

- `MainScreenBackgroundService.java`: безопасный `fileLoader.openStream()` с try-catch
- `HrmMainScreen.java`: прямой URL для пользовательского фона через dispatch/download
