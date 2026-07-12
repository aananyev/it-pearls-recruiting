# Production Liquibase and CUBA update inventory

Дата: 2026-07-10

## Production history table

Production uses CUBA `sys_db_changelog`.

Total records:

- `sys_db_changelog`: 995

Latest observed scripts:

- `70-IT-Pearls/update/postgres/26/260701-2-updateVacancyPromptTemplate01.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateProject_DropScript.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateProject.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updatePerson_DropScript.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updatePerson.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateOpenPosition_DropScript.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateOpenPosition.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateJobCandidate_DropScript.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateJobCandidate.sql`
- `70-IT-Pearls/update/postgres/26/260701-2-updateIteractionList_DropScript.sql`

## AI changeset status

AI-related structures from local project are already present on production:

- `itpearls_user_ai_configuration`
- `itpearls_vacancy_prompt_template`
- `itpearls_open_position.raw_description`

## Production application risk

Production `WEB-INF/local.app.properties` has:

- `cuba.automaticDatabaseUpdate=true`

Before any future deployment or datasource switch, this must be explicitly addressed. Automatic DB update on production can apply update scripts at application startup.
