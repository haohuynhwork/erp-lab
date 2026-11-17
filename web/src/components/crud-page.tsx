import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { type ReactNode, useState } from "react";
import { type Column, DataTable } from "@/components/data-table";
import { EntityForm, type FieldDef } from "@/components/entity-form";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { api, del, type Page, post, put, qs } from "@/lib/api";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";

type Props<T extends { id: number }> = {
  title: string;
  description?: string;
  /** REST collection, e.g. "/units"; items at `${endpoint}/{id}`. */
  endpoint: string;
  columns: Column<T>[];
  fields: FieldDef[];
  newValues: Record<string, unknown>;
  /** Form values for an existing row (default: the row itself). */
  toValues?: (row: T) => Record<string, unknown>;
  /** Request body from form values (default: the values). */
  // biome-ignore lint/suspicious/noExplicitAny: form values are dynamic per field config
  toBody?: (values: Record<string, any>) => unknown;
  writePermission: string;
  /** Extra list filters rendered next to the search box. */
  filters?: ReactNode;
  extraParams?: Record<string, string | number | boolean | undefined>;
  canDelete?: boolean;
  /** Loads full form values for a row (when the list lacks fields, e.g. a product's BOM). */
  loadValues?: (row: T) => Promise<Record<string, unknown>>;
  /** Custom create/update calls (e.g. accounts use a different update shape). */
  // biome-ignore lint/suspicious/noExplicitAny: body shape is page-specific
  save?: (id: number | null, body: any) => Promise<unknown>;
};

/** List + search + paging + create/edit dialog for simple master data. */
export function CrudPage<T extends { id: number }>(props: Props<T>) {
  const { endpoint } = props;
  const can = useCan();
  const writable = can(props.writePermission);
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<T | "new" | null>(null);
  const params = { q, page, size: 20, ...props.extraParams };

  const list = useQuery({
    queryKey: [endpoint, params],
    queryFn: () => api<Page<T>>(endpoint + qs(params)),
    placeholderData: keepPreviousData,
  });

  const invalidate = [[endpoint], ["options"]];
  const save = useAction(
    // biome-ignore lint/suspicious/noExplicitAny: form values are dynamic per field config
    (values: Record<string, any>) => {
      const body = props.toBody ? props.toBody(values) : values;
      const id = editing && editing !== "new" ? editing.id : null;
      if (props.save) return props.save(id, body);
      return id ? put(`${endpoint}/${id}`, body) : post(endpoint, body);
    },
    { success: "Saved", invalidate, onSuccess: () => setEditing(null) },
  );
  const remove = useAction((id: number) => del(`${endpoint}/${id}`), {
    success: "Deleted",
    invalidate,
    onSuccess: () => setEditing(null),
  });

  const current = editing && editing !== "new" ? editing : null;
  const loaded = useQuery({
    queryKey: [endpoint, "detail", current?.id],
    queryFn: () => (props.loadValues ? props.loadValues(current as T) : Promise.resolve({})),
    enabled: !!current && !!props.loadValues,
    gcTime: 0,
  });
  const values = current
    ? props.loadValues
      ? loaded.data
      : (props.toValues?.(current) ?? current)
    : props.newValues;

  return (
    <div>
      <PageHeader
        title={props.title}
        description={props.description}
        actions={
          writable && (
            <Button onClick={() => setEditing("new")}>
              <Plus /> New
            </Button>
          )
        }
      />
      <div className="mb-3 flex flex-wrap gap-2">
        <Input
          placeholder="Search…"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
          className="max-w-xs"
        />
        {props.filters}
      </div>
      <DataTable
        columns={props.columns}
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 20, total: list.data.total, onPage: setPage }}
        onRowClick={writable ? (row) => setEditing(row) : undefined}
      />
      <Dialog open={editing !== null} onOpenChange={(open) => !open && setEditing(null)}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {current
                ? `Edit ${props.title.replace(/s$/, "").toLowerCase()}`
                : `New ${props.title.replace(/s$/, "").toLowerCase()}`}
            </DialogTitle>
            <DialogDescription>{props.description}</DialogDescription>
          </DialogHeader>
          {editing !== null && !values && <p className="text-sm text-muted-foreground">Loading…</p>}
          {editing !== null && values && (
            <EntityForm
              key={current?.id ?? "new"}
              fields={props.fields}
              defaultValues={values}
              editing={!!current}
              submitting={save.isPending}
              onSubmit={(v) => save.mutate(v)}
              footer={
                current &&
                props.canDelete !== false && (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => confirm("Delete this record?") && remove.mutate(current.id)}
                  >
                    Delete
                  </Button>
                )
              }
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
