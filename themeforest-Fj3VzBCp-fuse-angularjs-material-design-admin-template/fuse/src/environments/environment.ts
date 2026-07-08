import { DEV_CREDENTIALS } from './dev-credentials';

export const environment = {
  production: false,
  url: 'http://localhost:8080/',
  // SOLO DESARROLLO: muestra el panel de usuarios de prueba en el login.
  showDevCredentials: true,
  devCredentials: DEV_CREDENTIALS,
};
