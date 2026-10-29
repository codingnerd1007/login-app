# Login App

Angular 17 + Spring Boot 3 login with captcha, AES-encrypted credentials and email OTP.

## Login flow

1. `GET /api/auth/captcha` returns `{ captchaId, captchaImage }` (a server-generated PNG; the answer is kept in memory for 2 minutes and can be used once).
2. `POST /api/auth/login` with `{ username, password, captchaId, captchaText, browser, os }`.
   - `password` is AES encrypted on the frontend (`EncryptionService`).
   - The backend checks the captcha first, then the credentials. `LoginRequestDto.getPassword()` decrypts the password (`AESUtil.decrypt`) and BCrypt matches it against `users.password_hash`.
   - On success a 6-digit OTP is generated and "emailed". `EmailService` is a mock that logs it, so look for `[MOCK EMAIL]` in the backend console.
3. `POST /api/auth/verify-otp` with `{ username, otp }`.
   - `otp` is AES encrypted; `OtpVerifyRequestDto.getOtp()` decrypts it.
   - The OTP is valid for 5 minutes, with 3 attempts. Returns a JWT, which the frontend stores in `sessionStorage`.
4. `GET /api/user/me` (requires `Authorization: Bearer <jwt>`) backs the dashboard.

Errors come back as `{ timestamp, status, errorCode, message }`, where `errorCode` is one of
`INVALID_CAPTCHA`, `INVALID_CREDENTIALS`, `INVALID_OTP`, `OTP_EXPIRED`, `DECRYPTION_FAILED` or `VALIDATION_FAILED`.

## Encryption

AES-128/CBC/PKCS5 (Java) = CryptoJS AES CBC/Pkcs7, with a fixed key and IV shared by both sides:

- Backend: `backend/src/main/java/com/example/loginapp/util/AESUtil.java`
- Frontend: `frontend/src/environments/environment.ts` (`aesKey`, `aesIv`)

## Run

```bash
# Backend  (http://localhost:8080, H2 console at /h2-console)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:4200, /api proxied to :8080)
cd frontend && npm install && npm start
```

Demo users: `admin / Admin@123`, `john / John@123`.

## Tests

```bash
cd backend && mvn test
```
