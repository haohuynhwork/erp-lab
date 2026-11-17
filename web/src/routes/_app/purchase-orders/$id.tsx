import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, Check, FileText, PackageCheck, Pencil, X } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { Info } from "@/components/info";
import { OrderForm, orderBody } from "@/components/order-form";
import { PageHeader } from "@/components/page-header";
import { QuantityDialog } from "@/components/quantity-dialog";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, post, put } from "@/lib/api";
import { fmtDate, fmtMoney, fmtQty } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";
import type { BillRef, PurchaseOrder, PurchaseOrderLine } from "./-lib/types";

export const Route = createFileRoute("/_app/purchase-orders/$id")({ component: PurchaseOrderPage });

type Line = PurchaseOrderLine;

type Detail = { order: PurchaseOrder; lines: Line[]; bills: BillRef[] };

function PurchaseOrderPage() {
  const { id } = Route.useParams();
  const can = useCan();
  const [editing, setEditing] = useState(false);
  const [receiving, setReceiving] = useState(false);
  const [supplierReference, setSupplierReference] = useState("");
  const [billing, setBilling] = useState(false);
  const navigate = useNavigate();
  const key = ["purchase-orders", id];
  const { data } = useQuery({
    queryKey: key,
    queryFn: () => api<Detail>(`/purchase-orders/${id}`),
  });
  const invalidate = [["purchase-orders"], ["dashboard"]];
  const confirm = useAction(() => post(`/purchase-orders/${id}/confirm`), {
    success: "Order confirmed",
    invalidate,
  });
  const cancel = useAction(() => post(`/purchase-orders/${id}/cancel`), {
    success: "Order cancelled",
    invalidate,
  });
  const save = useAction((body: unknown) => put(`/purchase-orders/${id}`, body), {
    success: "Saved",
    invalidate,
    onSuccess: () => setEditing(false),
  });
  const receive = useAction((body: unknown) => post(`/purchase-orders/${id}/receipts`, body), {
    success: "Goods received into stock",
    invalidate,
    onSuccess: () => setReceiving(false),
  });

  const bill = useAction(
    () => post<{ id: number }>(`/purchase-orders/${id}/bills`, { supplierReference }),
    {
      success: "Bill created",
      invalidate: [...invalidate, ["bills"]],
      onSuccess: ({ id: billId }) => navigate({ to: "/bills/$id", params: { id: String(billId) } }),
    },
  );

  if (!data) return null;
  const { order, lines, bills } = data;
  const toBill = lines.some((l) => l.receivedQuantity > l.billedQuantity);
  const write = can("purchasing:write");

  if (editing) {
    return (
      <div>
        <PageHeader
          title={`Edit ${order.number}`}
          actions={
            <Button variant="outline" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          }
        />
        <OrderForm
          kind="purchase"
          saving={save.isPending}
          onSave={(o) => save.mutate(orderBody("purchase", o))}
          initial={{
            partnerId: order.supplierId,
            warehouseId: order.warehouseId,
            orderDate: order.orderDate,
            dueDate: order.expectedDate ?? "",
            taxRate: 0,
            validUntil: "",
            note: order.note ?? "",
            lines: lines.map((l) => ({
              productId: l.productId,
              quantity: l.quantity,
              unitPrice: l.unitPrice,
            })),
          }}
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title={
          <span className="flex items-center gap-3">
            {order.number} <StatusBadge status={order.status} />
          </span>
        }
        description={`${order.supplierName} · warehouse ${order.warehouseCode}`}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/purchase-orders">
                <ArrowLeft /> Back
              </Link>
            </Button>
            {write && order.status === "DRAFT" && (
              <>
                <Button variant="outline" onClick={() => setEditing(true)}>
                  <Pencil /> Edit
                </Button>
                <Button onClick={() => confirm.mutate(undefined)} disabled={confirm.isPending}>
                  <Check /> Confirm
                </Button>
              </>
            )}
            {(write || can("inventory:write")) && order.status === "CONFIRMED" && (
              <Button onClick={() => setReceiving(true)}>
                <PackageCheck /> Receive goods
              </Button>
            )}
            {(write || can("accounting:write")) && toBill && (
              <Button onClick={() => setBilling(true)}>
                <FileText /> Enter bill
              </Button>
            )}
            {write && (order.status === "DRAFT" || order.status === "CONFIRMED") && (
              <Button
                variant="outline"
                onClick={() => window.confirm("Cancel this order?") && cancel.mutate(undefined)}
              >
                <X /> Cancel order
              </Button>
            )}
          </>
        }
      />
      <Card>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-4">
          <Info label="Order date" value={fmtDate(order.orderDate)} />
          <Info label="Expected" value={fmtDate(order.expectedDate) || "—"} />
          <Info label="Total" value={fmtMoney(order.total)} />
          <Info label="Note" value={order.note || "—"} />
        </CardContent>
      </Card>
      <DataTable
        data={lines}
        columns={[
          { accessorKey: "sku", header: "SKU" },
          { accessorKey: "productName", header: "Product" },
          {
            accessorKey: "quantity",
            header: "Ordered",
            meta: { align: "right" },
            cell: ({ row }) => `${fmtQty(row.original.quantity)} ${row.original.unitCode ?? ""}`,
          },
          {
            accessorKey: "receivedQuantity",
            header: "Received",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtQty(getValue()),
          },
          {
            accessorKey: "unitPrice",
            header: "Unit price",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            accessorKey: "amount",
            header: "Amount",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
        ]}
      />
      {bills.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Supplier bills</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {bills.map((b) => (
              <Link
                key={b.id}
                to="/bills/$id"
                params={{ id: String(b.id) }}
                className="flex items-center justify-between rounded-md px-2 py-1.5 hover:bg-accent"
              >
                <span className="font-medium">{b.number}</span>
                <span className="text-muted-foreground">{fmtDate(b.billDate)}</span>
                <StatusBadge status={b.status} />
                <span className="tabular-nums">{fmtMoney(b.total)}</span>
              </Link>
            ))}
          </CardContent>
        </Card>
      )}
      <Dialog open={billing} onOpenChange={setBilling}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Enter supplier bill</DialogTitle>
            <DialogDescription>
              Bills everything received but not yet billed, at the order prices. Due date follows
              the supplier's payment terms.
            </DialogDescription>
          </DialogHeader>
          <div>
            <Label className="mb-1.5">Supplier's invoice number</Label>
            <Input
              value={supplierReference}
              onChange={(e) => setSupplierReference(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button onClick={() => bill.mutate(undefined)} disabled={bill.isPending}>
              Create bill
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <QuantityDialog
        title="Receive goods"
        description="Received quantities go into stock and are booked to accounts payable."
        open={receiving}
        onOpenChange={setReceiving}
        busy={receive.isPending}
        lines={lines.map((l) => ({
          id: l.id,
          sku: l.sku,
          productName: l.productName,
          remaining: l.quantity - l.receivedQuantity,
        }))}
        onSubmit={(date, ls) => receive.mutate({ date, lines: ls })}
      />
    </div>
  );
}
