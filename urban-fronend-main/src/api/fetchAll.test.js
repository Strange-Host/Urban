import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("./api", () => ({ api: { get: vi.fn() } }));

import { api } from "./api";
import { fetchAllPages } from "./fetchAll";

const page = (content, totalPages, last) => ({ data: { content, totalPages, last } });

describe("fetchAllPages", () => {
  beforeEach(() => api.get.mockReset());

  it("reads every page until the last one", async () => {
    api.get
      .mockResolvedValueOnce(page([1, 2], 3, false))
      .mockResolvedValueOnce(page([3, 4], 3, false))
      .mockResolvedValueOnce(page([5], 3, true));

    const rows = await fetchAllPages("/x", { sort: "name,asc" }, { pageSize: 2 });

    expect(rows).toEqual([1, 2, 3, 4, 5]);
    expect(api.get).toHaveBeenCalledTimes(3);
    expect(api.get.mock.calls[1][1].params).toMatchObject({ page: 1, size: 2, sort: "name,asc" });
  });

  it("returns a plain array response as is", async () => {
    api.get.mockResolvedValueOnce({ data: [1, 2, 3] });
    expect(await fetchAllPages("/x")).toEqual([1, 2, 3]);
  });

  it("stops at maxPages", async () => {
    api.get.mockResolvedValue(page([1], 99, false));
    const rows = await fetchAllPages("/x", {}, { maxPages: 3 });
    expect(rows).toHaveLength(3);
  });

  it("stops when a page comes back empty", async () => {
    api.get.mockResolvedValueOnce(page([1], undefined, false)).mockResolvedValueOnce(page([], undefined, false));
    expect(await fetchAllPages("/x")).toEqual([1]);
  });
});
