# PR #128 — Запрос на исправление: пункты меню без наименования

**Тип:** `error_fix_request`
**PR:** https://github.com/aananyev/it-pearls-recruiting/pull/128 (merged 2026-08-12, `2a055ba8`)
**Ветка/основание:** master `758d1146`
**Автор отчёта:** Hermes

## Проблема (P2 — визуальный дефект UI)

Три новых пункта AI-меню **не имеют caption** — в интерфейсе отображаются как служебные id `menu-config.hunttech_...browse` вместо человекочитаемых названий.

## Evidence

`modules/web/src/com/company/hunttech/web-menu.xml` (строки 107–116):

```xml
<menu id="aiAdministration" caption="mainMsg://menu_config.aiAdministration" icon="MAGIC"
      insertAfter="administration">
    <item screen="hunttech_AiFunctionConfiguration.browse" icon="MAGIC"/>   <!-- строка 109: БЕЗ caption -->
    <item screen="hunttech_AdminAiConfiguration.browse" icon="LOCK"/>        <!-- строка 110: БЕЗ caption -->
    <item screen="hunttech_UserAiFunctionOverride.browse" icon="SLIDERS"/>   <!-- строка 111: БЕЗ caption -->
    <item screen="hunttech_VacancyPromptTemplate.browse"
          caption="mainMsg://menu_config.hunttech_VacancyPromptTemplate.browse" icon="FILE_TEXT_O"/>  <!-- есть caption -->
    <item screen="hunttech_UserAiConfiguration.browse"
          caption="mainMsg://menu_config.hunttech_UserAiConfiguration.browse" icon="KEY"/>            <!-- есть caption -->
</menu>
```

В messages отсутствуют ключи для трёх новых экранов (проверено `modules/web/src/com/company/hunttech/web/messages.properties` и `messages_ru.properties`):

- `menu_config.hunttech_AiFunctionConfiguration.browse` — НЕТ
- `menu_config.hunttech_AdminAiConfiguration.browse` — НЕТ
- `menu_config.hunttech_UserAiFunctionOverride.browse` — НЕТ

(есть только `menu_config.aiAdministration`, `menu_config.hunttech_UserAiConfiguration.browse`, `menu_config.hunttech_VacancyPromptTemplate.browse` — строки 57/89/90 в `_ru`, 71/106/107 в default).

## Наблюдаемый результат (скриншот-подтверждение из CDP-smoke 2026-08-12)

В боковом меню «Управление AI» пункты рендерятся как:

```
menu-config.hunttech_AiFunctionConfiguration.browse
menu-config.hunttech_AdminAiConfiguration.browse
menu-config.hunttech_UserAiFunctionOverride.browse
```

## Требуемое исправление

1. В `web-menu.xml` добавить `caption="mainMsg://menu_config.hunttech_{Screen}.browse"` для трёх пунктов (строки 109–111).
2. Добавить ключи в `messages.properties` (default, английский) и `messages_ru.properties` (русский) — по образцу соседних (`VacancyPromptTemplate` = «Шаблоны промптов», `UserAiConfiguration` = «Мониторинг ключей пользователей»).

Предлагаемые русские названия (на усмотрение ChatGPT, но осмысленные):
- `AiFunctionConfiguration` → «AI-функции» / «Функции AI»
- `AdminAiConfiguration` → «AI-подключения» / «Корпоративные AI-ключи»
- `UserAiFunctionOverride` → «Переопределения функций» / «Личные настройки AI-функций»

## Статус

- [x] Диагностика Hermes (read-only): причина подтверждена
- [ ] Фикс от ChatGPT
- [ ] Повторная проверка Hermes (CDP-smoke: пункты меню с читаемыми подписями)
