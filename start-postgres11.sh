#!/bin/bash
# Start/stop/status PostgreSQL 11 for IT-Pearls (data: /usr/local/var/postgresql@11)
set -euo pipefail

PG11_BIN="/usr/local/opt/postgresql@11/bin"
PGDATA="/usr/local/var/postgresql@11"
LABEL="com.itpearls.postgresql11"
PLIST="$HOME/Library/LaunchAgents/${LABEL}.plist"

export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

cmd="${1:-status}"

case "$cmd" in
  start)
    launchctl bootout "gui/$(id -u)/${LABEL}" 2>/dev/null || true
    launchctl bootstrap "gui/$(id -u)" "$PLIST"
    sleep 2
    "$PG11_BIN/pg_isready" && echo "PostgreSQL 11 is ready on port 5432"
    ;;
  stop)
    launchctl bootout "gui/$(id -u)/${LABEL}" 2>/dev/null || \
      "$PG11_BIN/pg_ctl" -D "$PGDATA" stop -m fast 2>/dev/null || true
    echo "PostgreSQL 11 stopped"
    ;;
  status)
    if "$PG11_BIN/pg_isready" >/dev/null 2>&1; then
      echo "PostgreSQL 11: running"
      "$PG11_BIN/psql" -U cuba -d itpearls -c "SELECT version();" 2>/dev/null || true
    else
      echo "PostgreSQL 11: not running"
      exit 1
    fi
    ;;
  *)
    echo "Usage: $0 {start|stop|status}"
    exit 1
    ;;
esac
