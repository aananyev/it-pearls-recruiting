# Task: Исправить отсутствие фона на главном экране — HrmMainScreen не резолвится

## Контекст

PR #56 (feat) и #57 (fix) добавили `HrmMainScreen` с персональным фоном. Код, тесты и сборка проходят. Но фон **не отображается**.

## Диагноз

### Проблема 1: HrmMainScreen не зарегистрирован в web-screens.xml

`modules/web/src/com/company/hunttech/web-screens.xml`:
```xml
<screen id="sec$User.browse" template="..."/>
<screen id="sec$User.lookup" template="..."/>
<screen id="sec$User.edit" template="..."/>
<screen id="settings" template="...ext-settings-window-main-background.xml"/>
```
**Нет** записи для `hrmMainScreen`.

А в `web-app.properties`:
```
cuba.web.mainScreenId=hrmMainScreen
```

В CUBA 7.3 main screen может не резолвиться по `@UiController` без явной регистрации в `web-screens.xml`. Нужно добавить:
```xml
<screen id="hrmMainScreen" template="/com/company/hunttech/web/screens/mainscreen/hrm-main-screen.xml"/>
```

### Проблема 2 (потенциальная): Старый `extMainScreen` конфликтует с `hrmMainScreen`

`com/company/hunttech/web-app.properties` (в корне проекта):
```
cuba.web.mainScreenId=extMainScreen
```
Этот файл — app-component конфиг. Убедиться что он НЕ переопределяет `modules/web/src/com/company/hunttech/web-app.properties` при деплое.

### Проблема 3 (потенциальная): CUBA может не находить `@UiController("hrmMainScreen")` как main screen

В CUBA 7.3 `@UiController` для main screen может работать не через стандартный screen resolver. Нужно проверить механизм: если `cuba.web.mainScreenId` не находит экран, CUBA молча использует дефолтный main screen.

## Что требуется сделать

1. Добавить регистрацию `hrmMainScreen` в `web-screens.xml`
2. Проверить что `com/company/hunttech/web-app.properties` не переопределяет `mainScreenId`
3. Убедиться что `HrmMainScreen` импортирует `mainVBox` из родительского `ext-main-screen.xml`
4. Проверить что `backgroundResourceHolder` (Vaadin Image) корректно добавляется в layout

## Проверка

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
APP_CONTEXT=hrm ./scripts/rebuild-widgetset-and-start.sh
```

После deploy — открыть `http://localhost:8080/hrm/?restartApplication` в новом инкогнито. Фон должен отображаться.
