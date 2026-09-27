#!/bin/sh
set -eu

validate_identifier() {
    case "$1" in
        ''|*[!a-zA-Z0-9_]*)
            echo "$2 must contain only letters, digits, and underscores" >&2
            exit 1
            ;;
    esac
}

validate_identifier "$USER_POSTGRES_DB" USER_POSTGRES_DB
validate_identifier "$POSTGRES_USER" POSTGRES_USER

export PGPASSWORD="$POSTGRES_PASSWORD"

database_exists=$(psql \
    --host=postgres \
    --username="$POSTGRES_USER" \
    --dbname="$POSTGRES_DB" \
    --no-psqlrc \
    --tuples-only \
    --no-align \
    --command="SELECT 1 FROM pg_database WHERE datname = '$USER_POSTGRES_DB'")

if [ "$database_exists" != "1" ]; then
    psql \
        --host=postgres \
        --username="$POSTGRES_USER" \
        --dbname="$POSTGRES_DB" \
        --no-psqlrc \
        --set=ON_ERROR_STOP=1 \
        --command="CREATE DATABASE \"$USER_POSTGRES_DB\" OWNER \"$POSTGRES_USER\""
fi