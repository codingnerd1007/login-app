import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { EncryptionService } from '../../core/services/encryption.service';
import { ErrorResponse } from '../../core/models/auth.models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  otpForm: FormGroup;

  captchaId = '';
  captchaImage = '';

  otpSent = false;
  maskedEmail = '';
  otpSecondsLeft = 0;

  loading = false;
  errorMessage = '';
  infoMessage = '';

  private otpTimer?: ReturnType<typeof setInterval>;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private encryptionService: EncryptionService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      captchaText: ['', [Validators.required]]
    });
    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadCaptcha();
  }

  ngOnDestroy(): void {
    this.stopOtpTimer();
  }

  loadCaptcha(): void {
    this.loginForm.get('captchaText')?.reset('');
    this.authService.getCaptcha().subscribe({
      next: res => {
        this.captchaId = res.captchaId;
        this.captchaImage = res.captchaImage;
      },
      error: () => (this.errorMessage = 'Unable to load captcha. Please refresh the page.')
    });
  }

  /** Step 1: captcha + username/password. */
  onLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const { username, password, captchaText } = this.loginForm.value;
    this.errorMessage = '';
    this.loading = true;

    this.authService.login({
      username: username.trim(),
      password: this.encryptionService.encrypt(password),
      captchaId: this.captchaId,
      captchaText: captchaText.trim(),
      browser: navigator.userAgent,
      os: navigator.platform
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: res => {
          this.otpSent = res.otpRequired;
          this.maskedEmail = res.maskedEmail;
          this.infoMessage = res.message;
          this.loginForm.disable();
          this.startOtpTimer(res.otpExpirySeconds);
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage = this.extractError(err);
          // A captcha can only be used once, so always get a new one after a failed attempt
          this.loadCaptcha();
        }
      });
  }

  /** Step 2: OTP. */
  onVerifyOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }
    this.errorMessage = '';
    this.loading = true;

    this.authService.verifyOtp({
      username: this.loginForm.getRawValue().username.trim(),
      otp: this.encryptionService.encrypt(this.otpForm.value.otp)
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.stopOtpTimer();
          this.router.navigate(['/dashboard']);
        },
        error: (err: HttpErrorResponse) => {
          const error = err.error as ErrorResponse | null;
          if (error?.errorCode === 'OTP_EXPIRED') {
            this.resetToLogin();
          } else {
            this.otpForm.reset();
          }
          this.errorMessage = this.extractError(err);
        }
      });
  }

  resetToLogin(): void {
    this.stopOtpTimer();
    this.otpSent = false;
    this.infoMessage = '';
    this.errorMessage = '';
    this.otpForm.reset();
    this.loginForm.enable();
    this.loginForm.get('password')?.reset('');
    this.loadCaptcha();
  }

  get otpTimeLeft(): string {
    const m = Math.floor(this.otpSecondsLeft / 60);
    const s = this.otpSecondsLeft % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private startOtpTimer(seconds: number): void {
    this.stopOtpTimer();
    this.otpSecondsLeft = seconds;
    this.otpTimer = setInterval(() => {
      this.otpSecondsLeft--;
      if (this.otpSecondsLeft <= 0) {
        this.resetToLogin();
        this.errorMessage = 'OTP has expired. Please login again.';
      }
    }, 1000);
  }

  private stopOtpTimer(): void {
    if (this.otpTimer) {
      clearInterval(this.otpTimer);
      this.otpTimer = undefined;
    }
  }

  private extractError(err: HttpErrorResponse): string {
    const error = err.error as ErrorResponse | null;
    if (error?.message) {
      return error.message;
    }
    return err.status === 0 ? 'Unable to reach the server.' : 'Something went wrong. Please try again.';
  }
}
