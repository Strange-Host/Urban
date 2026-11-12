import { api } from "./api";

// Dropdown/reference lists change rarely, so share one fetch across pages.
const CACHE_TTL_MS = 120000;

const cache = new Map();

function cached(key, loader) {
  const entry = cache.get(key);
  if (entry && entry.expiresAt > Date.now()) {
    return entry.promise;
  }

  const promise = loader().then(
    (value) => {
      cache.set(key, { promise, expiresAt: Date.now() + CACHE_TTL_MS });
      return value;
    },
    (error) => {
      cache.delete(key);
      throw error;
    }
  );

  cache.set(key, { promise, expiresAt: Date.now() + CACHE_TTL_MS });
  return promise;
}

function rows(response) {
  const data = response.data;
  if (Array.isArray(data)) return data;
  return data?.content || [];
}

async function fetchList(path, params) {
  const response = await api.get(path, { params });
  return rows(response);
}

export function loadProducts({ force = false } = {}) {
  if (force) cache.delete("products");
  return cached("products", () =>
    fetchList("/api/v1/products", { page: 0, size: 500, sort: "name,asc" })
  );
}

export function loadCategories({ force = false } = {}) {
  if (force) cache.delete("categories");
  return cached("categories", () =>
    fetchList("/api/v1/category", { page: 0, size: 200, sort: "name,asc" })
  );
}

export function loadStaff({ force = false } = {}) {
  if (force) cache.delete("staff");
  return cached("staff", () =>
    fetchList("/api/v1/auth/staffs", { page: 0, size: 500, sort: "createdAt,desc" })
  );
}

export function invalidateProducts() {
  cache.delete("products");
}

export function invalidateCategories() {
  cache.delete("categories");
}

export function invalidateStaff() {
  cache.delete("staff");
}
