# JobCandidateEdit Loading Map

Дата: 2026-07-13.

## Files

| Area | Files |
| --- | --- |
| Entity | `modules/global/src/com/company/hunttech/entity/JobCandidate.java` |
| Views | `modules/global/src/com/company/hunttech/views.xml` |
| Screen XML | `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml` |
| Controller | `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java` |
| Messages | `modules/web/src/com/company/hunttech/web/screens/jobcandidate/messages*.properties` |
| Tests | `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/*PerfTest*.java` |
| Services | `InteractionService`, `GetRoleService`, `ParseCVService`, `PdfParserService`, `StarsAndOtherService`, `ResumeRecognitionService`, `OpenPositionService` |

## Initial Entity Graph

Inline XML view for `jobCandidateDc` extends `_local` and includes:

```text
JobCandidate
├── candidateCv fetch=BATCH view=_local
│   ├── candidate._local.personPosition
│   ├── resumePosition._local
│   ├── toVacancy._local
│   │   ├── grade._local
│   │   ├── positionType._local
│   │   ├── projectName._local.projectLogo/projectDepartment/companyName
│   │   └── owner._local
│   ├── someFiles._local.fileDescriptor/fileType/candidateCV
│   └── fileImageFace._local
├── iteractionList fetch=BATCH view=_local
│   ├── candidate._minimal
│   ├── vacancy openPosition-iteraction-list-picker-view
│   ├── iteractionType iteraction-list-type-view
│   └── recrutier extUser-picker-view
├── socialNetwork fetch=BATCH view=_local.socialNetworkURL.logo
├── cityOfResidence._local
├── currentCompany._local.companyGroup._local
├── fileImageFace._local
├── positionList fetch=BATCH.positionList._local
└── personPosition._local
```

This is the heaviest fetch plan in the screen because it loads multiple collections before any tab-specific lazy logic can reduce them.

## Data Loaders

| Loader | Query | Trigger | Rows in local DB | Risk |
| --- | --- | --- | ---: | --- |
| `jobCandidateDl` | by edited entity id | `@LoadDataBeforeShow` | 1 root + collections | broad inline view |
| `lastProjectDl` | interactions grouped by vacancy | initial card code | 23 for heavy candidate | OK with index |
| `openPositionDl` | active open positions ordered by name | comments tab first open | 4253 active | full lookup list |
| `suggestOpenPositionDl` | active positions by position type | suggestion logic | limited to 1 in code | OK |
| `currentCompaniesLc` | all companies ordered by name | `tabCandidate` first open | 5623 active | high UI/options cost |
| `citiesDl` | all cities ordered by name | `tabCandidate` first open | 292 active | low |
| `personPositionsLc` | all positions except marker | `tabCandidate` first open | 216 active | low |
| `interactionCommentDl` | candidate comments ordered by date | comments tab first open | candidate dependent | OK with existing index |

## TabSheet

`tabSheetSocialNetworks` has `lazy="true"`.

Tabs:

| Tab id | Meaning | Initialization |
| --- | --- | --- |
| `jobCandidateCard` | first card | initial |
| `tabCandidate` | "Кандидат" | `initTabCandidate()` |
| `tabContactInfo` | contacts/social | `initTabContactInfo()` |
| `tabIteraction` | interactions | `initTabInteractions()` |
| `tabResume` | CV | `initTabResume()` |
| `commentsTab` | comments | `initTabComments()` |

## Controller Hot Spots

- `@LoadDataBeforeShow` loads the broad entity view.
- `initTabCandidate()` loads three reference containers once.
- `setupNameSearchExecutors()` uses `lower(field) LIKE '%text%'`, causing seq scans for leading wildcard search.
- `getLastCVText()` scans all loaded CVs in Java.
- `setupSkillBox()` depends on last CV text and can touch large CV text if `candidateCv` is already loaded.
- Contact tab creates missing social URL rows for new candidates only; not a major edit-existing baseline cost.

