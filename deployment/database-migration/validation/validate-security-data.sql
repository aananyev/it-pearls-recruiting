-- Validate restored CUBA security aggregates only.

\pset pager off
\pset tuples_only off
\pset format aligned

select 'sec_user' as table_name, count(*) as row_count from sec_user
union all select 'sec_user_active', count(*) from sec_user where active = true and delete_ts is null
union all select 'sec_user_inactive_or_blocked', count(*) from sec_user where coalesce(active, false) = false and delete_ts is null
union all select 'sec_role', count(*) from sec_role
union all select 'sec_user_role', count(*) from sec_user_role
union all select 'sec_group', count(*) from sec_group
union all select 'sec_permission', count(*) from sec_permission
union all select 'sec_constraint', count(*) from sec_constraint
union all select 'sec_user_setting', count(*) from sec_user_setting
union all select 'sec_remember_me', count(*) from sec_remember_me
union all select 'sec_session_log', count(*) from sec_session_log
union all select 'sys_file', count(*) from sys_file
union all select 'sys_db_changelog', count(*) from sys_db_changelog
order by table_name;

select
    'security_orphan' as check_name,
    'sec_user_role.user_id -> sec_user.id' as relation_name,
    count(*) as orphan_count
from sec_user_role ur
left join sec_user u on u.id = ur.user_id
where u.id is null
union all
select 'security_orphan', 'sec_user_role.role_id -> sec_role.id', count(*)
from sec_user_role ur
left join sec_role r on r.id = ur.role_id
where r.id is null
union all
select 'security_orphan', 'sec_user.group_id -> sec_group.id', count(*)
from sec_user u
left join sec_group g on g.id = u.group_id
where u.group_id is not null and g.id is null
union all
select 'security_orphan', 'sec_permission.role_id -> sec_role.id', count(*)
from sec_permission p
left join sec_role r on r.id = p.role_id
where p.role_id is not null and r.id is null
union all
select 'security_orphan', 'sec_user_setting.user_id -> sec_user.id', count(*)
from sec_user_setting s
left join sec_user u on u.id = s.user_id
where s.user_id is not null and u.id is null;
