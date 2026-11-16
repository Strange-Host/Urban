import { getErrorDetail } from "./errors";

/**
 * Splits the outcome of independently loaded dashboard sections into the ones the user is
 * simply not allowed to see (403 - hidden quietly) and real failures (shown as an error).
 *
 * @param {string[]} names   section names, in the same order as results
 * @param {PromiseSettledResult[]} results
 */
export function summariseSections(names, results) {
  const denied = {};
  const failures = [];

  results.forEach((result, index) => {
    if (result.status === "fulfilled") return;

    const status = result.reason?.response?.status;
    if (status === 403) {
      denied[names[index]] = true;
    } else {
      failures.push(getErrorDetail(result.reason, `Failed to load ${names[index]}.`));
    }
  });

  return { denied, error: failures[0] || "" };
}
