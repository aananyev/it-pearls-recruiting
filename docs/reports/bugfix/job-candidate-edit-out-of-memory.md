# JobCandidateEdit OutOfMemoryError investigation

Date: 2026-07-13
Branch: `fix/job-candidate-edit-out-of-memory`
Base commit: `e246a7bc Optimize JobCandidateEdit data loading without functional changes`

## 1. Cause

Opening `JobCandidateEdit` executed `setupSkillBox()` from `onBeforeShow()`.

Actual chain:

```text
JobCandidateEdit open
-> onBeforeShow
-> setupSkillBox
-> getLastCVText
-> load latest CandidateCV with _local view
-> CandidateCV.TEXT_CV LOB is loaded
-> Skillsbar.generateSkillLabels
-> PdfParserService.parseSkillTree + ParseCVService.countMachesSkill
-> large CV text parsed during the Vaadin UI request
-> repeated opens keep doing the same heavy work
-> Java heap pressure / OutOfMemoryError
```

The local logs confirm this path immediately before the OOM:

```text
Skillsbar.generateSkillLabels
JobCandidateEdit.setupSkillBox(JobCandidateEdit.java:351)
JobCandidateEdit.onBeforeShow(JobCandidateEdit.java:621)
...
Caused by: java.lang.OutOfMemoryError: Java heap space
Too long request processing [85182 ms]: /hrm/UIDL/
```

The same screen also had a lifecycle bug: `onAfterClose` and `onBeforeClose` were adding new `jobCandidateCandidateCvsDc` listeners instead of removing anything. This could keep closed screen components reachable longer than necessary.

## 2. Reproduction Conditions

Local environment:

- Java: OpenJDK `22.0.1`
- Local DB: PostgreSQL database `hunttech`
- DB size: `6383 MB`
- Candidates: `11549`
- Candidate CV rows: `8148`
- User: `admin`
- Role: administrator
- Local Tomcat heap from `etc/tomcat-setenv.sh`: `-Xmx1024m`; effective diagnostic GC log showed current local run using a `256M` heap because `JAVA_OPTS` diagnostics were used with the local Gradle/Tomcat launch.

Heavy local data examples:

| Candidate | ID | Related Data |
| --- | --- | ---: |
| Овчинников Иван | `67d497f5-b1a2-33a2-10e8-c6dd67c68dc3` | `TEXT_CV` length about `9.4 MB` |
| Кишинский Олег | `ee1cd239-6454-9b4a-9e87-cd294bf6296c` | `72` interactions, `14` CV rows |
| Никифорова Александра | `2ab70c2d-9a06-1066-aa0a-1fbf4eb19d05` | `71` interactions, `14` CV rows |

## 3. Heap / SQL Notes

`jcmd` attach was blocked by the local macOS/sandbox environment with `Operation not permitted`, so class histogram and heap dump could not be collected through `jcmd` in this session. GC logging and heap-dump-on-OOM were enabled locally under `/private/tmp/hunttech-oom-diagnostics`; no `.hprof` was produced after the fix.

| Metric | Before Fix | After Fix |
| --- | ---: | ---: |
| Heap before opening | not available via `jcmd` | app startup GC log: `13M->5M(256M)` early startup |
| Heap after opening | request reached OOM in existing log | no OOM during startup/smoke test |
| Heap after close and GC | not available | not available via `jcmd` |
| Peak heap | OOM with `Java heap space` | GC log during startup peaked around `223M` used before young GC |
| Full GC | not available | no `Pause Full` found in new GC log |
| Retained heap JobCandidateEdit | not available | not available via `jcmd` |
| SQL queries | latest CV text loaded on initial open | `CandidateCV` list is not loaded on initial open by web test |
| Opening time | existing log request: `85182 ms` before OOM | web test succeeds; local `/hrm/` returns HTTP 200 |

SQL check for the heavy latest CV load:

```sql
explain analyze
select e.id, e.text_cv
from hunttech_candidate_cv e
where e.candidate_id = '67d497f5-b1a2-33a2-10e8-c6dd67c68dc3'
order by e.date_post desc
limit 1;
```

Result: index-backed query, `Execution Time: 0.770 ms`. The problem is not the SQL plan; it is loading and parsing a LOB in the UI open request.

## 4. Changed Files

- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java`
  - Removed eager `setupSkillBox()` call from `onBeforeShow`.
  - Moved skill parsing to first resume-tab initialization.
  - Added `skillBoxInitialized` guard.
  - Removed incorrect listener registration on close.

- `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidateEditPerfTest.java`
  - Added regression test proving `CandidateCV` rows are not list-loaded when the editor opens.

- `modules/web/src/test/java/com/company/hunttech/web/screens/jobcandidate/JobCandidatePerfTestSupport.java`
  - Added metrics for `hunttech_CandidateCV` list loads.

## 5. Key Diff

```java
// before: called on every initial form open
setupSkillBox();

// after: called only when the resume tab is initialized
if (!cvTabInitialized) {
    ensureCandidateCvLoaded();
    setupSkillBox();
}
```

```java
private void setupSkillBox() {
    if (!skillBoxInitialized && !PersistenceHelper.isNew(getEditedEntity())) {
        skillBoxInitialized = true;
        ...
    }
}
```

## 6. Why This Is Safe

- Business data and saving behavior are unchanged.
- No fields, tabs, tables, relations, or actions were removed.
- The skill bar feature remains available, but it is initialized when the resume tab is opened instead of during initial card opening.
- The fix does not increase heap and does not mask the root cause.
- No global fetch type changes were made.
- No N+1 was introduced; the heavy CV load is moved out of the initial lifecycle and remains a single bounded latest-CV operation for the skill parser.
- Incorrect close handlers no longer add extra listeners.

## 7. Test Results

Command:

```bash
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home ./gradlew \
  --project-cache-dir /private/tmp/hunttech-gradle-project-cache-oom \
  :app-web:compileJava :app-web:compileTestJava \
  :app-web:test --tests com.company.hunttech.web.screens.jobcandidate.JobCandidateEditPerfTest --stacktrace
```

Result:

```text
BUILD SUCCESSFUL in 33s
```

Local deploy:

```text
deploy start: BUILD SUCCESSFUL in 3m 15s
local Tomcat PID: 55126
HTTP /hrm/: 200
```

Fresh-start logs after the clean restart contain no new `OutOfMemoryError`, `Java heap space`, `Cannot get unfetched attribute`, or `LazyInitializationException`.

## 8. Test Server

Not deployed in this session. No test-server host, SSH target, artifact path, or credentials were provided in the task context. Production was not changed.

## 9. JVM Recommendation

Do not change `-Xmx` as the main fix. The confirmed issue was eager LOB parsing in `JobCandidateEdit`.

Current local Tomcat config:

```text
CATALINA_OPTS="-Xmx1024m -Dfile.encoding=UTF-8 -Dapp.home=\"$APP_HOME\""
```

If the test server still uses a very small heap after this fix, tune it separately after collecting server RAM, PostgreSQL memory usage, current `-Xms`, current `-Xmx`, and post-fix GC logs.

## 10. Additional Risks

- `JobCandidateBrowse`: previously fixed separately for unfetched `EmployeeWorkStatus.inStaff`; also has similar detached-view risk around `candidateCv` in old logs.
- `OpenPositionEdit`: broad `openPosition-edit-view` includes nested collections and should be profiled separately before change.
- `EmployeeEdit`: may have similar generated-column/view risks where status/reference fields are accessed after detach.

## 11. Rollback

Local rollback:

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech-fix-jobcandidate-oom
deploy/tomcat/bin/shutdown.sh
git revert <commit>
GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home ./gradlew \
  --project-cache-dir /private/tmp/hunttech-gradle-project-cache-oom \
  deploy start
curl -I http://localhost:8080/hrm/
```

Server rollback:

```text
1. Stop Tomcat.
2. Restore the previous deployable artifact.
3. Restore previous configuration only if configuration was changed.
4. Clean only standard Tomcat temp/work directories.
5. Start Tomcat.
6. Check logs.
7. Check login.
8. Open JobCandidateBrowse and a candidate card.
```

Do not delete file storage or user data.
