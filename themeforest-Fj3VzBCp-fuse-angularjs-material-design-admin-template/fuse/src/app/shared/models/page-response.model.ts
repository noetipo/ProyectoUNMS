export interface PageResponse<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}