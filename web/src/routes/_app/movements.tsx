import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { DataTable } from "@/components/data-table";
import { EntitySelect } from "@/components/entity-select";
import { PageHeader } from "@/components/page-header";
import { StatusTabs } from "@/components/status-tabs";
import { api, type Page, qs } from "@/lib/api";
import { fmtDateTime, fmtMoney, fmtQty } from "@/lib/format";

export const Route = createFileRoute("/_app/movements")({ component: Movements });

type Movement = {
  id: number;
  movedAt: string;
  type: string;
  sku: string;
  productName: string;
  warehouseCode: string;
  quantity: number;
  value: number;
  refType: string | null;
  refNumber: string | null;
  note: string | null;
  movedBy: string;
};

function Movements() {
  const [productId, setProductId] = useState<number | null>(null);
  const [type, setType] = useState("all");
  const [page, setPage] = useState(0);
  const params = { productId, type: type === "all" ? undefined : type, page, size: 50 };
  const list = useQuery({
    queryKey: ["movements", params],
    queryFn: () => api<Page<Movement>>(`/inventory/movements${qs(params)}`),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader title="Stock movements" description="Every change to stock, newest first" />
      <div className="mb-3 flex flex-wrap gap-3">
        <EntitySelect<{ id: number; sku: string; name: string }>
          endpoint="/products"
          label={(p) => `${p.sku} · ${p.name}`}
          value={productId}
          onChange={(v) => {
            setProductId(v);
            setPage(0);
          }}
          placeholder="All products"
          allowEmpty
          className="w-72"
        />
        <StatusTabs
          value={type}
          onChange={(t) => {
            setType(t);
            setPage(0);
          }}
          statuses={["RECEIPT", "ISSUE", "TRANSFER_IN", "TRANSFER_OUT", "ADJUSTMENT"]}
        />
      </div>
      <DataTable
        data={list.data?.items}
        loading={list.isLoading}
        paging={list.data && { page, size: 50, total: list.data.total, onPage: setPage }}
        columns={[
          {
            accessorKey: "movedAt",
            header: "When",
            cell: ({ getValue }) => fmtDateTime(getValue()),
          },
          {
            accessorKey: "type",
            header: "Type",
            cell: ({ getValue }) => getValue().replace("_", " ").toLowerCase(),
          },
          { accessorKey: "sku", header: "SKU" },
          { accessorKey: "productName", header: "Product" },
          { accessorKey: "warehouseCode", header: "Warehouse" },
          {
            accessorKey: "quantity",
            header: "Quantity",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtQty(getValue()),
          },
          {
            accessorKey: "value",
            header: "Value",
            meta: { align: "right" },
            cell: ({ getValue }) => fmtMoney(getValue()),
          },
          {
            id: "ref",
            header: "Reference",
            cell: ({ row }) => row.original.refNumber ?? row.original.refType?.toLowerCase(),
          },
          { accessorKey: "note", header: "Note" },
          { accessorKey: "movedBy", header: "By" },
        ]}
      />
    </div>
  );
}
