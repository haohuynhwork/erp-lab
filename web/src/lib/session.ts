import { queryOptions, useQuery } from "@tanstack/react-query";
import { api } from "./api";
import type { Schemas } from "./schemas";

export type Role = "ADMIN" | "MANAGER" | "SALES" | "PURCHASING" | "WAREHOUSE" | "ACCOUNTANT";

export type Me = Schemas["UserView"];

export const meQuery = queryOptions({
  queryKey: ["me"],
  queryFn: () => api<Me>("/auth/me"),
  staleTime: 5 * 60_000,
});

/** Permission check for showing menu items and buttons (the api enforces them too). */
export function useCan() {
  const { data } = useQuery(meQuery);
  return (permission: string) => data?.permissions.includes(permission) ?? false;
}
