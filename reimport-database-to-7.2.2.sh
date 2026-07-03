#!/bin/bash
export PSQL = "/usr/local/Cellar/postgresql@11/11.7/bin/psql -U cuba HuntTech"
echo "Drop database HuntTech ..."
echo "DROP DATABASE HuntTech;" | $PSQL
echo "Create empty database ..."
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-HuntTech/init/postgres/10.create-db.sql | $PSQL
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-HuntTech/init/postgres/20.create-db.sql | $PSQL
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-HuntTech/init/postgres/30.create-db.sql | $PSQL
