import { graphqlUrl, restUrl } from "./config";
import { clearTokens, getTokens, setTokens, type Tokens } from "./tokens";

/** Body of every api response (see the api's ResponseEnvelope). */
export type ResponseEnvelope<T> =
  | { status: "success"; data: T }
  | {
      status: "fail";
      code: string;
      message: string;
      data?: { issues?: Issue[] };
    }
  | { status: "error"; code: string; message: string };

export type Issue = { path: string; message: string };

/** Paged list response. */
export type Page<T> = { items: T[]; total: number; page: number; size: number };

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly issues: Issue[] = [],
  ) {
    super(message);
  }
}

let refreshing: Promise<boolean> | null = null;

/** Exchanges the refresh token once for all callers waiting on a 401. */
function refresh(): Promise<boolean> {
  refreshing ??= (async () => {
    const tokens = getTokens();
    if (!tokens) return false;
    const res = await fetch(`${restUrl}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: tokens.refreshToken }),
    });
    const body = (await res.json().catch(() => null)) as ResponseEnvelope<Tokens> | null;
    if (body?.status === "success") {
      setTokens(body.data);
      return true;
    }
    clearTokens();
    return false;
  })().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

async function send(
  path: string,
  init: RequestInit | undefined,
  retry: boolean,
): Promise<Response> {
  const token = getTokens()?.accessToken;
  const res = await fetch(`${restUrl}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  if (res.status === 401 && retry && getTokens() && (await refresh())) {
    return send(path, init, false);
  }
  if (res.status === 401 && !path.startsWith("/auth/")) {
    clearTokens();
    window.dispatchEvent(new Event("erp:signed-out"));
  }
  return res;
}

/** Calls a REST endpoint (path relative to /v1) and unwraps the envelope. */
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await send(path, init, true);
  if (res.status === 204) return undefined as T;
  const body = (await res.json().catch(() => null)) as ResponseEnvelope<T> | null;
  if (body?.status === "success") return body.data;
  if (body?.status === "fail") {
    throw new ApiError(res.status, body.code, body.message, body.data?.issues);
  }
  throw new ApiError(res.status, body?.code ?? "network_error", body?.message ?? res.statusText);
}

export const post = <T>(path: string, body?: unknown) =>
  api<T>(path, { method: "POST", body: JSON.stringify(body ?? {}) });

export const put = <T>(path: string, body: unknown) =>
  api<T>(path, { method: "PUT", body: JSON.stringify(body) });

export const del = (path: string) => api<void>(path, { method: "DELETE" });

/** Builds `?a=1&b=2`, skipping empty values. */
export function qs(params: Record<string, string | number | boolean | null | undefined>) {
  const s = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== "") s.set(k, String(v));
  }
  const str = s.toString();
  return str ? `?${str}` : "";
}

/** Downloads a file endpoint (Excel, PDF) with the auth header. */
export async function download(path: string) {
  const res = await send(path, undefined, true);
  if (!res.ok) throw new ApiError(res.status, "download_failed", "Download failed");
  const name =
    /filename="?([^";]+)"?/.exec(res.headers.get("Content-Disposition") ?? "")?.[1] ?? "download";
  const url = URL.createObjectURL(await res.blob());
  const a = Object.assign(document.createElement("a"), { href: url, download: name });
  a.click();
  URL.revokeObjectURL(url);
}

/** Runs a GraphQL operation. */
export async function gql<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const res = await fetch(graphqlUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  const body = (await res.json()) as { data?: T; errors?: { message: string }[] };
  if (body.errors?.length) throw new ApiError(res.status, "graphql_error", body.errors[0].message);
  return body.data as T;
}
