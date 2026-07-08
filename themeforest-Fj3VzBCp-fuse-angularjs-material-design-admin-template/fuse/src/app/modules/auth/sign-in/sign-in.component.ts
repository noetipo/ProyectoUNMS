import {
  Component,
  inject,
  OnInit,
  ViewEncapsulation,
} from '@angular/core';
import {
  ReactiveFormsModule,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { OauthService } from '@/app/providers/services/oauth/oauth.service';
import { environment } from '@/environments/environment';

@Component({
  selector: 'auth-login',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './sign-in.component.html',
})
export class SignInComponent implements OnInit {
  private _fb = inject(UntypedFormBuilder);
  private _router = inject(Router);
  private _oauthService = inject(OauthService);

  signInForm!: UntypedFormGroup;
  showAlert = false;
  showPassword = false;
  alert = { type: '', message: '' };
  readonly year = new Date().getFullYear();

  /** SOLO DESARROLLO: panel de usuarios de prueba (oculto y ausente en prod). */
  protected readonly showDevCredentials = !environment.production && environment.showDevCredentials;
  protected readonly devCredentials = environment.devCredentials;
  protected devPanelOpen = false;

  ngOnInit(): void {
    this.signInForm = this._fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
      rememberMe: [''],
    });
  }

  /** SOLO DESARROLLO: autocompleta las credenciales e inicia sesión. */
  usar(cred: { username: string; password: string }): void {
    if (this.signInForm.disabled) return;
    this.signInForm.patchValue({ username: cred.username, password: cred.password });
    this.signIn();
  }

  signIn(): void {
    if (this.signInForm.invalid) return;

    this.signInForm.disable();
    this.showAlert = false;

    this._oauthService.authenticate(this.signInForm.value).subscribe({
      next: () => {
        this._router.navigateByUrl('/admin/dashboards');
      },
      error: () => {
        this.signInForm.enable();
        this.signInForm.reset();
        this.alert = { type: 'error', message: 'Contraseña o correo incorrecto' };
        this.showAlert = true;
      },
    });
  }
}
