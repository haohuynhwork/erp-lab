import { useQuery } from "@tanstack/react-query";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api, type Page } from "@/lib/api";

type Props<T> = {
  /** List endpoint, e.g. "/products?active=true" (up to 500 rows are loaded). */
  endpoint: string;
  label: (row: T) => string;
  value: number | null | undefined;
  onChange: (id: number | null) => void;
  placeholder?: string;
  allowEmpty?: boolean;
  id?: string;
  className?: string;
};

/** Select over a master data list. */
export function EntitySelect<T extends { id: number }>({
  endpoint,
  label,
  value,
  onChange,
  placeholder,
  allowEmpty,
  id,
  className,
}: Props<T>) {
  const sep = endpoint.includes("?") ? "&" : "?";
  const { data } = useQuery({
    queryKey: ["options", endpoint],
    queryFn: () => api<Page<T>>(`${endpoint}${sep}size=500`),
    staleTime: 60_000,
  });
  return (
    <Select
      value={value == null ? "" : String(value)}
      onValueChange={(v) => onChange(v === "__none" || v === "" ? null : Number(v))}
    >
      <SelectTrigger id={id} className={className ?? "w-full"}>
        <SelectValue placeholder={placeholder ?? "Select…"} />
      </SelectTrigger>
      <SelectContent>
        {allowEmpty && <SelectItem value="__none">—</SelectItem>}
        {data?.items.map((row) => (
          <SelectItem key={row.id} value={String(row.id)}>
            {label(row)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
