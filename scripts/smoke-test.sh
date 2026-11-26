#!/usr/bin/env sh
# Signs in against a running instance with the demo users and checks the role rules end to end.
set -eu

base="${1:-http://localhost:8080}"
jar="$(mktemp)"
trap 'rm -f "$jar"' EXIT

status() {
  curl -s -o /dev/null -w '%{http_code}' -b "$jar" -c "$jar" "$@"
}

expect() {
  if [ "$1" != "$2" ]; then
    echo "expected $2 but got $1 for $3" >&2
    exit 1
  fi
  echo "ok $2 $3"
}

curl -s -o /dev/null -c "$jar" "$base/secure/menu"
curl -s -o /dev/null -L -b "$jar" -c "$jar" \
  --data-urlencode 'j_username=ventas' --data-urlencode 'j_password=ventas-demo' \
  "$base/secure/j_security_check"

expect "$(status "$base/secure/ventas/facturas")" 200 /secure/ventas/facturas
expect "$(status "$base/secure/nominas/gestion")" 403 /secure/nominas/gestion
expect "$(status "$base/secure/compras/autorizar")" 403 /secure/compras/autorizar
expect "$(status -X POST "$base/secure/salir")" 302 /secure/salir
expect "$(status "$base/secure/ventas/facturas")" 200 "login form after logout"

curl -s -b "$jar" "$base/secure/ventas/facturas" | grep -q j_security_check
echo "ok login form shown after logout"
