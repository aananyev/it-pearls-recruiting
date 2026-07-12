-- Transforms a restored copy from legacy itpearls-prefixed object names
-- to hunttech-prefixed object names. Run only against target copy.
\set ON_ERROR_STOP on

begin;

create table if not exists public.hunttech_migration_run_log (
    id bigserial primary key,
    migration_id text not null,
    step text not null,
    details text,
    created_at timestamp not null default now()
);

insert into public.hunttech_migration_run_log(migration_id, step, details)
values (:'migration_id', 'start_transform', current_database());

create table if not exists public.hunttech_migration_quarantine (
    id bigserial primary key,
    migration_id text not null,
    issue_code text not null,
    source_table text not null,
    source_id uuid,
    source_column text,
    source_value text,
    resolution text not null,
    created_at timestamp not null default now()
);

do $$
declare
  cnt bigint;
begin
  if current_database() = 'itpearls' then
    raise exception 'Refusing to transform database named itpearls';
  end if;

  execute 'select count(*) from public.itpearls_job_candidate_position_link__u59616' into cnt;
  if cnt <> 0 then
    raise exception 'Legacy table itpearls_job_candidate_position_link__u59616 has % rows and requires approved mapping', cnt;
  end if;

  execute 'select count(*) from public.itpearls_open_position_city_link__u70664' into cnt;
  if cnt <> 0 then
    raise exception 'Legacy table itpearls_open_position_city_link__u70664 has % rows and requires approved mapping', cnt;
  end if;
end
$$;

insert into public.hunttech_migration_quarantine(
    migration_id,
    issue_code,
    source_table,
    source_id,
    source_column,
    source_value,
    resolution
)
select
    :'migration_id',
    'orphan_current_company_id',
    'itpearls_job_candidate',
    jc.id,
    'current_company_id',
    jc.current_company_id::text,
    'created_placeholder_company_with_same_uuid'
from public.itpearls_job_candidate jc
left join public.itpearls_company c on c.id = jc.current_company_id
where jc.current_company_id is not null
  and c.id is null;

insert into public.itpearls_company(
    id,
    version,
    create_ts,
    created_by,
    our_client,
    our_legal_entity,
    comany_name,
    company_short_name,
    company_description
)
select
    missing.company_id,
    1,
    now(),
    'migration',
    false,
    false,
    left('MIGRATION_PLACEHOLDER_MISSING_COMPANY_' || missing.company_id::text, 80),
    left('MISSING_COMPANY_' || missing.company_id::text, 80),
    'Created during local migration to preserve existing itpearls_job_candidate.current_company_id references. Original referenced company row was absent in source database.'
from (
    select distinct jc.current_company_id as company_id
    from public.itpearls_job_candidate jc
    left join public.itpearls_company c on c.id = jc.current_company_id
    where jc.current_company_id is not null
      and c.id is null
) missing;

do $$
declare
  cnt bigint;
begin
  select count(*) into cnt
  from public.itpearls_job_candidate jc
  left join public.itpearls_company c on c.id = jc.current_company_id
  where jc.current_company_id is not null
    and c.id is null;

  if cnt <> 0 then
    raise exception 'current_company_id orphan references remain after placeholder creation: %', cnt;
  end if;
end
$$;

drop table if exists public.itpearls_job_candidate_position_link__u59616 cascade;
drop table if exists public.itpearls_open_position_city_link__u70664 cascade;

do $$
declare
  r record;
  new_name text;
begin
  for r in
    select n.nspname, c.relname, c.relkind
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relname like 'itpearls\_%' escape '\'
      and c.relkind in ('r','p','S','v','m','i')
    order by case c.relkind when 'i' then 2 else 1 end, c.relname
  loop
    new_name := regexp_replace(r.relname, '^itpearls_', 'hunttech_');
    if to_regclass(format('%I.%I', r.nspname, new_name)) is null then
      execute format(
        'alter %s %I.%I rename to %I',
        case r.relkind
          when 'S' then 'sequence'
          when 'v' then 'view'
          when 'm' then 'materialized view'
          when 'i' then 'index'
          else 'table'
        end,
        r.nspname,
        r.relname,
        new_name
      );
    else
      raise exception 'Target object already exists: %.%', r.nspname, new_name;
    end if;
  end loop;
end
$$;

do $$
declare
  r record;
  new_name text;
begin
  for r in
    select n.nspname, c.relname
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relkind = 'i'
      and c.relname like '%itpearls%'
    order by c.relname
  loop
    new_name := replace(r.relname, 'itpearls', 'hunttech');
    if to_regclass(format('%I.%I', r.nspname, new_name)) is null then
      execute format('alter index %I.%I rename to %I', r.nspname, r.relname, new_name);
    else
      raise exception 'Target index already exists while renaming %.% -> %', r.nspname, r.relname, new_name;
    end if;
  end loop;
end
$$;

do $$
declare
  r record;
  new_name text;
begin
  for r in
    select conrelid::regclass as table_name, conname
    from pg_constraint
    where connamespace = 'public'::regnamespace
      and conname like '%itpearls%'
  loop
    new_name := replace(r.conname, 'itpearls', 'hunttech');
    execute format('alter table %s rename constraint %I to %I', r.table_name, r.conname, new_name);
  end loop;
end
$$;

alter table if exists public.hunttech_vacancy_prompt_template
  alter column temperature set default 0.7;

update public.sec_permission
set target = replace(target, 'itpearls', 'hunttech')
where target like '%itpearls%';

update public.sec_user_setting
set name = replace(name, 'itpearls', 'hunttech')
where name like '%itpearls%';

update public.sec_user
set dtype = replace(dtype, 'itpearls', 'hunttech')
where dtype like '%itpearls%';

update public.sec_filter
set
  component = replace(component, 'itpearls', 'hunttech'),
  xml = replace(xml, 'itpearls', 'hunttech')
where component like '%itpearls%'
   or xml like '%itpearls%';

update public.sec_presentation
set
  component = replace(component, 'itpearls', 'hunttech'),
  xml = replace(xml, 'itpearls', 'hunttech')
where component like '%itpearls%'
   or xml like '%itpearls%';

update public.dashboard_persistent_dashboard
set dashboard_model = replace(dashboard_model, 'itpearls', 'hunttech')
where dashboard_model like '%itpearls%';

update public.dashboard_widget_template
set widget_model = replace(widget_model, 'itpearls', 'hunttech')
where widget_model like '%itpearls%';

update public.sys_config
set
  name = replace(name, 'itpearls', 'hunttech'),
  value_ = replace(value_, 'itpearls', 'hunttech')
where name like '%itpearls%'
   or value_ like '%itpearls%';

insert into public.hunttech_migration_run_log(migration_id, step, details)
values (:'migration_id', 'finish_transform', current_database());

commit;
