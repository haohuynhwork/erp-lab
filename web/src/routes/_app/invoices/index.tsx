import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { StatusBadge } from "@/components/status-badge";
import { StatusTabs } from "@/components/status-tabs";
import { Input } from "@/components/ui/input";
import { api, type Page, qs } from "@/lib/api";
import { fmtDate, fmtMoney } from "@/lib/format";
import type { Invoice } from "./-lib/types";

export const Route = createFileRoute("/_app/invoices/")({ component: Invoices });

function Invoices() {
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("all");
  const [page, setPage] = useState(0);
  const params = {
    q,
    status: status === "all" || status === "OVERDUE" ? undefined : status,
    overdueOnly: status === "OVERDUE" || undefined,
    page,
    size: 20,
  };
  const list = useQuery({
    queryKey: ["invoices", params],
    queryFn: () => api<Page<Invoice>>(`/invoices${qs(params)}`),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader
        title="Invoices"
        description="Customer invoices and payments. Create invoices from a sales order."
      />
      <div className="mb-3 flex flex-wrap gap-2">
        <Input
          placeholder="Search number, customer, order…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          className="max-w-xs"
        />
        <StatusTabs
          value={status}
          onChange={(s) => {
            setStatus(s);
            setPage(0);
          }}
          statuses={["ISSUED", "PARTIALLY_PAID", "PAID", "OVERDUE"]}
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 20, total: list.data.total, onPage: setPage }}
        onRowClick={(i) => navigate({ to: "/invoices/$id", params: { id: String(i.id) } })}
        columns={[
          { accessorKey: "number", header: "Number" },
          { accessorKey: "issueDate", header: "Date", cell: ({ getValue }) => fmtDate(getValue()) },
          { accessorKey: "customerName", header: "Customer" },
          { accessorKey: "salesOrderNumber", header: "Order" },
          {
            accessorKey: "dueDate",
            header: "Due",
            cell: ({ row }) => (
              <span className={row.original.overdue ? "font-medium text-destructive" : ""}>
                {fmtDate(row.original.dueDate)}
              </span>
            ),
          },
          {
            accessorKey: "status",
            header: "Status",
            cell: ({ row }) => (
              <StatusBadge status={row.original.overdue ? "OVERDUE" : row.original.status} />
            ),
          },
          {
            accessorKey: "total",
            header: "Total",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            accessorKey: "balance",
            header: "Balance",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
        ]}
      />
    </div>
  );
}
