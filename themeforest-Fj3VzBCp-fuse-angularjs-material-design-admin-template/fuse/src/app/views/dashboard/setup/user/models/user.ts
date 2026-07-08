export class User {
  self?: any;
  id?: string;
  userId?: string | null;
  origin?: any;
  createdTimestamp?: number;
  username?: string;
  enabled?: boolean;
  totp?: boolean;
  emailVerified?: boolean;
  firstName?: string;
  lastName?: string;
  email?: string;
  // Campos del reporte (JOIN con persona + roles)
  status?: string;
  active?: boolean;
  personaId?: string | null;
  personaNombre?: string | null;
  numeroDocumento?: string | null;
  roles?: string[];
  federationLink?: any;
  serviceAccountClientId?: any;
  attributes?: any;
  credentials?: any;
  disableableCredentialTypes?: string[];
  requiredActions?: string[];
  federatedIdentities?: any;
  realmRoles?: any;
  clientRoles?: any;
  clientConsents?: any;
  notBefore?: number;
  applicationRoles?: any;
  socialLinks?: any;
  groups?: any;
  access?: Access;
}

export class Access {
  manageGroupMembership!: boolean;
  view!: boolean;
  mapRoles!: boolean;
  impersonate!: boolean;
  manage!: boolean;
}
