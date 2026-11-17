import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { StatusBadge } from "@/components/status-badge";
import { StatusTabs } from "@/components/status-tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, type Page, qs } from "@/lib/api";
import { fmtDate, fmtMoney } from "@/lib/format";
import { useCan } from "@/lib/session";
import type { PurchaseOrder } from "./-lib/types";

export const Route = createFileRoute("/_app/purchase-orders/")({ component: PurchaseOrders });

function PurchaseOrders() {
  const can = useCan();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("all");
  const [page, setPage] = useState(0);
  const params = { q, status: status === "all" ? undefined : status, page, size: 20 };
  const list = useQuery({
    queryKey: ["purchase-orders", params],
    queryFn: () => api<Page<PurchaseOrder>>(`/purchase-orders${qs(params)}`),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader
        title="Purchase orders"
        description="Order from suppliers, then receive the goods into stock"
        actions={
          can("purchasing:write") && (
            <Button asChild>
              <Link to="/purchase-orders/new">
                <Plus /> New order
              </Link>
            </Button>
          )
        }
      />
      <div className="mb-3 flex flex-wrap gap-2">
        <Input
          placeholder="Search number or supplier…"
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
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 20, total: list.data.total, onPage: setPage }}
        onRowClick={(o) => navigate({ to: "/purchase-orders/$id", params: { id: String(o.id) } })}
        columns={[
          { accessorKey: "number", header: "Number" },
          { accessorKey: "orderDate", header: "Date", cell: ({ getValue }) => fmtDate(getValue()) },
          { accessorKey: "supplierName", header: "Supplier" },
          { accessorKey: "warehouseCode", header: "Warehouse" },
          {
            accessorKey: "expectedDate",
            header: "Expected",
            cell: ({ getValue }) => fmtDate(getValue()),
          },
          {
            accessorKey: "status",
            header: "Status",
            cell: ({ getValue }) => <StatusBadge status={getValue()} />,
          },
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
