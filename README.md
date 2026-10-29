# Login App

Angular 17 + Spring Boot 3 login with captcha, RSA-encrypted password/OTP and email OTP.

## Login flow

0. `GET /api/auth/public-key` returns `{ publicKey }`, the server's RSA public key (X.509 SPKI, Base64). The login page fetches it once.
   `GET /api/auth/nonce` returns `{ nonce }`, a single-use value. The frontend fetches a fresh one before encrypting each password or OTP (see Replay protection).
1. `GET /api/auth/captcha` returns `{ captchaId, captchaImage }` (a server-generated PNG; the answer is kept in memory for 2 minutes and can be used once).
2. `POST /api/auth/login` with `{ username, password, captchaId, captchaText, browser, os }`.
   - `password` is RSA encrypted on the frontend (`EncryptionService`).
   - The backend checks the captcha first, then the credentials. `LoginRequestDto.getPassword()` decrypts the password (`RSAUtil.decrypt`) and BCrypt matches it against `users.password_hash`.
   - On success a 6-digit OTP is generated and "emailed". `EmailService` is a mock that logs it, so look for `[MOCK EMAIL]` in the backend console.
3. `POST /api/auth/verify-otp` with `{ username, otp }`.
   - `otp` is RSA encrypted; `OtpVerifyRequestDto.getOtp()` decrypts it.
   - The OTP is valid for 5 minutes, with 3 attempts. Returns a JWT, which the frontend stores in `sessionStorage`.
4. `GET /api/user/me` (requires `Authorization: Bearer <jwt>`) backs the dashboard.

Errors come back as `{ timestamp, status, errorCode, message }`, where `errorCode` is one of
`INVALID_CAPTCHA`, `INVALID_CREDENTIALS`, `INVALID_OTP`, `OTP_EXPIRED`, `DECRYPTION_FAILED`, `REPLAY_DETECTED`,
`NONCE_UNAVAILABLE` or `VALIDATION_FAILED`.

## Encryption

The password and OTP are encrypted with **RSA-OAEP (SHA-256, MGF1 SHA-256), 2048-bit key**.

- The frontend gets the public key from `/api/auth/public-key` and encrypts with the browser's
  Web Crypto API (`frontend/src/app/core/services/encryption.service.ts`). Nothing in the frontend
  bundle can decrypt, so finding the public key in the JS is harmless.
- The private key stays on the server, in a PKCS12 keystore loaded at startup
  (`RsaKeyStoreConfig` → `RSAUtil`). The DTO getters (`getPassword()`, `getOtp()`) call `RSAUtil.decrypt`.
- Java must use an explicit `OAEPParameterSpec` with MGF1 SHA-256: the shorthand
  `OAEPWithSHA-256AndMGF1Padding` uses SHA-1 for MGF1 and fails against Web Crypto.
- Web Crypto needs a secure context (HTTPS, or `localhost` in development).
- RSA-2048 OAEP/SHA-256 can encrypt at most 190 bytes. The nonce and separator take 23, so passwords are
  limited to 128 characters (and the frontend checks the encoded size).

### Replay protection

Every encrypted value is `nonce:value`, where the nonce comes from `GET /api/auth/nonce`
(16 random bytes, Base64URL). `RSAUtil.decrypt` splits off the nonce and consumes it via `NonceService`;
an unknown, expired (2 minutes) or already-used nonce is rejected with `400 REPLAY_DETECTED`.
A captured encrypted password or OTP therefore cannot be sent again, even with a freshly solved captcha.

- The DTOs and `AuthService` are unchanged: `getPassword()` / `getOtp()` still just call `RSAUtil.decrypt`.
- Because decrypting consumes the nonce, **call `getPassword()` / `getOtp()` only once per request** and keep the result.
- At most `app.nonce.max-active` (10,000) nonces are outstanding; beyond that `/nonce` returns `503 NONCE_UNAVAILABLE`.
- Nonces are kept in memory, which suits a single server. Several servers would need a shared store (e.g. Redis).

### Keystore

| Setting | Env var | Default |
|---|---|---|
| `app.rsa.keystore.path` | `LOGIN_KEYSTORE_PATH` | `keys/login-keys.p12` |
| `app.rsa.keystore.password` | `LOGIN_KEYSTORE_PASSWORD` | none (required) |
| `app.rsa.keystore.alias` | `LOGIN_KEYSTORE_ALIAS` | `login-rsa` |

Create one with `backend/scripts/generate-keystore.sh [path]`. If `LOGIN_KEYSTORE_PASSWORD` is not set,
the script generates a random password and writes it to `<keystore>.password` (mode 600).
The backend refuses to start if the keystore is missing or the password is wrong.
Keystores and password files are git-ignored; never commit them.

## Run

```bash
# Backend  (http://localhost:8080, H2 console at /h2-console)
cd backend
scripts/generate-keystore.sh                       # first time only
export LOGIN_KEYSTORE_PASSWORD="$(cat keys/login-keys.p12.password)"
mvn spring-boot:run

# Frontend (http://localhost:4200, /api proxied to :8080)
cd frontend && npm install && npm start
```

Demo users: `admin / Admin@123`, `john / John@123`.

## Tests

```bash
cd backend && mvn test
```

The tests create a throwaway keystore with `keytool`, so no keystore is needed to run them.
