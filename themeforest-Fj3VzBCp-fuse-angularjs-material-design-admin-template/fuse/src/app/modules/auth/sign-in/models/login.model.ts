export interface LoginResponse {
  success: boolean;
  message: string;
  data: AuthData;
  error: string | null;
  timestamp: string;
}

export interface AuthData {
  accessToken?: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];      // nombres de rol (para mostrar)
  roleCodes: string[];  // códigos de rol (para autorización/guards)
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  lastLogin: string;
}

export type UserRole = 'ADMIN' | 'MANAGER' | 'USER';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
