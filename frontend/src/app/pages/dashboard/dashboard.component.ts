import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models/auth.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="card">
        <h1>Welcome{{ profile ? ', ' + profile.fullName : '' }}</h1>
        <ng-container *ngIf="profile">
          <p><strong>Username:</strong> {{ profile.username }}</p>
          <p><strong>Email:</strong> {{ profile.email }}</p>
          <p><strong>Role:</strong> {{ profile.role }}</p>
          <p><strong>Logged in at:</strong> {{ profile.lastLoginAt | date: 'medium' }}</p>
        </ng-container>
        <button class="btn-primary" (click)="logout()">Logout</button>
      </div>
    </div>
  `,
  styles: [`
    .page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 16px; }
    .card { width: 100%; max-width: 480px; background: #fff; border-radius: 8px; padding: 32px 28px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
    h1 { margin-top: 0; font-size: 22px; }
  `]
})
export class DashboardComponent implements OnInit {
  profile?: UserProfile;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.authService.getProfile().subscribe(profile => (this.profile = profile));
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
