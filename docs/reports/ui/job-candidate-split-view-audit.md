# JobCandidateEdit split view audit

## Scope

Screen: `hunttech_JobCandidate.edit`

Files reviewed:
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java`
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/messages*.properties`
- `modules/web/themes/*/com.company.hunttech/*-ext.scss`

Backup point: branch `backup/job-candidate-split-view-redesign-base`.

## Existing sections

- Header options block: hidden `fullNameTextField`, hidden `blockCandidateCheckBox`, candidate summary labels.
- `jobCandidateCard`: candidate card, photo upload/drop zone, skills fragment area, last projects and suggested vacancies.
- `tabCandidate`: candidate main fields.
- `tabContactInfo`: contact fields, priority contact and social network grid.
- `tabIteraction`: interactions grid and vacancy filter.
- `tabResume`: CV grid and resume actions.
- `commentsTab`: comments feed and quick comment form.
- Bottom actions: block candidate, subscribe, save+close, cancel, created/updated label.

## Component id to Java dependency

Critical injected ids that must be preserved:
- Candidate summary/photo: `labelCV`, `labelQualityPercent`, `personPositionTitle`, `emailTitle`, `phoneTitle`, `telegramTitle`, `skypeTitle`, `jobTitleTitle`, `candidatePic`, `candidateDefaultPic`, `fileImageFaceUpload`, `fullNameField`, `currentCompanyLabel`, `emailLinkButton`, `skypeLinkButton`, `telegrammLinkButton`, `telegrammGroupLinkButton`.
- Main fields: `firstNameField`, `secondNameField`, `middleNameField`, `currentCompanyField`, `personPositionField`, `jobCityCandidateField`, `birdhDateField`, `positionsLabel`.
- Contacts/social networks: `emailField`, `phoneField`, `mobilePhoneField`, `skypeNameField`, `telegramNameField`, `telegramGroupField`, `whatsupNameField`, `wiberNameField`, `priorityCommunicationMethodRadioButton`, `socialNetworkTable`, `addSocialNetworkListsButton`.
- Interactions: `jobCandidateIteractionListTable`, `copyIteractionButton`, `openPositionProjectDescriptionButton`, `frequentInteractionPopupButton`, `vacancyFilterLookupPickerField`.
- Resume: `jobCandidateCandidateCvTable`, `copyCVButton`, `scanContactsFromCVButton`, `checkSkillFromJD`.
- Comments/history/actions: `jobCandidateCommentsDataGrid`, `chatMessageTextField`, `vacancyPopupPickerField`, `sendCommentButton`, `createdUpdatedLabel`, `blockCandidateButton`, `buttonSubscribe`.
- Containers/loaders: `jobCandidateDc`, `jobCandidateDl`, `jobCandidateCandidateCvsDc`, `jobCandidateSocialNetworksDc`, `jobCandidateIteractionDc`, `lastProjectDl`, `openPositionDl`, `suggestOpenPositionDl`, `currentCompaniesLc`, `currentCompaniesDc`, `citiesDl`, `personPositionsLc`, `interactionCommentDl`.
- Tabs: `tabSheetSocialNetworks`, `tabCandidate`, `tabContactInfo`, `tabIteraction`, `tabResume`, `commentsTab`.

The controller also resolves several components lazily through `getWindow().getComponent(...)`, so ids must remain globally unique after layout changes.

## Data containers and loaders

- `jobCandidateDc` / `jobCandidateDl`: edited `JobCandidate`, local view with `cityOfResidence`, `currentCompany.companyGroup`, `fileImageFace`, `positionList.positionList`, `personPosition`.
- `jobCandidateCandidateCvsDc`: property container `candidateCv`, loaded lazily by `ensureCandidateCvLoaded()`.
- `jobCandidateSocialNetworksDc`: property container `socialNetwork`, loaded lazily by `ensureSocialNetworksLoaded()`.
- `jobCandidateIteractionDc`: property container `iteractionList`, loaded lazily by `ensureInteractionsLoaded()`.
- `lastProjectDc` / `lastProjectDl`: key-value loader for candidate vacancies grouped by latest interaction date.
- `openPositionDc` / `openPositionDl`: open vacancies for comments and selectors.
- `suggestOpenPositionDc` / `suggestOpenPositionDl`: open suggested vacancies filtered by position type.
- `personPositionsDc` / `personPositionsLc`: position dictionary.
- `currentCompaniesDc` / `currentCompaniesLc`: company dictionary; `currentCompanyField` also uses a custom search executor.
- `citiesDc` / `citiesDl`: city dictionary.
- `interactionCommentDc` / `interactionCommentDl`: comment interactions.

## Required fields

- Main: `firstNameField`, `currentCompanyField`, `personPositionField`, `jobCityCandidateField`.
- Contacts: `emailField`, `telegramNameField`, `phoneField`, `telegramGroupField`, `mobilePhoneField`, `wiberNameField`, `skypeNameField`, `whatsupNameField`, `priorityCommunicationMethodRadioButton`.

## Actions and handlers

- XML invokes: `openPositionMasterBrowseStart`, `addPositionList`, `addSocialNetworksListsInvoke`, `addMissingSocialNetworksListsInvoke`, `removeEmptySocialNetworkListsButton`, `scanContactsFromCVs`, `sendCommentButtonInvoke`, `blockCandidateButton`, `onButtonSubscribeClick`.
- Table actions preserved: create/edit/remove/refresh on interaction, CV and social network grids.
- Screen subscriptions and generators preserved for upload, image click, contact fields, chat field, link buttons and generated columns.
- `currentCompanyField` receives dynamic `createCompany` action from Java.

## Potential XML/Java errors

- `interactionCommentDl` query orders by `e.deteIteraction`; this looks suspicious next to `dateIteraction`, but is existing behavior and is not changed by the UI redesign.
- `tabContactInfo` currently initializes both contact fields and the social network grid. A separate social networks tab requires controller split so opening either tab initializes only the needed components.
- `dialogMode` is fixed at `1200x750`; split view should reduce reliance on this fixed size.
- `tabCandidate` uses icon `BOMB`; redesign must replace it.

## Redesign risks

- Renaming ids would break injection, lazy `getComponent(...)`, generated columns or subscribed handlers.
- Moving `socialNetworkTable` without updating selected-tab initialization would leave social networks unloaded.
- Moving `createdUpdatedLabel` away from the bottom action area requires preserving its id because Java writes to it.
- New wrappers must not become fixed-height traps around grids; tables need enough expandable space.

## Proposed split-view structure

- Root `job-candidate-editor` hbox:
  - Left `job-candidate-sidebar` width around 280px with photo/upload, candidate name, position, status/rating/city/company/contact/CV summary and quick HR-master/block/subscribe actions.
  - Right `job-candidate-workspace` with top toolbar and a full-height lazy tab sheet.
- Tabs:
  - `tabCandidate`: `Основное`
  - `tabContactInfo`: `Контакты`
  - `jobCandidateCard`: `Позиции и вакансии`
  - `tabIteraction`: `Взаимодействия`
  - `tabResume`: `Резюме и файлы`
  - new `tabSocialNetworks`: `Социальные сети`
  - `commentsTab`: `Комментарии`
  - new `tabHistory`: `История`
- Preserve existing ids/actions/loaders and change only XML layout, isolated SCSS and minimal controller wiring needed for the new social/history tabs.
