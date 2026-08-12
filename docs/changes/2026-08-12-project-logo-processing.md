# 2026-08-12 — Автоматическая обработка логотипа проекта при загрузке

## Компоненты

- `Project` (сущность) — **структура не менялась**; меняется только содержимое файла `projectLogo` при загрузке.
- `ProjectEdit` (экран) — **XML и Java-логика не менялись**; обработка встроена в кастомный загрузчик.

## Назначение

Логотип проекта приводится к единому виду перед записью в файловое хранилище:
любой растровый формат → PNG, ресайз до 300×300, удаление белого фона,
вписывание в круг (чтобы в круглом аватаре `ovaFallbackImage` не было обрезки по углам).

## Причина

Логотипы грузились «как есть»: белый фон и неквадратные пропорции выглядели
неаккуратно в круглых аватарах; большие файлы занимали лишнее место в хранилище.

## Прежнее vs новое поведение

| Аспект | Прежнее | Новое |
|--------|---------|-------|
| Формат | как загружено (jpg/png/...) | PNG |
| Размер | без ограничений | ≤ 300×300 (пропорционально) |
| Белый фон | сохранялся | удаляется (flood-fill от краёв, порог 235) |
| Обрезка в круглом аватаре | возможна по углам | исключена (канвас ≥ диагонали логотипа) |
| Не-изображение | сохранялось | сохраняется без изменений (fallback) |

## Изменённые файлы

- `modules/global/src/com/company/hunttech/config/HunttechProjectLogoConfig.java` — новый конфиг (`hunttech.projectLogo.*`).
- `modules/global/src/com/company/hunttech/app/ProjectLogoImageProcessingService.java` — новый интерфейс сервиса.
- `modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java` — реализация (ImageIO, flood-fill, вписывание в круг).
- `modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java` — кастомный загрузчик (наследник `WebFileUploadField`).
- `modules/web/src/com/hunttech/hrm/web/cuba-ui-component.xml` — регистрация компонента под именем `upload`.
- `modules/core/test/com/company/hunttech/hunttech/core/ProjectLogoImageProcessingServiceBeanTest.java` — тесты (5 шт.).
- `docs/entities/project/Project.md`, `docs/screens/project/hunttech_Project.edit_Spec.md` — документация.

## Ключевые решения

- Загрузчик зарегистрирован под стандартным именем `upload` → заменяет `WebFileUploadField`
  во всех экранах **без правки их XML**. Обработка включается только для полей,
  привязанных к свойству `projectLogo`; остальные загрузки работают как раньше.
- Точка перехвата — `saveFile()` в режиме `IMMEDIATE`: файл уже во временном хранилище,
  но ещё не записан в `FileStorage`; обработанные байты перезаписывают временный файл,
  дескриптор получает расширение `png` и актуальный размер.
- Любая ошибка обработки → сохраняется исходный файл (загрузка не ломается).

## Проверка и откат

- Тест: `./gradlew :app-core:test --tests '*ProjectLogoImageProcessingServiceBeanTest'`.
- Ручная проверка: ProjectEdit → загрузка логотипа (jpg/png) → сохранение → просмотр в browse/аватаре.
- Откат: удалить `<component name="upload">` из `cuba-ui-component.xml` и перезапустить (`./gradlew restart`).

## Ограничения

- Удаляется белый фон, **соединённый с краями** изображения; белые элементы внутри логотипа сохраняются.
- Изображения с прозрачностью (PNG) обрабатываются так же; существующая прозрачность сохраняется.
- Порог белого, размер и флаг включения настраиваются в `hunttech.projectLogo.*`.
