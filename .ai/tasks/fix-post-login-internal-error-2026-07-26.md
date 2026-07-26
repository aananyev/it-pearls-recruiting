# Task: Исправить InternalError после логина (связано с логотипом)

## Проблема

После фикса логотипа (`f24bc12b`) пользователь сообщает об InternalError после логина.

В логах сервера (catalina.out) видно:
- HrmMainScreen загружается корректно (CHECKPOINT 0-17 проходят)
- Фон применяется
- HTTP 200

Возможная причина: `InternalError` — это Vaadin-клиентская ошибка, которая не всегда попадает в server-логи. Может быть вызвана проблемами в:
1. `ExtMainScreen.initLogoImage()` — после моих изменений
2. Дублирующимися импортами `UiController`/`UiDescriptor` в `ExtMainScreen.java`
3. Конфликтом `CandidateCVEdit` (PR #67) с существующим кодом

## Требования

### 1. Очистить дублирующиеся импорты в ExtMainScreen.java

В файле `ExtMainScreen.java` после моего коммита есть дубликаты:
```java
import com.haulmont.cuba.gui.screen.UiController;    // строка 24 (оригинал)
import com.haulmont.cuba.gui.screen.UiDescriptor;     // строка 25 (оригинал)
...
import com.haulmont.cuba.gui.screen.UiController;    // строка 36 (дубликат)
import com.haulmont.cuba.gui.screen.UiDescriptor;     // строка 37 (дубликат)
```

Удалить дубликаты (строки 36-37).

### 2. Проверить initLogoImage()

Метод должен работать с моим try-catch:
```java
try {
    logoImage.setSource(FileDescriptorResource.class)
            .setFileDescriptor(fileDescriptor);
    return;
} catch (RuntimeException e) {
    log.warn("Cannot set company logo...");
}
logoImage.setSource(ThemeResource.class)...
```

Проблема: если `fileDescriptor` не null, но `logoImage` не инициализирован → `NullPointerException`.

Добавить проверку: `if (logoImage == null) return;` перед всеми операциями.

### 3. Проверить, что InternalError не из CandidateCV

PR #67 добавил 168 строк в `CandidateCVEdit.java`. Проверить, что нет конфликта с `@Inject` полями, OvaFallbackImage или datasource binding.

### 4. Верификация

- `./gradlew :app-web:compileJava` — BUILD SUCCESSFUL
- `./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest'`
- `./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest'`

## Приоритет

P1 — блокирует вход пользователя.
