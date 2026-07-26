# architecture

Архитектурный раздел для решений и верхнеуровневых карт приложения. Детальные спецификации сущностей перенесены в соответствующие папки `docs/entities/<entity>/`, а рабочие deployment-архитектурные материалы оставлены в `deployment/`.

| Где искать | Ссылка |
| ---------- | ------ |
| Запрет legacy-сущностей `itpearls_*` и единый runtime namespace `hunttech_*` | [Entity_Namespace_Migration_itpearls_to_hunttech.md](Entity_Namespace_Migration_itpearls_to_hunttech.md) |
| Основная обязательная концепция UI/UX для всех будущих редизайнов HRM HuntTech | [HRM_HuntTech_UI_UX_Design_Concept.md](HRM_HuntTech_UI_UX_Design_Concept.md) |
| Архитектура ускорения JobCandidateEdit | [JobCandidateEdit_Performance_Architecture.md](JobCandidateEdit_Performance_Architecture.md) |
| Архитектура вакансии OpenPosition | [../entities/open-position/OpenPosition_Spec.md](../entities/open-position/OpenPosition_Spec.md) |
| AI integration architecture | [../integrations/ai/AI_INTEGRATION.md](../integrations/ai/AI_INTEGRATION.md) |
| Production migration architecture audits | [../../deployment/database-migration/audit/](../../deployment/database-migration/audit/) |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Добавлен обязательный контракт единого namespace `hunttech_*` и запрет runtime-сущностей `itpearls_*` |
| 2026-07-25 | Общая концепция UI/UX назначена основным обязательным документом для проектирования и редизайна будущих форм HRM HuntTech |
| 2026-07-24 | В архитектурный каталог добавлена общая концепция UI/UX HRM HuntTech |
