import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type { ApiError } from "./api";

/**
 * Mutation that toasts success / the api's error message and refreshes the given query keys
 * (prefix match, e.g. ["sales-orders"]).
 */
export function useAction<TVars, TResult>(
  fn: (vars: TVars) => Promise<TResult>,
  opts: { success?: string; invalidate?: unknown[][]; onSuccess?: (r: TResult) => void } = {},
) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: async (result) => {
      if (opts.success) toast.success(opts.success);
      await Promise.all(
        (opts.invalidate ?? []).map((key) => qc.invalidateQueries({ queryKey: key })),
      );
      opts.onSuccess?.(result);
    },
    onError: (e) => {
      const err = e as ApiError;
      const detail = err.issues?.length
        ? err.issues.map((i) => `${i.path}: ${i.message}`).join(", ")
        : undefined;
      toast.error(err.message ?? "Something went wrong", { description: detail });
    },
  });
}
