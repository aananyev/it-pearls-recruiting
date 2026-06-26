# JobCandidate Detail Fragment (`itpearls_JobCanidateDetailScreenFragment`)

> Фрагмент детальной панели кандидата (раскрытие строки browse / кадровый резерв).
> Сущность: [JobCandidate.md](../entities/JobCandidate.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Read-only сводка по кандидату: фото, ФИО, должность, компания, город, контакты (с условной видимостью), соцсети (иконки-клики), данные последнего взаимодействия и статистика процессинга (динамические label в `statisticsHLabelBox`).

### Точки встраивания

- `JobCandidateBrowse` → `jobCandidatesTable.detailsGenerator`
- `PersonelReserveBrowse` → аналогичный details pattern

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_JobCanidateDetailScreenFragment`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_JobCanidateDetailScreenFragment` |
| **Java-класс** | `com.company.itpearls.web.screens.jobcandidate.JobCanidateDetailScreenFragment` |
| **XML-дескриптор** | `job-canidate-detail-screen-fragment.xml` |
| **Базовый класс** | `ScreenFragment` |
| **Тип** | Fragment (не самостоятельный экран меню) |

### Назначение

Read-only сводка по кандидату: фото, ФИО, должность, компания, город, контакты (с условной видимостью), соцсети (иконки-клики), данные последнего взаимодействия и статистика процессинга (динамические label в `statisticsHLabelBox`).

### Точки встраивания

- `JobCandidateBrowse` → `jobCandidatesTable.detailsGenerator`
- `PersonelReserveBrowse` → аналогичный details pattern

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Контейнер | Тип | provided | View |
|-----------|-----|----------|------|
| `jobCandidatesDc` | instance `JobCandidate` | `true` (от родителя) | `extends="_local"`, `iteractionList` → `recrutier.group` |

### Дополнительные загрузки в Java (не в fragment view)

| Query | View | Назначение |
|-------|------|------------|
| `QUERY_ALL_ITERACIONS` | `iteractionList-view` | все взаимодействия кандидата |
| `QUERY_ALL_CV` | `candidateCV-view` | счётчик резюме |
| `QUERY_LAST_SALARY` | `iteractionList-view` | ожидания по зарплате (`addString`) |

### Property bindings в XML

`fullName`, `personPosition`, `currentCompany`, `cityOfResidence`, `phone`, `mobilePhone`, `wiberName`, `whatsupName`, `fileImageFace`.

Динамические labels (без property): `companyLabel`, `vacancyNameLabel`, `departamentLabel`, `projectNameLabel`, `lastIteractionLabel`, `lastRecruterLabel`, `lastResearcherLabel`, `iteractionCountLabel`, `resumeCountLabel`, `salaryExpectationLabel`.

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```mermaid
flowchart TD
    Browse[itpearls_JobCandidate.browse] -->|detailsGenerator| Fragment[itpearls_JobCanidateDetailScreenFragment]
    Reserve[PersonelReserveBrowse] -->|detailsGenerator| Fragment
    Fragment -->|setJobCandidate| JC[JobCandidate entity]
```

Родитель передаёт entity через `setJobCandidate(JobCandidate)` и вызывает методы инициализации: `setVisibleContactsLabels()`, `setLinkButtonEmail()`, `setStatistics()`, `setStatisticsLabel()`, `setVisibleLogo()`, `setLastSalaryLabel()`.

---

## 4. Модель поведения и интерактивность (Behavior Model)

| Метод / событие | Логика |
|-----------------|--------|
| `onInit` | `setVisibleContactsLabels()` — скрытие пустых контактов |
| `setVisibleContactsLabels` | visibility `emailHBox`, `phoneHbox`, … по null-полям |
| `setStatistics` | последний рекрутер (группа «Хантинг»), researcher («Ресерчинг»), компания/департамент/вакансия/проект из `iteractionList[0].vacancy`, счётчики |
| `setStatisticsLabel` | динамические badge: активность, даты процессинга, дни свободен, дни на проекте, CV у заказчика, интервью |
| `createSocialNetworkFlowBox` | Image 25px с logo, click → `webBrowserTools.showWebPage` |
| `setVisibleLogo` | toggle `candidateFaceImage` / `candidateFaceDefaultImage` (`icons/no-programmer.jpeg`) |
| Link clicks | `mailto:`, `t.me/`, `skype:?chat` |

Группы пользователей (константы): `Менеджмент`, `Хантинг`, `Ресерчинг` — для определения роли последнего рекрутера.

Стили статусов: `button_table_green/yellow/red/gray/blue` по календарным порогам и текущему пользователю.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

Фрагмент не содержит CRUD-кнопок; интерактивные элементы:

| Элемент | Действие |
|---------|----------|
| `emailLinkButton` | открыть mailto |
| `skypeLinkButton`, `telegrammLinkButton`, `telegrammGroupLinkButton` | внешние протоколы |
| Иконки в `socialNetworkFlowBox` | переход по URL |
| `statisticsGroupBox` | collapsable, заполняется программно |

Кнопки действий (edit, new interaction, CV…) создаются **родительским** `detailsGenerator`, не во фрагменте.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

```
layout (expand=mainHbox)
├── statisticsGroupBox (collapsable, light)
│   └── statisticsHLabelBox (динамические Label)
└── mainHbox (expand=infoHBox)
    ├── candidateFaceImage | candidateFaceDefaultImage (150px, renderer-photo-150px)
    └── infoHBox
        ├── jobCandidateVBox (25%): ФИО, должность, компания, город
        ├── jobCandidateContactsVBox (25%): email/phone/… + socialNetworkFlowBox
        ├── jobCandidateIteractionInfoVBox (25%): компания, вакансия, департамент, проект, взаимодействие, зарплата
        └── jobCandidateStatisticsVBox (25%): рекрутер, researcher, счётчики
```

**CSS:** `job-candidate-fragment-label`, `h3`/`bold` для заголовков секций.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первичная UI Spec из `job-canidate-detail-screen-fragment.xml` и `JobCanidateDetailScreenFragment.java` |
