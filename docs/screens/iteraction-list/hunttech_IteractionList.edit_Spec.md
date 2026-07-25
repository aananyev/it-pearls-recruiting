# hunttech_IteractionList.edit — legacy-спецификация

> Каноническая UI-спецификация: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)  
> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`

## Назначение и бизнес-смысл (What & Why)

Экран создаёт и редактирует взаимодействие рекрутёра с кандидатом по вакансии. Он сохраняет кандидата, вакансию, тип взаимодействия, рейтинг, рекрутёра, способ связи, дополнительное значение и комментарий. Любой reflow должен сохранять существующие CUBA-компоненты и lifecycle контроллера.

## UI Context & Navigation

- вход из browse взаимодействий, карточки кандидата и связанных рекрутинговых сценариев;
- lookup/open кандидата и вакансии;
- lookup типа взаимодействия и рекрутёра;
- динамическое действие, определяемое выбранным типом;
- подписка через существующую кнопку;
- стандартные save-and-close и cancel.

## Behavior Summary

- форма открывается → loaders и Java lifecycle работают как раньше → отображается двухпанельная композиция;
- кандидат или вакансия меняются → существующие listeners обновляют контекст → sidebar показывает актуальные данные;
- тип взаимодействия меняется → Java включает один из dynamic controls → layout сохраняет соседнюю колонку;
- сохранение или отмена → выполняются прежние actions → footer остаётся доступным в workspace.

## Компоновка от 2026-07-25

```text
sidebar full height 252 px
└─ candidate / project / service fields / vacancy context

workspace
├─ toolbar 52 px
├─ TabSheet + ScrollBox
│  ├─ popular interactions
│  ├─ candidate + vacancy
│  ├─ interaction type + dynamic field
│  ├─ rating + recruiter
│  ├─ communication method
│  └─ comment
└─ footer 54 px
```

Ключевое отличие от первой версии редизайна: toolbar и footer больше не проходят над и под sidebar. Тёмная панель формирует непрерывную вертикальную область, а все рабочие действия остаются справа.

## Сохранённые контракты

- `dialogMode` — `1000 × 650`, modal;
- `iteractionListDc` — `iteractionList-edit-view`;
- loaders, JPQL, conditions, views — без изменений;
- Java-контроллер — без изменений;
- все component ID — сохранены;
- picker actions — сохранены;
- `buttonCallAction.invoke="callActionEntity"` — сохранён;
- `subscribeButton.invoke="onButtonSubscribeClick"` — сохранён;
- `windowCommitAndClose` и `windowClose` — сохранены;
- `candidateImage` и `projectLogoImage` остаются отдельными `Image`;
- dynamic `visible`, `required`, caption и value задаются Java как раньше.

## Локальный SCSS

Namespace: `.iteraction-list-editor`.

Поддерживаемые темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Вне namespace глобальные Vaadin-селекторы не изменяются.

## Проверки

Обязательны:

- XML well-formed;
- component ID / binding / action audit;
- `ScreenViewIntegrityTest` — 8/8;
- Data View Integrity;
- SCSS build;
- `clean assemble`;
- local deploy;
- HTTP `/hrm/` = 200;
- functional и visual smoke семи тем;
- Tomcat logs без новых critical errors.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Уточнена компоновка `IteractionListEdit`: непрерывный sidebar, toolbar/footer внутри workspace, более компактная геометрия и новый порядок рабочих полей |
| 2026-07-25 | Зафиксирован визуальный редизайн формы по общей UI/UX-концепции HRM HuntTech без изменения бизнес-логики |
