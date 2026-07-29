# Hermes verification — JobCandidateEdit layout regression

PROJECT: HRM HuntTech
REPOSITORY: aananyev/it-pearls-recruiting
BRANCH: agent/job-candidate-edit-layout-regression
BASE: master
MODE: verification only, no code changes

## Scope and strict guard

Verify only the local SCSS correction for `hunttech_JobCandidate.edit`.

Business logic is unchanged. Do not change Java, XML bindings, entity fields, loaders,
views, JPQL, actions, `invoke`, validators, required/editable/visible contracts, database,
Tomcat configuration, deploy scripts, or production. The PR must contain only local
`job-candidate-*` styles, UI documentation, and this instruction.

## Required HEAD checks

1. Confirm the local branch exists and equals the PR HEAD SHA.
2. Confirm the PR is open from `agent/job-candidate-edit-layout-regression` to `master`.
3. If the SHA in the PR body differs from the checked-out branch, stop with `HEAD_MISMATCH`.
4. Confirm there are no merge conflicts.

## Commands

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## Visual smoke

1. Open the local `/hrm/` and log in as `alan` / `Dodo-2012`.
2. Open an existing JobCandidate edit screen and the `Основное` tab.
3. At a compact desktop width (1280px) verify:
   - labels `Отчество`, `Фамилия`, `Должность`, `Компания` remain whole words;
   - the input control starts after the label and does not overlap it;
   - the label column is compact enough that values and picker controls remain readable;
   - cards, sidebar and tabs remain inside their parent bounds.
4. At a wide desktop width verify the standard 118px label column and aligned fields.
5. Confirm no field value, action, tab switch, loader, Save/Cancel behavior changed.

## Expected report

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
