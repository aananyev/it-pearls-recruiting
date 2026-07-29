# Hermes verification — JobCandidateEdit shared style polish

PROJECT: HRM HuntTech
REPOSITORY: aananyev/it-pearls-recruiting
BRANCH: agent/job-candidate-edit-shared-style-polish
BASE: master
MODE: verification only, no code changes

## Scope

Verify the visual refactoring of `hunttech_JobCandidate.edit` / `job-candidate-edit.xml`
against:

- `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md`
- `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`
- `docs/ui/JobCandidateEdit_Spec.md`
- `docs/screens/job-candidate/JobCandidateEdit_Spec.md`

## Strict guard

Business logic must remain unchanged. Do not change functional code, XML bindings,
entity fields, loaders, views, JPQL, actions, `invoke`, validators, required/editable/visible
contracts, database, production, Tomcat configuration, or deploy scripts.

The intended Java change is presentation-only: synchronization of `label-nav-item-active`
and preserving `edit-sidebar-title` when the existing blocked-state style is refreshed.

## Required HEAD checks

1. Confirm local branch exists and equals the PR HEAD SHA.
2. Confirm PR is open from `agent/job-candidate-edit-shared-style-polish` to `master`.
3. If HEAD differs from this instruction/PR body, stop with `HEAD_MISMATCH`.
4. Confirm there are no merge conflicts.

## Commands

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Note: the all-module command
`./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace`
may fail in this repository because `app-web:test` has no matching test class. The
actual test class is in `app-core`.

## Visual smoke

1. Open `/hrm/` locally and log in as `alan` / `Dodo-2012`.
2. Open an existing JobCandidate edit screen.
3. Verify sidebar:
   - no inner card or button exits the sidebar bounds;
   - candidate photo/fallback remains centered and circular;
   - full name and position stay under the photo and wrap cleanly;
   - visible `label-navigation` appears after profile/contact summary;
   - active navigation state follows the selected tab.
4. Verify workspace:
   - tabs keep existing order and behavior;
   - fields use consistent height and font;
   - cards/accordion sections do not overlap;
   - footer actions stay aligned.
5. Verify behavior:
   - existing tab switching, loaders, Save/Cancel, `Еще`, block/subscription actions still work;
   - no new data load or save behavior is introduced by visual styles.

## Expected report

On success:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo/Branch/PR/Base: ...
Verified HEAD: <SHA>
HEAD match: PASS
Conflicts: NONE
Checks: PASS
Visual smoke: PASS
Business logic unchanged: PASS
Tomcat errors: NONE
P1/P2: 0
Merge: not performed
Production: not changed
```

On failure use `STATUS: FAILED_VERIFICATION`, include the failed step, root cause,
logs/stack trace, and do not commit, push, merge, or change production.
