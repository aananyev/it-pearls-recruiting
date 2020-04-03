#!/bin/bash
export PSQL = "/usr/local/Cellar/postgresql@11/11.7/bin/psql -U cuba itpearls"
echo "Drop database itpearls ..."
echo "DROP DATABASE itpearls;" | $PSQL
echo "Create empty database ..."
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-IT-Pearls/init/postgres/10.create-db.sql | $PSQL
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-IT-Pearls/init/postgres/20.create-db.sql | $PSQL
cat ./deploy/tomcat/webapps/app-core/WEB-INF/db/70-IT-Pearls/init/postgres/30.create-db.sql | $PSQL
