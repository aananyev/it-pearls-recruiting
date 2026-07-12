-- Applies known post-refactoring performance indexes idempotently.
\set ON_ERROR_STOP on

\i modules/core/db/update/postgres/26/260704-2-updateProjectPerformanceIndexes.sql
\i modules/core/db/update/postgres/26/260704-3-updateJobCandidatePerformanceIndexes.sql
\i modules/core/db/update/postgres/26/260704-4-updateCandidateCvPerformanceIndexes.sql
\i modules/core/db/update/postgres/26/260704-5-updateIteractionPerformanceIndexes.sql
