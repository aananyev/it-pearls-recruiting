# ImageProcessingService (`hunttech_ImageProcessingService`)

> Серверная обработка загружаемых изображений профиля в HRM HuntTech: сжатие и уменьшение размеров перед сохранением в файловое хранилище CUBA.

**Связанные документы:** [ExtUser](../entities/ExtUser.md) (слоты фото) · UI: [ExtUserEdit](../ui/itpearls_ExtUserEdit_Spec.md) · [ExtSettingsWindow](../ui/ExtSettingsWindow_Spec.md) *(при наличии)*

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Рекрутёры и HR загружают фотографии профиля через админский экран пользователя и через личные настройки. Без нормализации крупные файлы увеличивают объём хранилища, замедляют отрисовку аватаров в списках вакансий, комментариях и виджете «Моё фото». **ImageProcessingService** централизует правила: если изображение уже укладывается в лимит по пикселям — возвращается как есть; иначе — масштабируется и перекодируется в целевой формат (по умолчанию PNG).

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Точка вызова | Роль |
|--------------|------|
| `ExtUserEdit` | Админ загружает `officialPhoto`; лимиты показываются в подсказке upload |
| `ExtSettingsWindow` | Пользователь загружает `userAvatar` |
| `AvatarImageUploadHelper` | Общий web-слой: читает байты из `FileLoader`, вызывает сервис, при `processed=true` обновляет `FileDescriptor` и перезаписывает файл в хранилище |

Конфигурация лимитов доступна в UI через `HunttechImageConfig` (подсказки в upload-компонентах).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Загрузка файла** → web-слой читает байты → `process(data, fileName)` → если `processed=false`, дескриптор не меняется → если `processed=true`, обновляются `extension`, `size` и содержимое в `FileStorageService`.
- **Пустые данные** → `DevelopmentException` («Empty image data»).
- **Нераспознанный формат** (байты не читаются `ImageIO`) → исходные байты и имя/расширение без изменений, `processed=false`.
- **Превышение лимита по ширине/высоте** → Thumbnailator уменьшает до `targetImageSize` по большей стороне, выход в `targetImageFormat`; расширение нормализуется (`jpeg` → `jpg`).

---

## 1. Архитектура и размещение

| Элемент | Путь |
|---------|------|
| Интерфейс | `modules/global/src/com/company/hunttech/app/ImageProcessingService.java` |
| Реализация | `modules/core/src/com/company/hunttech/app/ImageProcessingServiceBean.java` |
| DTO результата | `modules/global/src/com/company/hunttech/app/ProcessedImage.java` |
| Конфигурация | `modules/global/src/com/company/hunttech/config/HunttechImageConfig.java` |
| CUBA bean name | `hunttech_ImageProcessingService` |

Зависимости: **Thumbnailator** (`net.coobird:thumbnailator`), **Apache Commons Lang**, CUBA `Configuration`.

Spring component-scan в `modules/core/src/com/company/itpearls/spring.xml` включает пакет `com.company.hunttech`.

---

## 2. Конфигурация (`HunttechImageConfig`)

Источник: `@Source(type = SourceType.DATABASE)` — значения в таблице настроек CUBA (`SYS_CONFIG`), ключи:

| Свойство | Ключ в БД | Тип | По умолчанию | Смысл |
|----------|-----------|-----|--------------|-------|
| `targetImageSize` | `hunttech.image.resize.size` | int | **1024** | Максимальная ширина и высота (px); при превышении любой стороны — масштабирование |
| `targetImageFormat` | `hunttech.image.resize.format` | String | **png** | Формат выходного файла для Thumbnailator (`jpeg`, `png`, …) |
| `defaultFallbackImagePath` | `hunttech.defaultFallbackImagePath` | String | **images/hunttech-placeholder.svg** | Theme-путь для UI-компонента [FallbackImage](../ui/FallbackImage_Component.md) |

Изменение лимитов — через администрирование CUBA (Database Storage) без пересборки кода.

### Миграция ключей (legacy → hunttech)

| Legacy (`itpearls.image.*`) | Новый ключ (`hunttech.image.*`) | Примечание |
|-----------------------------|----------------------------------|------------|
| `itpearls.image.maxPixels` | `hunttech.image.resize.size` | Единый лимит по пикселям; дефолт 1024 (было 800) |
| `itpearls.image.maxSizeKb` | — | Удалён; обработка только по размеру в пикселях |
| `itpearls.image.targetFormat` | `hunttech.image.resize.format` | Дефолт `png` (было `jpeg`) |

---

## 3. API сервиса

```java
ProcessedImage process(byte[] data, String fileName);
```

### `ProcessedImage`

| Поле | Тип | Описание |
|------|-----|----------|
| `data` | `byte[]` | Итоговое содержимое файла |
| `name` | `String` | Имя без расширения (из `fileName`) |
| `extension` | `String` | Расширение без точки; при обработке JPEG — `jpg` |
| `processed` | `boolean` | `true` — байты перекодированы/уменьшены; `false` — возврат оригинала |

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

Цепочка: `FileLoader.openStream` → `imageProcessingService.process` → при `isProcessed()` — `descriptor.setExtension`, `setSize`, `fileStorageService.saveFile`, `dataManager.commit`.

При ошибках хранилища или обработки — лог `warn`, возврат исходного дескриптора (загрузка не прерывается жёстко).

### Экраны

- **ExtUserEdit** — `@Install` на upload `officialPhoto`; подсказка с `targetImageSize` и `targetImageFormat` из `HunttechImageConfig`.
- **ExtSettingsWindow** — upload `userAvatar` на `extUserDs`; тот же helper и конфиг.

---

## 6. Тестирование

| Файл | Модуль |
|------|--------|
| `modules/core/test/com/company/hunttech/app/ImageProcessingServiceBeanTest.java` | `app-core` |

Unit-тесты: stub `HunttechImageConfig` через mock `Configuration` (reflection inject). Изображения генерируются программно (`BufferedImage` + `ImageIO`).

Запуск:

```bash
export JAVA_HOME=/path/to/jdk11
./gradlew :app-core:test --tests "*ImageProcessing*" --no-daemon
```

---

## 7. Инструкция по развертыванию

- Код входит в артефакты `app-global` и `app-core`; отдельной миграции БД для сервиса нет.
- При первом использовании в проде при необходимости задать ключи `hunttech.image.resize.*` в **Administration → Application Properties** (Database storage); иначе действуют дефолты из `@DefaultInt(1024)` / `@DefaultString("png")`.
- Thumbnailator уже объявлен в `build.gradle` модуля `global`.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-29 | Дефолт `defaultFallbackImagePath`: `images/hunttech-placeholder.svg` (фирменный SVG в темах hover/halo) |
| 2026-06-29 | Рефакторинг в пакет `com.company.hunttech`: `HunttechImageConfig` (`hunttech.image.resize.size` / `format`), bean `hunttech_ImageProcessingService`; удалён лимит по KB; дефолты 1024 px и PNG; component-scan `com.company.hunttech` |
| 2026-06-29 | Сервис обработки изображений: `ImageProcessingServiceBean`, `ImageProcessingConfig`, `ProcessedImage`; интеграция в `ExtUserEdit`, `ExtSettingsWindow`, `AvatarImageUploadHelper`; unit-тесты `ImageProcessingServiceBeanTest` |
