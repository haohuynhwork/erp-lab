import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Input } from "@/components/ui/input";
import { api, type Page, qs } from "@/lib/api";
import { fmtDate, fmtMoney } from "@/lib/format";
import type { Schemas } from "@/lib/schemas";

export const Route = createFileRoute("/_app/credit-notes")({ component: CreditNotes });

function CreditNotes() {
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const params = { q, page, size: 50 };
  const list = useQuery({
    queryKey: ["invoices", "credit-notes", params],
    queryFn: () => api<Page<Schemas["CreditNoteView"]>>(`/invoices/credit-notes${qs(params)}`),
    placeholderData: keepPreviousData,
  });
  return (
    <div>
      <PageHeader
        title="Credit notes"
        description="Issued from an invoice; they reduce what the customer owes"
      />
      <div className="mb-3">
        <Input
          placeholder="Search number, invoice, customer…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="max-w-xs"
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        onRowClick={(n) => navigate({ to: "/invoices/$id", params: { id: String(n.invoiceId) } })}
        columns={[
          { accessorKey: "number", header: "Number" },
          { accessorKey: "issueDate", header: "Date", cell: ({ getValue }) => fmtDate(getValue()) },
          { accessorKey: "customerName", header: "Customer" },
          { accessorKey: "invoiceNumber", header: "Invoice" },
          { accessorKey: "reason", header: "Reason" },
          {
            accessorKey: "total",
            header: "Total",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
        ]}
      />
    </div>
  );
}
