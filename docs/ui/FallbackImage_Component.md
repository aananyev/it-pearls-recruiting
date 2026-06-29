# FallbackImage — краткая UI-заметка

> **Каноническая документация:** [components/FallbackImage.md](../components/FallbackImage.md) — архитектура, приоритет fallback, примеры XML/Java, конфигурация, история.

Компонент `fallbackImage` расширяет стандартный CUBA `image`: при привязке к `FileDescriptor` и пустом значении показывает запасное изображение из темы вместо пустого блока.

---

## Регистрация

| Артефакт | Путь |
|----------|------|
| Интерфейс (gui) | `modules/gui/src/com/hunttech/hrm/gui/components/FallbackImage.java` |
| Web-реализация | `modules/web/src/com/hunttech/hrm/web/components/WebFallbackImage.java` |
| XML-loader | `modules/web/src/com/hunttech/hrm/web/loaders/FallbackImageLoader.java` |
| Регистрация компонента | `modules/web/src/com/hunttech/hrm/web/cuba-ui-component.xml` |
| `web-app.properties` | `cuba.web.componentsConfig = +...,+com/hunttech/hrm/web/cuba-ui-component.xml` |

Имя компонента в XML: `fallbackImage` (`FallbackImage.NAME`).

---

## Глобальная конфигурация

Интерфейс `com.company.hunttech.config.HunttechImageConfig` (`@Source DATABASE`):

| Ключ | Дефолт | Назначение |
|------|--------|------------|
| `hunttech.defaultFallbackImagePath` | `images/hunttech-placeholder.svg` | Путь к theme-ресурсу по умолчанию для всех `fallbackImage` без атрибута `fallbackThemePath` |

Переопределение: **Administration → Application Properties** (хранение DATABASE). Комментарий с ключом — в `modules/core/src/com/company/itpearls/app.properties`.

---

## Использование в screen XML

```xml
<fallbackImage id="candidateFace"
               width="80px"
               height="80px"
               dataContainer="jobCandidateDc"
               property="fileImageFace"
               fallbackThemePath="icons/no-programmer.jpeg"/>
```

- `fallbackThemePath` — опционально; если не задан, берётся `hunttech.defaultFallbackImagePath` из конфигурации.
- Остальные атрибуты наследуются от `image` (`ImageLoader`).

---

## Поведение (WebFallbackImage)

1. После инициализации (`afterPropertiesSet`) загружается дефолтный путь из `HunttechImageConfig`.
2. Атрибут `fallbackThemePath` в XML переопределяет дефолт при загрузке экрана.
3. При обновлении value binding (`updateComponent`): если `valueSource.getValue() == null` и задан `fallbackResource` — на Vaadin-компонент ставится theme-ресурс; иначе — стандартная логика `WebImage`.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-29 | Добавлен cross-link на канон `docs/components/FallbackImage.md` |
| 2026-06-29 | Дефолтный путь заглушки: `images/hunttech-placeholder.svg` (фирменный SVG в `themes/hover/images/` и `themes/halo/images/`); обновлён `@DefaultString` in `HunttechImageConfig` |
| 2026-06-29 | Первоначальная реализация `FallbackImage` в пакетах `com.hunttech.hrm.*`; свойство `hunttech.defaultFallbackImagePath` в `HunttechImageConfig` |
