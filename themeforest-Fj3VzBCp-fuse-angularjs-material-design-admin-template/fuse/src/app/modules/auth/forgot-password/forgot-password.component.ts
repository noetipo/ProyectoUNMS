import { Component, inject, OnInit } from '@angular/core';
import {
  ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { SignupService } from '@/app/providers/services/oauth/signup.service';

@Component({
  selector: 'auth-forgot-password',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _router = inject(Router);
  private _authService = inject(SignupService);

  forgotPasswordForm!: UntypedFormGroup;

  ngOnInit(): void {
    this.forgotPasswordForm = this._fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  sendResetLink(): void {
    if (this.forgotPasswordForm.invalid) return;

    this.forgotPasswordForm.disable();

    const email = this.forgotPasswordForm.get('email')?.value;
    this._authService.forgotPassword(email).subscribe({
      next: () => {
        this._router.navigateByUrl('/confirmation-forgot');
      },
      error: (err) => {
        console.error(err);
        this.forgotPasswordForm.enable();
      },
    });
  }
}
