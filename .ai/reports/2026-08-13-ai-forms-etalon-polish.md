# AI Control Plane — Edit-формы приведены к эталону IteractionListEdit (2026-08-13)

REPO: `aananyev/it-pearls-recruiting` · BRANCH: `master` · STATUS: `HERMES_DONE`

## Что сделано (Hermes, локальный UI-полиш)

Три задачи по визуальному приведению AI-форм к общему эталону `IteractionListEdit` (CUBA 7.3, HRM HuntTech):

### 1. Меню «Управление AI» — caption
- `web-menu.xml`: у трёх пунктов (AiFunctionConfiguration, AdminAiConfiguration, UserAiFunctionOverride) отсутствовали caption → CUBA выводила сырые ключи `menu-config.*`. Добавлены `caption="mainMsg://menu_config.hunttech_*.browse"` + ключи EN/RU в `web/messages.properties`/`messages_ru.properties`.

### 2. Edit-формы «Конфигурация AI-функции» (AiFunctionConfigurationEdit) и «Корпоративное AI-подключение» (AdminAiConfigurationEdit) — детальный дифф 30 отличий от эталона
- footer: паттерн эталона `expand=editActionsSpacer` + группа AUTO/MIDDLE_RIGHT + `spacing="true"` (межкнопочный зазор 10px); OK — локальный primary (белый на rgb(77,122,178)), Отмена — secondary (transparent); кнопки прижаты в правый нижний угол (Отмена.left=1461, gap 21px).
- SCSS-партиал `admin-ai-configuration-editor.scss` (7 тем, sha256-идентичен): sidebar title 18px/700 `#ffb11b`/24px, subtitle 12px/400 `rgba(248,250,252,0.72)`/17px (без uppercase), toolbar title 20px/700, description mix 60%/18px, border-bottom 0.16, карточки margin-bottom 12px, чекбокс padding 3px 8px, textarea 15px/21.75px/4px, footer min-height 62px/padding 11px 20px/border-top 0.16/shadow вверх, кнопки 40px/padding 0 18px/600/radius 4px + hover brightness(0.98) + focus-ring.
- Бизнес-логика, Java-контроллеры, entity, views, БД и эталон НЕ изменялись.

### 3. Sidebar-логотип ovaFallbackImage — 176×176 во всех 5 AI Edit-формах
- AiFunctionConfigurationEdit, AdminAiConfigurationEdit, UserAiConfigurationEdit, UserAiFunctionOverrideEdit, VacancyPromptTemplateEdit: width/height/ovalWidth/ovalHeight 96px → **176px** — как фото кандидата в sidebar JobCandidateEdit.

## Верификация (все независимо, CDP-сверка computed styles, тема hover)

| Форма | Вердикт | Метрики |
|---|---|---|
| AiFunctionConfigurationEdit | ACCEPTED | 54 PASS / 0 FAIL |
| AdminAiConfigurationEdit | ACCEPTED | 58 PASS / 0 FAIL |
| Итерация 1 | Admin REJECTED (зазор кнопок 0px vs 10px) → фикс spacing → R2 ACCEPTED | |

- Файлы вердиктов: `.team/ai-forms-diff/03b-qa-verdict-ai-function.md`, `03c-qa-verdict-admin-ai.md`, `03d-qa-verdict-r2.md`; таблицы диффов `01b/01c-differences-*.md`; скриншоты `.team/ai-forms-diff/screenshots/`.
- Ad-hoc верификация: 25/25 и 22/22 PASS (XML, SCSS 7 тем, deployed jar, docs, HTTP 200).

## Изменённые файлы (коммит)

- `modules/web/.../screens/{aifunctionconfiguration,adminaiconfiguration,useraiconfiguration,useraifunctionoverride,vacancyprompttemplate}/*-edit.xml` (footer-паттерн, spacing, логотипы 176px)
- `modules/web/themes/*/com.company.hunttech/admin-ai-configuration-editor.scss` (7 тем)
- `docs/ui/{AiFunctionConfigurationEdit,AdminAiConfigurationEdit,UserAiConfigurationEdit,UserAiFunctionOverrideEdit,VacancyPromptTemplateEdit}_Spec.md`, `docs/entities/admin-ai-configuration/AdminAiConfiguration.md`
- `.team/ai-forms-diff/` (01c, 03b, 03c, 03d, screenshots), `.ai/reports/2026-08-13-ai-forms-etalon-polish.md`

## Ожидание от ChatGPT

Нет блокеров. Можно приступать к следующей задаче (например, Edit «Корпоративное AI-подключение» — уже приведена; кандидаты: остальные Edit-формы на тот же контракт).
