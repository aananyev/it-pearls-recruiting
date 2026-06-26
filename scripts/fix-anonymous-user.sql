-- Restore system users required for CUBA startup (anonymous session) and admin login.
-- Root cause: soft-deleted rows (delete_ts IS NOT NULL) are excluded by JPA @SoftDelete on sec$User.
UPDATE sec_user
SET delete_ts = NULL,
    deleted_by = NULL
WHERE login_lc IN ('anonymous', 'admin')
  AND delete_ts IS NOT NULL;
