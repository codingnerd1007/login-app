import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  CaptchaResponse,
  LoginRequest,
  LoginResponse,
  NonceResponse,
  OtpVerifyRequest,
  PublicKeyResponse,
  UserProfile
} from '../models/auth.models';

const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  getCaptcha(): Observable<CaptchaResponse> {
    return this.http.get<CaptchaResponse>(`${this.authUrl}/captcha`);
  }

  getPublicKey(): Observable<PublicKeyResponse> {
    return this.http.get<PublicKeyResponse>(`${this.authUrl}/public-key`);
  }

  getNonce(): Observable<NonceResponse> {
    return this.http.get<NonceResponse>(`${this.authUrl}/nonce`);
  }

  /** Step 1: captcha + credentials. Password must already be encrypted. */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, request);
  }

  /** Step 2: OTP. OTP must already be encrypted. Stores the JWT on success. */
  verifyOtp(request: OtpVerifyRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/verify-otp`, request).pipe(
      tap(response => {
        sessionStorage.setItem(TOKEN_KEY, response.token);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
          username: response.username,
          fullName: response.fullName,
          role: response.role
        }));
      })
    );
  }

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${environment.apiUrl}/user/me`);
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  }
}
