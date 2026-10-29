#!/usr/bin/env bash
# Creates the PKCS12 keystore holding the RSA-2048 key pair used to encrypt the password and OTP.
#
# Usage: scripts/generate-keystore.sh [keystore-path]     (default: keys/login-keys.p12)
#
# The keystore password is taken from LOGIN_KEYSTORE_PASSWORD. If that is not set, a random
# password is generated and written next to the keystore as <keystore>.password (mode 600).
# Never commit the keystore or its password.
set -euo pipefail

KEYSTORE="${1:-keys/login-keys.p12}"
ALIAS="${LOGIN_KEYSTORE_ALIAS:-login-rsa}"

if [[ -e "$KEYSTORE" ]]; then
  echo "Keystore already exists at $KEYSTORE; remove it first to create a new key." >&2
  exit 1
fi

mkdir -p "$(dirname "$KEYSTORE")"

if [[ -z "${LOGIN_KEYSTORE_PASSWORD:-}" ]]; then
  export LOGIN_KEYSTORE_PASSWORD="$(openssl rand -base64 24)"
  (umask 077 && printf '%s\n' "$LOGIN_KEYSTORE_PASSWORD" > "$KEYSTORE.password")
  echo "Generated keystore password in $KEYSTORE.password"
fi

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 3650 \
  -dname "CN=login-app" \
  -storetype PKCS12 \
  -keystore "$KEYSTORE" \
  -storepass:env LOGIN_KEYSTORE_PASSWORD

chmod 600 "$KEYSTORE"
echo "Created $KEYSTORE with key alias '$ALIAS'"
