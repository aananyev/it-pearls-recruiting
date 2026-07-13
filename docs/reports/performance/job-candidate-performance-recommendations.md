# JobCandidate Performance Recommendations

Дата: 2026-07-13.

## Main Finding

The main problem appears to be application loading shape, not a missing critical index:

- root edit view loads multiple collections immediately;
- `tabCandidate` loads full lookup option sets, especially all active companies;
- suggestion search uses leading wildcard scans;
- existing candidate/interaction/CV indexes already cover key measured queries.

## Package A: Safe Fast Improvements

| Priority | Proposal | Files | Expected effect | Risk |
| --- | --- | --- | --- | --- |
| P1 | Replace full company `LookupPickerField` options with search/lookup-only selection or capped options | `job-candidate-edit.xml`, `JobCandidateEdit.java` | reduce first `tabCandidate` UI work | UI behavior change |
| P1 | Keep `currentCompaniesLc` lazy and avoid loading until field focus/search if possible | same | removes 5623-row options load from tab switch | requires UI testing |
| P1 | Ensure `jobCandidateDc` edit view does not load CV text/byte-heavy fields for first render | screen XML/views | reduce payload for candidates with many CVs | fetch plan change, needs testing |
| P2 | Limit interaction/CV tables initially or load per tab only | screen XML/controller | reduce opening payload | bigger screen refactor |
| P2 | Add search expression support for name suggestions using trigram index if allowed | DB + query | faster `%text%` suggestion | requires extension/DB decision |

## Package B: Architectural Improvements

| Priority | Proposal | Expected effect | Risk |
| --- | --- | --- | --- |
| P1 | Split first render view and tab views | root open becomes lighter | fetch-plan regression possible |
| P1 | Load `candidateCv`, `iteractionList`, `socialNetwork` only when their tabs/components are opened | major for heavy candidates | requires DataContext and UI table testing |
| P2 | Replace full lookup option containers with lookup screens/suggestion fields | large tab-candidate improvement | UX change |
| P2 | Add bounded/paginated interactions and CV lists | stable performance for high-link candidates | more UI work |
| P3 | Cache small dictionaries like city/position for session | small improvement | cache invalidation |

## Index Recommendations

Do not create indexes yet. Existing local indexes cover measured FK paths. Proposed SQL is a validation package only.

Strongest candidate if name suggestions remain `LIKE '%text%'`: PostgreSQL trigram indexes. This requires `pg_trgm` and explicit approval because it adds an extension and non-btree indexes.

## Services

- `InteractionService.getMostPolularIteraction` runs on screen setup and should be measured in real SQL tracing.
- `StarsAndOtherService` and file/image helpers should be checked only if UI timing shows image/label rendering dominates.
- Avoid service calls inside generated column loops; current candidate edit has generated columns for icons/logos/images that should be kept under watch.

