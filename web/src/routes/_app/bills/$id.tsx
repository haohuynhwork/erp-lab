import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Banknote } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { Info } from "@/components/info";
import { PageHeader } from "@/components/page-header";
import { PaymentDialog } from "@/components/payment-dialog";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api, post } from "@/lib/api";
import { fmtDate, fmtMoney, fmtQty } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/bills/$id")({ component: BillPage });

function BillPage() {
  const { id } = Route.useParams();
  const can = useCan();
  const [paying, setPaying] = useState(false);
  const { data } = useQuery({
    queryKey: ["bills", id],
    queryFn: () => api<Schemas["BillDetail"]>(`/bills/${id}`),
  });
  const pay = useAction((body: unknown) => post(`/bills/${id}/payments`, body), {
    success: "Payment recorded",
    invalidate: [["bills"], ["purchase-orders"], ["balances"], ["dashboard"]],
    onSuccess: () => setPaying(false),
  });

  if (!data) return null;
  const { bill, lines, payments } = data;

  return (
    <div className="space-y-4">
      <PageHeader
        title={
          <span className="flex items-center gap-3">
            {bill.number} <StatusBadge status={bill.overdue ? "OVERDUE" : bill.status} />
          </span>
        }
        description={
          <>
            {bill.supplierName}
            {bill.supplierReference ? ` · their ref. ${bill.supplierReference}` : ""} · order{" "}
            <Link
              to="/purchase-orders/$id"
              params={{ id: String(bill.purchaseOrderId) }}
              className="underline"
            >
              {bill.purchaseOrderNumber}
            </Link>
          </>
        }
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/bills">
                <ArrowLeft /> Back
              </Link>
            </Button>
            {can("accounting:write") && bill.balance > 0 && (
              <Button onClick={() => setPaying(true)}>
                <Banknote /> Pay
              </Button>
            )}
          </>
        }
      />
      <Card>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-5">
          <Info label="Bill date" value={fmtDate(bill.billDate)} />
          <Info label="Due" value={fmtDate(bill.dueDate)} />
          <Info label="Total" value={fmtMoney(bill.total)} />
          <Info label="Paid" value={fmtMoney(bill.paidAmount)} />
          <Info label="Balance" value={fmtMoney(bill.balance)} />
        </CardContent>
      </Card>
      <DataTable
        data={lines}
        columns={[
          { accessorKey: "sku", header: "SKU" },
          { accessorKey: "productName", header: "Product" },
          {
            accessorKey: "quantity",
            header: "Quantity",
            meta: { align: "right" },
            cell: ({ row }) => `${fmtQty(row.original.quantity)} ${row.original.unitCode ?? ""}`,
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
      <Card>
        <CardHeader>
          <CardTitle>Payments</CardTitle>
        </CardHeader>
        <CardContent>
          {payments.length === 0 ? (
            <p className="text-sm text-muted-foreground">No payments yet</p>
          ) : (
            <div className="space-y-1 text-sm">
              {payments.map((p) => (
                <div key={p.id} className="flex justify-between gap-4">
                  <span>{fmtDate(p.paymentDate)}</span>
                  <span className="text-muted-foreground">
                    {p.method}
                    {p.reference ? ` · ${p.reference}` : ""}
                  </span>
                  <span className="tabular-nums">{fmtMoney(p.amount)}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
      {paying && (
        <PaymentDialog
          title="Pay supplier bill"
          description="Booked as payables against cash."
          balance={bill.balance}
          open
          onOpenChange={setPaying}
          busy={pay.isPending}
          onSubmit={(p) => pay.mutate(p)}
        />
      )}
    </div>
  );
}
