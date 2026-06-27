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

**`{FormName}`** — `@UiController("…")` (приоритет); примеры: `itpearls_JobCandidate.browse`, `itpearls_OpenPosition.edit`.

Шаблон: [templates/ui-template.md](../templates/ui-template.md)

---

## Обязательные разделы UI Spec (6)

**Перед техническими разделами** — [Business & Context Intro](../.cursor/rules/living-documentation.mdc) (3 подраздела: What & Why, UI Context, Behavior Summary). Затем:

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
| Browse | `itpearls_JobCandidate.browse` | [itpearls_JobCandidate.browse_Spec.md](itpearls_JobCandidate.browse_Spec.md) |
| Edit | `itpearls_JobCandidate.edit` | [itpearls_JobCandidate.edit_Spec.md](itpearls_JobCandidate.edit_Spec.md) |
| Detail fragment | `itpearls_JobCanidateDetailScreenFragment` | [itpearls_JobCanidateDetailScreenFragment_Spec.md](itpearls_JobCanidateDetailScreenFragment_Spec.md) |
| Image face | `itpearls_JobCandidateImageFace` | [itpearls_JobCandidateImageFace_Spec.md](itpearls_JobCandidateImageFace_Spec.md) |
| Select positions | `itpearls_SelectPersonPositions` | [itpearls_SelectPersonPositions_Spec.md](itpearls_SelectPersonPositions_Spec.md) |

### OpenPosition

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_OpenPosition.browse` | [itpearls_OpenPosition.browse_Spec.md](itpearls_OpenPosition.browse_Spec.md) |
| Edit | `itpearls_OpenPosition.edit` | [itpearls_OpenPosition.edit_Spec.md](itpearls_OpenPosition.edit_Spec.md) |
| Detail fragment | `itpearls_OpenPositionDetailScreenFragment` | [itpearls_OpenPositionDetailScreenFragment_Spec.md](itpearls_OpenPositionDetailScreenFragment_Spec.md) |

### IteractionList

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_IteractionList.browse` | [itpearls_IteractionList.browse_Spec.md](itpearls_IteractionList.browse_Spec.md) |
| Edit | `itpearls_IteractionList.edit` | [itpearls_IteractionList.edit_Spec.md](itpearls_IteractionList.edit_Spec.md) |
| Simple browse | `itpearls_IteractionListSimple.browse` | [itpearls_IteractionListSimple.browse_Spec.md](itpearls_IteractionListSimple.browse_Spec.md) |
| Fragment (jobcandidate) | `itpearls_IteractionListBrowse` | [itpearls_IteractionListBrowse_Spec.md](itpearls_IteractionListBrowse_Spec.md) |

### Iteraction

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_Iteraction.browse` | [itpearls_Iteraction.browse_Spec.md](itpearls_Iteraction.browse_Spec.md) |
| Edit | `itpearls_Iteraction.edit` | [itpearls_Iteraction.edit_Spec.md](itpearls_Iteraction.edit_Spec.md) |
| Tree browse | `itpearls_Iteraction._tree.browse` | [itpearls_Iteraction._tree.browse_Spec.md](itpearls_Iteraction._tree.browse_Spec.md) |
| Tree edit | `itpearls_Iteraction_tree.edit` | [itpearls_Iteraction_tree.edit_Spec.md](itpearls_Iteraction_tree.edit_Spec.md) |
| Requirement browse | `itpearls_IteractionRequirement.browse` | [itpearls_IteractionRequirement.browse_Spec.md](itpearls_IteractionRequirement.browse_Spec.md) |

### Project

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_Project.browse` | [itpearls_Project.browse_Spec.md](itpearls_Project.browse_Spec.md) |
| Edit | `itpearls_Project.edit` | [itpearls_Project.edit_Spec.md](itpearls_Project.edit_Spec.md) |

### Person

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_Person.browse` | [itpearls_Person.browse_Spec.md](itpearls_Person.browse_Spec.md) |
| Edit | `itpearls_Person.edit` | [itpearls_Person.edit_Spec.md](itpearls_Person.edit_Spec.md) |

### Company

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_Company.browse` | [itpearls_Company.browse_Spec.md](itpearls_Company.browse_Spec.md) |
| Edit | `itpearls_Company.edit` | [itpearls_Company.edit_Spec.md](itpearls_Company.edit_Spec.md) |
| Our company browse | `itpearls_OurCompany.browse` | [itpearls_OurCompany.browse_Spec.md](itpearls_OurCompany.browse_Spec.md) |
| Clients browse | `itpearls_ClientsCompany.browse` | [itpearls_ClientsCompany.browse_Spec.md](itpearls_ClientsCompany.browse_Spec.md) |

### CompanyDepartament

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_CompanyDepartament.browse` | [itpearls_CompanyDepartament.browse_Spec.md](itpearls_CompanyDepartament.browse_Spec.md) |
| Edit | `itpearls_CompanyDepartament.edit` | [itpearls_CompanyDepartament.edit_Spec.md](itpearls_CompanyDepartament.edit_Spec.md) |

### CompanyGroup

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_CompanyGroup.browse` | [itpearls_CompanyGroup.browse_Spec.md](itpearls_CompanyGroup.browse_Spec.md) |
| Edit | `itpearls_CompanyGroup.edit` | [itpearls_CompanyGroup.edit_Spec.md](itpearls_CompanyGroup.edit_Spec.md) |

### Position

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_Position.browse` | [itpearls_Position.browse_Spec.md](itpearls_Position.browse_Spec.md) |
| Edit | `itpearls_Position.edit` | [itpearls_Position.edit_Spec.md](itpearls_Position.edit_Spec.md) |

### SkillTree

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_SkillTree.browse` | [itpearls_SkillTree.browse_Spec.md](itpearls_SkillTree.browse_Spec.md) |
| Edit | `itpearls_SkillTree.edit` | [itpearls_SkillTree.edit_Spec.md](itpearls_SkillTree.edit_Spec.md) |

### OpenPositionNews / OpenPositionComment

| Форма | Controller | Документ |
|-------|------------|----------|
| News browse | `itpearls_OpenPositionNews.browse` | [itpearls_OpenPositionNews.browse_Spec.md](itpearls_OpenPositionNews.browse_Spec.md) |
| News edit | `itpearls_OpenPositionNews.edit` | [itpearls_OpenPositionNews.edit_Spec.md](itpearls_OpenPositionNews.edit_Spec.md) |
| Comment browse | `itpearls_OpenPositionComment.browse` | [itpearls_OpenPositionComment.browse_Spec.md](itpearls_OpenPositionComment.browse_Spec.md) |
| Comment edit | `itpearls_OpenPositionComment.edit` | [itpearls_OpenPositionComment.edit_Spec.md](itpearls_OpenPositionComment.edit_Spec.md) |

### RecrutiesTasks

| Форма | Controller | Документ |
|-------|------------|----------|
| Browse | `itpearls_RecrutiesTasks.browse` | [itpearls_RecrutiesTasks.browse_Spec.md](itpearls_RecrutiesTasks.browse_Spec.md) |
| Edit | `itpearls_RecrutiesTasks.edit` | [itpearls_RecrutiesTasks.edit_Spec.md](itpearls_RecrutiesTasks.edit_Spec.md) |
| Group subscribe | `itpearls_RecrutiesGroupSubscribeTasks.browse` | [itpearls_RecrutiesGroupSubscribeTasks.browse_Spec.md](itpearls_RecrutiesGroupSubscribeTasks.browse_Spec.md) |

### Справочники (Grade, City, Country, Region)

| Сущность | Browse | Edit |
|----------|--------|------|
| Grade | [browse](itpearls_Grade.browse_Spec.md) | [edit](itpearls_Grade.edit_Spec.md) |
| City | [browse](itpearls_City.browse_Spec.md) | [edit](itpearls_City.edit_Spec.md) |
| Country | [browse](itpearls_Country.browse_Spec.md) | [edit](itpearls_Country.edit_Spec.md) |
| Region | [browse](itpearls_Region.browse_Spec.md) | [edit](itpearls_Region.edit_Spec.md) |

### Прочее (legacy)

| Форма | Controller | Документ | Статус |
|-------|------------|----------|--------|
| Login (брендированный) | `loginBranded` | [login-screen.md](login-screen.md) | legacy (kebab) |

### AI-администрирование

| Форма | Controller | Документ |
|-------|------------|----------|
| Шаблоны промптов (browse) | `itpearls_VacancyPromptTemplate.browse` | [itpearls_VacancyPromptTemplate.browse_Spec.md](itpearls_VacancyPromptTemplate.browse_Spec.md) |
| Шаблон промпта (edit) | `itpearls_VacancyPromptTemplate.edit` | [itpearls_VacancyPromptTemplate.edit_Spec.md](itpearls_VacancyPromptTemplate.edit_Spec.md) |
| Мониторинг ключей (browse) | `itpearls_UserAiConfiguration.browse` | [itpearls_UserAiConfiguration.browse_Spec.md](itpearls_UserAiConfiguration.browse_Spec.md) |

### ExtUser (Security)

| Форма | Controller | Документ |
|-------|------------|----------|
| Edit | `sec$User.edit` / `itpearls_ExtUserEdit` | [itpearls_ExtUserEdit_Spec.md](itpearls_ExtUserEdit_Spec.md) |
| AI config edit | `itpearls_UserAiConfiguration.edit` | (модаль внутри ExtUser edit) · entity [UserAiConfiguration.md](../entities/UserAiConfiguration.md) |

---

## Связь с документацией сущностей

| Сценарий | Entity doc | UI Spec |
|----------|------------|---------|
| Browse/Edit сущности | `docs/entities/{EntityName}.md` §2 | `docs/ui/{FormName}_Spec.md` — детализация формы |
| Экран без одной entity (login, dashboard) | — | только `docs/ui/{FormName}_Spec.md` |
| Триггер «Сделай документацию сущности …» | `docs/architecture/{EntityName}_Spec.md` | §3–5 architecture могут ссылаться на UI Spec |

Cross-links в шапке Spec ↔ entity/architecture при наличии привязки.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-27 | AI-администрирование: VacancyPromptTemplate browse/edit, UserAiConfiguration browse, меню aiAdministration |
| 2026-06-26 | Каталог дополнен UI Spec для 16 documented entities (40 новых файлов + 7 ранее созданных JobCandidate/OpenPosition) |
| 2026-06-26 | Введён каталог UI Spec, соглашение `{FormName}_Spec.md`, archive/, связь с living-ui-documentation |
