import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Placeholder: forgot-password is out of scope for the current task. */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page">
      <div class="card">
        <h1>Forgot password</h1>
        <p>Password reset is not implemented in this copy of the application.</p>
        <a routerLink="/login">Back to login</a>
      </div>
    </div>
  `,
  styles: [`
    .page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 16px; }
    .card { width: 100%; max-width: 400px; background: #fff; border-radius: 8px; padding: 32px 28px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
    h1 { margin-top: 0; font-size: 22px; }
  `]
})
export class ForgotPasswordComponent {}
