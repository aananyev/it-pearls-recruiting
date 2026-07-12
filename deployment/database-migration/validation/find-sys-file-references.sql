SET default_transaction_read_only = on;
SET statement_timeout = '5min';
SET lock_timeout = '5s';
SET idle_in_transaction_session_timeout = '5min';

BEGIN READ ONLY;

-- Lists UUID columns that can be scanned for references to sys_file IDs.
-- The migration operator should use the generated column list with a VALUES
-- block of missing sys_file IDs. This script itself performs no writes.
SELECT
    quote_ident(n.nspname) || '.' || quote_ident(c.relname) AS table_name,
    quote_ident(a.attname) AS column_name,
    EXISTS (
        SELECT 1
        FROM pg_attribute ad
        WHERE ad.attrelid = c.oid
          AND ad.attname = 'delete_ts'
          AND NOT ad.attisdropped
    ) AS has_delete_ts,
    n.nspname || '.' || c.relname || '.' || a.attname AS reference_name
FROM pg_attribute a
JOIN pg_class c ON c.oid = a.attrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE a.atttypid = 'uuid'::regtype
  AND a.attnum > 0
  AND NOT a.attisdropped
  AND c.relkind IN ('r', 'p')
  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND NOT (n.nspname = 'public' AND c.relname = 'sys_file' AND a.attname = 'id')
ORDER BY 1, 2;

ROLLBACK;
