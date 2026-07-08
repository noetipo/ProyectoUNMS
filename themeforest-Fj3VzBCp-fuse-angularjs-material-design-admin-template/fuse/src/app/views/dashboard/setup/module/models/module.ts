import { ParentModule } from '../../parentModule/models/parent-module';

export class Module {
  id?: string;
  title?: string;
  subtitle?: string;
  icon?: string;
  link?: string;
  type?: string;
  moduleOrder?: number;
  status?: boolean;
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string | null;
  parentModuleId?: string;
  parentModule?: ParentModule;
  assigned?: boolean;
  selected?: boolean;
}

export class PaginatedResponse {
  totalPages?: number;
  currentPage?: number;
  content: Module[] = [];
  totalElements?: number;
}

export class ModuleFilter {
  name?: string;
}
