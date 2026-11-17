import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Banknote } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { EntityForm } from "@/components/entity-form";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { api, type Page, post, qs } from "@/lib/api";
import { fmtDate, fmtMoney, today } from "@/lib/format";
import { useCan } from "@/lib/session";
import { useAction } from "@/lib/use-action";

export const Route = createFileRoute("/_app/supplier-payments")({ component: SupplierPayments });

type Payment = {
  id: number;
  number: string;
  supplierName: string;
  paymentDate: string;
  amount: number;
  method: string;
  reference: string | null;
};

function SupplierPayments() {
  const can = useCan();
  const [page, setPage] = useState(0);
  const [paying, setPaying] = useState(false);
  const params = { page, size: 50 };
  const list = useQuery({
    queryKey: ["supplier-payments", params],
    queryFn: () => api<Page<Payment>>(`/accounting/supplier-payments${qs(params)}`),
    placeholderData: keepPreviousData,
  });
  const pay = useAction((body: unknown) => post("/accounting/supplier-payments", body), {
    success: "Payment recorded",
    invalidate: [["supplier-payments"], ["balances"], ["dashboard"]],
    onSuccess: () => setPaying(false),
  });

  return (
    <div>
      <PageHeader
        title="Supplier payments"
        description="Pay what we owe suppliers (Dr payables / Cr cash)"
        actions={
          can("accounting:write") && (
            <Button onClick={() => setPaying(true)}>
              <Banknote /> Pay supplier
            </Button>
          )
        }
      />
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        columns={[
          { accessorKey: "number", header: "Number" },
          {
            accessorKey: "paymentDate",
            header: "Date",
            cell: ({ getValue }) => fmtDate(getValue()),
          },
          { accessorKey: "supplierName", header: "Supplier" },
          { accessorKey: "method", header: "Method" },
          { accessorKey: "reference", header: "Reference" },
          {
            accessorKey: "amount",
            header: "Amount",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
        ]}
      />
      <Dialog open={paying} onOpenChange={setPaying}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Pay supplier</DialogTitle>
            <DialogDescription>Up to the open payable balance of the supplier.</DialogDescription>
          </DialogHeader>
          <EntityForm
            fields={[
              {
                name: "supplierId",
                label: "Supplier",
                type: "entity",
                endpoint: "/partners?role=supplier",
                optionLabel: (p) => `${p.code} · ${p.name}`,
                required: true,
                wide: true,
              },
              { name: "amount", label: "Amount", type: "number", required: true },
              { name: "date", label: "Date", type: "date" },
              {
                name: "method",
                label: "Method",
                type: "select",
                options: [
                  { value: "BANK", label: "Bank transfer" },
                  { value: "CASH", label: "Cash" },
                ],
                required: true,
              },
              { name: "reference", label: "Reference" },
            ]}
            defaultValues={{
              supplierId: null,
              amount: null,
              date: today(),
              method: "BANK",
              reference: "",
            }}
            editing={false}
            submitting={pay.isPending}
            onSubmit={(v) => pay.mutate(v)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
