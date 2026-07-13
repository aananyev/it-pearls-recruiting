# JobCandidateBrowse EmployeeWorkStatus.inStaff unfetched fix

Date: 2026-07-13

Branch: `fix/job-candidate-browse-unfetched-instaff`

Base commit: `e246a7bcf7d4a231cbac3b327e498b672406d981`

## 1. Confirmed Cause

- Detached object: `EmployeeWorkStatus`.
- Unfetched attribute: `inStaff`.
- Access point: `JobCandidateBrowse.setEployeeStatusIcon(...)`.
- The status column does not receive `EmployeeWorkStatus` from the `JobCandidate` loader. It uses the page-level batch cache `refreshEmployeeCache(...)`.
- Before the fix, `refreshEmployeeCache(...)` loaded `Employee.workStatus` with `_minimal`, which includes the name pattern field but not `inStaff`.

## 2. Error Chain

```text
jobCandidatesDl
-> JobCandidate page
-> refreshEmployeeCache(candidates)
-> dataManager.load(Employee)
-> Employee view with workStatus _minimal
-> detached Employee / EmployeeWorkStatus
-> jobCandidatesTable.status generated column
-> setEployeeStatusIcon(...)
-> employee.getWorkStatus().getInStaff()
-> IllegalStateException
```

## 3. Changed Files

- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateBrowse.java`
  - Added a narrow `EMPLOYEE_STATUS_CACHE_VIEW`.
  - Reused it in `refreshEmployeeCache(...)`.
  - The nested `workStatus` view now loads `workStatusName` and `inStaff`.
- `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateBrowsePerfTest.java`
  - Added a regression assertion that the status-cache view contains `workStatus.inStaff`.

## 4. Key Diff

```java
static final View EMPLOYEE_STATUS_CACHE_VIEW = ViewBuilder.of(Employee.class)
        .add("jobCandidate", "_minimal")
        .add("workStatus", view -> view
                .add("workStatusName")
                .add("inStaff"))
        .build();
```

```java
.view(EMPLOYEE_STATUS_CACHE_VIEW)
```

## 5. Why This Fix

- It changes the fetch plan at the same place where the detached `EmployeeWorkStatus` is loaded.
- It keeps the existing batch query and therefore does not add N+1 queries from the column generator.
- It does not change business logic or the `Boolean.TRUE/false/null` behavior of `setEployeeStatusIcon(...)`.
- It does not change entity mappings to `EAGER`.
- It does not reload entities inside the generated column.

## 6. Local Verification

- Local PostgreSQL 11 was running.
- Local app was deployed from this branch and started on `http://localhost:8080/hrm/`.
- Login verified with user `admin`.
- Local DB contains employees with `EmployeeWorkStatus.in_staff = true` and `null`, so the status-column scenario exists locally.
- Log scan in the fixed branch showed no `EmployeeWorkStatus.getInStaff` unfetched exception.

SQL data check:

```sql
select e.id, e.job_candidate_id, s.work_status_name, s.in_staff
from hunttech_employee e
left join hunttech_employee_work_status s on s.id = e.work_status_id
limit 10;
```

Manual limitation:
- Browser automation logged in successfully but did not reliably activate the Vaadin side-menu item for `JobCandidateBrowse`; direct route hash also stayed on `#main`.
- Verification therefore relies on the exact fetch-plan regression test, successful local deployment, and log scan.

## 7. Automated Tests

Command:

```bash
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home \
./gradlew --project-cache-dir /private/tmp/hunttech-gradle-project-cache-instaff \
  :app-web:compileJava :app-web:compileTestJava \
  :app-web:test --tests com.company.hunttech.web.screens.jobcandidate.JobCandidateBrowsePerfTest --stacktrace
```

Result: `BUILD SUCCESSFUL`.

Added test:
- `testEmployeeStatusCacheViewLoadsInStaff`

## 8. Test Server

Not deployed to a test server in this run.

Reason:
- The project contains production deployment scripts, but no explicit test-server target or credentials were provided in the task context.
- No production deployment was attempted.

Local deploy command used:

```bash
APP_HOME=/Users/alekseyananyev/StudioProjects/hunttech_recruiting/deploy/app_home \
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home \
./gradlew --project-cache-dir /private/tmp/hunttech-gradle-project-cache-instaff restart --stacktrace
```

The clean worktree initially lacked a full local Tomcat installation, so `setupTomcat start` was run afterwards.

## 9. Performance

- Before: one batch query for employees per loaded candidate page, but `workStatus` was too narrow.
- After: still one batch query for employees per loaded candidate page.
- No database access was added to `jobCandidatesTableStatusColumnGenerator(...)`.
- Added data volume is one scalar column, `in_staff`, plus the already-used status name.

## 10. Potential Similar Risks

- `JobCandidateBrowse.java`: accesses `candidateCv` in UI logic. During local log scan, a separate unfetched exception was present:
  - property: `JobCandidate.candidateCv`
  - log: `Cannot get unfetched attribute [candidateCv] from detached object`
  - recommended separate fix: audit the loader/view for the screen path that opens or binds candidate CV data.
- Other generated-column reads that should remain under watch:
  - `iteractionList`
  - `socialNetwork.socialNetworkURL.logo`
  - `positionList.positionList.positionRuName`
  - `iteractionList.recrutier.name`

These were not changed because they are outside the current `EmployeeWorkStatus.inStaff` failure.

## 11. Rollback

Local rollback:

```bash
git switch hunttech-main
./gradlew restart
```

Git rollback of this fix:

```bash
git revert <fix_commit_sha>
```
