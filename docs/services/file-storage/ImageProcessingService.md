# ImageProcessingService (`hunttech_ImageProcessingService`)

> Серверная обработка загружаемых изображений профиля в HRM HuntTech: сжатие и уменьшение размеров перед сохранением в файловое хранилище CUBA.

**Связанные документы:** [ExtUser](../../entities/ext-user/ExtUser.md) (слоты фото) · UI: [ExtUserEdit](../../screens/ext-user/hunttech_ExtUserEdit_Spec.md) · [ExtSettingsWindow](../../ui/ExtSettingsWindow_Spec.md)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Рекрутёры и HR загружают фотографии профиля через админский экран пользователя и через личные настройки. Без нормализации крупные файлы увеличивают объём хранилища, замедляют отрисовку аватаров в списках вакансий, комментариях и виджете «Моё фото». **ImageProcessingService** централизует правила: если изображение уже укладывается в лимит по пикселям — возвращается как есть; иначе — масштабируется и перекодируется в целевой формат (по умолчанию PNG).

Обработка должна оставаться в middleware: она использует серверную конфигурацию, `ImageIO` и Thumbnailator. Перенос реализации в web-контроллер создал бы дублирование правил, зависимость web-модуля от библиотек обработки изображений и расхождение между административной и пользовательской загрузкой фотографий.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Точка вызова | Роль |
|--------------|------|
| `ExtUserEdit` | Администратор загружает `officialPhoto`; сервис инжектируется как зарегистрированный web-side middleware proxy |
| `ExtSettingsWindow` | Пользователь загружает `userAvatar`; сервис получается через `AppBeans.get(ImageProcessingService.NAME)` |
| `AvatarImageUploadHelper` | Общий web-слой: читает байты из `FileLoader`, вызывает сервис, при `processed=true` обновляет `FileDescriptor` и перезаписывает файл в хранилище |
| `web-spring.xml` | Регистрирует интерфейс сервиса в `WebRemoteProxyBeanCreator`, чтобы web Spring context содержал proxy-bean `hunttech_ImageProcessingService` |

Конфигурация лимитов доступна в UI через `HunttechImageConfig` (подсказки в upload-компонентах).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Запуск web-приложения** → `WebRemoteProxyBeanCreator` читает `remoteServices` → создаёт локальный proxy-bean `hunttech_ImageProcessingService`, направленный в middleware.
- **Открытие `ExtSettingsWindow`** → контроллер получает зарегистрированный proxy через `AppBeans.get(ImageProcessingService.NAME)` → окно открывается без доступа к core Spring context.
- **Загрузка файла** → web-слой читает байты → удалённо вызывает `process(data, fileName)` → если `processed=false`, дескриптор не меняется → если `processed=true`, обновляются `extension`, `size` и содержимое в `FileStorageService`.
- **Пустые данные** → `DevelopmentException` («Empty image data»).
- **Нераспознанный формат** (байты не читаются `ImageIO`) → исходные байты и имя/расширение без изменений, `processed=false`.
- **Превышение лимита по ширине/высоте** → Thumbnailator уменьшает до `targetImageSize` по большей стороне, выход в `targetImageFormat`; расширение нормализуется (`jpeg` → `jpg`).
- **Ошибка обработки** → `AvatarImageUploadHelper` журналирует warning и возвращает исходный дескриптор → загрузка пользователя не завершается жёсткой ошибкой.
- **Отсутствие записи в `remoteServices`** → proxy-bean не создаётся → `AppBeans.get(ImageProcessingService.NAME)` и `@Inject ImageProcessingService` завершаются `NoSuchBeanDefinitionException`.

---

## 1. Архитектура и размещение

| Элемент | Путь |
|---------|------|
| Интерфейс Service API | `modules/global/src/com/company/hunttech/app/ImageProcessingService.java` |
| Реализация middleware | `modules/core/src/com/company/hunttech/app/ImageProcessingServiceBean.java` |
| DTO результата | `modules/global/src/com/company/hunttech/app/ProcessedImage.java` |
| Конфигурация | `modules/global/src/com/company/hunttech/config/HunttechImageConfig.java` |
| Реестр web proxy | `modules/web/src/com/company/hunttech/web-spring.xml` |
| CUBA service name | `hunttech_ImageProcessingService` |

Зависимости реализации: **Thumbnailator** (`net.coobird:thumbnailator`), **Apache Commons Lang**, CUBA `Configuration`.

Spring component-scan в `modules/core/src/com/company/hunttech/spring.xml` включает пакет `com.company.hunttech`. Реализация зарегистрирована как `@Service(ImageProcessingService.NAME)`.

### 1.1. Граница web/core

`ImageProcessingService` является CUBA middleware Service API, а не локальным web-бином. Интерфейс и возвращаемый `ProcessedImage` размещены в `global`; `ProcessedImage` реализует `Serializable`, поэтому результат может передаваться через CUBA service proxy.

В web-контроллере применяется именованный lookup:

```java
imageProcessingService = (ImageProcessingService) AppBeans.get(ImageProcessingService.NAME);
```

Однако стабильного `NAME` и аннотации core-реализации недостаточно. Web-приложение должно зарегистрировать интерфейс в `WebRemoteProxyBeanCreator`:

```xml
<entry key="hunttech_ImageProcessingService"
       value="com.company.hunttech.app.ImageProcessingService"/>
```

После создания proxy-bean допустимы как именованный `AppBeans.get(NAME)`, так и штатная инъекция интерфейса в других web-экранах. Без этой записи `AppBeans` обращается к локальному web BeanFactory и получает `NoSuchBeanDefinitionException`.

Class-based lookup запрещён:

```java
AppBeans.get(ImageProcessingService.class); // не создаёт middleware proxy автоматически
```

Причина: Java-тип интерфейса доступен web-модулю, но фактический `ImageProcessingServiceBean` живёт в Spring-контексте отдельного middleware webapp. `WebRemoteProxyBeanCreator` создаёт локальное представление удалённого Service API только для явно перечисленных `remoteServices`.

---

## 2. Конфигурация (`HunttechImageConfig`)

Источник: `@Source(type = SourceType.DATABASE)` — значения в таблице настроек CUBA (`SYS_CONFIG`), ключи:

| Свойство | Ключ в БД | Тип | По умолчанию | Смысл |
|----------|-----------|-----|--------------|-------|
| `targetImageSize` | `hunttech.image.resize.size` | int | **1024** | Максимальная ширина и высота (px); при превышении любой стороны — масштабирование |
| `targetImageFormat` | `hunttech.image.resize.format` | String | **png** | Формат выходного файла для Thumbnailator (`jpeg`, `png`, …) |
| `defaultFallbackImagePath` | `hunttech.defaultFallbackImagePath` | String | **images/hunttech-placeholder.svg** | Theme-путь для UI-компонента [FallbackImage](../../screens/components/FallbackImage_Component.md) |

Изменение лимитов — через администрирование CUBA (Database Storage) без пересборки кода.

### Миграция ключей (legacy → hunttech)

| Legacy (`HuntTech.image.*`) | Новый ключ (`hunttech.image.*`) | Примечание |
|-----------------------------|----------------------------------|------------|
| `HuntTech.image.maxPixels` | `hunttech.image.resize.size` | Единый лимит по пикселям; дефолт 1024 (было 800) |
| `HuntTech.image.maxSizeKb` | — | Удалён; обработка только по размеру в пикселях |
| `HuntTech.image.targetFormat` | `hunttech.image.resize.format` | Дефолт `png` (было `jpeg`) |

---

## 3. API сервиса

```java
String NAME = "hunttech_ImageProcessingService";
ProcessedImage process(byte[] data, String fileName);
```

### `ProcessedImage`

| Поле | Тип | Описание |
|------|-----|----------|
| `data` | `byte[]` | Итоговое содержимое файла |
| `name` | `String` | Имя без расширения (из `fileName`) |
| `extension` | `String` | Расширение без точки; при обработке JPEG — `jpg` |
| `processed` | `boolean` | `true` — байты перекодированы/уменьшены; `false` — возврат оригинала |

DTO реализует `Serializable`; это обязательная часть удалённого контракта между `hrm` и `hrm-core`.

---

## 4. Правила обработки

1. `data == null` или `length == 0` → `DevelopmentException("Empty image data")`.
2. `ImageIO.read(data)` вернул `null` → `ProcessedImage(исходные байты, name, extension, false)`.
3. Если `width <= targetImageSize` **и** `height <= targetImageSize` → оригинал, `processed=false`.
4. Иначе: `Thumbnails.of(...).size(targetImageSize, targetImageSize).outputFormat(targetImageFormat)` → новые байты, `processed=true`, расширение из `normalizeFormatExtension(targetImageFormat)`.
5. Ошибка IO при обработке → `DevelopmentException` с сообщением и cause.

Имя файла: `extractName` / `extractExtension` — разбор по последней точке; пустое имя → `"image"`.

---

## 5. Интеграционные точки (web)

### `AvatarImageUploadHelper.processUploadedImage`

Цепочка: `FileLoader.openStream` → `imageProcessingService.process` через CUBA middleware proxy → при `isProcessed()` — `descriptor.setExtension`, `setSize`, `fileStorageService.saveFile`, `dataManager.commit`.

При ошибках хранилища или обработки — лог `warn`, возврат исходного дескриптора (загрузка не прерывается жёстко).

### Экраны

- **ExtUserEdit** — обработка upload `officialPhoto`; зарегистрированный proxy позволяет сохранить существующую `@Inject ImageProcessingService`.
- **ExtSettingsWindow** — upload `userAvatar` на `extUserDs`; сервис разрешается по `ImageProcessingService.NAME`, затем передаётся в тот же helper.

---

## 6. Тестирование

| Файл | Назначение |
|------|------------|
| `modules/core/test/com/company/hunttech/app/ImageProcessingServiceBeanTest.java` | правила изменения размеров, формата и возврата оригинала |
| `modules/core/test/com/company/hunttech/core/ExtSettingsWindowCoreBeanLookupTest.java` | именованный lookup, обязательная запись в `WebRemoteProxyBeanCreator`, запрет class-based lookup, сериализуемость DTO |

Запуск:

```bash
./gradlew :app-global:compileJava \
          :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests '*ImageProcessingServiceBeanTest*' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowCoreBeanLookupTest' \
          --no-daemon --stacktrace
```

Ожидается: `ExtSettingsWindowCoreBeanLookupTest` — 3/3 PASS.

Runtime smoke выполняется на одном точном SHA: открыть `ExtSettingsWindow`, загрузить изображение крупнее `targetImageSize`, подтвердить успешную обработку, сохранение и повторное отображение без `NoSuchBeanDefinitionException` и cross-context ошибок.

---

## 7. Инструкция по развертыванию

- Код входит в артефакты `app-global`, `app-core` и web-клиент; отдельной миграции БД для сервиса нет.
- Web-артефакт обязан содержать обе записи `hunttech_ImageProcessingService` и `hunttech_UserAiContextService` в `WebRemoteProxyBeanCreator`.
- При первом использовании при необходимости задать ключи `hunttech.image.resize.*` в **Administration → Application Properties** (Database storage); иначе действуют дефолты из `@DefaultInt(1024)` / `@DefaultString("png")`.
- Thumbnailator остаётся серверной зависимостью обработки и не переносится в UI-контроллер.
- После сборки требуется локальный deploy точного HEAD, перезапуск Tomcat, HTTP `/hrm/` = 200 и обязательный smoke открытия `ExtSettingsWindow` до проверки аватара.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-25 | Исправлена регистрация remoting: `hunttech_ImageProcessingService` и `hunttech_UserAiContextService` добавлены в `WebRemoteProxyBeanCreator`; устранена причина `NoSuchBeanDefinitionException` при открытии `ExtSettingsWindow` |
| 2026-07-25 | `ExtSettingsWindow` переведён с class-based `AppBeans` lookup на именованный CUBA service proxy `ImageProcessingService.NAME`; закреплены граница web/core и сериализуемый удалённый контракт |
| 2026-06-29 | Дефолт `defaultFallbackImagePath`: `images/hunttech-placeholder.svg` (фирменный SVG в темах hover/halo) |
| 2026-06-29 | Рефакторинг в пакет `com.company.hunttech`: `HunttechImageConfig` (`hunttech.image.resize.size` / `format`), bean `hunttech_ImageProcessingService`; удалён лимит по KB; дефолты 1024 px и PNG; component-scan `com.company.hunttech` |
| 2026-06-29 | Сервис обработки изображений: `ImageProcessingServiceBean`, `ImageProcessingConfig`, `ProcessedImage`; интеграция в `ExtUserEdit`, `ExtSettingsWindow`, `AvatarImageUploadHelper`; unit-тесты `ImageProcessingServiceBeanTest` |
