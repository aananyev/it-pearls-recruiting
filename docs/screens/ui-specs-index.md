# UI-спецификации HRM HuntTech (`docs/ui/`)

Living-документация экранов, окон, фрагментов и составных UI-потоков **без замены** entity living-doc (`docs/entities/`) и architecture spec (`docs/architecture/`).

Правила агента: [living-ui-documentation.mdc](../../.cursor/rules/living-ui-documentation.mdc) · [`.cursorrules`](../../.cursorrules)

---

## Соглашения имён

| Тип | Путь | Когда |
|-----|------|-------|
| **UI Spec (канон)** | `docs/ui/{FormName}_Spec.md` | create / modify / fix UI — GLOBAL UI TRIGGER |
| **Legacy** | `docs/ui/{kebab-name}.md` | документы до введения `_Spec`; при крупных правках — cross-link или миграция |
| **Архив** | `docs/ui/archive/{FormName}_Spec.md` | полное удаление UI из кода |

**`{FormName}`** — `@UiController("…")` (приоритет); примеры: `hunttech_JobCandidate.browse`, `hunttech_OpenPosition.edit`.

Шаблон: [templates/ui-template.md](../project/templates/ui-template.md)

---

## Обязательные разделы UI Spec (6)

**Перед техническими разделами** — [Business & Context Intro](../../.cursor/rules/living-documentation.mdc) (3 подраздела: What & Why, UI Context, Behavior Summary). Затем:

1. Точка вызова и контекст (Invocation & Context)
2. Связь с моделью данных (Data & Entity Binding)
3. Иерархия и взаимосвязь форм (Form Hierarchy)
4. Модель поведения и интерактивность (Behavior Model)
5. Логика управляющих элементов (Actions & Buttons Logic)
6. Визуальная компоновка элементов (Visual Layout Schema)

+ **История изменений** (YYYY-MM-DD сверху).

---

## Каталог UI-документов

### JobCandidate

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_JobCandidate.browse` | [hunttech_JobCandidate.browse_Spec.md](job-candidate/hunttech_JobCandidate.browse_Spec.md) |
| Edit | `hunttech_JobCandidate.edit` | [hunttech_JobCandidate.edit_Spec.md](job-candidate/hunttech_JobCandidate.edit_Spec.md) · [JobCandidateEdit_Spec.md](job-candidate/JobCandidateEdit_Spec.md) |
| Detail fragment | `hunttech_JobCanidateDetailScreenFragment` | [hunttech_JobCanidateDetailScreenFragment_Spec.md](review-needed/hunttech_JobCanidateDetailScreenFragment_Spec.md) |
| Image face | `hunttech_JobCandidateImageFace` | [hunttech_JobCandidateImageFace_Spec.md](job-candidate/hunttech_JobCandidateImageFace_Spec.md) |
| Select positions | `hunttech_SelectPersonPositions` | [hunttech_SelectPersonPositions_Spec.md](person/hunttech_SelectPersonPositions_Spec.md) |

### CandidateCV

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse (реестр) | `hunttech_CandidateCVReestr.browse` | [ReestrBrowseFallbackNoCandidate_Spec.md](../ui/ReestrBrowseFallbackNoCandidate_Spec.md) |

### OpenPosition

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_OpenPosition.browse` | [hunttech_OpenPosition.browse_Spec.md](open-position/hunttech_OpenPosition.browse_Spec.md) |
| Edit | `hunttech_OpenPosition.edit` | [hunttech_OpenPosition.edit_Spec.md](open-position/hunttech_OpenPosition.edit_Spec.md) |
| Detail fragment | `hunttech_OpenPositionDetailScreenFragment` | [hunttech_OpenPositionDetailScreenFragment_Spec.md](open-position/hunttech_OpenPositionDetailScreenFragment_Spec.md) |

### IteractionList

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_IteractionList.browse` | [hunttech_IteractionList.browse_Spec.md](iteraction-list/hunttech_IteractionList.browse_Spec.md) |
| Edit | `hunttech_IteractionList.edit` | [hunttech_IteractionList.edit_Spec.md](iteraction-list/hunttech_IteractionList.edit_Spec.md) |
| Simple browse | `hunttech_IteractionListSimple.browse` | [hunttech_IteractionListSimple.browse_Spec.md](iteraction-list/hunttech_IteractionListSimple.browse_Spec.md) |
| Fragment (jobcandidate) | `hunttech_IteractionListBrowse` | [hunttech_IteractionListBrowse_Spec.md](iteraction-list/hunttech_IteractionListBrowse_Spec.md) |
| Browse (реестр) | `hunttech_IteractionListReestr.browse` | [ReestrBrowseFallbackNoCandidate_Spec.md](../ui/ReestrBrowseFallbackNoCandidate_Spec.md) |

### Iteraction

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_Iteraction.browse` | [hunttech_Iteraction.browse_Spec.md](iteraction/hunttech_Iteraction.browse_Spec.md) |
| Edit | `hunttech_Iteraction.edit` | [hunttech_Iteraction.edit_Spec.md](iteraction/hunttech_Iteraction.edit_Spec.md) |
| Tree browse | `hunttech_Iteraction._tree.browse` | [hunttech_Iteraction._tree.browse_Spec.md](iteraction/hunttech_Iteraction._tree.browse_Spec.md) |
| Tree edit | `hunttech_Iteraction_tree.edit` | [hunttech_Iteraction_tree.edit_Spec.md](iteraction/hunttech_Iteraction_tree.edit_Spec.md) |
| Requirement browse | `hunttech_IteractionRequirement.browse` | [hunttech_IteractionRequirement.browse_Spec.md](iteraction/hunttech_IteractionRequirement.browse_Spec.md) |

### Project

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_Project.browse` | [hunttech_Project.browse_Spec.md](project/hunttech_Project.browse_Spec.md) |
| Edit | `hunttech_Project.edit` | [hunttech_Project.edit_Spec.md](project/hunttech_Project.edit_Spec.md) |

### Person

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_Person.browse` | [hunttech_Person.browse_Spec.md](person/hunttech_Person.browse_Spec.md) |
| Edit | `hunttech_Person.edit` | [hunttech_Person.edit_Spec.md](person/hunttech_Person.edit_Spec.md) |

### Company

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_Company.browse` | [hunttech_Company.browse_Spec.md](company/hunttech_Company.browse_Spec.md) |
| Edit | `hunttech_Company.edit` | [hunttech_Company.edit_Spec.md](company/hunttech_Company.edit_Spec.md) |
| Our company browse | `hunttech_OurCompany.browse` | [hunttech_OurCompany.browse_Spec.md](company/hunttech_OurCompany.browse_Spec.md) |
| Clients browse | `hunttech_ClientsCompany.browse` | [hunttech_ClientsCompany.browse_Spec.md](company/hunttech_ClientsCompany.browse_Spec.md) |

### CompanyDepartament

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_CompanyDepartament.browse` | [hunttech_CompanyDepartament.browse_Spec.md](company-departament/hunttech_CompanyDepartament.browse_Spec.md) |
| Edit | `hunttech_CompanyDepartament.edit` | [hunttech_CompanyDepartament.edit_Spec.md](company-departament/hunttech_CompanyDepartament.edit_Spec.md) |

### CompanyGroup

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_CompanyGroup.browse` | [hunttech_CompanyGroup.browse_Spec.md](company-group/hunttech_CompanyGroup.browse_Spec.md) |
| Edit | `hunttech_CompanyGroup.edit` | [hunttech_CompanyGroup.edit_Spec.md](company-group/hunttech_CompanyGroup.edit_Spec.md) |

### Position

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_Position.browse` | [hunttech_Position.browse_Spec.md](position/hunttech_Position.browse_Spec.md) |
| Edit | `hunttech_Position.edit` | [hunttech_Position.edit_Spec.md](position/hunttech_Position.edit_Spec.md) |

### SkillTree

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_SkillTree.browse` | [hunttech_SkillTree.browse_Spec.md](skill-tree/hunttech_SkillTree.browse_Spec.md) |
| Edit | `hunttech_SkillTree.edit` | [hunttech_SkillTree.edit_Spec.md](skill-tree/hunttech_SkillTree.edit_Spec.md) |

### OpenPositionNews / OpenPositionComment

| Форма | Controller | Документ |
|-------|------------|----------|
| News browse | `hunttech_OpenPositionNews.browse` | [hunttech_OpenPositionNews.browse_Spec.md](open-position-news/hunttech_OpenPositionNews.browse_Spec.md) |
| News edit | `hunttech_OpenPositionNews.edit` | [hunttech_OpenPositionNews.edit_Spec.md](open-position-news/hunttech_OpenPositionNews.edit_Spec.md) |
| Comment browse | `hunttech_OpenPositionComment.browse` | [hunttech_OpenPositionComment.browse_Spec.md](open-position-comment/hunttech_OpenPositionComment.browse_Spec.md) |
| Comment edit | `hunttech_OpenPositionComment.edit` | [hunttech_OpenPositionComment.edit_Spec.md](open-position-comment/hunttech_OpenPositionComment.edit_Spec.md) |

### RecrutiesTasks

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `hunttech_RecrutiesTasks.browse` | [hunttech_RecrutiesTasks.browse_Spec.md](recruties-tasks/hunttech_RecrutiesTasks.browse_Spec.md) |
| Edit | `hunttech_RecrutiesTasks.edit` | [hunttech_RecrutiesTasks.edit_Spec.md](recruties-tasks/hunttech_RecrutiesTasks.edit_Spec.md) |
| Group subscribe | `hunttech_RecrutiesGroupSubscribeTasks.browse` | [hunttech_RecrutiesGroupSubscribeTasks.browse_Spec.md](recruties-tasks/hunttech_RecrutiesGroupSubscribeTasks.browse_Spec.md) |

### Справочники (Grade, City, Country, Region)

| Сущность | Browse | Edit |
|----------|--------|------|
| Grade | [browse](grade/hunttech_Grade.browse_Spec.md) | [edit](grade/hunttech_Grade.edit_Spec.md) |
| City | [browse](city/hunttech_City.browse_Spec.md) | [edit](city/hunttech_City.edit_Spec.md) |
| Country | [browse](country/hunttech_Country.browse_Spec.md) | [edit](country/hunttech_Country.edit_Spec.md) |
| Region | [browse](region/hunttech_Region.browse_Spec.md) | [edit](region/hunttech_Region.edit_Spec.md) |

### Прочее (legacy)

| Форма | Controller | Документ | Статус |
|-------|------------|----------|--------|
| Login (брендированный) | `loginBranded` | [login-screen.md](login/login-screen.md) | legacy (kebab) |

### AI-администрирование

| Форма | Controller | Документ |
|-------|------------|----------|
| Шаблоны промптов (browse) | `hunttech_VacancyPromptTemplate.browse` | [hunttech_VacancyPromptTemplate.browse_Spec.md](vacancy-prompt-template/hunttech_VacancyPromptTemplate.browse_Spec.md) |
| Шаблон промпта (edit) | `hunttech_VacancyPromptTemplate.edit` | [hunttech_VacancyPromptTemplate.edit_Spec.md](vacancy-prompt-template/hunttech_VacancyPromptTemplate.edit_Spec.md) |
| Мониторинг ключей (browse) | `hunttech_UserAiConfiguration.browse` | [hunttech_UserAiConfiguration.browse_Spec.md](user-ai-configuration/hunttech_UserAiConfiguration.browse_Spec.md) |

### ExtUser (Security)

| Форма | Controller | Документ |
|-------|------------|----------|
| Edit | `sec$User.edit` / `hunttech_ExtUserEdit` | [hunttech_ExtUserEdit_Spec.md](ext-user/hunttech_ExtUserEdit_Spec.md) |
| AI config edit | `hunttech_UserAiConfiguration.edit` | (модаль внутри ExtUser edit) · entity [UserAiConfiguration.md](../entities/user-ai-configuration/UserAiConfiguration.md) |

---

## Связь с документацией сущностей

| Сценарий | Entity doc | UI Spec |
|----------|------------|---------|
| Browse/Edit сущности | `docs/entities/{EntityName}.md` §2 | `docs/ui/{FormName}_Spec.md` — детализация формы |
| Экран без одной entity (login, dashboard) | — | только `docs/ui/{FormName}_Spec.md` |
| Триггер «Сделай документацию сущности …» | `docs/architecture/{EntityName}_Spec.md` | §3–5 architecture могут ссылаться на UI Spec |

Cross-links в шапке Spec ↔ entity/architecture при наличии привязки.

### Кастомные UI-компоненты

| Документ | Описание |
|----------|----------|
| [FallbackImage_Component.md](components/FallbackImage_Component.md) | `fallbackImage` — image с theme-fallback при пустом FileDescriptor |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-28 | В каталог добавлены реестры `hunttech_IteractionListReestr.browse` (взаимодействия) и `hunttech_CandidateCVReestr.browse` (резюме): контракт sidebar-аватара `logoPic` 120×120 — fallback `icons/no-candidate.png`, `SCALE_DOWN` ([ReestrBrowseFallbackNoCandidate_Spec.md](../ui/ReestrBrowseFallbackNoCandidate_Spec.md)) |
| 2026-07-05 | Добавлены подтемы hunttech-modern-light и hunttech-modern-dark в стиле Jmix Default/Dark, настроена регистрация в web-app.properties (cuba.theme.hover.modes) |
| 2026-06-29 | Документация кастомного компонента FallbackImage |
| 2026-06-27 | AI-администрирование: VacancyPromptTemplate browse/edit, UserAiConfiguration browse, меню aiAdministration |
| 2026-06-26 | Каталог дополнен UI Spec для 16 documented entities (40 новых файлов + 7 ранее созданных JobCandidate/OpenPosition) |
| 2026-06-26 | Введён каталог UI Spec, соглашение `{FormName}_Spec.md`, archive/, связь с living-ui-documentation |
