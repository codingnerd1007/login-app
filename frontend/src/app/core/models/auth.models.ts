export interface CaptchaResponse {
  captchaId: string;
  captchaImage: string;
}

export interface PublicKeyResponse {
  publicKey: string; // X.509 SPKI, Base64
}

export interface NonceResponse {
  nonce: string; // single use, goes inside the encrypted value
}

export interface LoginRequest {
  username: string;
  password: string; // RSA-OAEP encrypted "nonce:password", Base64
  captchaId: string;
  captchaText: string;
  browser?: string;
  os?: string;
}

export interface LoginResponse {
  otpRequired: boolean;
  maskedEmail: string;
  otpExpirySeconds: number;
  message: string;
}

export interface OtpVerifyRequest {
  username: string;
  otp: string; // RSA-OAEP encrypted "nonce:otp", Base64
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  username: string;
  fullName: string;
  role: string;
}

export interface UserProfile {
  username: string;
  fullName: string;
  email: string;
  role: string;
  lastLoginAt: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
}
