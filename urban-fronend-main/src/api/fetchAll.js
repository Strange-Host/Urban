import { api } from "./api";

// Reads every page of a paged endpoint, so lists and totals do not silently stop at the first
// page. Stops at the last page, or after `maxPages` as a safety net against runaway loops.
export async function fetchAllPages(path, params = {}, { pageSize = 200, maxPages = 50, signal } = {}) {
  const rows = [];

  for (let page = 0; page < maxPages; page += 1) {
    const response = await api.get(path, { params: { ...params, page, size: pageSize }, signal });
    const data = response.data;

    if (Array.isArray(data)) return data;

    rows.push(...(data?.content || []));

    const totalPages = data?.totalPages;
    if (data?.last === true || (typeof totalPages === "number" && page + 1 >= totalPages)) break;
    if (!data?.content?.length) break;
  }

  return rows;
}
