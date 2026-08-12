# architecture

Архитектурный раздел для решений и верхнеуровневых карт приложения. Детальные спецификации сущностей перенесены в соответствующие папки `docs/entities/<entity>/`, а рабочие deployment-архитектурные материалы оставлены в `deployment/`.

| Где искать | Ссылка |
| ---------- | ------ |
| AI upload описания проекта через admin-managed `PROJECT_DESCRIPTION_GENERATE` | [HRM_HuntTech_Project_Description_AI_Upload.md](HRM_HuntTech_Project_Description_AI_Upload.md) |
| Запрет legacy-сущностей `itpearls_*` и единый runtime namespace `hunttech_*` | [Entity_Namespace_Migration_itpearls_to_hunttech.md](Entity_Namespace_Migration_itpearls_to_hunttech.md) |
| Основная обязательная концепция UI/UX для всех будущих редизайнов HRM HuntTech | [HRM_HuntTech_UI_UX_Design_Concept.md](HRM_HuntTech_UI_UX_Design_Concept.md) |
| Контракт наследования Halo, палитр и системных компонентов тем `hunttech-modern-light` / `hunttech-modern-dark` | [HRM_HuntTech_Modern_Themes_Contract.md](HRM_HuntTech_Modern_Themes_Contract.md) |
| Обязательный стандарт смыслового документирования создаваемых и изменяемых XML-экранов | [XML_Screen_Documentation_Standard.md](XML_Screen_Documentation_Standard.md) |
| Общий контракт stylename для sidebar, `label-navigation` и правой части Edit-экранов | [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) |
| Архитектура централизованного управления AI-функциями, credentials, per-function override и fallback | [HRM_HuntTech_AI_Function_Management_Architecture.md](HRM_HuntTech_AI_Function_Management_Architecture.md) |
| Архитектура ускорения JobCandidateEdit | [JobCandidateEdit_Performance_Architecture.md](JobCandidateEdit_Performance_Architecture.md) |
| Архитектура вакансии OpenPosition | [OpenPosition_Spec.md](OpenPosition_Spec.md) |
| Архитектура рейтов по аутстафу | [OutstaffingRates_Spec.md](OutstaffingRates_Spec.md) |
| AI integration architecture | [../integrations/ai/AI_INTEGRATION.md](../integrations/ai/AI_INTEGRATION.md) |
| Production migration architecture audits | [../../deployment/database-migration/audit/](../../deployment/database-migration/audit/) |

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-12 | Добавлена архитектура AI upload описания проекта через `PROJECT_DESCRIPTION_GENERATE` |
| 2026-08-12 | Добавлена архитектура централизованного управления AI-функциями HRM HuntTech: AI Control Plane, корпоративные и пользовательские credentials, per-function override, resolver/fallback и требования к AI Browse/Edit-формам |
| 2026-08-04 | Добавлен контракт системного слоя и палитр тем `hunttech-modern-light` / `hunttech-modern-dark` |
| 2026-07-31 | Добавлена архитектурная спецификация рейтов по аутстафу [OutstaffingRates_Spec.md](OutstaffingRates_Spec.md) |
| 2026-07-29 | Общий контракт Edit-экранов дополнен точными regression-правилами, полученными при доведении IteractionListEdit |
| 2026-07-28 | Контракт Edit-экранов дополнен обязательным preflight для нейросети, правилами типовой формы, sidebar, состояний и поэкранного рефакторинга |
| 2026-07-28 | Добавлен обязательный стандарт смыслового документирования каждого элемента создаваемого или изменяемого XML-дескриптора |
| 2026-07-27 | Добавлен обязательный контракт общих stylename Edit-экранов и единое имя блока `label-navigation` |
| 2026-07-26 | Добавлен обязательный контракт единого namespace `hunttech_*` и запрет runtime-сущностей `itpearls_*` |
| 2026-07-25 | Общая концепция UI/UX назначена основным обязательным документом для проектирования и редизайна будущих форм HRM HuntTech |
| 2026-07-24 | В архитектурный каталог добавлена общая концепция UI/UX HRM HuntTech |
