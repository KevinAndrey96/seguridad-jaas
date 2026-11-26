#!/usr/bin/env sh
# Prints a PBKDF2 hash for the users file using Tomcat's own digest tool, with the same
# parameters as the CredentialHandler in META-INF/context.xml. The password is read from the
# terminal so it never shows up in the shell history.
set -eu

printf 'Contraseña: ' >&2
stty -echo
read -r password
stty echo
printf '\n' >&2

docker run --rm -e PASSWORD="$password" tomcat:11.0-jre21 sh -c \
  'bin/digest.sh -a PBKDF2WithHmacSHA512 -i 210000 -s 16 -k 256 \
     -h org.apache.catalina.realm.SecretKeyCredentialHandler "$PASSWORD"' \
  | sed 's/^.*://'
