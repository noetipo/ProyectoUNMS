import { Component, inject, OnInit, ViewEncapsulation } from '@angular/core';
import {
  ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { SignupService } from '@/app/providers/services/oauth/signup.service';

@Component({
  selector: 'auth-sign-up',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './sign-up.component.html',
})
export class SignUpComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _router = inject(Router);
  private _authService = inject(SignupService);

  signUpForm!: UntypedFormGroup;
  showAlert = false;
  showPassword = false;
  showConfirmPassword = false;
  alert = { type: '', message: '' };

  ngOnInit(): void {
    this.signUpForm = this._fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      ruc: ['', Validators.required],
      companyName: ['', Validators.required],
      password: ['', Validators.required],
      c_password: ['', Validators.required],
      address: ['', Validators.required],
      roles: ['ADMIN'],
    });
  }

  signUp(): void {
    if (this.signUpForm.invalid) return;

    this.signUpForm.disable();
    this.showAlert = false;

    this._authService.signUp(this.signUpForm.value).subscribe({
      next: () => {
        this._router.navigateByUrl('/confirmation-required');
      },
      error: () => {
        this.signUpForm.enable();
        this.signUpForm.reset();
        this.alert = { type: 'error', message: 'Algo salió mal, inténtalo de nuevo.' };
        this.showAlert = true;
      },
    });
  }
}
