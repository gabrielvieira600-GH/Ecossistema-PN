#!/bin/sh
set -eu
# Build a JDBC URL from Render's independent database properties.
if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${DATABASE_HOST:-}" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_HOST}:5432/${DATABASE_NAME:-feiras}"
fi
if [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  echo 'Configure DATABASE_HOST or SPRING_DATASOURCE_URL, plus the database user and password.' >&2
  exit 1
fi
exec java -jar /app/app.jar
