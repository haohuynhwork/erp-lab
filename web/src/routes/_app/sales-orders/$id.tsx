import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, Check, FileText, Pencil, Truck, X } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { Info } from "@/components/info";
import { OrderForm, orderBody } from "@/components/order-form";
import { PageHeader } from "@/components/page-header";
import { QuantityDialog } from "@/components/quantity-dialog";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api, post, put } from "@/lib/api";
import { fmtDate, fmtMoney, fmtQty } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";
import type { InvoiceRef, SalesOrder, SalesOrderLine } from "./-lib/types";

export const Route = createFileRoute("/_app/sales-orders/$id")({ component: SalesOrderPage });

type Detail = { order: SalesOrder; lines: SalesOrderLine[]; invoices: InvoiceRef[] };

function SalesOrderPage() {
  const { id } = Route.useParams();
  const can = useCan();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const [delivering, setDelivering] = useState(false);
  const { data } = useQuery({
    queryKey: ["sales-orders", id],
    queryFn: () => api<Detail>(`/sales-orders/${id}`),
  });
  const invalidate = [["sales-orders"], ["invoices"], ["dashboard"]];
  const confirm = useAction(() => post(`/sales-orders/${id}/confirm`), {
    success: "Order confirmed",
    invalidate,
  });
  const cancel = useAction(() => post(`/sales-orders/${id}/cancel`), {
    success: "Order cancelled",
    invalidate,
  });
  const save = useAction((body: unknown) => put(`/sales-orders/${id}`, body), {
    success: "Saved",
    invalidate,
    onSuccess: () => setEditing(false),
  });
  const deliver = useAction((body: unknown) => post(`/sales-orders/${id}/deliveries`, body), {
    success: "Delivered: stock issued",
    invalidate,
    onSuccess: () => setDelivering(false),
  });
  const invoice = useAction(() => post<{ id: number }>(`/sales-orders/${id}/invoices`, {}), {
    success: "Invoice created",
    invalidate,
    onSuccess: ({ id: invoiceId }) =>
      navigate({ to: "/invoices/$id", params: { id: String(invoiceId) } }),
  });

  if (!data) return null;
  const { order, lines, invoices } = data;
  const write = can("sales:write");
  const toInvoice = lines.some((l) => l.deliveredQuantity > l.invoicedQuantity);

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
          kind="sales"
          saving={save.isPending}
          onSave={(o) => save.mutate(orderBody("sales", o))}
          initial={{
            partnerId: order.customerId,
            warehouseId: order.warehouseId,
            orderDate: order.orderDate,
            dueDate: order.deliveryDate ?? "",
            taxRate: order.taxRate,
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
        description={`${order.customerName} · warehouse ${order.warehouseCode}`}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/sales-orders">
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
            {(write || can("inventory:write")) &&
              order.status === "CONFIRMED" &&
              lines.some((l) => l.quantity > l.deliveredQuantity) && (
                <Button onClick={() => setDelivering(true)}>
                  <Truck /> Deliver
                </Button>
              )}
            {write && toInvoice && (
              <Button onClick={() => invoice.mutate(undefined)} disabled={invoice.isPending}>
                <FileText /> Create invoice
              </Button>
            )}
            {write &&
              (order.status === "DRAFT" || order.status === "CONFIRMED") &&
              lines.every((l) => l.deliveredQuantity === 0) && (
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
        <CardContent className="grid gap-4 text-sm sm:grid-cols-3 lg:grid-cols-6">
          <Info label="Order date" value={fmtDate(order.orderDate)} />
          <Info label="Delivery" value={fmtDate(order.deliveryDate) || "—"} />
          <Info label="Subtotal" value={fmtMoney(order.subtotal)} />
          <Info label={`VAT ${order.taxRate}%`} value={fmtMoney(order.taxAmount)} />
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
            accessorKey: "deliveredQuantity",
            header: "Delivered",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtQty(getValue()),
          },
          {
            accessorKey: "invoicedQuantity",
            header: "Invoiced",
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
      {invoices.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Invoices</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {invoices.map((i) => (
              <Link
                key={i.id}
                to="/invoices/$id"
                params={{ id: String(i.id) }}
                className="flex items-center justify-between rounded-md px-2 py-1.5 hover:bg-accent"
              >
                <span className="font-medium">{i.number}</span>
                <span className="text-muted-foreground">{fmtDate(i.issueDate)}</span>
                <StatusBadge status={i.status} />
                <span className="tabular-nums">{fmtMoney(i.total)}</span>
              </Link>
            ))}
          </CardContent>
        </Card>
      )}
      <QuantityDialog
        title="Deliver"
        description="Delivered quantities leave stock now; there must be enough on hand in the warehouse."
        open={delivering}
        onOpenChange={setDelivering}
        busy={deliver.isPending}
        lines={lines.map((l) => ({
          id: l.id,
          sku: l.sku,
          productName: l.productName,
          remaining: l.quantity - l.deliveredQuantity,
        }))}
        onSubmit={(date, ls) => deliver.mutate({ date, lines: ls })}
      />
    </div>
  );
}
