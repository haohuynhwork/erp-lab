import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { EntitySelect } from "@/components/entity-select";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { api, type Page } from "@/lib/api";
import { fmtMoney, today } from "@/lib/format";

type ProductOption = {
  id: number;
  sku: string;
  name: string;
  purchasePrice: number;
  salePrice: number;
};

export type OrderLineInput = {
  productId: number | null;
  quantity: number | null;
  unitPrice: number | null;
};

export type OrderInput = {
  partnerId: number | null;
  warehouseId: number | null;
  orderDate: string;
  /** Expected (purchase) or delivery (sales) date. */
  dueDate: string;
  /** Sales only: the quotation is valid until (while Draft). */
  validUntil: string;
  taxRate: number;
  note: string;
  lines: OrderLineInput[];
};

export const emptyOrder = (): OrderInput => ({
  partnerId: null,
  warehouseId: null,
  orderDate: today(),
  dueDate: "",
  validUntil: "",
  taxRate: 10,
  note: "",
  lines: [{ productId: null, quantity: 1, unitPrice: null }],
});

/** Header + lines editor shared by purchase and sales orders (Draft only). */
export function OrderForm({
  kind,
  initial,
  saving,
  onSave,
}: {
  kind: "purchase" | "sales";
  initial: OrderInput;
  saving: boolean;
  onSave: (order: OrderInput) => void;
}) {
  const [order, setOrder] = useState(initial);
  const products = useQuery({
    queryKey: ["options", "/products?active=true"],
    queryFn: () => api<Page<ProductOption>>("/products?active=true&size=500"),
  });
  const priceOf = (id: number | null) => {
    const p = products.data?.items.find((x) => x.id === id);
    return p ? (kind === "purchase" ? p.purchasePrice : p.salePrice) : null;
  };
  const set = (patch: Partial<OrderInput>) => setOrder((o) => ({ ...o, ...patch }));
  const setLine = (i: number, patch: Partial<OrderLineInput>) =>
    set({ lines: order.lines.map((l, j) => (j === i ? { ...l, ...patch } : l)) });

  const subtotal = order.lines.reduce((s, l) => s + (l.quantity ?? 0) * (l.unitPrice ?? 0), 0);
  const tax = kind === "sales" ? (subtotal * order.taxRate) / 100 : 0;
  const valid =
    order.partnerId != null &&
    order.warehouseId != null &&
    order.orderDate &&
    order.lines.length > 0 &&
    order.lines.every((l) => l.productId != null && (l.quantity ?? 0) > 0);

  return (
    <form
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) onSave(order);
      }}
    >
      <Card>
        <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <Label className="mb-1.5">{kind === "purchase" ? "Supplier" : "Customer"} *</Label>
            <EntitySelect<{ id: number; code: string; name: string }>
              endpoint={`/partners?role=${kind === "purchase" ? "supplier" : "customer"}`}
              label={(p) => `${p.code} · ${p.name}`}
              value={order.partnerId}
              onChange={(id) => set({ partnerId: id })}
            />
          </div>
          <div>
            <Label className="mb-1.5">Warehouse *</Label>
            <EntitySelect<{ id: number; code: string; name: string }>
              endpoint="/warehouses"
              label={(w) => `${w.code} · ${w.name}`}
              value={order.warehouseId}
              onChange={(id) => set({ warehouseId: id })}
            />
          </div>
          <div>
            <Label className="mb-1.5">Order date *</Label>
            <Input
              type="date"
              value={order.orderDate}
              onChange={(e) => set({ orderDate: e.target.value })}
            />
          </div>
          <div>
            <Label className="mb-1.5">
              {kind === "purchase" ? "Expected date" : "Delivery date"}
            </Label>
            <Input
              type="date"
              value={order.dueDate}
              onChange={(e) => set({ dueDate: e.target.value })}
            />
          </div>
          {kind === "sales" && (
            <div>
              <Label className="mb-1.5">Quotation valid until</Label>
              <Input
                type="date"
                value={order.validUntil}
                onChange={(e) => set({ validUntil: e.target.value })}
              />
            </div>
          )}
          {kind === "sales" && (
            <div>
              <Label className="mb-1.5">VAT %</Label>
              <Input
                type="number"
                min={0}
                max={100}
                step="any"
                value={order.taxRate}
                onChange={(e) => set({ taxRate: Number(e.target.value) })}
              />
            </div>
          )}
          <div className="sm:col-span-2 lg:col-span-3">
            <Label className="mb-1.5">Note</Label>
            <Textarea value={order.note} onChange={(e) => set({ note: e.target.value })} rows={2} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-2 ">
          <div className="grid grid-cols-[1fr_7rem_8rem_8rem_2.5rem] gap-2 text-xs font-medium text-muted-foreground">
            <span>Product</span>
            <span className="text-right">Quantity</span>
            <span className="text-right">Unit price</span>
            <span className="text-right">Amount</span>
            <span />
          </div>
          {order.lines.map((line, i) => (
            // biome-ignore lint/suspicious/noArrayIndexKey: draft lines have no id
            <div key={i} className="grid grid-cols-[1fr_7rem_8rem_8rem_2.5rem] items-center gap-2">
              <EntitySelect<ProductOption>
                endpoint="/products?active=true"
                label={(p) => `${p.sku} · ${p.name}`}
                value={line.productId}
                onChange={(id) => setLine(i, { productId: id, unitPrice: priceOf(id) })}
              />
              <Input
                type="number"
                step="any"
                min={0}
                className="text-right"
                value={line.quantity ?? ""}
                onChange={(e) =>
                  setLine(i, { quantity: e.target.value === "" ? null : Number(e.target.value) })
                }
              />
              <Input
                type="number"
                step="any"
                min={0}
                className="text-right"
                value={line.unitPrice ?? ""}
                onChange={(e) =>
                  setLine(i, { unitPrice: e.target.value === "" ? null : Number(e.target.value) })
                }
              />
              <span className="text-right tabular-nums">
                {fmtMoney((line.quantity ?? 0) * (line.unitPrice ?? 0))}
              </span>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Remove line"
                onClick={() => set({ lines: order.lines.filter((_, j) => j !== i) })}
              >
                <Trash2 />
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() =>
              set({ lines: [...order.lines, { productId: null, quantity: 1, unitPrice: null }] })
            }
          >
            <Plus /> Add line
          </Button>
          <div className="ml-auto w-64 space-y-1 border-t pt-3 text-sm">
            <Row label="Subtotal" value={fmtMoney(subtotal)} />
            {kind === "sales" && <Row label={`VAT ${order.taxRate}%`} value={fmtMoney(tax)} />}
            <Row label="Total" value={fmtMoney(subtotal + tax)} bold />
          </div>
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button type="submit" disabled={!valid || saving}>
          {saving ? "Saving…" : kind === "sales" ? "Save quotation" : "Save draft"}
        </Button>
      </div>
    </form>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <div className={bold ? "flex justify-between font-semibold" : "flex justify-between"}>
      <span>{label}</span>
      <span className="tabular-nums">{value}</span>
    </div>
  );
}

/** API body of an order (purchase uses expectedDate, sales deliveryDate + taxRate). */
export function orderBody(kind: "purchase" | "sales", o: OrderInput) {
  const lines = o.lines.map((l) => ({
    productId: l.productId,
    quantity: l.quantity,
    unitPrice: l.unitPrice,
  }));
  return kind === "purchase"
    ? {
        supplierId: o.partnerId,
        warehouseId: o.warehouseId,
        orderDate: o.orderDate,
        expectedDate: o.dueDate || null,
        note: o.note,
        lines,
      }
    : {
        customerId: o.partnerId,
        warehouseId: o.warehouseId,
        orderDate: o.orderDate,
        deliveryDate: o.dueDate || null,
        validUntil: o.validUntil || null,
        taxRate: o.taxRate,
        note: o.note,
        lines,
      };
}
