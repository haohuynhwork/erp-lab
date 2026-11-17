import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ArrowLeftRight, ClipboardCheck, PackageMinus, PackagePlus } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { EntityForm, type FieldDef } from "@/components/entity-form";
import { EntitySelect } from "@/components/entity-select";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, type Page, post, qs } from "@/lib/api";
import { fmtMoney, fmtQty } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/stock")({ component: Stock });

type StockRow = {
  productId: number;
  sku: string;
  productName: string;
  unitCode: string;
  warehouseId: number;
  warehouseCode: string;
  quantity: number;
  averageCost: number;
  value: number;
  minStock: number;
  low: boolean;
};

const product: FieldDef = {
  name: "productId",
  label: "Product",
  type: "entity",
  endpoint: "/products?active=true",
  optionLabel: (p) => `${p.sku} · ${p.name}`,
  required: true,
  wide: true,
};
const warehouse = (name = "warehouseId", label = "Warehouse"): FieldDef => ({
  name,
  label,
  type: "entity",
  endpoint: "/warehouses",
  optionLabel: (w) => `${w.code} · ${w.name}`,
  required: true,
});
const note: FieldDef = { name: "note", label: "Note", type: "textarea" };

const operations = {
  receipts: {
    title: "Receive stock",
    description:
      "Stock without a purchase order, e.g. opening balance. Booked against opening equity.",
    icon: <PackagePlus />,
    fields: [
      product,
      warehouse(),
      { name: "quantity", label: "Quantity", type: "number", required: true },
      { name: "unitCost", label: "Unit cost", type: "number", required: true },
      note,
    ],
  },
  issues: {
    title: "Issue stock",
    description: "Scrap or internal use, valued at average cost.",
    icon: <PackageMinus />,
    fields: [
      product,
      warehouse(),
      { name: "quantity", label: "Quantity", type: "number", required: true },
      note,
    ],
  },
  transfers: {
    title: "Transfer",
    description: "Move stock between warehouses at average cost.",
    icon: <ArrowLeftRight />,
    fields: [
      product,
      warehouse("fromWarehouseId", "From"),
      warehouse("toWarehouseId", "To"),
      { name: "quantity", label: "Quantity", type: "number", required: true },
      note,
    ],
  },
  counts: {
    title: "Stock count",
    description:
      "Set the on-hand quantity to what you counted; the difference is booked as an adjustment.",
    icon: <ClipboardCheck />,
    fields: [
      product,
      warehouse(),
      { name: "countedQuantity", label: "Counted quantity", type: "number", required: true },
      note,
    ],
  },
} satisfies Record<
  string,
  { title: string; description: string; icon: React.ReactNode; fields: FieldDef[] }
>;

type Op = keyof typeof operations;

function Stock() {
  const can = useCan();
  const [q, setQ] = useState("");
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [lowOnly, setLowOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [op, setOp] = useState<Op | null>(null);
  const params = { q, warehouseId, lowOnly, page, size: 50 };
  const list = useQuery({
    queryKey: ["stock", params],
    queryFn: () => api<Page<StockRow>>(`/inventory/stock${qs(params)}`),
    placeholderData: keepPreviousData,
  });
  const run = useAction((body: Record<string, unknown>) => post(`/inventory/${op}`, body), {
    success: "Stock updated",
    invalidate: [["stock"], ["movements"], ["dashboard"]],
    onSuccess: () => setOp(null),
  });
  const total = list.data?.items.reduce((s, r) => s + r.value, 0) ?? 0;

  return (
    <div>
      <PageHeader
        title="Stock"
        description="On-hand quantities per warehouse, valued at moving average cost"
        actions={
          can("inventory:write") &&
          (Object.keys(operations) as Op[]).map((k) => (
            <Button key={k} variant="outline" onClick={() => setOp(k)}>
              {operations[k].icon} {operations[k].title}
            </Button>
          ))
        }
      />
      <div className="mb-3 flex flex-wrap items-center gap-3">
        <Input
          placeholder="Search SKU or name…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="max-w-xs"
        />
        <EntitySelect<{ id: number; code: string; name: string }>
          endpoint="/warehouses"
          label={(w) => `${w.code} · ${w.name}`}
          value={warehouseId}
          onChange={(v) => {
            setWarehouseId(v);
            setPage(0);
          }}
          placeholder="All warehouses"
          allowEmpty
          className="w-56"
        />
        <div className="flex items-center gap-2 text-sm">
          <Checkbox
            id="low-only"
            checked={lowOnly}
            onCheckedChange={(c) => {
              setLowOnly(c === true);
              setPage(0);
            }}
          />
          <Label htmlFor="low-only">Below minimum only</Label>
        </div>
        <span className="ml-auto text-sm text-muted-foreground">
          Value on this page: <span className="font-medium text-foreground">{fmtMoney(total)}</span>
        </span>
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        columns={[
          { accessorKey: "sku", header: "SKU" },
          { accessorKey: "productName", header: "Product" },
          { accessorKey: "warehouseCode", header: "Warehouse" },
          {
            accessorKey: "quantity",
            header: "On hand",
            meta: { align: "right" },
            cell: ({ row }) => `${fmtQty(row.original.quantity)} ${row.original.unitCode ?? ""}`,
          },
          {
            accessorKey: "minStock",
            header: "Minimum",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtQty(getValue()),
          },
          {
            accessorKey: "averageCost",
            header: "Avg cost",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            accessorKey: "value",
            header: "Value",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            accessorKey: "low",
            header: "",
            cell: ({ getValue }) => getValue() && <Badge variant="warning">low</Badge>,
          },
        ]}
      />
      <Dialog open={op !== null} onOpenChange={(o) => !o && setOp(null)}>
        <DialogContent className="sm:max-w-xl">
          {op && (
            <>
              <DialogHeader>
                <DialogTitle>{operations[op].title}</DialogTitle>
                <DialogDescription>{operations[op].description}</DialogDescription>
              </DialogHeader>
              <EntityForm
                key={op}
                fields={operations[op].fields}
                defaultValues={{ warehouseId, note: "" }}
                editing={false}
                submitting={run.isPending}
                onSubmit={(v) => run.mutate(v)}
              />
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
