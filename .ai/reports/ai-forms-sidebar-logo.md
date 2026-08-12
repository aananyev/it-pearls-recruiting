# AI-формы: логотип по умолчанию в sidebar (ovaFallbackImage)

**Тип:** `build_report` (реализованное изменение, статус: в рабочем дереве, не закоммичено)
**Дата:** 2026-08-12
**Автор:** Hermes

## Что сделано

В identity-блок sidebar 5 AI-форм добавлен `<ovaFallbackImage>` 96×96 (круглый аватар:
`ovalWidth="96px" ovalHeight="96px"`, `align="MIDDLE_CENTER"`, `scaleMode="SCALE_DOWN"`)
с `fallbackThemePath="icons/hunttech-logo.png"` — изображение по умолчанию = круглый
логотип компании HuntTech. Без привязки к данным (dataContainer/property нет).

Формы и id элементов:

1. `adminaiconfiguration/admin-ai-configuration-edit.xml` → `adminAiConfigurationLogoImage`
2. `aifunctionconfiguration/ai-function-configuration-edit.xml` → `aiFunctionConfigurationLogoImage`
3. `useraiconfiguration/user-ai-configuration-edit.xml` → `userAiConfigurationLogoImage`
4. `useraifunctionoverride/user-ai-function-override-edit.xml` → `userAiFunctionOverrideLogoImage`
5. `vacancyprompttemplate/vacancy-prompt-template-edit.xml` → `vacancyPromptTemplateLogoImage`

## Ассет

`modules/web/images/hunttech-logo.png` (379×379, круглый, alpha) скопирован в `icons/`
всех 7 тем: halo, havana, helium, hover, hunttech-modern, hunttech-modern-dark,
hunttech-modern-light (идентичные хэши).

## Верификация

- XML well-formed: python xml.etree parse — 5/5 OK.
- Атрибуты поддержаны лоадером: `OvaFallbackImageLoader` (ovalWidth/ovalHeight/fallbackThemePath).
- Контракт-тесты не затронуты: `AiControlPlaneScreenContractTest` (edit-sidebar/label-nav/apiKey —
  без изменений), `VacancyPromptTemplateEditContractTest` (ассерты не про identity).
- Контроллеры форм: ссылок на identity/sidebar нет — правка безопасна.

## Просьба к параллельной сессии

В этих же 5 XML лежат незакоммиченные правки AI-форм. Не откатывать блоки
`ovaFallbackImage id="*LogoImage"` при дальнейшей полировке. Коммит — после
завершения текущего раунда AI-форм (или по решению владельца).
