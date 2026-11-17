import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft, Banknote, Download } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { Info } from "@/components/info";
import { PageHeader } from "@/components/page-header";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api, download, post } from "@/lib/api";
import { fmtDate, fmtMoney, fmtQty, today } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";
import type { Invoice } from "./-lib/types";

export const Route = createFileRoute("/_app/invoices/$id")({ component: InvoicePage });

type Detail = {
  invoice: Invoice;
  lines: {
    productId: number;
    sku: string;
    productName: string;
    unitCode: string;
    quantity: number;
    unitPrice: number;
    amount: number;
  }[];
  payments: {
    id: number;
    paymentDate: string;
    amount: number;
    method: string;
    reference: string | null;
  }[];
};

function InvoicePage() {
  const { id } = Route.useParams();
  const can = useCan();
  const [paying, setPaying] = useState(false);
  const { data } = useQuery({
    queryKey: ["invoices", id],
    queryFn: () => api<Detail>(`/invoices/${id}`),
  });

  if (!data) return null;
  const { invoice: inv, lines, payments } = data;
  const canPay = (can("sales:write") || can("accounting:write")) && inv.balance > 0;

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
            {canPay && (
              <Button onClick={() => setPaying(true)}>
                <Banknote /> Record payment
              </Button>
            )}
          </>
        }
      />
      <Card>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-3 lg:grid-cols-6">
          <Info label="Issued" value={fmtDate(inv.issueDate)} />
          <Info label="Due" value={fmtDate(inv.dueDate)} />
          <Info label="Subtotal" value={fmtMoney(inv.subtotal)} />
          <Info label={`VAT ${inv.taxRate}%`} value={fmtMoney(inv.taxAmount)} />
          <Info label="Total" value={fmtMoney(inv.total)} />
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
      <PaymentDialog invoice={inv} open={paying} onOpenChange={setPaying} />
    </div>
  );
}

function PaymentDialog({
  invoice,
  open,
  onOpenChange,
}: {
  invoice: Invoice;
  open: boolean;
  onOpenChange: (o: boolean) => void;
}) {
  const [amount, setAmount] = useState<number>(invoice.balance);
  const [date, setDate] = useState(today());
  const [method, setMethod] = useState("BANK");
  const [reference, setReference] = useState("");
  const pay = useAction(
    () =>
      post(`/invoices/${invoice.id}/payments`, {
        amount,
        date,
        method,
        reference: reference || null,
      }),
    {
      success: "Payment recorded",
      invalidate: [["invoices"], ["sales-orders"], ["dashboard"]],
      onSuccess: () => onOpenChange(false),
    },
  );
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Record payment</DialogTitle>
          <DialogDescription>
            Open balance {fmtMoney(invoice.balance)}. Booked as cash against receivables.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label className="mb-1.5">Amount</Label>
            <Input
              type="number"
              step="any"
              min={0}
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
            />
          </div>
          <div>
            <Label className="mb-1.5">Date</Label>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div>
            <Label className="mb-1.5">Method</Label>
            <Select value={method} onValueChange={setMethod}>
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="BANK">Bank transfer</SelectItem>
                <SelectItem value="CASH">Cash</SelectItem>
                <SelectItem value="CARD">Card</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label className="mb-1.5">Reference</Label>
            <Input value={reference} onChange={(e) => setReference(e.target.value)} />
          </div>
        </div>
        <DialogFooter>
          <Button onClick={() => pay.mutate(undefined)} disabled={pay.isPending || amount <= 0}>
            {pay.isPending ? "Saving…" : "Record payment"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
