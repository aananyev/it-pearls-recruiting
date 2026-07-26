# Task: Исправить MainScreenBackgroundContractTest — NoSuchFileException

## Контекст

PR #56: feat(main-screen): добавить персональные фоновые изображения
Ветка: agent/main-screen-personal-backgrounds
HEAD: a10d267af18a3e65ff4bff93c0176b0257d5084f

## Ошибка

Все 5 тестов `MainScreenBackgroundContractTest` падают с `NoSuchFileException`:

```
com.company.hunttech.core.MainScreenBackgroundContractTest
  > generatedCatalogContainsTenVariantsForAllSevenThemes FAILED
    java.nio.file.NoSuchFileException at MainScreenBackgroundContractTest.java:96

  > mainScreenExtensionPreservesExtMainScreenBusinessLogic FAILED
    java.nio.file.NoSuchFileException at MainScreenBackgroundContractTest.java:96

  > customBackgroundHasPriorityOverRandomThemeCatalog FAILED
    java.nio.file.NoSuchFileException at MainScreenBackgroundContractTest.java:96

  > settingsExtensionUsesExistingUserSettingsFileWithoutEntityOrDatabaseChange FAILED
    java.nio.file.NoSuchFileException at MainScreenBackgroundContractTest.java:96

  > clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles FAILED
    java.nio.file.NoSuchFileException at MainScreenBackgroundContractTest.java:96
```

## Причина

Метод `source()` (строка 94-97) использует прямой `Paths.get(relativePath)`:

```java
private String source(String relativePath) throws IOException {
    Path path = Paths.get(relativePath);
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
}
```

Во время `./gradlew :app-core:test` текущая директория может не совпадать с корнем проекта, поэтому относительный путь не резолвится.

## Исправление

В проекте есть готовый паттерн — `IteractionListRpcCompatibilityContractTest.projectRoot()` (на ветке `agent/iteraction-list-rpc-session-fix`):

```java
private Path projectRoot() {
    Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
    while (root != null && !Files.exists(root.resolve("build.gradle"))) {
        root = root.getParent();
    }
    assertNotNull("Не найден корень проекта HRM HuntTech", root);
    return root;
}
```

Нужно:
1. Добавить метод `projectRoot()` в `MainScreenBackgroundContractTest`
2. Исправить `source()`:
   ```java
   private String source(String relativePath) throws IOException {
       Path path = projectRoot().resolve(relativePath);
       return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
   }
   ```

## Проверка

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' --no-daemon --stacktrace
```

Ожидается: BUILD SUCCESSFUL, 5/5 PASS.
