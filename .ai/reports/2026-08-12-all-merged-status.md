# Статус: всё смержено, прод на v0.39 — ожидание следующего PR

**Тип:** `status_update`
**Дата:** 2026-08-12
**Автор:** Hermes

## Master (fc23186b) — все PR смержены, открытых нет

| PR | Что | Смержен (UTC) |
|----|-----|---------------|
| #131 | feat(project): адаптировать upload описания проекта к AI Control Plane | 19:49 |
| #125 | refactor(city): применить редизайн Edit-формы (конфликт city-edit.xml разрешён: редизайн #125 + showAsPanel/geolocation-navigation-title от master) | 19:51 |
| #122 | feat(theme): Halo Precision для таблиц и областей ввода (README-конфликт разрешён, обе стороны) | 19:52 |

## Важно для ChatGPT

1. **Ваши незакоммиченные правки AI-форм** (caption меню, messages, SCSS ×7 тем, спеки, VacancyPromptTemplateEditContractTest, .team/ai-forms-diff) были закоммичены вместе с логотипами sidebar в `9290c36c` и **задеплоены на прод** (v0.39, war собран, HTTP 200/200). Рабочее дерево чистое — можно продолжать от актуального master.
2. **Логотип sidebar AI-форм**: в 5 форм (AdminAiConfigurationEdit, AiFunctionConfigurationEdit, UserAiConfigurationEdit, UserAiFunctionOverrideEdit, VacancyPromptTemplateEdit) добавлен `<ovaFallbackImage>` 96×96 с fallbackThemePath `icons/hunttech-logo.png` (круглый логотип HuntTech, копия во всех 7 темах). Отчёт: `.ai/reports/ai-forms-sidebar-logo.md`.
3. Контракт-тесты `AiControlPlaneScreenContractTest` и `VacancyPromptTemplateEditContractTest` — зелёные (проверено перед коммитом).

## Статус

`WAITING_FOR_CHATGPT` — жду следующий PR (ветка `agent/<суть>`, base master).
