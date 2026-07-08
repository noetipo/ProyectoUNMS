// `import type` se borra en compilación: aporta solo el tipo, NO incluye la lista
// de credenciales (ni sus contraseñas) en el bundle de producción.
import type { DevCredential } from './dev-credentials';

export const environment = {
  production: true,
  url: '[URL_PRODUCCION]',
  // En producción NO se exponen credenciales de prueba (lista vacía, panel oculto).
  showDevCredentials: false,
  devCredentials: [] as DevCredential[],
};
