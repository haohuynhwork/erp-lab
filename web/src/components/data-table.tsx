import {
  type ColumnDef,
  flexRender,
  type RowData,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

/** Plain tables: core features only (paging is done by the api). */
const features = tableFeatures({});

// biome-ignore lint/suspicious/noExplicitAny: cell values differ per column
export type Column<T extends RowData> = ColumnDef<typeof features, T, any> & {
  meta?: { align?: "right" };
};

type Props<T extends RowData> = {
  columns: Column<T>[];
  data: T[] | undefined;
  loading?: boolean;
  /** Server-side paging; omit for a plain table. */
  paging?: { page: number; size: number; total: number; onPage: (page: number) => void };
  onRowClick?: (row: T) => void;
  empty?: string;
};

/** TanStack Table rendered with shadcn table parts. Column `meta.align = "right"` right-aligns numbers. */
export function DataTable<T extends RowData>({
  columns,
  data,
  loading,
  paging,
  onRowClick,
  empty,
}: Props<T>) {
  const table = useTable({ features, data: data ?? [], columns });
  const pages = paging ? Math.max(1, Math.ceil(paging.total / paging.size)) : 1;

  return (
    <div className="rounded-lg border bg-card">
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((hg) => (
            <TableRow key={hg.id}>
              {hg.headers.map((h) => (
                <TableHead
                  key={h.id}
                  className={cn(
                    (h.column.columnDef.meta as { align?: string })?.align === "right" &&
                      "text-right",
                  )}
                >
                  {h.isPlaceholder ? null : flexRender(h.column.columnDef.header, h.getContext())}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.map((row) => (
            <TableRow
              key={row.id}
              onClick={onRowClick ? () => onRowClick(row.original) : undefined}
              className={cn(onRowClick && "cursor-pointer")}
            >
              {row.getAllCells().map((cell) => (
                <TableCell
                  key={cell.id}
                  className={cn(
                    (cell.column.columnDef.meta as { align?: string })?.align === "right" &&
                      "text-right tabular-nums",
                  )}
                >
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </TableCell>
              ))}
            </TableRow>
          ))}
          {!loading && table.getRowModel().rows.length === 0 && (
            <TableRow>
              <TableCell
                colSpan={columns.length}
                className="py-8 text-center text-muted-foreground"
              >
                {empty ?? "Nothing here yet"}
              </TableCell>
            </TableRow>
          )}
          {loading && (
            <TableRow>
              <TableCell
                colSpan={columns.length}
                className="py-8 text-center text-muted-foreground"
              >
                Loading…
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
      {paging && paging.total > paging.size && (
        <div className="flex items-center justify-end gap-2 border-t px-3 py-2 text-sm text-muted-foreground">
          <span>
            Page {paging.page + 1} of {pages} · {paging.total} rows
          </span>
          <Button
            variant="outline"
            size="icon"
            disabled={paging.page === 0}
            onClick={() => paging.onPage(paging.page - 1)}
            aria-label="Previous page"
          >
            <ChevronLeft />
          </Button>
          <Button
            variant="outline"
            size="icon"
            disabled={paging.page + 1 >= pages}
            onClick={() => paging.onPage(paging.page + 1)}
            aria-label="Next page"
          >
            <ChevronRight />
          </Button>
        </div>
      )}
    </div>
  );
}
