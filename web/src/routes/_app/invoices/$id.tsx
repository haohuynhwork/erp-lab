import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Banknote, Download, ReceiptText } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { Info } from "@/components/info";
import { PageHeader } from "@/components/page-header";
import { PaymentDialog } from "@/components/payment-dialog";
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
import { api, download, post } from "@/lib/api";
import { fmtDate, fmtMoney, fmtQty, today } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";
import type { InvoiceDetail } from "./-lib/types";

export const Route = createFileRoute("/_app/invoices/$id")({ component: InvoicePage });

const invalidate = [["invoices"], ["sales-orders"], ["dashboard"], ["balances"]];

function InvoicePage() {
  const { id } = Route.useParams();
  const can = useCan();
  const [paying, setPaying] = useState(false);
  const [crediting, setCrediting] = useState(false);
  const { data } = useQuery({
    queryKey: ["invoices", id],
    queryFn: () => api<InvoiceDetail>(`/invoices/${id}`),
  });
  const pay = useAction((body: unknown) => post(`/invoices/${id}/payments`, body), {
    success: "Payment recorded",
    invalidate,
    onSuccess: () => setPaying(false),
  });

  if (!data) return null;
  const { invoice: inv, lines, payments, creditNotes } = data;
  const write = can("sales:write") || can("accounting:write");

  return (
    <div className="space-y-4">
      <PageHeader
        title={
          <span className="flex items-center gap-3">
            {inv.number} <StatusBadge status={inv.overdue ? "OVERDUE" : inv.status} />
          </span>
        }
        description={
          <>
            {inv.customerName} · order{" "}
            <Link
              to="/sales-orders/$id"
              params={{ id: String(inv.salesOrderId) }}
              className="underline"
            >
              {inv.salesOrderNumber}
            </Link>
          </>
        }
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/invoices">
                <ArrowLeft /> Back
              </Link>
            </Button>
            <Button variant="outline" onClick={() => download(`/reports/invoices/${inv.id}.pdf`)}>
              <Download /> PDF
            </Button>
            {write && inv.balance > 0 && (
              <Button variant="outline" onClick={() => setCrediting(true)}>
                <ReceiptText /> Credit note
              </Button>
            )}
            {write && inv.balance > 0 && (
              <Button onClick={() => setPaying(true)}>
                <Banknote /> Record payment
              </Button>
            )}
          </>
        }
      />
      <Card>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-4 lg:grid-cols-8">
          <Info label="Issued" value={fmtDate(inv.issueDate)} />
          <Info label="Due" value={fmtDate(inv.dueDate)} />
          <Info label="Subtotal" value={fmtMoney(inv.subtotal)} />
          <Info label={`VAT ${inv.taxRate}%`} value={fmtMoney(inv.taxAmount)} />
          <Info label="Total" value={fmtMoney(inv.total)} />
          <Info label="Paid" value={fmtMoney(inv.paidAmount)} />
          <Info label="Credited" value={fmtMoney(inv.creditedAmount)} />
          <Info label="Balance due" value={fmtMoney(inv.balance)} />
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
            accessorKey: "creditedQuantity",
            header: "Credited",
            meta: { align: "right" },
            cell: ({ getValue }) => (getValue() ? fmtQty(getValue()) : ""),
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
      <div className="grid gap-4 lg:grid-cols-2">
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
        <Card>
          <CardHeader>
            <CardTitle>Credit notes</CardTitle>
          </CardHeader>
          <CardContent>
            {creditNotes.length === 0 ? (
              <p className="text-sm text-muted-foreground">None</p>
            ) : (
              <div className="space-y-1 text-sm">
                {creditNotes.map((n) => (
                  <div key={n.id} className="flex justify-between gap-4">
                    <span className="font-medium">{n.number}</span>
                    <span>{fmtDate(n.issueDate)}</span>
                    <span className="truncate text-muted-foreground">{n.reason}</span>
                    <span className="tabular-nums">{fmtMoney(n.total)}</span>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
      {paying && (
        <PaymentDialog
          title="Record customer payment"
          description="Booked as cash against receivables."
          balance={inv.balance}
          open
          onOpenChange={setPaying}
          busy={pay.isPending}
          onSubmit={(p) => pay.mutate(p)}
        />
      )}
      {crediting && <CreditNoteDialog detail={data} onClose={() => setCrediting(false)} />}
    </div>
  );
}

function CreditNoteDialog({ detail, onClose }: { detail: InvoiceDetail; onClose: () => void }) {
  const { invoice, lines } = detail;
  const [qty, setQty] = useState<Record<number, number>>({});
  const [reason, setReason] = useState("");
  const [date, setDate] = useState(today());
  const subtotal = lines.reduce((s, l) => s + (qty[l.position] ?? 0) * l.unitPrice, 0);
  const total = subtotal + Math.round(subtotal * invoice.taxRate) / 100;
  const credit = useAction(
    () =>
      post(`/invoices/${invoice.id}/credit-notes`, {
        date,
        reason,
        lines: Object.entries(qty)
          .filter(([, q]) => q > 0)
          .map(([position, quantity]) => ({ position: Number(position), quantity })),
      }),
    { success: "Credit note issued", invalidate, onSuccess: onClose },
  );
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Credit note for {invoice.number}</DialogTitle>
          <DialogDescription>
            Credits invoiced quantities (price correction, rejected goods) up to the unpaid balance
            of {fmtMoney(invoice.balance)}. Booked against revenue and VAT.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          {lines.map((l) => {
            const left = l.quantity - l.creditedQuantity;
            return (
              <div key={l.position} className="flex items-center gap-3">
                <div className="min-w-0 flex-1 text-sm">
                  <div className="truncate font-medium">{l.productName}</div>
                  <div className="text-xs text-muted-foreground">
                    {fmtQty(left)} creditable · {fmtMoney(l.unitPrice)} each
                  </div>
                </div>
                <Input
                  type="number"
                  step="any"
                  min={0}
                  max={left}
                  disabled={left <= 0}
                  className="w-28 text-right"
                  value={qty[l.position] ?? ""}
                  onChange={(e) => setQty({ ...qty, [l.position]: Number(e.target.value) })}
                />
              </div>
            );
          })}
          <div className="grid gap-3 sm:grid-cols-[10rem_1fr]">
            <div>
              <Label className="mb-1.5">Date</Label>
              <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div>
              <Label className="mb-1.5">Reason *</Label>
              <Input value={reason} onChange={(e) => setReason(e.target.value)} />
            </div>
          </div>
          <div className="text-right text-sm">
            Credit incl. VAT: <span className="font-semibold tabular-nums">{fmtMoney(total)}</span>
          </div>
        </div>
        <DialogFooter>
          <Button
            disabled={credit.isPending || !reason || subtotal <= 0}
            onClick={() => credit.mutate(undefined)}
          >
            Issue credit note
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
