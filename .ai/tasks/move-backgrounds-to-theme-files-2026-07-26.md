# Task: Переработать фоны главного экрана — вынести из Java в темы

## Problem

Сейчас `MainScreenBackgroundService` генерирует SVG на лету через Java-код (10 SVG-генераторов + 7 палитр). Это неправильно:

1. Стандартные фоны должны быть **файлами в директориях тем**, а не Java-кодом
2. Изображения должны перекликаться с оформлением темы (цвета, стиль)
3. Пользовательский фон — отдельный файл в FileStorage, ссылка в UserSettings

## Требования

### 1. Стандартные фоны тем

Файлы размещаются в каждой теме приложения:

```
modules/web/themes/halo/backgrounds/
  1.jpg
  2.jpg
  ...
  10.jpg

modules/web/themes/havana/backgrounds/
  (аналогично)

... для всех 7 тем: halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark
```

Каждая тема содержит ровно **10 фоновых изображений**: `1.jpg … 10.jpg`.

**Важно:** ChatGPT может сгенерировать эти изображения сам (у него есть image generation). Изображения должны быть
нейтральными, деловыми, в цветах темы. Не переиспользуйте старые SVG-генераторы — нужны растровые JPG.

### 2. Алгоритм выбора фона

При логине:
1. Проверить `UserSettings.fileImageFace` — если содержит дескриптор с префиксом `hrm-main-background-` → **использовать пользовательский фон** из FileStorage
2. Иначе → выбрать случайный `{n}.jpg` из `VAADIN/themes/{activeTheme}/backgrounds/`
3. Исключить повторение предыдущего варианта (текущая логика `LAST_VARIANT_ATTRIBUTE` в сессии)

### 3. MainScreenBackgroundService

- Удалить все 10 SVG-генераторов (`circles()`, `diagonalBands()` и т.д.)
- Удалить `Palette` class и `createPalettes()`
- Метод `createGeneratedResource()` должен создавать `ThemeResource` или `FileResource` указывающий на файл `VAADIN/themes/{theme}/backgrounds/{variant}.jpg`
- Логика `resolveForUser()` — без изменений (сначала custom, потом тема)

### 4. Пользовательский фон

- Без изменений: загрузка → ImageProcessor → FileStorage → UserSettings.fileImageFace
- Сохраняется с префиксом `hrm-main-background-`
- Имеет приоритет над тематическим

### 5. HrmMainScreen

- Без изменений: CSS-инъекция работает
- URL ресурса БЕЗ `app://APP` (исправлено в последнем коммите)

### 6. Тесты

- `MainScreenBackgroundContractTest` — переписать под новую архитектуру (нет Java-генераторов)
- Проверить что каждый theme/backgrounds/ содержит ровно 10 jpg

## Приоритет

P1 — это блокирует отображение фона.
