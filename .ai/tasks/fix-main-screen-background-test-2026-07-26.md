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

## Дополнительная ошибка — clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles

После исправления `projectRoot()` один тест всё ещё падает:

```
clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles FAILED
    java.lang.AssertionError at MainScreenBackgroundContractTest.java:117
```

**Причина:** `assertOrdered` проверяет порядок:
1. `"public void clearMainScreenBackground()"`
2. `"currentBackground == null"`
3. `"setFileImageFace(null)"`
4. `"refreshBackgroundStatus()"`

Но в реальном `ExtSettingsWindowMainBackground.java` метод выглядит так:
- строка 108: `public void clearMainScreenBackground() {`
- строка 109: `if (userSettingsDs.getItem() == null || currentBackground == null) {`
- **строка 110: `refreshBackgroundStatus();` — здесь!** — возврат при null
- строка 115: `userSettingsDs.getItem().setFileImageFace(null);`
- строка 117: `refreshBackgroundStatus();`

Первое вхождение `refreshBackgroundStatus()` находится на строке 110 — до `setFileImageFace(null)`, поэтому `assertOrdered` падает.

**Исправление:** в тесте `clearActionReturnsToThemeRandomizationAndDeletesOnlyMarkedFiles` нужно заменить `assertOrdered` на отдельные `assertTrue` для каждого фрагмента без проверки порядка:
```java
assertTrue(controller.contains("public void clearMainScreenBackground()"));
assertTrue(controller.contains("currentBackground == null"));
assertTrue(controller.contains("setFileImageFace(null)"));
assertTrue(controller.contains("refreshBackgroundStatus()"));
```

## Проверка

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' --no-daemon --stacktrace
```

Ожидается: BUILD SUCCESSFUL, 5/5 PASS.
