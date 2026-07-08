import { Module } from '../../module/models/module';

export class Role {
  id?: string;
  name?: string;
  description?: string;
  status?: boolean;
  modules?: Module[];
  deleted?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  deletedAt?: Date | null;
  selected?: boolean;
}

export class PaginatedResponseRole {
  totalPages?: number;
  currentPage?: number;
  content: Role[] = [];
  totalElements?: number;
}

export class RoleFilter {
  name?: string;
}