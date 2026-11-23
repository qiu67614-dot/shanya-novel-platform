#!/usr/bin/env bash
set -euo pipefail
umask 077

backup_dir=/var/backups/novel
stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$backup_dir/novel-$stamp.sql.gz"
partial_file="$backup_file.partial"
mkdir -p "$backup_dir"
trap 'rm -f "$partial_file"' EXIT

mysqldump --defaults-extra-file=/etc/novel/mysql-backup.cnf \
  --single-transaction --no-tablespaces --set-gtid-purged=OFF novel | gzip -9 > "$partial_file"
mv "$partial_file" "$backup_file"
test -s "$backup_file"
ossutil cp "$backup_file" "$NOVEL_BACKUP_OSS_URI/$(basename "$backup_file")"
