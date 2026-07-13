# JobCandidateEdit split view verification

Date: 2026-07-13

Branch: `feature/job-candidate-split-view-redesign`

## Checks

- XML syntax check:
  - `xmllint --noout modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml`
  - Result: passed.
- Whitespace check:
  - `git diff --check`
  - Result: passed.
- Web compilation:
  - `GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home ./gradlew --project-cache-dir /private/tmp/hunttech-gradle-project-cache :app-web:compileJava :app-web:compileTestJava --stacktrace`
  - Result: passed.
- JobCandidate editor regression test:
  - `GRADLE_USER_HOME=/private/tmp/hunttech-gradle-user-home ./gradlew --project-cache-dir /private/tmp/hunttech-gradle-project-cache :app-web:test --tests com.company.hunttech.web.screens.jobcandidate.JobCandidateEditPerfTest --stacktrace`
  - Result: passed.

## Notes

- The default Gradle project cache inside the worktree was blocked by local filesystem permissions. Verification used temporary Gradle cache directories under `/private/tmp`.
- The redesign keeps existing screen ids, table actions, XML invokes and controller handlers. The only controller change separates contact-tab initialization from the new social-networks tab initialization.
