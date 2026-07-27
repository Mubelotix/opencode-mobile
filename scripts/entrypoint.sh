#!/usr/bin/env bash
set -e

/usr/local/bin/helper-server &

exec opencode serve --hostname 0.0.0.0 --port 4096
