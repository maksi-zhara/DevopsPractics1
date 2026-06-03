#!/bin/sh
set -eu

API_BASE_URL="${API_BASE_URL:-/api}"

cat > /app/config.js <<EOF
window.APP_CONFIG = {
  API_BASE_URL: "${API_BASE_URL}"
};
EOF

exec python -m http.server 80 --directory /app
