import { Component, inject, OnInit } from '@angular/core';
import {
  AbstractControl,
  ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { SignupService } from '@/app/providers/services/oauth/signup.service';

function mustMatch(controlName: string, matchingControlName: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const control = group.get(controlName);
    const matchingControl = group.get(matchingControlName);
    if (!control || !matchingControl) return null;
    if (control.value !== matchingControl.value) {
      matchingControl.setErrors({ mustMatch: true });
      return { mustMatch: true };
    }
    matchingControl.setErrors(null);
    return null;
  };
}

@Component({
  selector: 'auth-reset-password',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './reset-password.component.html',
})
export class ResetPasswordComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _router = inject(Router);
  private _route = inject(ActivatedRoute);
  private _authService = inject(SignupService);

  resetPasswordForm!: UntypedFormGroup;
  token = '';
  showPassword = false;
  showConfirmPassword = false;

  ngOnInit(): void {
    this._route.queryParams.subscribe((params) => {
      this.token = params['token'] ?? '';
    });

    this.resetPasswordForm = this._fb.group(
      {
        password: ['', Validators.required],
        passwordConfirm: ['', Validators.required],
      },
      { validators: mustMatch('password', 'passwordConfirm') }
    );
  }

  resetPassword(): void {
    if (this.resetPasswordForm.invalid) return;

    this.resetPasswordForm.disable();

    const newPassword = this.resetPasswordForm.get('password')?.value;
    this._authService.resetPassword(this.token, newPassword).subscribe({
      next: () => {
        this._router.navigateByUrl('/confirm-rest-password');
      },
      error: () => {
        this.resetPasswordForm.enable();
      },
    });
  }
}
