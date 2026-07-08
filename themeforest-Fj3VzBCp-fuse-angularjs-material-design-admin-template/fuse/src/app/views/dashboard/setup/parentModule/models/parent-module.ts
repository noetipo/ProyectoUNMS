import { Module } from '../../module/models/module';

export class ParentModule {
  id?: string;
  title?: string;
  icon?: string;
  link?: string;
  moduleOrder?: number;
  status?: boolean;
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string | null;
  moduleDTOS?: Module[];
}

export class PaginatedResponse {
  totalPages?: number;
  currentPage?: number;
  content: ParentModule[] = [];
  totalElements?: number;
}

export class ParentModuleFilter {
  name?: string;
}
