SET default_transaction_read_only = on;
SET statement_timeout = '5min';
SET lock_timeout = '5s';
SET idle_in_transaction_session_timeout = '5min';

BEGIN READ ONLY;

-- Template for manual classification after the operator injects missing IDs.
-- Replace the VALUES block with rows: ('<uuid>'::uuid, '<relative-path>').
WITH missing(id, expected_relative_path) AS (
    VALUES
    ('00000000-0000-0000-0000-000000000000'::uuid, 'placeholder/remove-before-run')
),
sys_file_metadata AS (
    SELECT
        m.id,
        m.expected_relative_path,
        sf.create_date,
        sf.ext,
        sf.file_size,
        sf.delete_ts IS NOT NULL AS sys_file_soft_deleted
    FROM missing m
    JOIN sys_file sf ON sf.id = m.id
)
SELECT *
FROM sys_file_metadata
ORDER BY expected_relative_path;

ROLLBACK;
